package com.stripe.android.paymentsheet.state

import com.stripe.android.core.Logger
import com.stripe.android.core.injection.IOContext
import com.stripe.android.googlepaylauncher.GooglePayEnvironment
import com.stripe.android.googlepaylauncher.GooglePayRepository
import com.stripe.android.link.LinkPaymentLauncher
import com.stripe.android.link.LinkPaymentLauncher.Companion.supportedFundingSources
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethod.Type.Link
import com.stripe.android.model.StripeIntent
import com.stripe.android.payments.core.injection.APP_NAME
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PrefsRepository
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.addresselement.toIdentifierMap
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.ClientSecret
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.SavedSelection
import com.stripe.android.paymentsheet.model.StripeIntentValidator
import com.stripe.android.paymentsheet.model.getPMsToAdd
import com.stripe.android.paymentsheet.model.getSupportedSavedCustomerPMs
import com.stripe.android.paymentsheet.repositories.CustomerRepository
import com.stripe.android.paymentsheet.repositories.StripeIntentRepository
import com.stripe.android.ui.core.Amount
import com.stripe.android.ui.core.forms.resources.LpmRepository
import com.stripe.android.ui.core.forms.resources.LpmRepository.ServerSpecState
import com.stripe.android.ui.core.forms.resources.ResourceRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

/**
 * Loads the information necessary to display [PaymentSheet], either directly or via
 * [PaymentSheet.FlowController].
 */
internal interface PaymentSheetLoader {

    suspend fun load(
        clientSecret: ClientSecret,
        paymentSheetConfiguration: PaymentSheet.Configuration? = null
    ): Result

    sealed class Result {
        data class Success(val state: PaymentSheetState.Full) : Result()
        class Failure(val throwable: Throwable) : Result()
    }
}

