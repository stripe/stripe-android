package com.stripe.android.paymentsheet.state

import com.stripe.android.common.coroutines.runCatching
import com.stripe.android.core.Logger
import com.stripe.android.core.exception.StripeException
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.utils.UserFacingLogger
import com.stripe.android.googlepaylauncher.GooglePayEnvironment
import com.stripe.android.googlepaylauncher.GooglePayRepository
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.account.LinkStore
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.link.ui.inline.LinkSignupMode
import com.stripe.android.lpmfoundations.luxe.LpmRepository
import com.stripe.android.lpmfoundations.luxe.isSaveForFutureUseValueChangeable
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.toPaymentSheetSaveConsentBehavior
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.StripeIntent
import com.stripe.android.payments.core.analytics.ErrorReporter
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
import com.stripe.android.ui.core.elements.ExternalPaymentMethodSpec
import com.stripe.android.ui.core.elements.ExternalPaymentMethodsRepository
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
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
        initializedViaCompose: Boolean,
    ): Result<PaymentSheetState.Full>
}

/**
 * A default implementation of [PaymentSheetLoader] used to load necessary information for
 * building [PaymentSheet]. See the linked flow diagram to understand how this implementation
 * loads [PaymentSheet] information based its provided initialization options.
 *
 * @see <a href="https://whimsical.com/paymentsheet-loading-flow-diagram-EwTmrwvNmhcD9B2PKuSu82/">Flow Diagram</a>
 */
