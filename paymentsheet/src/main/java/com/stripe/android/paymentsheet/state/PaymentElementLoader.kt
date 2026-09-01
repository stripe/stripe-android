package com.stripe.android.paymentsheet.state

import android.os.Parcelable
import androidx.annotation.VisibleForTesting
import com.stripe.android.DefaultCardBrandFilter
import com.stripe.android.DefaultCardFundingFilter
import com.stripe.android.PaymentConfiguration
import com.stripe.android.SharedPaymentTokenSessionPreview
import com.stripe.android.common.analytics.experiment.LogFcLiteExperiment
import com.stripe.android.common.analytics.experiment.LogLinkHoldbackExperiment
import com.stripe.android.common.analytics.experiment.PaymentMethodMessagePromotionsExperimentHandler
import com.stripe.android.common.coroutines.runCatching
import com.stripe.android.common.model.CommonConfiguration
import com.stripe.android.common.model.asCommonConfiguration
import com.stripe.android.core.Logger
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.utils.DurationProvider
import com.stripe.android.core.utils.FeatureFlag
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.core.utils.UserFacingLogger
import com.stripe.android.googlepaylauncher.GooglePayEnvironment
import com.stripe.android.googlepaylauncher.injection.GooglePayRepositoryFactory
import com.stripe.android.link.LinkController
import com.stripe.android.lpmfoundations.luxe.LpmRepository
import com.stripe.android.lpmfoundations.paymentmethod.AnalyticsMetadata
import com.stripe.android.lpmfoundations.paymentmethod.CustomerMetadata
import com.stripe.android.lpmfoundations.paymentmethod.IntegrationMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.create
import com.stripe.android.model.ClientAttributionMetadata
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.ElementsSession.ExperimentAssignment
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackIdentifier
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackReferences
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbacks
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheet.IntentConfiguration
import com.stripe.android.paymentsheet.PaymentSheet.PaymentMethodLayout
import com.stripe.android.paymentsheet.PrefsRepository
import com.stripe.android.paymentsheet.analytics.LoadingEventReporter
import com.stripe.android.paymentsheet.model.PaymentIntentClientSecret
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.SavedSelection
import com.stripe.android.paymentsheet.model.SetupIntentClientSecret
import com.stripe.android.paymentsheet.model.validate
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.CustomerRepository
import com.stripe.android.paymentsheet.repositories.PaymentMethodMessagePromotionsHelper
import com.stripe.android.ui.core.elements.ExternalPaymentMethodSpec
import com.stripe.android.ui.core.elements.ExternalPaymentMethodsRepository
import com.stripe.attestation.IntegrityRequestManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

/**
 * Loads the information necessary to display [PaymentSheet], either directly or via
 * [PaymentSheet.FlowController].
 */
internal interface PaymentElementLoader {

    suspend fun load(
        initializationMode: InitializationMode,
        integrationConfiguration: Configuration,
        metadata: Metadata,
    ): Result<State>

    suspend fun loadForCheckoutSession(
        initializationMode: InitializationMode.CheckoutSession,
        integrationConfiguration: Configuration,
        metadata: Metadata,
    ): Result<State> = load(
        initializationMode = initializationMode,
        integrationConfiguration = integrationConfiguration,
        metadata = metadata,
    )

    data class Metadata(
        val isReloadingAfterProcessDeath: Boolean = false,
        val initializedViaCompose: Boolean,
    )

    sealed interface Configuration {
        val commonConfiguration: CommonConfiguration

        data class PaymentSheet(
            val configuration: PaymentSheet.Configuration,
        ) : Configuration {
            override val commonConfiguration: CommonConfiguration = configuration.asCommonConfiguration()
        }

        data class Embedded(
            val isRowSelectionImmediateAction: Boolean,
            val configuration: EmbeddedPaymentElement.Configuration,
            val paymentMethodLayout: PaymentMethodLayout,
        ) : Configuration {
            override val commonConfiguration: CommonConfiguration = configuration.asCommonConfiguration()
        }

        data class CryptoOnramp(
            val configuration: LinkController.Configuration.State
        ) : Configuration {
            override val commonConfiguration: CommonConfiguration = configuration.asCommonConfiguration()
        }

