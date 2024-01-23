package com.stripe.android.paymentsheet.state

import com.stripe.android.core.Logger
import com.stripe.android.core.injection.IOContext
import com.stripe.android.googlepaylauncher.GooglePayEnvironment
import com.stripe.android.googlepaylauncher.GooglePayRepository
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.account.LinkStore
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.link.ui.inline.LinkSignupMode
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheet.InitializationMode.DeferredIntent
import com.stripe.android.paymentsheet.PrefsRepository
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.addresselement.toIdentifierMap
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.SavedSelection
import com.stripe.android.paymentsheet.model.currency
import com.stripe.android.paymentsheet.model.getPMAddForm
import com.stripe.android.paymentsheet.model.getPMsToAdd
import com.stripe.android.paymentsheet.model.getSupportedSavedCustomerPMs
import com.stripe.android.paymentsheet.model.requireValidOrThrow
import com.stripe.android.paymentsheet.repositories.CustomerRepository
import com.stripe.android.paymentsheet.repositories.ElementsSessionRepository
import com.stripe.android.ui.core.BillingDetailsCollectionConfiguration
import com.stripe.android.ui.core.forms.resources.LpmRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

/**
 * Loads the information necessary to display [PaymentSheet], either directly or via
 * [PaymentSheet.FlowController].
 */
internal interface PaymentSheetLoader {

    suspend fun load(
        initializationMode: PaymentSheet.InitializationMode,
        paymentSheetConfiguration: PaymentSheet.Configuration
    ): Result<PaymentSheetState.Full>
}

