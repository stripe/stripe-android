package com.stripe.android.paymentsheet.state

import android.os.Parcelable
import com.stripe.android.common.coroutines.runCatching
import com.stripe.android.common.model.CommonConfiguration
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
import com.stripe.android.lpmfoundations.paymentmethod.PaymentSheetCardBrandFilter
import com.stripe.android.lpmfoundations.paymentmethod.link.LinkInlineConfiguration
import com.stripe.android.lpmfoundations.paymentmethod.toPaymentSheetSaveConsentBehavior
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.StripeIntent
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheet.IntentConfiguration
import com.stripe.android.paymentsheet.PrefsRepository
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.addresselement.toIdentifierMap
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
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
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
        isReloadingAfterProcessDeath: Boolean = false,
        initializedViaCompose: Boolean,
    ): Result<State>

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
                // Nothing to do here
            }
        }
    }

    @Parcelize
    data class State(
        val config: CommonConfiguration,
        val customer: CustomerState?,
        val linkState: LinkState?,
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
    private val accountStatusProvider: LinkAccountStatusProvider,
    private val linkStore: LinkStore,
    private val externalPaymentMethodsRepository: ExternalPaymentMethodsRepository,
    private val userFacingLogger: UserFacingLogger,
    private val cvcRecollectionHandler: CvcRecollectionHandler
) : PaymentElementLoader {

    @Suppress("LongMethod")
    override suspend fun load(
        initializationMode: PaymentElementLoader.InitializationMode,
        configuration: CommonConfiguration,
        isReloadingAfterProcessDeath: Boolean,
        initializedViaCompose: Boolean,
    ): Result<PaymentElementLoader.State> = workContext.runCatching(::reportFailedLoad) {
        eventReporter.onLoadStarted(initializedViaCompose)

        val savedPaymentMethodSelection = retrieveSavedPaymentMethodSelection(configuration)

        val elementsSession = retrieveElementsSession(
            initializationMode = initializationMode,
            customer = configuration.customer,
            externalPaymentMethods = configuration.externalPaymentMethods,
            defaultPaymentMethodId = savedPaymentMethodSelection?.id,
        ).getOrThrow()

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
            )
        }

        val metadata = async {
            createPaymentMethodMetadata(
                configuration = configuration,
                elementsSession = elementsSession,
                isGooglePayReady = isGooglePayReady,
                linkState = linkState.await(),
            )
        }

        val customer = async {
            createCustomerState(
                customerInfo = customerInfo,
                metadata = metadata.await(),
                savedSelection = savedSelection,
                cardBrandFilter = PaymentSheetCardBrandFilter(configuration.cardBrandAcceptance)
            )
        }

        val initialPaymentSelection = async {
            retrieveInitialPaymentSelection(savedSelection, customer)
        }

        val stripeIntent = elementsSession.stripeIntent
        val paymentMethodMetadata = metadata.await()

        warnUnactivatedIfNeeded(stripeIntent)

        if (!supportsIntent(paymentMethodMetadata)) {
            val requested = stripeIntent.paymentMethodTypes.joinToString(separator = ", ")
            throw PaymentSheetLoadingException.NoPaymentMethodTypesAvailable(requested)
        }

        val state = PaymentElementLoader.State(
            config = configuration,
            customer = customer.await(),
            linkState = linkState.await(),
            paymentSelection = initialPaymentSelection.await(),
            validationError = stripeIntent.validate(),
            paymentMethodMetadata = paymentMethodMetadata,
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
        initializationMode: PaymentElementLoader.InitializationMode,
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
        configuration: CommonConfiguration,
        elementsSession: ElementsSession,
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

        return PaymentMethodMetadata.create(
            elementsSession = elementsSession,
            configuration = configuration,
            sharedDataSpecs = sharedDataSpecsResult.sharedDataSpecs,
            externalPaymentMethodSpecs = externalPaymentMethodSpecs,
            isGooglePayReady = isGooglePayReady,
            linkInlineConfiguration = createLinkInlineConfiguration(linkState),
        )
    }

    private fun createCustomerInfo(
        configuration: CommonConfiguration,
        elementsSession: ElementsSession,
    ): CustomerInfo? {
        val customer = configuration.customer

        return when (val accessType = customer?.accessType) {
            is PaymentSheet.CustomerAccessType.CustomerSession -> {
                elementsSession.customer?.let { elementsSessionCustomer ->
                    CustomerInfo.CustomerSession(elementsSessionCustomer)
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
        customerInfo: CustomerInfo?,
        metadata: PaymentMethodMetadata,
        savedSelection: Deferred<SavedSelection>,
        cardBrandFilter: PaymentSheetCardBrandFilter
    ): CustomerState? {
        val customerState = when (customerInfo) {
            is CustomerInfo.CustomerSession -> {
                CustomerState.createForCustomerSession(
                    customer = customerInfo.elementsSessionCustomer,
                    supportedSavedPaymentMethodTypes = metadata.supportedSavedPaymentMethodTypes()
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
                    .withLastUsedPaymentMethodFirst(savedSelection.await()).filter { cardBrandFilter.isAccepted(it) }
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
        configuration: CommonConfiguration,
        customer: CustomerInfo?,
    ): LinkState? {
        return if (elementsSession.isLinkEnabled &&
            !configuration.billingDetailsCollectionConfiguration.collectsAnything
        ) {
            loadLinkState(
                configuration = configuration,
                customer = customer,
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
        configuration: CommonConfiguration,
        customer: CustomerInfo?,
        elementsSession: ElementsSession,
        merchantCountry: String?,
        passthroughModeEnabled: Boolean,
        linkSignUpDisabled: Boolean,
        flags: Map<String, Boolean>,
    ): LinkState {
        val linkConfig = createLinkConfiguration(
            configuration = configuration,
            customer = customer,
            elementsSession = elementsSession,
            merchantCountry = merchantCountry,
            passthroughModeEnabled = passthroughModeEnabled,
            flags = flags,
        )

        val accountStatus = accountStatusProvider(linkConfig)

        val loginState = when (accountStatus) {
            AccountStatus.Verified -> LinkState.LoginState.LoggedIn
            AccountStatus.NeedsVerification,
            AccountStatus.VerificationStarted -> LinkState.LoginState.NeedsVerification
            AccountStatus.SignedOut,
            AccountStatus.Error -> LinkState.LoginState.LoggedOut
        }

        val isSaveForFutureUseValueChangeable = isSaveForFutureUseValueChangeable(
            code = PaymentMethod.Type.Card.code,
            intent = elementsSession.stripeIntent,
            paymentMethodSaveConsentBehavior = elementsSession.toPaymentSheetSaveConsentBehavior(),
            hasCustomerConfiguration = configuration.customer != null,
        )
        val hasUsedLink = linkStore.hasUsedLink()

        val linkSignupMode = if (hasUsedLink || linkSignUpDisabled) {
            null
        } else if (isSaveForFutureUseValueChangeable) {
            LinkSignupMode.AlongsideSaveForFutureUse
        } else {
            LinkSignupMode.InsteadOfSaveForFutureUse
        }

        return LinkState(
            configuration = linkConfig,
            loginState = loginState,
            signupMode = linkSignupMode.takeIf {
                val validFundingSource = linkConfig.stripeIntent.linkFundingSources
                    .contains(PaymentMethod.Type.Card.code)

                val notLoggedIn = accountStatus == AccountStatus.SignedOut

                validFundingSource && notLoggedIn
            },
        )
    }

    private suspend fun createLinkConfiguration(
        configuration: CommonConfiguration,
        customer: CustomerInfo?,
        elementsSession: ElementsSession,
        merchantCountry: String?,
        passthroughModeEnabled: Boolean,
        flags: Map<String, Boolean>,
    ): LinkConfiguration {
        val shippingDetails: AddressDetails? = configuration.shippingDetails

        val customerPhone = if (shippingDetails?.isCheckboxSelected == true) {
            shippingDetails.phoneNumber
        } else {
            configuration.defaultBillingDetails?.phone
        }

        val shippingAddress = if (shippingDetails?.isCheckboxSelected == true) {
            shippingDetails.toIdentifierMap(configuration.defaultBillingDetails)
        } else {
            null
        }

        val customerEmail = configuration.defaultBillingDetails?.email ?: customer?.let {
            customerRepository.retrieveCustomer(
                CustomerRepository.CustomerInfo(
                    id = it.id,
                    ephemeralKeySecret = it.ephemeralKeySecret
                )
            )
        }?.email

        val merchantName = configuration.merchantDisplayName

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

        return LinkConfiguration(
            stripeIntent = elementsSession.stripeIntent,
            merchantName = merchantName,
            merchantCountryCode = merchantCountry,
            customerInfo = customerInfo,
            shippingValues = shippingAddress,
            passthroughModeEnabled = passthroughModeEnabled,
            cardBrandChoice = cardBrandChoice,
            flags = flags,
        )
    }

    private suspend fun isGooglePayReady(
        configuration: CommonConfiguration,
        elementsSession: ElementsSession,
    ): Boolean {
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

    private fun createLinkInlineConfiguration(state: LinkState?): LinkInlineConfiguration? {
        return state?.run {
            signupMode?.let { linkSignupMode ->
                LinkInlineConfiguration(
                    linkConfiguration = configuration,
                    signupMode = linkSignupMode,
                )
            }
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
        isGooglePaySupported: Boolean,
        initializationMode: PaymentElementLoader.InitializationMode,
    ) {
        elementsSession.sessionsError?.let { sessionsError ->
            eventReporter.onElementsSessionLoadFailed(sessionsError)
        }

        val treatValidationErrorAsFailure = !state.stripeIntent.isConfirmed || isReloadingAfterProcessDeath

        if (state.validationError != null && treatValidationErrorAsFailure) {
            eventReporter.onLoadFailed(state.validationError)
        } else {
            eventReporter.onLoadSucceeded(
                linkMode = elementsSession.linkSettings?.linkMode,
                googlePaySupported = isGooglePaySupported,
                currency = elementsSession.stripeIntent.currency,
                paymentSelection = state.paymentSelection,
                initializationMode = initializationMode,
                orderedLpms = state.paymentMethodMetadata.sortedSupportedPaymentMethods().map { it.code },
                requireCvcRecollection = cvcRecollectionHandler.cvcRecollectionEnabled(
                    state.paymentMethodMetadata.stripeIntent,
                    initializationMode
                )
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

    private sealed interface CustomerInfo {
        val id: String
        val ephemeralKeySecret: String

        data class CustomerSession(
            val elementsSessionCustomer: ElementsSession.Customer,
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