        data class StandaloneLink(
            val configuration: LinkController.Configuration.State
        ) : Configuration {
            override val commonConfiguration: CommonConfiguration = configuration.asCommonConfiguration()
        }
    }

    sealed class InitializationMode : Parcelable {
        abstract fun validate()
        abstract fun integrationMetadata(paymentElementCallbacks: PaymentElementCallbacks?): IntegrationMetadata

        fun walletsDisabledReason(): WalletsDisabledReason? {
            val shouldDisable = (this as? CheckoutSession)
                ?.checkoutSessionResponse
                ?.collectsTaxFromBillingAddress == true

            return if (shouldDisable) {
                WalletsDisabledReason.AutomaticTaxBillingAddress
            } else {
                null
            }
        }

        enum class WalletsDisabledReason {
            AutomaticTaxBillingAddress;
        }

        @Parcelize
        data class PaymentIntent(
            val clientSecret: String,
        ) : InitializationMode() {

            override fun validate() {
                PaymentIntentClientSecret(clientSecret).validate()
            }

            override fun integrationMetadata(paymentElementCallbacks: PaymentElementCallbacks?): IntegrationMetadata {
                return IntegrationMetadata.IntentFirst(clientSecret)
            }
        }

        @Parcelize
        data class SetupIntent(
            val clientSecret: String,
        ) : InitializationMode() {

            override fun validate() {
                SetupIntentClientSecret(clientSecret).validate()
            }

            override fun integrationMetadata(paymentElementCallbacks: PaymentElementCallbacks?): IntegrationMetadata {
                return IntegrationMetadata.IntentFirst(clientSecret)
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

            @OptIn(SharedPaymentTokenSessionPreview::class)
            override fun integrationMetadata(paymentElementCallbacks: PaymentElementCallbacks?): IntegrationMetadata {
                return when {
                    paymentElementCallbacks?.preparePaymentMethodHandler != null -> {
                        IntegrationMetadata.DeferredIntent.WithSharedPaymentToken(intentConfiguration)
                    }
                    paymentElementCallbacks?.createIntentWithConfirmationTokenCallback != null -> {
                        IntegrationMetadata.DeferredIntent.WithConfirmationToken(intentConfiguration)
                    }
                    paymentElementCallbacks?.createIntentCallback != null -> {
                        IntegrationMetadata.DeferredIntent.WithPaymentMethod(intentConfiguration)
                    }
                    else -> throw IllegalStateException("No callback for deferred intent.")
                }
            }
        }

        @Parcelize
        data class CryptoOnramp(
            val paymentMethodTypes: List<String>? = null,
        ) : InitializationMode() {
            override fun validate() {
                // Nothing to validate.
            }

            override fun integrationMetadata(paymentElementCallbacks: PaymentElementCallbacks?): IntegrationMetadata {
                return IntegrationMetadata.CryptoOnramp
            }
        }

        @Parcelize
        data class StandaloneLink(
            val paymentMethodTypes: List<String>? = null,
        ) : InitializationMode() {
            override fun validate() {
                // Nothing to validate.
            }

            override fun integrationMetadata(paymentElementCallbacks: PaymentElementCallbacks?): IntegrationMetadata {
                return IntegrationMetadata.StandaloneLink
            }
        }

        @Parcelize
        data class CheckoutSession(
            val instancesKey: String,
            val checkoutSessionResponse: CheckoutSessionResponse,
        ) : InitializationMode() {
            override fun validate() {
                // Nothing to validate — the response was already loaded successfully.
            }

            override fun integrationMetadata(paymentElementCallbacks: PaymentElementCallbacks?): IntegrationMetadata {
                return IntegrationMetadata.CheckoutSession(
                    id = checkoutSessionResponse.id,
                    instancesKey = instancesKey,
                    checkoutSessionResponse = checkoutSessionResponse,
                )
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
    private val googlePayRepositoryFactory: GooglePayRepositoryFactory,
    private val lpmRepository: LpmRepository,
    private val logger: Logger,
    private val eventReporter: LoadingEventReporter,
    private val errorReporter: ErrorReporter,
    @IOContext private val workContext: CoroutineContext,
    private val createLinkState: CreateLinkState,
    private val logLinkHoldbackExperiment: LogLinkHoldbackExperiment,
    private val logFcLiteExperiment: LogFcLiteExperiment,
    private val externalPaymentMethodsRepository: ExternalPaymentMethodsRepository,
    private val userFacingLogger: UserFacingLogger,
    private val integrityRequestManager: IntegrityRequestManager,
    private val tapToAddConnectionStarter: TapToAddConnectionStarter,
    private val paymentConfiguration: Provider<PaymentConfiguration>,
    @PaymentElementCallbackIdentifier private val paymentElementCallbackIdentifier: String,
    private val analyticsMetadataFactory: AnalyticsMetadataFactory,
    private val customerRepository: CustomerRepository,
    private val createCustomerState: CreateCustomerState,
    private val checkoutSessionLoader: CheckoutSessionLoader,
    private val elementsSessionLoader: ElementsSessionLoader,
    private val createCustomerMetadata: CreateCustomerMetadata,
    private val paymentMethodMessagePromotionsHelper: PaymentMethodMessagePromotionsHelper,
    private val tapToAddAvailabilityFactory: TapToAddAvailabilityFactory,
    private val durationProvider: DurationProvider,
    private val paymentMethodMessagePromotionsExperimentHandler: PaymentMethodMessagePromotionsExperimentHandler,
) : PaymentElementLoader {

    fun interface AnalyticsMetadataFactory {
        fun create(
            initializationMode: PaymentElementLoader.InitializationMode,
            integrationMetadata: IntegrationMetadata,
            elementsSession: ElementsSession,
            isGooglePaySupported: Boolean,
            configuration: PaymentElementLoader.Configuration,
            customerMetadata: CustomerMetadata?,
            linkStateResult: LinkStateResult?,
            isTapToAddAvailable: Boolean,
        ): AnalyticsMetadata
    }

    override suspend fun loadForCheckoutSession(
        initializationMode: PaymentElementLoader.InitializationMode.CheckoutSession,
        integrationConfiguration: PaymentElementLoader.Configuration,
        metadata: PaymentElementLoader.Metadata,
    ): Result<PaymentElementLoader.State> = loadInternal(
        initializationMode = initializationMode,
        integrationConfiguration = integrationConfiguration,
        metadata = metadata,
    )

    override suspend fun load(
        initializationMode: PaymentElementLoader.InitializationMode,
        integrationConfiguration: PaymentElementLoader.Configuration,
        metadata: PaymentElementLoader.Metadata,
    ): Result<PaymentElementLoader.State> = loadInternal(
        initializationMode = initializationMode,
        integrationConfiguration = integrationConfiguration,
        metadata = metadata,
    )

    private suspend fun loadInternal(
        initializationMode: PaymentElementLoader.InitializationMode,
        integrationConfiguration: PaymentElementLoader.Configuration,
        metadata: PaymentElementLoader.Metadata,
    ): Result<PaymentElementLoader.State> = workContext.runCatching(::reportFailedLoad) {
        val validatedConfiguration = validateConfiguration(
            initializationMode = initializationMode,
            integrationConfiguration = integrationConfiguration,
        )
        val initialLoadResult = loadInitialData(
            initializationMode = initializationMode,
            validatedConfiguration = validatedConfiguration,
            metadata = metadata,
        )
        val paymentMethodMetadataResult = createLinkStateAndPaymentMethodMetadata(
            initializationMode = initializationMode,
            integrationConfiguration = integrationConfiguration,
            validatedConfiguration = validatedConfiguration,
            initialLoadResult = initialLoadResult,
        )
        val loaderStateResult = createLoaderState(
            initializationMode = initializationMode,
            validatedConfiguration = validatedConfiguration,
            initialLoadResult = initialLoadResult,
            paymentMethodMetadataResult = paymentMethodMetadataResult,
        )
        completeLoading(
            metadata = metadata,
            initialLoadResult = initialLoadResult,
            loaderStateResult = loaderStateResult,
        ).state
    }

    private fun validateConfiguration(
        initializationMode: PaymentElementLoader.InitializationMode,
        integrationConfiguration: PaymentElementLoader.Configuration,
    ): ValidatedConfigurationResult {
        val configuration = integrationConfiguration.commonConfiguration
        initializationMode.validate()
        configuration.validate(
            initializationMode = initializationMode,
            isLiveMode = paymentConfiguration.get().isLiveMode(),
            callbackIdentifier = paymentElementCallbackIdentifier,
            isTapToAddSupported = tapToAddConnectionStarter.isSupported,
        )
        return ValidatedConfigurationResult(configuration)
    }

    private suspend fun CoroutineScope.loadInitialData(
        initializationMode: PaymentElementLoader.InitializationMode,
        validatedConfiguration: ValidatedConfigurationResult,
        metadata: PaymentElementLoader.Metadata,
    ): InitialLoadResult {
        val configuration = validatedConfiguration.configuration
        eventReporter.onLoadStarted(metadata.initializedViaCompose)
        tapToAddConnectionStarter.start(configuration)

        val googlePayChecks = startGooglePayChecks(configuration)

        val prefetchedPaymentMethods =
            if (initializationMode is PaymentElementLoader.InitializationMode.CheckoutSession) {
                null
            } else {
                prefetchPaymentMethodsForLegacyEphemeralKey(configuration)
            }

        val savedPaymentMethodSelection = if (
            initializationMode is PaymentElementLoader.InitializationMode.CheckoutSession
        ) {
            null
        } else {
            retrieveSavedPaymentMethodSelection(configuration)
        }
        val elementsSession = loadSession(
            initializationMode = initializationMode,
            configuration = configuration,
            savedPaymentMethodSelection = savedPaymentMethodSelection,
        )

        // Preemptively prepare Integrity asynchronously if needed, as warm up can take
        // a few seconds.
        if (elementsSession.shouldWarmUpIntegrity()) {
            launch { integrityRequestManager.prepare() }
        }

        fetchPaymentMethodMessaging(elementsSession)

        val isGooglePayReady = isGooglePayReady(
            configuration = configuration,
            elementsSession = elementsSession,
            initializationMode = initializationMode,
            isGooglePaySupportedByConfiguration = googlePayChecks.isSupportedByConfiguration,
        )

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

        val customerMetadata = createCustomerMetadata(
            initializationMode = initializationMode,
            configuration = configuration,
            elementsSession = elementsSession,
        )

        return InitialLoadResult(
            elementsSession = elementsSession,
            isGooglePaySupportedOnDevice = googlePayChecks.isSupportedOnDevice,
            isGooglePayReady = isGooglePayReady,
            savedSelection = savedSelection,
            clientAttributionMetadata = clientAttributionMetadata,
            customerMetadata = customerMetadata,
            prefetchedPaymentMethods = prefetchedPaymentMethods,
        )
    }

    private fun CoroutineScope.startGooglePayChecks(
        configuration: CommonConfiguration,
    ): GooglePayChecksResult {
        // Give immediately available results a chance to complete before later load work checks isCompleted.
        val isSupportedOnDevice = async(start = CoroutineStart.UNDISPATCHED) {
            durationProvider.measureDuration(
                DurationProvider.Key.PaymentSheetLoadIsGooglePaySupported
            ) {
                isGooglePaySupportedOnDevice()
            }
        }
        val isSupportedByConfiguration = async {
            durationProvider.measureDuration(
                DurationProvider.Key.PaymentSheetLoadIsGooglePayReady
            ) {
                configuration.isGooglePayReady()
            }
        }

        return GooglePayChecksResult(
            isSupportedOnDevice = isSupportedOnDevice,
            isSupportedByConfiguration = isSupportedByConfiguration,
        )
    }

    private fun CoroutineScope.createLinkStateAndPaymentMethodMetadata(
        initializationMode: PaymentElementLoader.InitializationMode,
        integrationConfiguration: PaymentElementLoader.Configuration,
        validatedConfiguration: ValidatedConfigurationResult,
        initialLoadResult: InitialLoadResult,
    ): PaymentMethodMetadataResult {
        val configuration = validatedConfiguration.configuration
        val elementsSession = initialLoadResult.elementsSession
        val linkState = async {
            durationProvider.measureDuration(DurationProvider.Key.PaymentSheetLoadCreateLinkState) {
                createLinkState(
                    elementsSession = elementsSession,
                    configuration = configuration,
                    initializationMode = initializationMode,
                    customerMetadata = initialLoadResult.customerMetadata,
                    clientAttributionMetadata = initialLoadResult.clientAttributionMetadata,
                )
            }
        }

        val paymentMethodMetadata = async {
            val linkStateResult = linkState.await()
            val isGooglePaySupported = initialLoadResult.isGooglePaySupportedOnDevice.completeResultOrNull {
                errorReporter.report(ErrorReporter.ExpectedErrorEvent.GOOGLE_PAY_SKIPPED_DURING_LOAD)
            } ?: false

            durationProvider.measureDuration(DurationProvider.Key.PaymentSheetLoadComputePaymentMethodTypes) {
                createPaymentMethodMetadata(
                    integrationConfiguration = integrationConfiguration,
                    elementsSession = elementsSession,
                    configuration = configuration,
                    linkStateResult = linkStateResult,
                    isGooglePayReady = initialLoadResult.isGooglePayReady,
                    isGooglePaySupported = isGooglePaySupported,
                    initializationMode = initializationMode,
                    customerMetadata = initialLoadResult.customerMetadata,
                    clientAttributionMetadata = initialLoadResult.clientAttributionMetadata,
                )
            }
        }

        return PaymentMethodMetadataResult(paymentMethodMetadata)
    }

    private suspend fun CoroutineScope.createLoaderState(
        initializationMode: PaymentElementLoader.InitializationMode,
        validatedConfiguration: ValidatedConfigurationResult,
        initialLoadResult: InitialLoadResult,
        paymentMethodMetadataResult: PaymentMethodMetadataResult,
    ): LoaderStateResult {
        val configuration = validatedConfiguration.configuration
        val elementsSession = initialLoadResult.elementsSession
        val paymentMethodMetadata = paymentMethodMetadataResult.paymentMethodMetadata
        val customer = async {
            val paymentMethodMetadata = paymentMethodMetadata.await()

            durationProvider.measureDuration(DurationProvider.Key.PaymentSheetLoadCreateCustomerState) {
                createCustomerState(
                    initializationMode = initializationMode,
                    elementsSession = elementsSession,
                    metadata = paymentMethodMetadata,
                    savedSelection = initialLoadResult.savedSelection,
                    prefetchedPaymentMethods = initialLoadResult.prefetchedPaymentMethods,
                )
            }
        }

        val initialPaymentSelection = async {
            val paymentMethodMetadata = paymentMethodMetadata.await()
            val customer = customer.await()

            durationProvider.measureDuration(
                DurationProvider.Key.PaymentSheetLoadRetrieveInitialPaymentSelection
            ) {
                retrieveInitialPaymentSelection(
                    savedSelection = initialLoadResult.savedSelection,
                    metadata = paymentMethodMetadata,
                    customer = customer,
                    isGooglePayReady = initialLoadResult.isGooglePayReady,
                    isUsingWalletButtons = configuration.walletButtons?.willDisplayExternally ?: false
                )
            }
        }

        val stripeIntent = elementsSession.stripeIntent
        val pmMetadata = paymentMethodMetadata.await()

        warnUnactivatedIfNeeded(stripeIntent)

        if (!supportsIntent(pmMetadata)) {
            val requested = stripeIntent.paymentMethodTypes.joinToString(separator = ", ")
            throw PaymentSheetLoadingException.NoPaymentMethodTypesAvailable(requested)
        }

        return LoaderStateResult(
            state = PaymentElementLoader.State(
                config = configuration,
                customer = customer.await(),
                paymentSelection = initialPaymentSelection.await(),
                validationError = stripeIntent.validate(),
                paymentMethodMetadata = pmMetadata,
            )
        )
    }

    private fun completeLoading(
        metadata: PaymentElementLoader.Metadata,
        initialLoadResult: InitialLoadResult,
        loaderStateResult: LoaderStateResult,
    ): LoadCompletionResult {
        val elementsSession = initialLoadResult.elementsSession
        val state = loaderStateResult.state
        logExperimentExposures(
            elementsSession = elementsSession,
            state = state
        )

        logPaymentMethodMessagingExposure(state.paymentMethodMetadata)

        reportSuccessfulLoad(
            elementsSession = elementsSession,
            state = state,
            isReloadingAfterProcessDeath = metadata.isReloadingAfterProcessDeath,
            paymentMethodMetadata = state.paymentMethodMetadata,
        )

        return LoadCompletionResult(state)
    }

    private data class ValidatedConfigurationResult(
        val configuration: CommonConfiguration,
    )

    private data class InitialLoadResult(
        val elementsSession: ElementsSession,
        val isGooglePaySupportedOnDevice: Deferred<Boolean>,
        val isGooglePayReady: Boolean,
        val savedSelection: Deferred<SavedSelection>,
        val clientAttributionMetadata: ClientAttributionMetadata,
        val customerMetadata: CustomerMetadata?,
        val prefetchedPaymentMethods: PrefetchedPaymentMethods?,
    )

    private data class GooglePayChecksResult(
        val isSupportedOnDevice: Deferred<Boolean>,
        val isSupportedByConfiguration: Deferred<Boolean>,
    )

    private data class PaymentMethodMetadataResult(
        val paymentMethodMetadata: Deferred<PaymentMethodMetadata>,
    )

    private data class LoaderStateResult(
        val state: PaymentElementLoader.State,
    )

    private data class LoadCompletionResult(
        val state: PaymentElementLoader.State,
    )

    private fun CoroutineScope.prefetchPaymentMethodsForLegacyEphemeralKey(
        configuration: CommonConfiguration,
    ): PrefetchedPaymentMethods? {
        val customer = configuration.customer ?: return null
        val accessType = customer.accessType
        if (accessType !is PaymentSheet.CustomerAccessType.LegacyCustomerEphemeralKey) return null

        return async {
            durationProvider.measureDuration(DurationProvider.Key.PaymentSheetLoadPrefetchPMs) {
                customerRepository.getPaymentMethods(
                    customerId = customer.id,
                    ephemeralKeySecret = accessType.ephemeralKeySecret,
                    types = listOf(
                        PaymentMethod.Type.Card,
                        PaymentMethod.Type.SepaDebit,
                        PaymentMethod.Type.USBankAccount,
                    ), // These are the only payment method types we support as saved payment methods.
                    silentlyFail = paymentConfiguration.get().isLiveMode(),
                )
            }
        }
    }

    private suspend fun loadSession(
        initializationMode: PaymentElementLoader.InitializationMode,
        configuration: CommonConfiguration,
        savedPaymentMethodSelection: SavedSelection.PaymentMethod?,
    ): ElementsSession {
        return durationProvider.measureDuration(
            DurationProvider.Key.PaymentSheetLoadSessionLoad
        ) {
            if (initializationMode is PaymentElementLoader.InitializationMode.CheckoutSession) {
                checkoutSessionLoader(initializationMode)
            } else {
                elementsSessionLoader(
                    initializationMode = initializationMode,
                    configuration = configuration,
                    savedPaymentMethodSelection = savedPaymentMethodSelection,
                )
            }
        }
    }

    private fun ElementsSession.shouldWarmUpIntegrity(): Boolean = when {
        stripeIntent.isLiveMode -> useAttestationEndpointsForLink
        else -> when (FeatureFlags.nativeLinkAttestationEnabled.value) {
            FeatureFlag.Flag.Disabled -> false
            FeatureFlag.Flag.Enabled -> true
            FeatureFlag.Flag.NotSet -> useAttestationEndpointsForLink
        }
    }

    private fun logExperimentExposures(
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
        logFcLiteExperiment(elementsSession, state.paymentMethodMetadata)
    }

    private fun createPaymentMethodMetadata(
        integrationConfiguration: PaymentElementLoader.Configuration,
        elementsSession: ElementsSession,
        configuration: CommonConfiguration,
        linkStateResult: LinkStateResult,
        isGooglePayReady: Boolean,
        isGooglePaySupported: Boolean,
        initializationMode: PaymentElementLoader.InitializationMode,
        customerMetadata: CustomerMetadata?,
        clientAttributionMetadata: ClientAttributionMetadata,
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

        val integrationMetadata = initializationMode.integrationMetadata(
            paymentElementCallbacks = PaymentElementCallbackReferences[paymentElementCallbackIdentifier]
        )

        val isTapToAddAvailable = tapToAddAvailabilityFactory.isAvailable(elementsSession, customerMetadata)

        val analyticsMetadata = analyticsMetadataFactory.create(
            initializationMode = initializationMode,
            integrationMetadata = integrationMetadata,
            elementsSession = elementsSession,
            isGooglePaySupported = isGooglePaySupported,
            configuration = integrationConfiguration,
            customerMetadata = customerMetadata,
            linkStateResult = linkStateResult,
            isTapToAddAvailable = isTapToAddAvailable,
        )

        val paymentMethodLayout = getPaymentMethodLayout(
            integrationConfiguration = integrationConfiguration,
            elementsSession = elementsSession,
        )

        val paymentMethodMetadata = PaymentMethodMetadata.createForPaymentElement(
            elementsSession = elementsSession,
            configuration = configuration,
            sharedDataSpecs = sharedDataSpecsResult.sharedDataSpecs,
            externalPaymentMethodSpecs = externalPaymentMethodSpecs,
            isGooglePayReady = isGooglePayReady,
            linkStateResult = linkStateResult,
            customerMetadata = customerMetadata,
            initializationMode = initializationMode,
            clientAttributionMetadata = clientAttributionMetadata,
            integrationMetadata = integrationMetadata,
            analyticsMetadata = analyticsMetadata,
            isTapToAddAvailable = isTapToAddAvailable,
            paymentMethodLayout = paymentMethodLayout,
        )

        return paymentMethodMetadata
    }

    private fun getPaymentMethodLayout(
        integrationConfiguration: PaymentElementLoader.Configuration,
        elementsSession: ElementsSession,
    ): PaymentMethodLayout {
        return when (integrationConfiguration) {
            is PaymentElementLoader.Configuration.CryptoOnramp,
            is PaymentElementLoader.Configuration.StandaloneLink -> PaymentMethodLayout.Vertical
            is PaymentElementLoader.Configuration.Embedded ->
                if (
                    elementsSession.forceVerticalPaymentMethodLayout &&
                    integrationConfiguration.paymentMethodLayout == PaymentMethodLayout.Automatic
                ) {
                    PaymentMethodLayout.Vertical
                } else {
                    integrationConfiguration.paymentMethodLayout
                }
            is PaymentElementLoader.Configuration.PaymentSheet ->
                if (
                    elementsSession.forceVerticalPaymentMethodLayout &&
                    integrationConfiguration.configuration.paymentMethodLayout == PaymentMethodLayout.Automatic
                ) {
                    PaymentMethodLayout.Vertical
                } else {
                    integrationConfiguration.configuration.paymentMethodLayout
                }
        }
    }

    @VisibleForTesting
    internal suspend fun isGooglePayReady(
        configuration: CommonConfiguration,
        elementsSession: ElementsSession,
        initializationMode: PaymentElementLoader.InitializationMode,
        isGooglePaySupportedByConfiguration: Deferred<Boolean>,
    ): Boolean {
        val shouldDisableForAutomaticTaxBilling =
            (initializationMode as? PaymentElementLoader.InitializationMode.CheckoutSession)
            ?.checkoutSessionResponse
            ?.let { checkoutSessionResponse ->
                checkoutSessionResponse.automaticTaxEnabled &&
                    checkoutSessionResponse.taxAddressSource == CheckoutSessionResponse.TaxAddressSource.BILLING &&
                    configuration.defaultBillingDetails == null
            } == true

        if (!elementsSession.isGooglePayEnabled) {
            userFacingLogger.logWarningWithoutPii(
                "Google Pay is not enabled for this session."
            )
        } else if (configuration.googlePay == null) {
            userFacingLogger.logWarningWithoutPii(
                "GooglePayConfiguration is not set."
            )
        } else if (shouldDisableForAutomaticTaxBilling) {
            userFacingLogger.logWarningWithoutPii(
                "Google Pay is disabled because automatic tax is configured to use the billing address and no " +
                    "default billing address was provided."
            )
            return false
        } else if (!isGooglePaySupportedByConfiguration.await()) {
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
        return elementsSession.isGooglePayEnabled && isGooglePaySupportedByConfiguration.await()
    }

    // Default filters are used here because this only determines the ready state,
    // not what's presented to Google Pay. This check runs async before we fetch the
    // elements session, so using merchant-defined filters would add latency.
    private suspend fun isGooglePayReadyForEnvironment(environment: GooglePayEnvironment): Boolean {
        return googlePayRepositoryFactory(
            environment = environment,
            cardFundingFilter = DefaultCardFundingFilter,
            cardBrandFilter = DefaultCardBrandFilter
        ).isReady().first()
    }

    private suspend fun CommonConfiguration.isGooglePayReady(): Boolean {
        return googlePay?.environment?.let { environment ->
            isGooglePayReadyForEnvironment(
                when (environment) {
                    PaymentSheet.GooglePayConfiguration.Environment.Production ->
                        GooglePayEnvironment.Production
                    PaymentSheet.GooglePayConfiguration.Environment.Test ->
                        GooglePayEnvironment.Test
                }
            )
        } ?: false
    }

    private suspend fun isGooglePaySupportedOnDevice(): Boolean {
        return isGooglePayReadyForEnvironment(GooglePayEnvironment.Production)
    }

    @Suppress("CyclomaticComplexMethod")
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
                is SavedSelection.Link ->
                    metadata.linkState?.configuration?.linkBrand
                        ?.let { PaymentSelection.Link(brand = it) }
                        ?.takeIf { !isUsingWalletButtons }
                is SavedSelection.PaymentMethod -> {
                    val customerPaymentMethod = customer?.paymentMethods?.find { it.id == selection.id }
                    if (customerPaymentMethod != null) {
                        customerPaymentMethod.toPaymentSelection()
                    } else if (selection.isLinkOrigin) {
                        // The payment method wasn't attached to the customer, but is of Link origin. Offer
                        // Link as the initial payment selection.
                        metadata.linkState?.configuration?.linkBrand
                            ?.let { PaymentSelection.Link(brand = it) }
                            ?.takeIf { !isUsingWalletButtons }
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
        return durationProvider.measureDuration(
            DurationProvider.Key.PaymentSheetLoadRetrieveSavedPaymentMethodSelection
        ) {
            val customerConfiguration = configuration.customer
            val prefsRepository = prefsRepositoryFactory.create(customerConfiguration?.id)

            prefsRepository.getSavedSelection(
                isGooglePayAvailable = isGooglePayReady,
                isLinkAvailable = isLinkAvailable,
            )
        }
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
        paymentMethodMetadata: PaymentMethodMetadata,
    ) {
        elementsSession.sessionsError?.let { sessionsError ->
            eventReporter.onElementsSessionLoadFailed(sessionsError)
        }

        val treatValidationErrorAsFailure = !state.stripeIntent.isConfirmed || isReloadingAfterProcessDeath

        if (state.validationError != null && treatValidationErrorAsFailure) {
            eventReporter.onLoadFailed(state.validationError)
        } else {
            eventReporter.onLoadSucceeded(
                paymentSelection = state.paymentSelection,
                paymentMethodMetadata = paymentMethodMetadata,
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

    private fun fetchPaymentMethodMessaging(elementsSession: ElementsSession) {
        val variant = elementsSession.experimentsData?.experimentAssignments[
            ExperimentAssignment.OCS_MOBILE_PAYMENT_METHOD_MESSAGING_PROMOTIONS
        ] ?: return

        if (variant == "treatment") {
            paymentMethodMessagePromotionsHelper.fetchPromotionsAsync(elementsSession.stripeIntent)
        }
    }

    private fun logPaymentMethodMessagingExposure(metadata: PaymentMethodMetadata) {
        paymentMethodMessagePromotionsExperimentHandler.logExposure(metadata)
    }
}

private fun PaymentMethod.toPaymentSelection(): PaymentSelection.Saved {
    return PaymentSelection.Saved(this)
}

private suspend fun <T> Deferred<T>.completeResultOrNull(
    skippedCallback: () -> Unit,
): T? = if (isCompleted) {
    await()
} else {
    skippedCallback()
    null
}