@Singleton
internal class DefaultPaymentSheetLoader @Inject constructor(
    private val prefsRepositoryFactory: @JvmSuppressWildcards (PaymentSheet.CustomerConfiguration?) -> PrefsRepository,
    private val googlePayRepositoryFactory: @JvmSuppressWildcards (GooglePayEnvironment) -> GooglePayRepository,
    private val elementsSessionRepository: ElementsSessionRepository,
    private val customerRepository: CustomerRepository,
    private val lpmRepository: LpmRepository,
    private val logger: Logger,
    private val eventReporter: EventReporter,
    private val errorReporter: ErrorReporter,
    @IOContext private val workContext: CoroutineContext,
    private val accountStatusProvider: LinkAccountStatusProvider,
    private val linkStore: LinkStore,
    private val externalPaymentMethodsRepository: ExternalPaymentMethodsRepository,
    private val userFacingLogger: UserFacingLogger,
) : PaymentSheetLoader {

    @Suppress("LongMethod")
    override suspend fun load(
        initializationMode: PaymentSheet.InitializationMode,
        paymentSheetConfiguration: PaymentSheet.Configuration,
        isReloadingAfterProcessDeath: Boolean,
        initializedViaCompose: Boolean,
    ): Result<PaymentSheetState.Full> = workContext.runCatching(::reportFailedLoad) {
        eventReporter.onLoadStarted(initializedViaCompose)

        val savedPaymentMethodSelection = retrieveSavedPaymentMethodSelection(paymentSheetConfiguration)

        val elementsSession = retrieveElementsSession(
            initializationMode = initializationMode,
            customer = paymentSheetConfiguration.customer,
            externalPaymentMethods = paymentSheetConfiguration.externalPaymentMethods,
            defaultPaymentMethodId = savedPaymentMethodSelection?.id,
        ).getOrThrow()

        val isGooglePayReady = isGooglePayReady(paymentSheetConfiguration, elementsSession)

        val metadata = createPaymentMethodMetadata(
            paymentSheetConfiguration = paymentSheetConfiguration,
            elementsSession = elementsSession,
            isGooglePayReady = isGooglePayReady,
        )

        val savedSelection = async {
            retrieveSavedSelection(
                config = paymentSheetConfiguration,
                isGooglePayReady = isGooglePayReady,
                elementsSession = elementsSession
            )
        }

        val customer = async {
            createCustomerState(
                config = paymentSheetConfiguration,
                elementsSession = elementsSession,
                metadata = metadata,
                savedSelection = savedSelection,
            )
        }

        val initialPaymentSelection = async {
            retrieveInitialPaymentSelection(savedSelection, customer)
        }

        val linkState = async {
            createLinkState(
                config = paymentSheetConfiguration,
                elementsSession = elementsSession,
                customer = customer,
            )
        }

        val stripeIntent = elementsSession.stripeIntent

        warnUnactivatedIfNeeded(stripeIntent)

        if (!supportsIntent(metadata)) {
            val requested = stripeIntent.paymentMethodTypes.joinToString(separator = ", ")
            throw PaymentSheetLoadingException.NoPaymentMethodTypesAvailable(requested)
        }

        val state = PaymentSheetState.Full(
            config = paymentSheetConfiguration,
            customer = customer.await(),
            linkState = linkState.await(),
            paymentSelection = initialPaymentSelection.await(),
            validationError = stripeIntent.validate(),
            paymentMethodMetadata = metadata,
        )

        reportSuccessfulLoad(
            elementsSession = elementsSession,
            state = state,
            isReloadingAfterProcessDeath = isReloadingAfterProcessDeath,
            isGooglePaySupported = isGooglePaySupported(),
            initializationMode = initializationMode,
        )

        return@runCatching state
    }

    private suspend fun retrieveElementsSession(
        initializationMode: PaymentSheet.InitializationMode,
        customer: PaymentSheet.CustomerConfiguration?,
        externalPaymentMethods: List<String>,
        defaultPaymentMethodId: String?,
    ): Result<ElementsSession> {
        return elementsSessionRepository.get(
            initializationMode = initializationMode,
            customer = customer,
            externalPaymentMethods = externalPaymentMethods,
            defaultPaymentMethodId = defaultPaymentMethodId
        )
    }

    private fun createPaymentMethodMetadata(
        paymentSheetConfiguration: PaymentSheet.Configuration,
        elementsSession: ElementsSession,
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
            requestedExternalPaymentMethods = paymentSheetConfiguration.externalPaymentMethods,
            actualExternalPaymentMethods = externalPaymentMethodSpecs
        )

        return PaymentMethodMetadata.create(
            elementsSession = elementsSession,
            configuration = paymentSheetConfiguration,
            sharedDataSpecs = sharedDataSpecsResult.sharedDataSpecs,
            externalPaymentMethodSpecs = externalPaymentMethodSpecs,
            isGooglePayReady = isGooglePayReady,
        )
    }

    private suspend fun createCustomerState(
        config: PaymentSheet.Configuration,
        elementsSession: ElementsSession,
        metadata: PaymentMethodMetadata,
        savedSelection: Deferred<SavedSelection>,
    ): CustomerState? {
        val customerConfig = config.customer

        val customerState = when (val accessType = customerConfig?.accessType) {
            is PaymentSheet.CustomerAccessType.CustomerSession -> {
                elementsSession.customer?.let { customer ->
                    CustomerState.createForCustomerSession(customer, metadata.supportedSavedPaymentMethodTypes())
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
                CustomerState.createForLegacyEphemeralKey(
                    customerId = customerConfig.id,
                    accessType = accessType,
                    paymentMethods = retrieveCustomerPaymentMethods(
                        metadata = metadata,
                        customerConfig = customerConfig,
                    )
                )
            }
            else -> null
        }

        return customerState?.let { state ->
            state.copy(
                paymentMethods = state.paymentMethods
                    .withLastUsedPaymentMethodFirst(savedSelection.await())
            )
        }
    }

    private suspend fun retrieveCustomerPaymentMethods(
        metadata: PaymentMethodMetadata,
        customerConfig: PaymentSheet.CustomerConfiguration,
    ): List<PaymentMethod> {
        val paymentMethodTypes = metadata.supportedSavedPaymentMethodTypes()

        val paymentMethods = customerRepository.getPaymentMethods(
            customerInfo = CustomerRepository.CustomerInfo(
                id = customerConfig.id,
                ephemeralKeySecret = customerConfig.ephemeralKeySecret
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
        config: PaymentSheet.Configuration,
        customer: Deferred<CustomerState?>,
    ): LinkState? {
        return if (elementsSession.isLinkEnabled && !config.billingDetailsCollectionConfiguration.collectsAnything) {
            loadLinkState(
                config = config,
                customer = customer.await(),
                elementsSession = elementsSession,
                merchantCountry = elementsSession.merchantCountry,
                passthroughModeEnabled = elementsSession.linkPassthroughModeEnabled,
                linkSignUpDisabled = elementsSession.disableLinkSignup,
                flags = elementsSession.linkFlags,
            )
        } else {
            null
        }
    }

    private suspend fun loadLinkState(
        config: PaymentSheet.Configuration,
        customer: CustomerState?,
        elementsSession: ElementsSession,
        merchantCountry: String?,
        passthroughModeEnabled: Boolean,
        linkSignUpDisabled: Boolean,
        flags: Map<String, Boolean>,
    ): LinkState {
        val linkConfig = createLinkConfiguration(
            config = config,
            customer = customer,
            elementsSession = elementsSession,
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
        customer: CustomerState?,
        elementsSession: ElementsSession,
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

        val customerEmail = config.defaultBillingDetails?.email ?: customer?.let {
            customerRepository.retrieveCustomer(
                CustomerRepository.CustomerInfo(
                    id = it.id,
                    ephemeralKeySecret = it.ephemeralKeySecret
                )
            )
        }?.email

        val merchantName = config.merchantDisplayName

        val isSaveForFutureUseValueChangeable = isSaveForFutureUseValueChangeable(
            code = PaymentMethod.Type.Card.code,
            intent = elementsSession.stripeIntent,
            paymentMethodSaveConsentBehavior = elementsSession.toPaymentSheetSaveConsentBehavior(),
            hasCustomerConfiguration = config.customer != null,
        )
        val hasUsedLink = linkStore.hasUsedLink()

        val linkSignupMode = if (hasUsedLink || linkSignUpDisabled) {
            null
        } else if (isSaveForFutureUseValueChangeable) {
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
            stripeIntent = elementsSession.stripeIntent,
            signupMode = linkSignupMode,
            merchantName = merchantName,
            merchantCountryCode = merchantCountry,
            customerInfo = customerInfo,
            shippingValues = shippingAddress,
            passthroughModeEnabled = passthroughModeEnabled,
            flags = flags,
        )
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

    private suspend fun retrieveInitialPaymentSelection(
        savedSelection: Deferred<SavedSelection>,
        customer: Deferred<CustomerState?>
    ): PaymentSelection? {
        return when (val selection = savedSelection.await()) {
            is SavedSelection.GooglePay -> PaymentSelection.GooglePay
            is SavedSelection.Link -> PaymentSelection.Link
            is SavedSelection.PaymentMethod -> {
                customer.await()?.paymentMethods?.find { it.id == selection.id }?.toPaymentSelection()
            }
            is SavedSelection.None -> null
        } ?: customer.await()?.paymentMethods?.firstOrNull()?.toPaymentSelection()
    }

    private suspend fun retrieveSavedSelection(
        config: PaymentSheet.Configuration,
        isGooglePayReady: Boolean,
        elementsSession: ElementsSession
    ): SavedSelection {
        return retrieveSavedSelection(
            config = config,
            isGooglePayReady = isGooglePayReady,
            isLinkAvailable = elementsSession.isLinkEnabled,
        )
    }

    private suspend fun retrieveSavedPaymentMethodSelection(
        config: PaymentSheet.Configuration,
    ): SavedSelection.PaymentMethod? {
        return when (config.customer?.accessType) {
            is PaymentSheet.CustomerAccessType.CustomerSession -> {
                /*
                 * For `CustomerSession`, `v1/elements/sessions` needs to know the client-side saved default payment
                 * method ID to ensure it is properly returned by the API when performing payment method deduping. We
                 * only care about the Stripe `payment_method` id when deduping since `Google Pay` and `Link` are
                 * locally defined LPMs and not recognized by the `v1/elements/sessions` API. We don't need to know
                 * if they are ready and can safely set them to `false`.
                 */
                retrieveSavedSelection(
                    config = config,
                    isGooglePayReady = false,
                    isLinkAvailable = false,
                ) as? SavedSelection.PaymentMethod
            }
            is PaymentSheet.CustomerAccessType.LegacyCustomerEphemeralKey,
            null -> null
        }
    }

    private suspend fun retrieveSavedSelection(
        config: PaymentSheet.Configuration,
        isGooglePayReady: Boolean,
        isLinkAvailable: Boolean,
    ): SavedSelection {
        val customerConfig = config.customer
        val prefsRepository = prefsRepositoryFactory(customerConfig)

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
        state: PaymentSheetState.Full,
        isReloadingAfterProcessDeath: Boolean,
        isGooglePaySupported: Boolean,
        initializationMode: PaymentSheet.InitializationMode,
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
                initializationMode = initializationMode,
                orderedLpms = state.paymentMethodMetadata.sortedSupportedPaymentMethods().map { it.code },
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
