package com.stripe.android.paymentsheet.state

import com.stripe.android.core.Logger
import com.stripe.android.core.injection.IOContext
import com.stripe.android.googlepaylauncher.GooglePayEnvironment
import com.stripe.android.googlepaylauncher.GooglePayRepository
import com.stripe.android.link.LinkPaymentLauncher
import com.stripe.android.link.LinkPaymentLauncher.Companion.supportedFundingSources
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethod.Type.Link
import com.stripe.android.model.StripeIntent
import com.stripe.android.payments.core.injection.APP_NAME
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PrefsRepository
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.addresselement.toIdentifierMap
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.SavedSelection
import com.stripe.android.paymentsheet.model.StripeIntentValidator
import com.stripe.android.paymentsheet.model.getSupportedSavedCustomerPMs
import com.stripe.android.paymentsheet.repositories.CustomerRepository
import com.stripe.android.paymentsheet.repositories.ElementsSessionRepository
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
        initializationMode: PaymentSheet.InitializationMode,
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
    private val elementsSessionRepository: ElementsSessionRepository,
    private val stripeIntentValidator: StripeIntentValidator,
    private val customerRepository: CustomerRepository,
    private val lpmResourceRepository: ResourceRepository<LpmRepository>,
    private val logger: Logger,
    private val eventReporter: EventReporter,
    @IOContext private val workContext: CoroutineContext,
    private val accountStatusProvider: LinkAccountStatusProvider,
) : PaymentSheetLoader {

    override suspend fun load(
        initializationMode: PaymentSheet.InitializationMode,
        paymentSheetConfiguration: PaymentSheet.Configuration?
    ): PaymentSheetLoader.Result = withContext(workContext) {
        val isGooglePayReady = isGooglePayReady(paymentSheetConfiguration)

        runCatching {
            retrieveElementsSession(initializationMode, paymentSheetConfiguration)
        }.fold(
            onSuccess = { stripeIntent ->
                create(
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

    private suspend fun create(
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

        return@coroutineScope PaymentSheetLoader.Result.Success(
            PaymentSheetState.Full(
                config = config,
                stripeIntent = stripeIntent,
                customerPaymentMethods = paymentMethods.await(),
                savedSelection = savedSelection.await(),
                isGooglePayReady = isGooglePayReady,
                linkState = linkState.await(),
                newPaymentSelection = null,
            )
        )
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
            paymentMethod.hasExpectedDetails() &&
                // PayPal isn't supported yet as a saved payment method (backend limitation).
                paymentMethod.type != PaymentMethod.Type.PayPal
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

    private suspend fun retrieveElementsSession(
        initializationMode: PaymentSheet.InitializationMode,
        configuration: PaymentSheet.Configuration?,
    ): StripeIntent {
        val elementsSession = elementsSessionRepository.get(
            initializationMode = initializationMode,
            configuration = configuration,
        )
        val lpmRepository = lpmResourceRepository.getRepository()

        lpmRepository.update(
            stripeIntent = elementsSession.stripeIntent,
            serverLpmSpecs = elementsSession.paymentMethodSpecs,
        )

        if (lpmRepository.serverSpecLoadingState is ServerSpecState.ServerNotParsed) {
            eventReporter.onLpmSpecFailure()
        }

        return stripeIntentValidator.requireValid(elementsSession.stripeIntent)
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
}
