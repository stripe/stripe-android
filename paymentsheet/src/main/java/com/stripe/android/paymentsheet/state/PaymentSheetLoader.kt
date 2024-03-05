package com.stripe.android.paymentsheet.state

import com.stripe.android.core.Logger
import com.stripe.android.core.injection.IOContext
import com.stripe.android.googlepaylauncher.GooglePayEnvironment
import com.stripe.android.googlepaylauncher.GooglePayRepository
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.account.LinkStore
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.link.ui.inline.LinkSignupMode
import com.stripe.android.lpmfoundations.luxe.LpmRepository
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.definitions.CardDefinition
import com.stripe.android.lpmfoundations.paymentmethod.getSetupFutureUsageFieldConfiguration
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PrefsRepository
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.addresselement.toIdentifierMap
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.SavedSelection
import com.stripe.android.paymentsheet.model.currency
import com.stripe.android.paymentsheet.model.validate
import com.stripe.android.paymentsheet.repositories.CustomerRepository
import com.stripe.android.paymentsheet.repositories.ElementsSessionRepository
import com.stripe.android.ui.core.BillingDetailsCollectionConfiguration
import kotlinx.coroutines.Deferred
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
        paymentSheetConfiguration: PaymentSheet.Configuration,
        isReloadingAfterProcessDeath: Boolean = false,
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
        paymentSheetConfiguration: PaymentSheet.Configuration,
        isReloadingAfterProcessDeath: Boolean,
    ): Result<PaymentSheetState.Full> = withContext(workContext) {
        eventReporter.onLoadStarted()

        val elementsSessionResult = retrieveElementsSession(
            initializationMode = initializationMode,
        )

        elementsSessionResult.mapCatching { elementsSession ->
            val billingDetailsCollectionConfig =
                paymentSheetConfiguration.billingDetailsCollectionConfiguration.toInternal()

            val sharedDataSpecsResult = lpmRepository.getSharedDataSpecs(
                stripeIntent = elementsSession.stripeIntent,
                serverLpmSpecs = elementsSession.paymentMethodSpecs,
            )
            val metadata = PaymentMethodMetadata(
                stripeIntent = elementsSession.stripeIntent,
                billingDetailsCollectionConfiguration = billingDetailsCollectionConfig,
                allowsDelayedPaymentMethods = paymentSheetConfiguration.allowsDelayedPaymentMethods,
                allowsPaymentMethodsRequiringShippingAddress = paymentSheetConfiguration
                    .allowsPaymentMethodsRequiringShippingAddress,
                paymentMethodOrder = paymentSheetConfiguration.paymentMethodOrder,
                sharedDataSpecs = sharedDataSpecsResult.sharedDataSpecs,
            )

            lpmRepository.update(metadata, sharedDataSpecsResult.sharedDataSpecs)

            if (sharedDataSpecsResult.failedToParseServerResponse) {
                eventReporter.onLpmSpecFailure()
            }

            create(
                elementsSession = elementsSession,
                config = paymentSheetConfiguration,
                metadata = metadata,
            ).let { state ->
                reportSuccessfulLoad(
                    elementsSession = elementsSession,
                    state = state,
                    isReloadingAfterProcessDeath = isReloadingAfterProcessDeath,
                    isGooglePaySupported = isGooglePaySupported(),
                )

                return@let state
            }
        }.onFailure(::reportFailedLoad)
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

    private suspend fun isGooglePaySupported(): Boolean {
        return googlePayRepositoryFactory(GooglePayEnvironment.Production).isReady().first()
    }

    private suspend fun create(
        elementsSession: ElementsSession,
        config: PaymentSheet.Configuration,
        metadata: PaymentMethodMetadata,
    ): PaymentSheetState.Full = coroutineScope {
        val stripeIntent = elementsSession.stripeIntent
        val merchantCountry = elementsSession.merchantCountry

        val isGooglePayReady = async {
            isGooglePayReady(config, elementsSession)
        }

        val paymentMethods = async {
            when (val customerConfig = config.customer) {
                null -> emptyList()
                else -> retrieveCustomerPaymentMethods(
                    metadata = metadata,
                    customerConfig = customerConfig,
                )
            }
        }

        val savedSelection = async {
            retrieveSavedSelection(
                config = config,
                isGooglePayReady = isGooglePayReady.await(),
                elementsSession = elementsSession
            )
        }

        val sortedPaymentMethods = async {
            paymentMethods.await().withLastUsedPaymentMethodFirst(savedSelection.await())
        }

        val initialPaymentSelection = async {
            retrieveInitialPaymentSelection(savedSelection, paymentMethods)
                ?: sortedPaymentMethods.await().firstOrNull()?.toPaymentSelection()
        }

        val linkState = async {
            if (elementsSession.isLinkEnabled && !config.billingDetailsCollectionConfiguration.collectsAnything) {
                loadLinkState(
                    config = config,
                    metadata = metadata,
                    merchantCountry = merchantCountry,
                    passthroughModeEnabled = elementsSession.linkPassthroughModeEnabled,
                    linkSignUpDisabled = elementsSession.disableLinkSignup,
                    flags = elementsSession.linkFlags,
                )
            } else {
                null
            }
        }

        warnUnactivatedIfNeeded(stripeIntent)

        if (supportsIntent(metadata)) {
            PaymentSheetState.Full(
                config = config,
                customerPaymentMethods = sortedPaymentMethods.await(),
                isGooglePayReady = isGooglePayReady.await(),
                linkState = linkState.await(),
                isEligibleForCardBrandChoice = elementsSession.isEligibleForCardBrandChoice,
                paymentSelection = initialPaymentSelection.await(),
                validationError = stripeIntent.validate(),
                paymentMethodMetadata = metadata,
            )
        } else {
            val requested = stripeIntent.paymentMethodTypes.joinToString(separator = ", ")
            throw PaymentSheetLoadingException.NoPaymentMethodTypesAvailable(requested)
        }
    }

    private suspend fun retrieveCustomerPaymentMethods(
        metadata: PaymentMethodMetadata,
        customerConfig: PaymentSheet.CustomerConfiguration,
    ): List<PaymentMethod> {
        val paymentMethodTypes = metadata.supportedSavedPaymentMethodTypes()

        val paymentMethods = customerRepository.getPaymentMethods(
            customerConfig = customerConfig,
            types = paymentMethodTypes,
            silentlyFail = metadata.stripeIntent.isLiveMode,
        ).getOrThrow()

        return paymentMethods.filter { paymentMethod ->
            paymentMethod.hasExpectedDetails()
        }
    }

    private suspend fun retrieveElementsSession(
        initializationMode: PaymentSheet.InitializationMode,
    ): Result<ElementsSession> {
        return elementsSessionRepository.get(initializationMode)
    }

    private suspend fun loadLinkState(
        config: PaymentSheet.Configuration,
        metadata: PaymentMethodMetadata,
        merchantCountry: String?,
        passthroughModeEnabled: Boolean,
        linkSignUpDisabled: Boolean,
        flags: Map<String, Boolean>,
    ): LinkState {
        val linkConfig = createLinkConfiguration(
            config = config,
            metadata = metadata,
            merchantCountry = merchantCountry,
            passthroughModeEnabled = passthroughModeEnabled,
            flags = flags,
            linkSignUpDisabled = linkSignUpDisabled,
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
        metadata: PaymentMethodMetadata,
        merchantCountry: String?,
        passthroughModeEnabled: Boolean,
        linkSignUpDisabled: Boolean,
        flags: Map<String, Boolean>,
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

        val setupFutureUsageFieldConfiguration = requireNotNull(
            CardDefinition.getSetupFutureUsageFieldConfiguration(
                metadata = metadata,
                customerConfiguration = config.customer,
            )
        )
        val hasUsedLink = linkStore.hasUsedLink()

        val linkSignupMode = if (hasUsedLink || linkSignUpDisabled) {
            null
        } else if (setupFutureUsageFieldConfiguration.showCheckbox) {
            LinkSignupMode.AlongsideSaveForFutureUse
        } else {
            LinkSignupMode.InsteadOfSaveForFutureUse
        }

        val customerInfo = LinkConfiguration.CustomerInfo(
            name = config.defaultBillingDetails?.name,
            email = customerEmail,
            phone = customerPhone,
            billingCountryCode = config.defaultBillingDetails?.address?.country,
        )

        return LinkConfiguration(
            stripeIntent = metadata.stripeIntent,
            signupMode = linkSignupMode,
            merchantName = merchantName,
            merchantCountryCode = merchantCountry,
            customerInfo = customerInfo,
            shippingValues = shippingAddress,
            passthroughModeEnabled = passthroughModeEnabled,
            flags = flags,
        )
    }

    private suspend fun retrieveInitialPaymentSelection(
        savedSelection: Deferred<SavedSelection>,
        paymentMethods: Deferred<List<PaymentMethod>>
    ): PaymentSelection? {
        return when (val selection = savedSelection.await()) {
            is SavedSelection.GooglePay -> PaymentSelection.GooglePay
            is SavedSelection.Link -> PaymentSelection.Link
            is SavedSelection.PaymentMethod -> {
                paymentMethods.await().find { it.id == selection.id }?.toPaymentSelection()
            }
            is SavedSelection.None -> null
        }
    }

    private suspend fun retrieveSavedSelection(
        config: PaymentSheet.Configuration,
        isGooglePayReady: Boolean,
        elementsSession: ElementsSession
    ): SavedSelection {
        val customerConfig = config.customer
        val prefsRepository = prefsRepositoryFactory(customerConfig)

        return prefsRepository.getSavedSelection(
            isGooglePayAvailable = isGooglePayReady,
            isLinkAvailable = elementsSession.isLinkEnabled,
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
        metadata: PaymentMethodMetadata,
    ): Boolean {
        return metadata.supportedPaymentMethodDefinitions().isNotEmpty()
    }

    private fun reportSuccessfulLoad(
        elementsSession: ElementsSession,
        state: PaymentSheetState.Full,
        isReloadingAfterProcessDeath: Boolean,
        isGooglePaySupported: Boolean,
    ) {
        elementsSession.sessionsError?.let { sessionsError ->
            eventReporter.onElementsSessionLoadFailed(sessionsError)
        }

        val treatValidationErrorAsFailure = !state.stripeIntent.isConfirmed || isReloadingAfterProcessDeath

        if (state.validationError != null && treatValidationErrorAsFailure) {
            eventReporter.onLoadFailed(state.validationError)
        } else {
            eventReporter.onLoadSucceeded(
                linkEnabled = elementsSession.isLinkEnabled,
                googlePaySupported = isGooglePaySupported,
                currency = elementsSession.stripeIntent.currency,
                paymentSelection = state.paymentSelection,
            )
        }
    }

    private fun reportFailedLoad(
        error: Throwable,
    ) {
        logger.error("Failure loading PaymentSheetState", error)
        eventReporter.onLoadFailed(error)
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
