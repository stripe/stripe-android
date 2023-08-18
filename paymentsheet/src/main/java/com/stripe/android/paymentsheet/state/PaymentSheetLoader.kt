package com.stripe.android.paymentsheet.state

import com.stripe.android.core.Logger
import com.stripe.android.core.injection.IOContext
import com.stripe.android.googlepaylauncher.GooglePayEnvironment
import com.stripe.android.googlepaylauncher.GooglePayRepository
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.LinkPaymentLauncher.Companion.supportedFundingSources
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethod.Type.Link
import com.stripe.android.model.StripeIntent
import com.stripe.android.payments.core.injection.APP_NAME
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always
import com.stripe.android.paymentsheet.PaymentSheet.InitializationMode.DeferredIntent
import com.stripe.android.paymentsheet.PrefsRepository
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.addresselement.toIdentifierMap
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.SavedSelection
import com.stripe.android.paymentsheet.model.getPMsToAdd
import com.stripe.android.paymentsheet.model.getSupportedSavedCustomerPMs
import com.stripe.android.paymentsheet.model.requireValidOrThrow
import com.stripe.android.paymentsheet.repositories.CustomerRepository
import com.stripe.android.paymentsheet.repositories.ElementsSessionRepository
import com.stripe.android.ui.core.CardBillingDetailsCollectionConfiguration
import com.stripe.android.ui.core.forms.resources.LpmRepository
import com.stripe.android.ui.core.forms.resources.LpmRepository.ServerSpecState
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
    ): Result<PaymentSheetState.Full>
}

