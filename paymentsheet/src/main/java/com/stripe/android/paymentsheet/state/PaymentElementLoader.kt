package com.stripe.android.paymentsheet.state

import android.os.Parcelable
import com.stripe.android.DefaultCardBrandFilter
import com.stripe.android.common.analytics.experiment.LogLinkHoldbackExperiment
import com.stripe.android.common.coroutines.runCatching
import com.stripe.android.common.model.CommonConfiguration
import com.stripe.android.common.validation.isSupportedWithBillingConfig
import com.stripe.android.core.Logger
import com.stripe.android.core.exception.StripeException
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.utils.FeatureFlag
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.core.utils.UserFacingLogger
import com.stripe.android.googlepaylauncher.GooglePayEnvironment
import com.stripe.android.googlepaylauncher.GooglePayRepository
import com.stripe.android.link.LinkAppearance
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.account.LinkStore
import com.stripe.android.link.gate.LinkGate
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.link.model.toLoginState
import com.stripe.android.link.ui.inline.LinkSignupMode
import com.stripe.android.lpmfoundations.luxe.LpmRepository
import com.stripe.android.lpmfoundations.luxe.isSaveForFutureUseValueChangeable
import com.stripe.android.lpmfoundations.paymentmethod.CustomerMetadata
import com.stripe.android.lpmfoundations.paymentmethod.CustomerMetadata.Permissions.Companion.createForPaymentSheetCustomerSession
import com.stripe.android.lpmfoundations.paymentmethod.CustomerMetadata.Permissions.Companion.createForPaymentSheetLegacyEphemeralKey
import com.stripe.android.lpmfoundations.paymentmethod.IS_PAYMENT_METHOD_SET_AS_DEFAULT_ENABLED_DEFAULT_VALUE
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentSheetCardBrandFilter
import com.stripe.android.lpmfoundations.paymentmethod.toPaymentSheetSaveConsentBehavior
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.ElementsSession.Flag.ELEMENTS_MOBILE_FORCE_SETUP_FUTURE_USE_BEHAVIOR_AND_NEW_MANDATE_TEXT
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.payments.financialconnections.GetFinancialConnectionsAvailability
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheet.IntentConfiguration
import com.stripe.android.paymentsheet.PrefsRepository
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.cvcrecollection.CvcRecollectionHandler
import com.stripe.android.paymentsheet.model.PaymentIntentClientSecret
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.SavedSelection
import com.stripe.android.paymentsheet.model.SetupIntentClientSecret
import com.stripe.android.paymentsheet.model.currency
import com.stripe.android.paymentsheet.model.validate
import com.stripe.android.paymentsheet.repositories.CustomerRepository
import com.stripe.android.paymentsheet.repositories.ElementsSessionRepository
import com.stripe.android.ui.core.elements.ExternalPaymentMethodSpec
import com.stripe.android.ui.core.elements.ExternalPaymentMethodsRepository
import com.stripe.attestation.IntegrityRequestManager
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

/**
 * Loads the information necessary to display [PaymentSheet], either directly or via
 * [PaymentSheet.FlowController].
 */
internal interface PaymentElementLoader {

    suspend fun load(
        initializationMode: InitializationMode,
        configuration: CommonConfiguration,
        metadata: Metadata,
    ): Result<State>

    data class Metadata(
        val isReloadingAfterProcessDeath: Boolean = false,
        val initializedViaCompose: Boolean,
    )

    sealed class InitializationMode : Parcelable {
        abstract fun validate()

        @Parcelize
        data class PaymentIntent(
            val clientSecret: String,
        ) : InitializationMode() {

            override fun validate() {
                PaymentIntentClientSecret(clientSecret).validate()
            }
        }

        @Parcelize
        data class SetupIntent(
            val clientSecret: String,
        ) : InitializationMode() {

            override fun validate() {
                SetupIntentClientSecret(clientSecret).validate()
            }
        }

        @Parcelize
        data class DeferredIntent(
            val intentConfiguration: IntentConfiguration,
        ) : InitializationMode() {

            override fun validate() {
                (intentConfiguration.mode as? IntentConfiguration.Mode.Payment)?.let {
                    if (it.amount <= 0) {
                        throw IllegalArgumentException(
                            "Payment IntentConfiguration requires a positive amount."
                        )
                    }
                }
            }
        }
    }

    @Parcelize
    data class State(
        val config: CommonConfiguration,
        val customer: CustomerState?,
        val paymentSelection: PaymentSelection?,
        val validationError: PaymentSheetLoadingException?,
        val paymentMethodMetadata: PaymentMethodMetadata,
    ) : Parcelable {
        val stripeIntent: StripeIntent
            get() = paymentMethodMetadata.stripeIntent
    }
}