@Singleton
internal class DefaultPaymentSheetLoader @Inject constructor(
    @Named(APP_NAME) private val appName: String,
    private val prefsRepositoryFactory: @JvmSuppressWildcards (PaymentSheet.CustomerConfiguration?) -> PrefsRepository,
    private val googlePayRepositoryFactory: @JvmSuppressWildcards (GooglePayEnvironment) -> GooglePayRepository,
    private val stripeIntentRepository: StripeIntentRepository,
    private val stripeIntentValidator: StripeIntentValidator,
    private val customerRepository: CustomerRepository,
    private val lpmResourceRepository: ResourceRepository<LpmRepository>,
    private val logger: Logger,
    private val eventReporter: EventReporter,
    @IOContext private val workContext: CoroutineContext,
    private val accountStatusProvider: LinkAccountStatusProvider,
) : PaymentSheetLoader {

    override suspend fun load(
        clientSecret: ClientSecret,
        paymentSheetConfiguration: PaymentSheet.Configuration?
    ) = withContext(workContext) {
        val isGooglePayReady = isGooglePayReady(paymentSheetConfiguration)
        runCatching {
            retrieveStripeIntent(clientSecret)
        }.fold(
            onSuccess = { stripeIntent ->
                warnUnactivatedIfNeeded(stripeIntent.unactivatedPaymentMethods)
                create(
                    clientSecret = clientSecret,
                    stripeIntent = stripeIntent,
                    customerConfig = paymentSheetConfiguration?.customer,
                    config = paymentSheetConfiguration,
                    isGooglePayReady = isGooglePayReady,
                )
            },
            onFailure = {
                logger.error("Failure initializing FlowController", it)
                PaymentSheetLoader.Result.Failure(it)
            }
        )
    }

    private suspend fun isGooglePayReady(
        paymentSheetConfiguration: PaymentSheet.Configuration?
    ): Boolean {
        return paymentSheetConfiguration?.googlePay?.environment?.let { environment ->
            googlePayRepositoryFactory(
                when (environment) {
                    PaymentSheet.GooglePayConfiguration.Environment.Production ->
                        GooglePayEnvironment.Production
                    PaymentSheet.GooglePayConfiguration.Environment.Test ->
                        GooglePayEnvironment.Test
                }
            )
        }?.isReady()?.first() ?: false
    }

    @Suppress("LongMethod")
    private suspend fun create(
        clientSecret: ClientSecret,
        stripeIntent: StripeIntent,
        customerConfig: PaymentSheet.CustomerConfiguration?,
        config: PaymentSheet.Configuration?,
        isGooglePayReady: Boolean,
    ): PaymentSheetLoader.Result = coroutineScope {
        val prefsRepository = prefsRepositoryFactory(customerConfig)

        val isLinkAvailable = stripeIntent.paymentMethodTypes.contains(Link.code) &&
            stripeIntent.linkFundingSources.intersect(supportedFundingSources).isNotEmpty()

        val paymentMethods = async {
            if (customerConfig != null) {
                retrieveCustomerPaymentMethods(
                    stripeIntent,
                    config,
                    customerConfig
                )
            } else {
                emptyList()
            }
        }

        val savedSelection = async {
            retrieveSavedPaymentSelection(
                prefsRepository,
                isGooglePayReady,
                isLinkAvailable,
                paymentMethods.await()
            )
        }

        val linkState = async {
            if (isLinkAvailable) {
                loadLinkState(config, stripeIntent)
            } else {
                null
            }
        }

        val supportedPaymentMethodTypes = async {
            lpmResourceRepository.waitUntilLoaded()
            getPMsToAdd(
                stripeIntent = stripeIntent,
                config = config,
                lpmRepository = lpmResourceRepository.getRepository(),
            ).map {
                it.code
            }
        }

        return@coroutineScope try {
            val amount = if (stripeIntent is PaymentIntent) {
                Amount(
                    value = requireNotNull(stripeIntent.amount),
                    currencyCode = requireNotNull(stripeIntent.currency),
                )
            } else {
                null
            }

            val supportedTypes = supportedPaymentMethodTypes.await()
            if (supportedTypes.isEmpty()) {
                val requested = stripeIntent.paymentMethodTypes.joinToString(", ")
                val supported = lpmResourceRepository.getRepository().values().joinToString(", ")

                PaymentSheetLoader.Result.Failure(
                    throwable = IllegalArgumentException(
                        "None of the requested payment methods ($requested) match " +
                            "the supported payment types ($supported)"
                    )
                )
            } else {
                PaymentSheetLoader.Result.Success(
                    PaymentSheetState.Full(
                        config = config,
                        clientSecret = clientSecret,
                        stripeIntent = stripeIntent,
                        customerPaymentMethods = paymentMethods.await(),
                        supportedPaymentMethodTypes = supportedTypes,
                        savedSelection = savedSelection.await(),
                        isGooglePayReady = isGooglePayReady,
                        linkState = linkState.await(),
                        newPaymentSelection = null,
                        selection = null,
                        amount = amount,
                        isEditing = false,
                        isProcessing = false,
                        notesText = null,
                        primaryButtonUiState = null,
                    )
                )
            }
        } catch (e: IllegalArgumentException) {
            // TODO Throw as IllegalStateException instead?
            PaymentSheetLoader.Result.Failure(e)
        }
    }

    private suspend fun retrieveCustomerPaymentMethods(
        stripeIntent: StripeIntent,
        config: PaymentSheet.Configuration?,
        customerConfig: PaymentSheet.CustomerConfiguration
    ): List<PaymentMethod> {
        val paymentMethodTypes = getSupportedSavedCustomerPMs(
            stripeIntent,
            config,
            lpmResourceRepository.getRepository()
        ).mapNotNull {
            // The SDK is only able to parse customer LPMs
            // that are hard coded in the SDK.
            PaymentMethod.Type.fromCode(it.code)
        }

        return customerRepository.getPaymentMethods(
            customerConfig,
            paymentMethodTypes
        ).filter { paymentMethod ->
            paymentMethod.hasExpectedDetails()
        }
    }

    private suspend fun retrieveSavedPaymentSelection(
        prefsRepository: PrefsRepository,
        isGooglePayReady: Boolean,
        isLinkReady: Boolean,
        paymentMethods: List<PaymentMethod>
    ): SavedSelection {
        val savedSelection = prefsRepository.getSavedSelection(isGooglePayReady, isLinkReady)
        if (savedSelection != SavedSelection.None) {
            return savedSelection
        }

        // No saved selection has been set yet, so we'll initialize it with a default
        // value based on which payment methods are available.
        val paymentSelection = determineDefaultPaymentSelection(
            isGooglePayReady,
            isLinkReady,
            paymentMethods
        )

        prefsRepository.savePaymentSelection(paymentSelection)

        return prefsRepository.getSavedSelection(isGooglePayReady, isLinkReady)
    }

    private fun determineDefaultPaymentSelection(
        isGooglePayReady: Boolean,
        isLinkReady: Boolean,
        paymentMethods: List<PaymentMethod>
    ): PaymentSelection? {
        return when {
            paymentMethods.isNotEmpty() -> PaymentSelection.Saved(paymentMethods.first())
            isLinkReady -> PaymentSelection.Link
            isGooglePayReady -> PaymentSelection.GooglePay
            else -> null
        }
    }

    private suspend fun retrieveStripeIntent(
        clientSecret: ClientSecret
    ): StripeIntent {
        val paymentMethodPreference = stripeIntentRepository.get(clientSecret)
        val lpmRepository = lpmResourceRepository.getRepository()

        lpmRepository.update(
            expectedLpms = paymentMethodPreference.intent.paymentMethodTypes,
            serverLpmSpecs = paymentMethodPreference.formUI,
        )

        if (lpmRepository.serverSpecLoadingState is ServerSpecState.ServerNotParsed) {
            eventReporter.onLpmSpecFailure()
        }

        return stripeIntentValidator.requireValid(paymentMethodPreference.intent)
    }

    private suspend fun loadLinkState(
        config: PaymentSheet.Configuration?,
        stripeIntent: StripeIntent,
    ): LinkState {
        val linkConfig = createLinkConfiguration(config, stripeIntent)

        val loginState = when (accountStatusProvider(linkConfig)) {
            AccountStatus.Verified -> LinkState.LoginState.LoggedIn
            AccountStatus.NeedsVerification,
            AccountStatus.VerificationStarted -> LinkState.LoginState.NeedsVerification
            AccountStatus.SignedOut,
            AccountStatus.Error -> LinkState.LoginState.LoggedOut
        }

        return LinkState(
            configuration = linkConfig,
            loginState = loginState,
        )
    }

    private suspend fun createLinkConfiguration(
        config: PaymentSheet.Configuration?,
        stripeIntent: StripeIntent,
    ): LinkPaymentLauncher.Configuration {
        val shippingDetails: AddressDetails? = config?.shippingDetails

        val customerPhone = if (shippingDetails?.isCheckboxSelected == true) {
            shippingDetails.phoneNumber
        } else {
            config?.defaultBillingDetails?.phone
        }

        val shippingAddress = if (shippingDetails?.isCheckboxSelected == true) {
            shippingDetails.toIdentifierMap(config.defaultBillingDetails)
        } else {
            null
        }

        val customerEmail = config?.defaultBillingDetails?.email ?: config?.customer?.let {
            customerRepository.retrieveCustomer(
                it.id,
                it.ephemeralKeySecret
            )
        }?.email

        val merchantName = config?.merchantDisplayName ?: appName

        return LinkPaymentLauncher.Configuration(
            stripeIntent = stripeIntent,
            merchantName = merchantName,
            customerEmail = customerEmail,
            customerPhone = customerPhone,
            customerName = config?.defaultBillingDetails?.name,
            customerBillingCountryCode = config?.defaultBillingDetails?.address?.country,
            shippingValues = shippingAddress
        )
    }

    private fun warnUnactivatedIfNeeded(unactivatedPaymentMethodTypes: List<String>) {
        if (unactivatedPaymentMethodTypes.isEmpty()) {
            return
        }

        val message = "[Stripe SDK] Warning: Your Intent contains the following payment method " +
            "types which are activated for test mode but not activated for " +
            "live mode: $unactivatedPaymentMethodTypes. These payment method types will not be " +
            "displayed in live mode until they are activated. To activate these payment method " +
            "types visit your Stripe dashboard." +
            "More information: https://support.stripe.com/questions/activate-a-new-payment-method"

        logger.warning(message)
    }
}