@Singleton
internal class DefaultPaymentSheetLoader @Inject constructor(
    @Named(APP_NAME) private val appName: String,
    private val prefsRepositoryFactory: @JvmSuppressWildcards (PaymentSheet.CustomerConfiguration?) -> PrefsRepository,
    private val googlePayRepositoryFactory: @JvmSuppressWildcards (GooglePayEnvironment) -> GooglePayRepository,
    private val elementsSessionRepository: ElementsSessionRepository,
    private val customerRepository: CustomerRepository,
    private val lpmRepository: LpmRepository,
    private val logger: Logger,
    private val eventReporter: EventReporter,
    @IOContext private val workContext: CoroutineContext,
    private val accountStatusProvider: LinkAccountStatusProvider,
) : PaymentSheetLoader {

    override suspend fun load(
        initializationMode: PaymentSheet.InitializationMode,
        paymentSheetConfiguration: PaymentSheet.Configuration?
    ): Result<PaymentSheetState.Full> = withContext(workContext) {
        val isGooglePayReady = isGooglePayReady(paymentSheetConfiguration)
        val isDecoupling = initializationMode is DeferredIntent

        eventReporter.onLoadStarted(isDecoupling = isDecoupling)

        retrieveElementsSession(
            initializationMode = initializationMode,
            configuration = paymentSheetConfiguration,
        ).mapCatching { elementsSession ->
            create(
                elementsSession = elementsSession,
                config = paymentSheetConfiguration,
                isGooglePayReady = isGooglePayReady,
            )
        }.also {
            reportLoadResult(loaderResult = it, isDecoupling = isDecoupling)
        }
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
        elementsSession: ElementsSession,
        config: PaymentSheet.Configuration?,
        isGooglePayReady: Boolean,
    ): PaymentSheetState.Full = coroutineScope {
        val customerConfig = config?.customer
        val prefsRepository = prefsRepositoryFactory(customerConfig)

        val stripeIntent = elementsSession.stripeIntent
        val merchantCountry = elementsSession.merchantCountry

        val isLinkAvailable = stripeIntent.paymentMethodTypes.contains(Link.code) &&
            stripeIntent.linkFundingSources.intersect(supportedFundingSources).isNotEmpty()

        val savedSelection = async {
            prefsRepository.getSavedSelection(
                isGooglePayAvailable = isGooglePayReady,
                isLinkAvailable = isLinkAvailable,
            )
        }

        val paymentMethods = async {
            if (customerConfig != null) {
                retrieveCustomerPaymentMethods(
                    stripeIntent = stripeIntent,
                    config = config,
                    customerConfig = customerConfig,
                )
            } else {
                emptyList()
            }
        }

        val sortedPaymentMethods = async {
            paymentMethods.await().withLastUsedPaymentMethodFirst(
                savedSelection = savedSelection.await(),
            )
        }

        val initialPaymentSelection = async {
            val desiredSelection = when (val selection = savedSelection.await()) {
                is SavedSelection.GooglePay -> {
                    PaymentSelection.GooglePay
                }
                is SavedSelection.Link -> {
                    PaymentSelection.Link
                }
                is SavedSelection.PaymentMethod -> {
                    paymentMethods.await().find { it.id == selection.id }?.toPaymentSelection()
                }
                is SavedSelection.None -> {
                    null
                }
            }

            desiredSelection ?: sortedPaymentMethods.await().firstOrNull()?.toPaymentSelection()
        }

        val linkState = async {
            if (isLinkAvailable) {
                loadLinkState(config, stripeIntent, merchantCountry)
            } else {
                null
            }
        }

        warnUnactivatedIfNeeded(stripeIntent)

        if (supportsIntent(stripeIntent, config)) {
            PaymentSheetState.Full(
                config = config,
                stripeIntent = stripeIntent,
                customerPaymentMethods = sortedPaymentMethods.await(),
                isGooglePayReady = isGooglePayReady,
                linkState = linkState.await(),
                paymentSelection = initialPaymentSelection.await(),
            )
        } else {
            val requested = stripeIntent.paymentMethodTypes.joinToString(separator = ", ")
            val supported = lpmRepository.values().joinToString(separator = ", ") { it.code }

            throw PaymentSheetLoadingException.NoPaymentMethodTypesAvailable(requested, supported)
        }
    }

    private suspend fun retrieveCustomerPaymentMethods(
        stripeIntent: StripeIntent,
        config: PaymentSheet.Configuration?,
        customerConfig: PaymentSheet.CustomerConfiguration,
    ): List<PaymentMethod> {
        val paymentMethodTypes = getSupportedSavedCustomerPMs(
            stripeIntent,
            config,
            lpmRepository,
        ).mapNotNull {
            // The SDK is only able to parse customer LPMs
            // that are hard coded in the SDK.
            PaymentMethod.Type.fromCode(it.code)
        }

        return customerRepository.getPaymentMethods(
            customerConfig = customerConfig,
            types = paymentMethodTypes,
            silentlyFail = true,
        ).getOrDefault(emptyList()).filter { paymentMethod ->
            paymentMethod.hasExpectedDetails() &&
                // PayPal isn't supported yet as a saved payment method (backend limitation).
                paymentMethod.type != PaymentMethod.Type.PayPal
        }
    }

    private suspend fun retrieveElementsSession(
        initializationMode: PaymentSheet.InitializationMode,
        configuration: PaymentSheet.Configuration?,
    ): Result<ElementsSession> {
        return elementsSessionRepository.get(initializationMode).mapCatching { elementsSession ->
            val billingDetailsCollectionConfig =
                configuration?.billingDetailsCollectionConfiguration?.toInternal()
                    ?: CardBillingDetailsCollectionConfiguration()

            lpmRepository.update(
                stripeIntent = elementsSession.stripeIntent,
                serverLpmSpecs = elementsSession.paymentMethodSpecs,
                cardBillingDetailsCollectionConfiguration = billingDetailsCollectionConfig,
            )

            if (lpmRepository.serverSpecLoadingState is ServerSpecState.ServerNotParsed) {
                eventReporter.onLpmSpecFailure(
                    isDecoupling = initializationMode is DeferredIntent,
                )
            }

            elementsSession.requireValidOrThrow()
        }
    }

    private suspend fun loadLinkState(
        config: PaymentSheet.Configuration?,
        stripeIntent: StripeIntent,
        merchantCountry: String?,
    ): LinkState {
        val linkConfig = createLinkConfiguration(config, stripeIntent, merchantCountry)

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
        merchantCountry: String?,
    ): LinkConfiguration {
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

        return LinkConfiguration(
            stripeIntent = stripeIntent,
            merchantName = merchantName,
            merchantCountryCode = merchantCountry,
            customerEmail = customerEmail,
            customerPhone = customerPhone,
            customerName = config?.defaultBillingDetails?.name,
            customerBillingCountryCode = config?.defaultBillingDetails?.address?.country,
            shippingValues = shippingAddress
        )
    }

    private fun warnUnactivatedIfNeeded(stripeIntent: StripeIntent) {
        if (stripeIntent.unactivatedPaymentMethods.isEmpty()) {
            return
        }

        val message = "[Stripe SDK] Warning: Your Intent contains the following payment method " +
            "types which are activated for test mode but not activated for " +
            "live mode: ${stripeIntent.unactivatedPaymentMethods}. These payment method types " +
            "will not be displayed in live mode until they are activated. To activate these " +
            "payment method types visit your Stripe dashboard." +
            "More information: https://support.stripe.com/questions/activate-a-new-payment-method"

        logger.warning(message)
    }

    private fun supportsIntent(
        stripeIntent: StripeIntent,
        config: PaymentSheet.Configuration?,
    ): Boolean {
        val availablePaymentMethods = getPMsToAdd(stripeIntent, config, lpmRepository)
        val requestedTypes = stripeIntent.paymentMethodTypes.toSet()
        val availableTypes = availablePaymentMethods.map { it.code }.toSet()
        return availableTypes.intersect(requestedTypes).isNotEmpty()
    }

    private fun reportLoadResult(
        loaderResult: Result<PaymentSheetState.Full>,
        isDecoupling: Boolean,
    ) {
        loaderResult.fold(
            onSuccess = {
                eventReporter.onLoadSucceeded(isDecoupling = isDecoupling)
            },
            onFailure = { error ->
                logger.error("Failure loading PaymentSheetState", error)
                eventReporter.onLoadFailed(
                    isDecoupling = isDecoupling,
                    error = error,
                )
            }
        )
    }
}

private fun List<PaymentMethod>.withLastUsedPaymentMethodFirst(
    savedSelection: SavedSelection,
): List<PaymentMethod> {
    val defaultPaymentMethodIndex = (savedSelection as? SavedSelection.PaymentMethod)?.let {
        indexOfFirst { it.id == savedSelection.id }.takeIf { it != -1 }
    }

    return if (defaultPaymentMethodIndex != null) {
        val primaryPaymentMethod = get(defaultPaymentMethodIndex)
        listOf(primaryPaymentMethod) + (this - primaryPaymentMethod)
    } else {
        this
    }
}

private fun PaymentMethod.toPaymentSelection(): PaymentSelection.Saved {
    return PaymentSelection.Saved(this)
}

internal fun PaymentSheet.BillingDetailsCollectionConfiguration.toInternal():
    CardBillingDetailsCollectionConfiguration {
    return CardBillingDetailsCollectionConfiguration(
        collectName = name == Always,
        collectEmail = email == Always,
        collectPhone = phone == Always,
        address = when (address) {
            PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic -> {
                CardBillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic
            }
            PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Never -> {
                CardBillingDetailsCollectionConfiguration.AddressCollectionMode.Never
            }
            PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full -> {
                CardBillingDetailsCollectionConfiguration.AddressCollectionMode.Full
            }
        },
    )
}