@Singleton
internal class DefaultPaymentSheetLoader @Inject constructor(
    private val prefsRepositoryFactory: @JvmSuppressWildcards (PaymentSheet.CustomerConfiguration?) -> PrefsRepository,
    private val googlePayRepositoryFactory: @JvmSuppressWildcards (GooglePayEnvironment) -> GooglePayRepository,
    private val elementsSessionRepository: ElementsSessionRepository,
    private val customerRepository: CustomerRepository,
    private val lpmRepository: LpmRepository,
    private val logger: Logger,
    private val eventReporter: EventReporter,
    @IOContext private val workContext: CoroutineContext,
    private val accountStatusProvider: LinkAccountStatusProvider,
    private val linkStore: LinkStore,
) : PaymentSheetLoader {

    override suspend fun load(
        initializationMode: PaymentSheet.InitializationMode,
        paymentSheetConfiguration: PaymentSheet.Configuration
    ): Result<PaymentSheetState.Full> = withContext(workContext) {
        eventReporter.onLoadStarted()

        val elementsSessionResult = retrieveElementsSession(
            initializationMode = initializationMode,
            configuration = paymentSheetConfiguration,
        )

        reportLoadResult(elementsSessionResult)

        elementsSessionResult.mapCatching { elementsSession ->
            create(
                elementsSession = elementsSession,
                config = paymentSheetConfiguration,
                isGooglePayReady = isGooglePayReady(paymentSheetConfiguration, elementsSession),
            )
        }
    }

    private suspend fun isGooglePayReady(
        paymentSheetConfiguration: PaymentSheet.Configuration,
        elementsSession: ElementsSession,
    ): Boolean {
        return elementsSession.isGooglePayEnabled && paymentSheetConfiguration.isGooglePayReady()
    }

    private suspend fun PaymentSheet.Configuration.isGooglePayReady(): Boolean {
        return googlePay?.environment?.let { environment ->
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
        config: PaymentSheet.Configuration,
        isGooglePayReady: Boolean,
    ): PaymentSheetState.Full = coroutineScope {
        val customerConfig = config.customer
        val prefsRepository = prefsRepositoryFactory(customerConfig)

        val stripeIntent = elementsSession.stripeIntent
        val merchantCountry = elementsSession.merchantCountry

        val savedSelection = async {
            prefsRepository.getSavedSelection(
                isGooglePayAvailable = isGooglePayReady,
                isLinkAvailable = elementsSession.isLinkEnabled,
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
            if (elementsSession.isLinkEnabled && !config.billingDetailsCollectionConfiguration.collectsAnything) {
                loadLinkState(
                    config = config,
                    stripeIntent = stripeIntent,
                    merchantCountry = merchantCountry,
                    passthroughModeEnabled = elementsSession.linkPassthroughModeEnabled,
                )
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
                isEligibleForCardBrandChoice = elementsSession.isEligibleForCardBrandChoice,
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
        config: PaymentSheet.Configuration,
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

        val paymentMethods = customerRepository.getPaymentMethods(
            customerConfig = customerConfig,
            types = paymentMethodTypes,
            silentlyFail = true,
        ).getOrDefault(emptyList())

        return paymentMethods.filter { paymentMethod ->
            paymentMethod.hasExpectedDetails()
        }
    }

    private suspend fun retrieveElementsSession(
        initializationMode: PaymentSheet.InitializationMode,
        configuration: PaymentSheet.Configuration,
    ): Result<ElementsSession> {
        return elementsSessionRepository.get(initializationMode).mapCatching { elementsSession ->
            val billingDetailsCollectionConfig =
                configuration.billingDetailsCollectionConfiguration.toInternal()

            val didParseServerResponse = lpmRepository.update(
                stripeIntent = elementsSession.stripeIntent,
                serverLpmSpecs = elementsSession.paymentMethodSpecs,
                billingDetailsCollectionConfiguration = billingDetailsCollectionConfig,
                isDeferred = initializationMode is DeferredIntent,
            )

            if (!didParseServerResponse) {
                eventReporter.onLpmSpecFailure()
            }

            elementsSession.requireValidOrThrow()
        }
    }

    private suspend fun loadLinkState(
        config: PaymentSheet.Configuration,
        stripeIntent: StripeIntent,
        merchantCountry: String?,
        passthroughModeEnabled: Boolean,
    ): LinkState {
        val linkConfig = createLinkConfiguration(
            config = config,
            stripeIntent = stripeIntent,
            merchantCountry = merchantCountry,
            passthroughModeEnabled = passthroughModeEnabled,
        )

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
        config: PaymentSheet.Configuration,
        stripeIntent: StripeIntent,
        merchantCountry: String?,
        passthroughModeEnabled: Boolean,
    ): LinkConfiguration {
        val shippingDetails: AddressDetails? = config.shippingDetails

        val customerPhone = if (shippingDetails?.isCheckboxSelected == true) {
            shippingDetails.phoneNumber
        } else {
            config.defaultBillingDetails?.phone
        }

        val shippingAddress = if (shippingDetails?.isCheckboxSelected == true) {
            shippingDetails.toIdentifierMap(config.defaultBillingDetails)
        } else {
            null
        }

        val customerEmail = config.defaultBillingDetails?.email ?: config.customer?.let {
            customerRepository.retrieveCustomer(
                it.id,
                it.ephemeralKeySecret
            )
        }?.email

        val merchantName = config.merchantDisplayName

        val layoutDescriptor = LpmRepository.HardcodedCard.getPMAddForm(stripeIntent, config)
        val hasUsedLink = linkStore.hasUsedLink()

        val linkSignupMode = if (hasUsedLink) {
            null
        } else if (layoutDescriptor.showCheckbox) {
            LinkSignupMode.AlongsideSaveForFutureUse
        } else {
            LinkSignupMode.InsteadOfSaveForFutureUse
        }

        val shouldAutofillBillingDetails = when (linkSignupMode) {
            LinkSignupMode.InsteadOfSaveForFutureUse -> true
            LinkSignupMode.AlongsideSaveForFutureUse,
            null -> false
        }

        val customerInfo = LinkConfiguration.CustomerInfo(
            name = config.defaultBillingDetails?.name,
            email = customerEmail,
            phone = customerPhone,
            billingCountryCode = config.defaultBillingDetails?.address?.country,
            shouldPrefill = shouldAutofillBillingDetails,
        )

        return LinkConfiguration(
            stripeIntent = stripeIntent,
            signupMode = linkSignupMode,
            merchantName = merchantName,
            merchantCountryCode = merchantCountry,
            customerInfo = customerInfo,
            shippingValues = shippingAddress,
            passthroughModeEnabled = passthroughModeEnabled,
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
        config: PaymentSheet.Configuration,
    ): Boolean {
        val availablePaymentMethods = getPMsToAdd(stripeIntent, config, lpmRepository)
        val requestedTypes = stripeIntent.paymentMethodTypes.toSet()
        val availableTypes = availablePaymentMethods.map { it.code }.toSet()
        return availableTypes.intersect(requestedTypes).isNotEmpty()
    }

    private fun reportLoadResult(
        loaderResult: Result<ElementsSession>,
    ) {
        loaderResult.fold(
            onSuccess = { elementsSession ->
                elementsSession.sessionsError?.let { sessionsError ->
                    eventReporter.onElementsSessionLoadFailed(sessionsError)
                }
                eventReporter.onLoadSucceeded(
                    linkEnabled = elementsSession.isLinkEnabled,
                    currency = elementsSession.stripeIntent.currency,
                )
            },
            onFailure = { error ->
                logger.error("Failure loading PaymentSheetState", error)
                eventReporter.onLoadFailed(error)
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

internal fun PaymentSheet.BillingDetailsCollectionConfiguration?.toInternal(): BillingDetailsCollectionConfiguration {
    return BillingDetailsCollectionConfiguration(
        collectName = this?.name == PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
        collectEmail = this?.email == PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
        collectPhone = this?.phone == PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
        address = when (this?.address) {
            PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic -> {
                BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic
            }
            PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Never -> {
                BillingDetailsCollectionConfiguration.AddressCollectionMode.Never
            }
            PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full -> {
                BillingDetailsCollectionConfiguration.AddressCollectionMode.Full
            }
            else -> BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic
        },
    )
}