/**
 * A default implementation of [PaymentElementLoader] used to load necessary information for
 * building [PaymentSheet]. See the linked flow diagram to understand how this implementation
 * loads [PaymentSheet] information based its provided initialization options.
 *
 * @see <a href="https://whimsical.com/paymentsheet-loading-flow-diagram-EwTmrwvNmhcD9B2PKuSu82/">Flow Diagram</a>
 */
@Singleton
@SuppressWarnings("LargeClass")
internal class DefaultPaymentElementLoader @Inject constructor(
    private val prefsRepositoryFactory: @JvmSuppressWildcards (PaymentSheet.CustomerConfiguration?) -> PrefsRepository,
    private val googlePayRepositoryFactory: @JvmSuppressWildcards (GooglePayEnvironment) -> GooglePayRepository,
    private val elementsSessionRepository: ElementsSessionRepository,
    private val customerRepository: CustomerRepository,
    private val lpmRepository: LpmRepository,
    private val logger: Logger,
    private val eventReporter: EventReporter,
    private val errorReporter: ErrorReporter,
    @IOContext private val workContext: CoroutineContext,
    private val retrieveCustomerEmail: RetrieveCustomerEmail,
    private val accountStatusProvider: LinkAccountStatusProvider,
    private val logLinkHoldbackExperiment: LogLinkHoldbackExperiment,
    private val linkStore: LinkStore,
    private val linkGateFactory: LinkGate.Factory,
    private val externalPaymentMethodsRepository: ExternalPaymentMethodsRepository,
    private val userFacingLogger: UserFacingLogger,
    private val cvcRecollectionHandler: CvcRecollectionHandler,
    private val integrityRequestManager: IntegrityRequestManager,
) : PaymentElementLoader {

    @Suppress("LongMethod")
    override suspend fun load(
        initializationMode: PaymentElementLoader.InitializationMode,
        configuration: CommonConfiguration,
        metadata: PaymentElementLoader.Metadata,
    ): Result<PaymentElementLoader.State> = workContext.runCatching(::reportFailedLoad) {
        eventReporter.onLoadStarted(metadata.initializedViaCompose)

        val savedPaymentMethodSelection = retrieveSavedPaymentMethodSelection(configuration)
        val elementsSession = retrieveElementsSession(
            initializationMode = initializationMode,
            customer = configuration.customer,
            customPaymentMethods = configuration.customPaymentMethods,
            externalPaymentMethods = configuration.externalPaymentMethods,
            savedPaymentMethodSelectionId = savedPaymentMethodSelection?.id,
        ).getOrThrow()

        // Preemptively prepare Integrity asynchronously if needed, as warm up can take
        // a few seconds.
        if (elementsSession.shouldWarmUpIntegrity()) {
            launch { integrityRequestManager.prepare() }
        }

        val customerInfo = createCustomerInfo(
            configuration = configuration,
            elementsSession = elementsSession,
        )

        val isGooglePayReady = isGooglePayReady(configuration, elementsSession)

        val savedSelection = async {
            retrieveSavedSelection(
                configuration = configuration,
                isGooglePayReady = isGooglePayReady,
                elementsSession = elementsSession
            )
        }

        val linkState = async {
            createLinkState(
                configuration = configuration,
                elementsSession = elementsSession,
                customer = customerInfo,
                initializationMode = initializationMode,
                linkAppearance = configuration.linkAppearance
            )
        }

        val paymentMethodMetadata = async {
            createPaymentMethodMetadata(
                configuration = configuration,
                elementsSession = elementsSession,
                customerInfo = customerInfo,
                isGooglePayReady = isGooglePayReady,
                linkState = linkState.await(),
            )
        }

        val customer = async {
            createCustomerState(
                configuration = configuration,
                customerInfo = customerInfo,
                metadata = paymentMethodMetadata.await(),
                savedSelection = savedSelection,
                cardBrandFilter = PaymentSheetCardBrandFilter(configuration.cardBrandAcceptance)
            )
        }

        val initialPaymentSelection = async {
            retrieveInitialPaymentSelection(
                savedSelection = savedSelection,
                metadata = paymentMethodMetadata.await(),
                customer = customer.await(),
                isGooglePayReady = isGooglePayReady,
                isUsingWalletButtons = configuration.walletButtons?.willDisplayExternally ?: false
            )
        }

        val stripeIntent = elementsSession.stripeIntent
        val pmMetadata = paymentMethodMetadata.await()

        warnUnactivatedIfNeeded(stripeIntent)

        if (!supportsIntent(pmMetadata)) {
            val requested = stripeIntent.paymentMethodTypes.joinToString(separator = ", ")
            throw PaymentSheetLoadingException.NoPaymentMethodTypesAvailable(requested)
        }

        val state = PaymentElementLoader.State(
            config = configuration,
            customer = customer.await(),
            paymentSelection = initialPaymentSelection.await(),
            validationError = stripeIntent.validate(),
            paymentMethodMetadata = pmMetadata,
        )

        logLinkExperimentExposures(
            elementsSession = elementsSession,
            state = state
        )

        reportSuccessfulLoad(
            elementsSession = elementsSession,
            state = state,
            isReloadingAfterProcessDeath = metadata.isReloadingAfterProcessDeath,
            isGooglePaySupported = isGooglePaySupported(),
            linkDisplay = configuration.link.display,
            initializationMode = initializationMode,
            customerInfo = customerInfo,
            paymentMethodMetadata = pmMetadata,
        )

        return@runCatching state
    }

    private fun ElementsSession.shouldWarmUpIntegrity(): Boolean = when {
        stripeIntent.isLiveMode -> useAttestationEndpointsForLink
        else -> when (FeatureFlags.nativeLinkAttestationEnabled.value) {
            FeatureFlag.Flag.Disabled -> false
            FeatureFlag.Flag.Enabled -> true
            FeatureFlag.Flag.NotSet -> useAttestationEndpointsForLink
        }
    }

    private fun logLinkExperimentExposures(
        elementsSession: ElementsSession,
        state: PaymentElementLoader.State
    ) {
        logLinkHoldbackExperiment(
            experimentAssignment = ElementsSession.ExperimentAssignment.LINK_GLOBAL_HOLD_BACK,
            elementsSession = elementsSession,
            state = state
        )

        logLinkHoldbackExperiment(
            elementsSession = elementsSession,
            state = state,
            experimentAssignment = ElementsSession.ExperimentAssignment.LINK_AB_TEST
        )
    }

    private suspend fun retrieveElementsSession(
        initializationMode: PaymentElementLoader.InitializationMode,
        customer: PaymentSheet.CustomerConfiguration?,
        customPaymentMethods: List<PaymentSheet.CustomPaymentMethod>,
        externalPaymentMethods: List<String>,
        savedPaymentMethodSelectionId: String?,
    ): Result<ElementsSession> {
        return elementsSessionRepository.get(
            initializationMode = initializationMode,
            customer = customer,
            externalPaymentMethods = externalPaymentMethods,
            customPaymentMethods = customPaymentMethods,
            savedPaymentMethodSelectionId = savedPaymentMethodSelectionId
        )
    }

    private fun createPaymentMethodMetadata(
        configuration: CommonConfiguration,
        elementsSession: ElementsSession,
        customerInfo: CustomerInfo?,
        linkState: LinkState?,
        isGooglePayReady: Boolean,
    ): PaymentMethodMetadata {
        val sharedDataSpecsResult = lpmRepository.getSharedDataSpecs(
            stripeIntent = elementsSession.stripeIntent,
            serverLpmSpecs = elementsSession.paymentMethodSpecs,
        )

        if (sharedDataSpecsResult.failedToParseServerResponse) {
            eventReporter.onLpmSpecFailure(sharedDataSpecsResult.failedToParseServerErrorMessage)
        }

        val externalPaymentMethodSpecs = externalPaymentMethodsRepository.getExternalPaymentMethodSpecs(
            elementsSession.externalPaymentMethodData
        )

        logIfMissingExternalPaymentMethods(
            requestedExternalPaymentMethods = configuration.externalPaymentMethods,
            actualExternalPaymentMethods = externalPaymentMethodSpecs
        )

        logCustomPaymentMethodErrors(elementsSession.customPaymentMethods)

        return PaymentMethodMetadata.createForPaymentElement(
            elementsSession = elementsSession,
            configuration = configuration,
            sharedDataSpecs = sharedDataSpecsResult.sharedDataSpecs,
            externalPaymentMethodSpecs = externalPaymentMethodSpecs,
            isGooglePayReady = isGooglePayReady,
            linkState = linkState,
            customerMetadata = getCustomerMetadata(
                configuration = configuration,
                elementsSession = elementsSession,
                customerInfo = customerInfo,
            ),
        )
    }

    private fun getCustomerMetadata(
        configuration: CommonConfiguration,
        elementsSession: ElementsSession,
        customerInfo: CustomerInfo?,
    ): CustomerMetadata {
        return CustomerMetadata(
            hasCustomerConfiguration = configuration.customer != null,
            isPaymentMethodSetAsDefaultEnabled = getDefaultPaymentMethodsEnabled(elementsSession),
            permissions = if (customerInfo is CustomerInfo.CustomerSession) {
                createForPaymentSheetCustomerSession(
                    configuration = configuration,
                    customer = customerInfo.elementsSessionCustomer
                )
            } else {
                createForPaymentSheetLegacyEphemeralKey(
                    configuration = configuration
                )
            }
        )
    }

    private fun getDefaultPaymentMethodsEnabled(elementsSession: ElementsSession): Boolean {
        val mobilePaymentElement = elementsSession.customer?.session?.components?.mobilePaymentElement
            as? ElementsSession.Customer.Components.MobilePaymentElement.Enabled
        return mobilePaymentElement?.isPaymentMethodSetAsDefaultEnabled
            ?: false
    }

    private fun createCustomerInfo(
        configuration: CommonConfiguration,
        elementsSession: ElementsSession,
    ): CustomerInfo? {
        val customer = configuration.customer

        return when (val accessType = customer?.accessType) {
            is PaymentSheet.CustomerAccessType.CustomerSession -> {
                elementsSession.customer?.let { elementsSessionCustomer ->
                    CustomerInfo.CustomerSession(elementsSessionCustomer, accessType.customerSessionClientSecret)
                } ?: run {
                    val exception = IllegalStateException(
                        "Excepted 'customer' attribute as part of 'elements_session' response!"
                    )

                    errorReporter.report(
                        ErrorReporter.UnexpectedErrorEvent.PAYMENT_SHEET_LOADER_ELEMENTS_SESSION_CUSTOMER_NOT_FOUND,
                        StripeException.create(exception)
                    )

                    if (!elementsSession.stripeIntent.isLiveMode) {
                        throw exception
                    }

                    null
                }
            }
            is PaymentSheet.CustomerAccessType.LegacyCustomerEphemeralKey -> {
                CustomerInfo.Legacy(
                    customerConfig = customer,
                    accessType = accessType,
                )
            }
            else -> null
        }
    }

    private suspend fun createCustomerState(
        configuration: CommonConfiguration,
        customerInfo: CustomerInfo?,
        metadata: PaymentMethodMetadata,
        savedSelection: Deferred<SavedSelection>,
        cardBrandFilter: PaymentSheetCardBrandFilter
    ): CustomerState? {
        val customerState = when (customerInfo) {
            is CustomerInfo.CustomerSession -> {
                CustomerState.createForCustomerSession(
                    customer = customerInfo.elementsSessionCustomer,
                    supportedSavedPaymentMethodTypes = metadata.supportedSavedPaymentMethodTypes(),
                    customerSessionClientSecret = customerInfo.customerSessionClientSecret,
                )
            }
            is CustomerInfo.Legacy -> {
                CustomerState.createForLegacyEphemeralKey(
                    customerId = customerInfo.id,
                    accessType = customerInfo.accessType,
                    paymentMethods = retrieveCustomerPaymentMethods(
                        metadata = metadata,
                        customerConfig = customerInfo.customerConfig,
                    )
                )
            }
            else -> null
        }

        return customerState?.let { state ->
            state.copy(
                paymentMethods = state.paymentMethods
                    .withDefaultPaymentMethodOrLastUsedPaymentMethodFirst(
                        savedSelection = savedSelection,
                        defaultPaymentMethodId = state.defaultPaymentMethodId,
                        isPaymentMethodSetAsDefaultEnabled = metadata.customerMetadata
                            ?.isPaymentMethodSetAsDefaultEnabled
                            ?: IS_PAYMENT_METHOD_SET_AS_DEFAULT_ENABLED_DEFAULT_VALUE
                    ).filter { paymentMethod ->
                        cardBrandFilter.isAccepted(paymentMethod) &&
                            paymentMethod.isSupportedWithBillingConfig(
                                configuration.billingDetailsCollectionConfiguration
                            )
                    },
            )
        }
    }

    private suspend fun retrieveCustomerPaymentMethods(
        metadata: PaymentMethodMetadata,
        customerConfig: PaymentSheet.CustomerConfiguration,
    ): List<PaymentMethod> {
        val paymentMethodTypes = metadata.supportedSavedPaymentMethodTypes()

        val customerSession = (customerConfig.accessType as? PaymentSheet.CustomerAccessType.CustomerSession)
        val customerSessionClientSecret = customerSession?.customerSessionClientSecret

        val paymentMethods = customerRepository.getPaymentMethods(
            customerInfo = CustomerRepository.CustomerInfo(
                id = customerConfig.id,
                ephemeralKeySecret = customerConfig.ephemeralKeySecret,
                customerSessionClientSecret = customerSessionClientSecret,
            ),
            types = paymentMethodTypes,
            silentlyFail = metadata.stripeIntent.isLiveMode,
        ).getOrThrow()

        return paymentMethods.filter { paymentMethod ->
            paymentMethod.hasExpectedDetails()
        }
    }

    private suspend fun createLinkState(
        elementsSession: ElementsSession,
        configuration: CommonConfiguration,
        customer: CustomerInfo?,
        initializationMode: PaymentElementLoader.InitializationMode,
        linkAppearance: LinkAppearance?
    ): LinkState? {
        val linkConfig =
            createLinkConfiguration(
                configuration = configuration,
                customer = customer,
                elementsSession = elementsSession,
                initializationMode = initializationMode,
                linkAppearance = linkAppearance
            ) ?: return null
        return loadLinkState(
            configuration = configuration,
            linkConfiguration = linkConfig,
            elementsSession = elementsSession,
            linkSignUpDisabled = elementsSession.disableLinkSignup,
        )
    }

    private suspend fun loadLinkState(
        configuration: CommonConfiguration,
        linkConfiguration: LinkConfiguration,
        elementsSession: ElementsSession,
        linkSignUpDisabled: Boolean,
    ): LinkState {
        val accountStatus = accountStatusProvider(linkConfiguration)

        val loginState = accountStatus.toLoginState()

        val isSaveForFutureUseValueChangeable = isSaveForFutureUseValueChangeable(
            code = PaymentMethod.Type.Card.code,
            intent = elementsSession.stripeIntent,
            paymentMethodSaveConsentBehavior = elementsSession.toPaymentSheetSaveConsentBehavior(),
            hasCustomerConfiguration = configuration.customer != null,
        )
        val hasUsedLink = linkStore.hasUsedLink()
        val signupToggleEnabled = elementsSession.linkSignUpOptInFeatureEnabled

        val linkSignupMode = when {
            // If signup toggle enabled, we show a future usage + link combined toggle
            signupToggleEnabled && !linkConfiguration.customerInfo.email.isNullOrBlank() -> {
                LinkSignupMode.InsteadOfSaveForFutureUse
            }
            // If inline signup is disabled or user has used Link, we don't show inline signup
            linkSignUpDisabled || hasUsedLink -> null
            // If inline signup and save for future use, we show it alongside save for future use
            isSaveForFutureUseValueChangeable -> LinkSignupMode.AlongsideSaveForFutureUse
            // If inline signup and save for future usage is not displayed, only show link signup
            else -> LinkSignupMode.InsteadOfSaveForFutureUse
        }

        return LinkState(
            configuration = linkConfiguration,
            loginState = loginState,
            signupMode = linkSignupMode.takeIf {
                val validFundingSource = linkConfiguration.stripeIntent.linkFundingSources
                    .contains(PaymentMethod.Type.Card.code)

                val notLoggedIn = accountStatus == AccountStatus.SignedOut

                validFundingSource && notLoggedIn
            },
        )
    }

    @Suppress("LongMethod")
    private suspend fun createLinkConfiguration(
        configuration: CommonConfiguration,
        customer: CustomerInfo?,
        elementsSession: ElementsSession,
        initializationMode: PaymentElementLoader.InitializationMode,
        linkAppearance: LinkAppearance?,
    ): LinkConfiguration? {
        if (!configuration.link.shouldDisplay || !elementsSession.isLinkEnabled) {
            return null
        }

        val isCardBrandFilteringRequired =
            elementsSession.linkPassthroughModeEnabled &&
                configuration.cardBrandAcceptance != PaymentSheet.CardBrandAcceptance.All

        val cardBrandFilter =
            if (isCardBrandFilteringRequired) {
                PaymentSheetCardBrandFilter(configuration.cardBrandAcceptance)
            } else {
                DefaultCardBrandFilter
            }

        val shippingDetails = configuration.shippingDetails

        val customerPhone = if (shippingDetails?.isCheckboxSelected == true) {
            shippingDetails.phoneNumber
        } else {
            configuration.defaultBillingDetails?.phone
        }

        val customerEmail = retrieveCustomerEmail(
            configuration = configuration,
            customer = customer?.toCustomerInfo()
        )

        val customerInfo = LinkConfiguration.CustomerInfo(
            name = configuration.defaultBillingDetails?.name,
            email = customerEmail,
            phone = customerPhone,
            billingCountryCode = configuration.defaultBillingDetails?.address?.country,
        )

        val cardBrandChoice = elementsSession.cardBrandChoice?.let { cardBrandChoice ->
            LinkConfiguration.CardBrandChoice(
                eligible = cardBrandChoice.eligible,
                preferredNetworks = cardBrandChoice.preferredNetworks,
            )
        }

        val linkConfiguration = LinkConfiguration(
            stripeIntent = elementsSession.stripeIntent,
            merchantName = configuration.merchantDisplayName,
            merchantCountryCode = elementsSession.merchantCountry,
            customerInfo = customerInfo,
            shippingDetails = shippingDetails?.takeIf { it.isCheckboxSelected == true },
            passthroughModeEnabled = elementsSession.linkPassthroughModeEnabled,
            cardBrandChoice = cardBrandChoice,
            cardBrandFilter = cardBrandFilter,
            financialConnectionsAvailability = GetFinancialConnectionsAvailability(elementsSession = elementsSession),
            flags = elementsSession.linkFlags,
            useAttestationEndpointsForLink = elementsSession.useAttestationEndpointsForLink,
            suppress2faModal = elementsSession.suppressLink2faModal,
            disableRuxInFlowController = elementsSession.disableRuxInFlowController,
            enableDisplayableDefaultValuesInEce = elementsSession.linkEnableDisplayableDefaultValuesInEce,
            linkSignUpOptInFeatureEnabled = elementsSession.linkSignUpOptInFeatureEnabled,
            linkSignUpOptInInitialValue = elementsSession.linkSignUpOptInInitialValue,
            elementsSessionId = elementsSession.elementsSessionId,
            initializationMode = initializationMode,
            linkMode = elementsSession.linkSettings?.linkMode,
            billingDetailsCollectionConfiguration = configuration.billingDetailsCollectionConfiguration,
            defaultBillingDetails = configuration.defaultBillingDetails,
            allowDefaultOptIn = elementsSession.allowLinkDefaultOptIn,
            googlePlacesApiKey = configuration.googlePlacesApiKey,
            collectMissingBillingDetailsForExistingPaymentMethods =
            configuration.link.collectMissingBillingDetailsForExistingPaymentMethods,
            allowUserEmailEdits = configuration.link.allowUserEmailEdits,
            skipWalletInFlowController = elementsSession.linkMobileSkipWalletInFlowController,
            customerId = elementsSession.customer?.session?.customerId,
            linkAppearance = linkAppearance,
            saveConsentBehavior = elementsSession.toPaymentSheetSaveConsentBehavior(),
            forceSetupFutureUseBehaviorAndNewMandate = elementsSession
                .flags[ELEMENTS_MOBILE_FORCE_SETUP_FUTURE_USE_BEHAVIOR_AND_NEW_MANDATE_TEXT] == true,
        )

        // CBF isn't currently supported in the web flow.
        val useWebLink = !linkGateFactory.create(linkConfiguration).useNativeLink
        if (isCardBrandFilteringRequired && useWebLink) {
            return null
        }

        return linkConfiguration
    }

    private suspend fun isGooglePayReady(
        configuration: CommonConfiguration,
        elementsSession: ElementsSession,
    ): Boolean {
        if (!elementsSession.isGooglePayEnabled) {
            userFacingLogger.logWarningWithoutPii(
                "Google Pay is not enabled for this session."
            )
        } else if (configuration.googlePay == null) {
            userFacingLogger.logWarningWithoutPii(
                "GooglePayConfiguration is not set."
            )
        } else if (!configuration.isGooglePayReady()) {
            @Suppress("MaxLineLength")
            userFacingLogger.logWarningWithoutPii(
                """
                    Google Pay API check failed.
                    Possible reasons:
                    - Google Play service is not available on this device.
                    - Google account is not signed in on this device.
                    See https://developers.google.com/android/reference/com/google/android/gms/wallet/PaymentsClient#public-taskboolean-isreadytopay-isreadytopayrequest-request for more details.
                """.trimIndent()
            )
        }
        return elementsSession.isGooglePayEnabled && configuration.isGooglePayReady()
    }

    private suspend fun CommonConfiguration.isGooglePayReady(): Boolean {
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

    private suspend fun retrieveInitialPaymentSelection(
        savedSelection: Deferred<SavedSelection>,
        metadata: PaymentMethodMetadata,
        customer: CustomerState?,
        isGooglePayReady: Boolean,
        isUsingWalletButtons: Boolean,
    ): PaymentSelection? {
        val isDefaultPaymentMethodEnabled = metadata.customerMetadata?.isPaymentMethodSetAsDefaultEnabled ?: false
        val primaryPaymentSelection = if (isDefaultPaymentMethodEnabled) {
            customer?.paymentMethods?.firstOrNull {
                customer.defaultPaymentMethodId != null && it.id == customer.defaultPaymentMethodId
            }?.toPaymentSelection()
        } else {
            when (val selection = savedSelection.await()) {
                is SavedSelection.GooglePay -> PaymentSelection.GooglePay.takeIf {
                    !isUsingWalletButtons && isGooglePayReady
                }
                is SavedSelection.Link -> PaymentSelection.Link().takeIf {
                    !isUsingWalletButtons
                }
                is SavedSelection.PaymentMethod -> {
                    val customerPaymentMethod = customer?.paymentMethods?.find { it.id == selection.id }
                    if (customerPaymentMethod != null) {
                        customerPaymentMethod.toPaymentSelection()
                    } else if (selection.isLinkOrigin) {
                        // The payment method wasn't attached to the customer, but is of Link origin. Offer
                        // Link as the initial payment selection.
                        PaymentSelection.Link().takeIf {
                            !isUsingWalletButtons
                        }
                    } else {
                        null
                    }
                }
                is SavedSelection.None -> null
            }
        }

        return primaryPaymentSelection
            ?: customer?.paymentMethods?.firstOrNull()?.toPaymentSelection()
            ?: PaymentSelection.GooglePay.takeIf {
                !isUsingWalletButtons && isGooglePayReady
            }
    }

    private suspend fun retrieveSavedSelection(
        configuration: CommonConfiguration,
        isGooglePayReady: Boolean,
        elementsSession: ElementsSession
    ): SavedSelection {
        return retrieveSavedSelection(
            configuration = configuration,
            isGooglePayReady = isGooglePayReady,
            isLinkAvailable = elementsSession.isLinkEnabled,
        )
    }

    private suspend fun retrieveSavedPaymentMethodSelection(
        configuration: CommonConfiguration,
    ): SavedSelection.PaymentMethod? {
        return when (configuration.customer?.accessType) {
            is PaymentSheet.CustomerAccessType.CustomerSession -> {
                /*
                 * For `CustomerSession`, `v1/elements/sessions` needs to know the client-side saved default payment
                 * method ID to ensure it is properly returned by the API when performing payment method deduping. We
                 * only care about the Stripe `payment_method` id when deduping since `Google Pay` and `Link` are
                 * locally defined LPMs and not recognized by the `v1/elements/sessions` API. We don't need to know
                 * if they are ready and can safely set them to `false`.
                 */
                retrieveSavedSelection(
                    configuration = configuration,
                    isGooglePayReady = false,
                    isLinkAvailable = false,
                ) as? SavedSelection.PaymentMethod
            }
            is PaymentSheet.CustomerAccessType.LegacyCustomerEphemeralKey,
            null -> null
        }
    }

    private suspend fun retrieveSavedSelection(
        configuration: CommonConfiguration,
        isGooglePayReady: Boolean,
        isLinkAvailable: Boolean,
    ): SavedSelection {
        val customerConfiguration = configuration.customer
        val prefsRepository = prefsRepositoryFactory(customerConfiguration)

        return prefsRepository.getSavedSelection(
            isGooglePayAvailable = isGooglePayReady,
            isLinkAvailable = isLinkAvailable,
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
        return metadata.supportedPaymentMethodTypes().isNotEmpty()
    }

    private fun reportSuccessfulLoad(
        elementsSession: ElementsSession,
        state: PaymentElementLoader.State,
        isReloadingAfterProcessDeath: Boolean,
        isGooglePaySupported: Boolean,
        linkDisplay: PaymentSheet.LinkConfiguration.Display,
        initializationMode: PaymentElementLoader.InitializationMode,
        customerInfo: CustomerInfo?,
        paymentMethodMetadata: PaymentMethodMetadata,
    ) {
        elementsSession.sessionsError?.let { sessionsError ->
            eventReporter.onElementsSessionLoadFailed(sessionsError)
        }

        val treatValidationErrorAsFailure = !state.stripeIntent.isConfirmed || isReloadingAfterProcessDeath

        val hasDefaultPaymentMethod: Boolean?
        val setAsDefaultEnabled: Boolean?

        if (customerInfo is CustomerInfo.CustomerSession) {
            hasDefaultPaymentMethod = elementsSession.customer?.defaultPaymentMethod != null
            setAsDefaultEnabled = paymentMethodMetadata.customerMetadata?.isPaymentMethodSetAsDefaultEnabled == true
        } else {
            hasDefaultPaymentMethod = null
            setAsDefaultEnabled = null
        }

        if (state.validationError != null && treatValidationErrorAsFailure) {
            eventReporter.onLoadFailed(state.validationError)
        } else {
            eventReporter.onLoadSucceeded(
                linkEnabled = state.paymentMethodMetadata.linkState != null,
                linkMode = elementsSession.linkSettings?.linkMode,
                googlePaySupported = isGooglePaySupported,
                linkDisplay = linkDisplay,
                currency = elementsSession.stripeIntent.currency,
                paymentSelection = state.paymentSelection,
                initializationMode = initializationMode,
                financialConnectionsAvailability = GetFinancialConnectionsAvailability(elementsSession),
                orderedLpms = state.paymentMethodMetadata.sortedSupportedPaymentMethods().map { it.code },
                requireCvcRecollection = cvcRecollectionHandler.cvcRecollectionEnabled(
                    state.paymentMethodMetadata.stripeIntent,
                    initializationMode
                ),
                hasDefaultPaymentMethod = hasDefaultPaymentMethod,
                setAsDefaultEnabled = setAsDefaultEnabled,
                setupFutureUsage = elementsSession.stripeIntent.setupFutureUsage(),
                paymentMethodOptionsSetupFutureUsage = elementsSession.stripeIntent
                    .paymentMethodOptionsSetupFutureUsageMap()
            )
        }
    }

    private fun reportFailedLoad(
        error: Throwable,
    ) {
        logger.error("Failure loading PaymentSheetState", error)
        eventReporter.onLoadFailed(error)
    }

    private fun logIfMissingExternalPaymentMethods(
        requestedExternalPaymentMethods: List<String>?,
        actualExternalPaymentMethods: List<ExternalPaymentMethodSpec>?,
    ) {
        if (requestedExternalPaymentMethods.isNullOrEmpty()) {
            return
        }
        val actualExternalPaymentMethodTypes = actualExternalPaymentMethods?.map { it.type }
        for (requestedExternalPaymentMethod in requestedExternalPaymentMethods) {
            if (actualExternalPaymentMethodTypes == null || !actualExternalPaymentMethodTypes.contains(
                    requestedExternalPaymentMethod
                )
            ) {
                userFacingLogger.logWarningWithoutPii(
                    "Requested external payment method $requestedExternalPaymentMethod is not supported. View all " +
                        "available external payment methods here: " +
                        "https://docs.stripe.com/payments/external-payment-methods?platform=android#" +
                        "available-external-payment-methods"
                )
            }
        }
    }

    private fun logCustomPaymentMethodErrors(
        customPaymentMethods: List<ElementsSession.CustomPaymentMethod>,
    ) {
        if (customPaymentMethods.isEmpty()) {
            return
        }

        val unavailableCustomPaymentMethods = customPaymentMethods
            .filterIsInstance<ElementsSession.CustomPaymentMethod.Unavailable>()

        for (unavailableCustomPaymentMethod in unavailableCustomPaymentMethods) {
            userFacingLogger.logWarningWithoutPii(
                "Requested custom payment method ${unavailableCustomPaymentMethod.type} contained an " +
                    "error \"${unavailableCustomPaymentMethod.error}\"!"
            )
        }
    }

    private sealed interface CustomerInfo {
        val id: String
        val ephemeralKeySecret: String

        data class CustomerSession(
            val elementsSessionCustomer: ElementsSession.Customer,
            val customerSessionClientSecret: String,
        ) : CustomerInfo {
            override val id: String = elementsSessionCustomer.session.customerId
            override val ephemeralKeySecret: String = elementsSessionCustomer.session.apiKey
        }

        data class Legacy(
            val customerConfig: PaymentSheet.CustomerConfiguration,
            val accessType: PaymentSheet.CustomerAccessType.LegacyCustomerEphemeralKey
        ) : CustomerInfo {
            override val id: String = customerConfig.id
            override val ephemeralKeySecret: String = accessType.ephemeralKeySecret
        }
    }

    private fun CustomerInfo.toCustomerInfo() = CustomerRepository.CustomerInfo(
        id = id,
        ephemeralKeySecret = ephemeralKeySecret,
        customerSessionClientSecret = (this as? CustomerInfo.CustomerSession)?.customerSessionClientSecret,
    )
}

private suspend fun List<PaymentMethod>.withDefaultPaymentMethodOrLastUsedPaymentMethodFirst(
    savedSelection: Deferred<SavedSelection>,
    isPaymentMethodSetAsDefaultEnabled: Boolean,
    defaultPaymentMethodId: String?,
): List<PaymentMethod> {
    val primaryPaymentMethodId = if (isPaymentMethodSetAsDefaultEnabled) {
        defaultPaymentMethodId
    } else {
        (savedSelection.await() as? SavedSelection.PaymentMethod)?.id
    }

    val primaryPaymentMethod = this.firstOrNull { it.id == primaryPaymentMethodId }

    return primaryPaymentMethod?.let {
        listOf(primaryPaymentMethod) + (this - primaryPaymentMethod)
    } ?: this
}

private fun PaymentMethod.toPaymentSelection(): PaymentSelection.Saved {
    return PaymentSelection.Saved(this)
}

private fun StripeIntent.paymentMethodOptionsSetupFutureUsageMap(): Boolean {
    return getPaymentMethodOptions().any { (_, value) ->
        (value as? Map<*, *>)?.get("setup_future_usage") != null
    }
}

private fun StripeIntent.setupFutureUsage(): StripeIntent.Usage? = when (this) {
    is SetupIntent -> usage
    is PaymentIntent -> setupFutureUsage
}
