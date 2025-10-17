package com.stripe.android.paymentsheet.state

import android.os.Parcelable
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
import com.stripe.android.lpmfoundations.luxe.LpmRepository
import com.stripe.android.lpmfoundations.paymentmethod.CustomerMetadata
import com.stripe.android.lpmfoundations.paymentmethod.CustomerMetadata.Permissions.Companion.createForPaymentSheetCustomerSession
import com.stripe.android.lpmfoundations.paymentmethod.CustomerMetadata.Permissions.Companion.createForPaymentSheetLegacyEphemeralKey
import com.stripe.android.lpmfoundations.paymentmethod.IS_PAYMENT_METHOD_SET_AS_DEFAULT_ENABLED_DEFAULT_VALUE
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentSheetCardBrandFilter
import com.stripe.android.lpmfoundations.paymentmethod.create
import com.stripe.android.model.ClientAttributionMetadata
import com.stripe.android.model.ElementsSession
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

    interface IntentFirst {
        val clientSecret: String
    }

    sealed class InitializationMode : Parcelable {
        abstract fun validate()

        @Parcelize
        data class PaymentIntent(
            override val clientSecret: String,
        ) : InitializationMode(), IntentFirst {

            override fun validate() {
                PaymentIntentClientSecret(clientSecret).validate()
            }
        }

        @Parcelize
        data class SetupIntent(
            override val clientSecret: String,
        ) : InitializationMode(), IntentFirst {

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
    private val prefsRepositoryFactory: PrefsRepository.Factory,
    private val googlePayRepositoryFactory: @JvmSuppressWildcards (GooglePayEnvironment) -> GooglePayRepository,
    private val elementsSessionRepository: ElementsSessionRepository,
    private val customerRepository: CustomerRepository,
    private val lpmRepository: LpmRepository,
    private val logger: Logger,
    private val eventReporter: EventReporter,
    private val errorReporter: ErrorReporter,
    @IOContext private val workContext: CoroutineContext,
    private val createLinkState: CreateLinkState,
    private val logLinkHoldbackExperiment: LogLinkHoldbackExperiment,
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
            link = configuration.link,
            countryOverride = configuration.userOverrideCountry,
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

        val clientAttributionMetadata = ClientAttributionMetadata.create(
            elementsSessionConfigId = elementsSession.elementsSessionConfigId,
            initializationMode = initializationMode,
            automaticPaymentMethodsEnabled = elementsSession.stripeIntent.automaticPaymentMethodsEnabled,
        )

        val linkState = async {
            createLinkState(
                elementsSession = elementsSession,
                configuration = configuration,
                customer = customerInfo?.toCustomerInfo(),
                initializationMode = initializationMode,
                clientAttributionMetadata = clientAttributionMetadata,
            )
        }

        val paymentMethodMetadata = async {
            createPaymentMethodMetadata(
                configuration = configuration,
                elementsSession = elementsSession,
                customerInfo = customerInfo,
                linkStateResult = linkState.await(),
                isGooglePayReady = isGooglePayReady,
                initializationMode = initializationMode,
                clientAttributionMetadata = clientAttributionMetadata,
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
            experimentAssignments = listOf(
                ElementsSession.ExperimentAssignment.LINK_GLOBAL_HOLD_BACK,
                ElementsSession.ExperimentAssignment.LINK_GLOBAL_HOLD_BACK_AA,
                ElementsSession.ExperimentAssignment.LINK_AB_TEST
            ),
            elementsSession = elementsSession,
            state = state
        )
    }

    private suspend fun retrieveElementsSession(
        initializationMode: PaymentElementLoader.InitializationMode,
        customer: PaymentSheet.CustomerConfiguration?,
        customPaymentMethods: List<PaymentSheet.CustomPaymentMethod>,
        externalPaymentMethods: List<String>,
        savedPaymentMethodSelectionId: String?,
        link: PaymentSheet.LinkConfiguration,
        countryOverride: String?,
    ): Result<ElementsSession> {
        return elementsSessionRepository.get(
            initializationMode = initializationMode,
            customer = customer,
            externalPaymentMethods = externalPaymentMethods,
            customPaymentMethods = customPaymentMethods,
            savedPaymentMethodSelectionId = savedPaymentMethodSelectionId,
            countryOverride = countryOverride,
            linkDisallowedFundingSourceCreation = link.disallowFundingSourceCreation,
        )
    }

    private fun createPaymentMethodMetadata(
        configuration: CommonConfiguration,
        elementsSession: ElementsSession,
        customerInfo: CustomerInfo?,
        linkStateResult: LinkStateResult,
        isGooglePayReady: Boolean,
        initializationMode: PaymentElementLoader.InitializationMode,
        clientAttributionMetadata: ClientAttributionMetadata?,
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
            linkStateResult = linkStateResult,
            customerMetadata = getCustomerMetadata(
                configuration = configuration,
                elementsSession = elementsSession,
                customerInfo = customerInfo,
            ),
            initializationMode = initializationMode,
            clientAttributionMetadata = clientAttributionMetadata,
        )
    }

    private fun getCustomerMetadata(
        configuration: CommonConfiguration,
        elementsSession: ElementsSession,
        customerInfo: CustomerInfo?,
    ): CustomerMetadata? {
        val customerId: String
        val ephemeralKeySecret: String
        val customerSessionClientSecret: String?

        when (customerInfo) {
            is CustomerInfo.CustomerSession -> {
                val customer = elementsSession.customer
                if (customer != null) {
                    customerId = customer.session.customerId
                    ephemeralKeySecret = customer.session.apiKey
                    customerSessionClientSecret = customerInfo.customerSessionClientSecret
                } else {
                    return null
                }
            }
            is CustomerInfo.Legacy -> {
                customerId = customerInfo.id
                ephemeralKeySecret = customerInfo.ephemeralKeySecret
                customerSessionClientSecret = null
            }
            null -> return null
        }

        return CustomerMetadata(
            id = customerId,
            ephemeralKeySecret = ephemeralKeySecret,
            customerSessionClientSecret = customerSessionClientSecret,
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
                )
            }
            is CustomerInfo.Legacy -> {
                CustomerState.createForLegacyEphemeralKey(
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
        val prefsRepository = prefsRepositoryFactory.create(customerConfiguration?.id)

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
            val linkState =
                state.paymentMethodMetadata.linkState
            val linkDisabledReasons =
                (state.paymentMethodMetadata.linkStateResult as? LinkDisabledState)?.linkDisabledReasons

            eventReporter.onLoadSucceeded(
                linkEnabled = linkState != null,
                linkMode = elementsSession.linkSettings?.linkMode,
                linkDisabledReasons = linkDisabledReasons,
                linkSignupDisabledReasons = linkState?.signupModeResult?.disabledReasons,
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
                    .paymentMethodOptionsSetupFutureUsageMap(),
                openCardScanAutomatically = state.paymentMethodMetadata.openCardScanAutomatically
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

    internal sealed interface CustomerInfo {
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
