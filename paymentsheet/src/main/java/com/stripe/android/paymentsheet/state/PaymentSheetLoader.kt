package com.stripe.android.paymentsheet.state

import com.stripe.android.core.Logger
import com.stripe.android.core.exception.StripeException
import com.stripe.android.core.injection.IOContext
import com.stripe.android.googlepaylauncher.GooglePayEnvironment
import com.stripe.android.googlepaylauncher.GooglePayRepository
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.account.LinkStore
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.link.ui.inline.LinkSignupMode
import com.stripe.android.lpmfoundations.luxe.LpmRepository
import com.stripe.android.lpmfoundations.luxe.isSaveForFutureUseValueChangeable
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
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
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import com.stripe.android.ui.core.elements.ExternalPaymentMethodSpec
import com.stripe.android.ui.core.elements.ExternalPaymentMethodsRepository
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
) : PaymentSheetLoader {

    override suspend fun load(
        initializationMode: PaymentSheet.InitializationMode,
        paymentSheetConfiguration: PaymentSheet.Configuration,
        isReloadingAfterProcessDeath: Boolean,
        initializedViaCompose: Boolean,
    ): Result<PaymentSheetState.Full> = withContext(workContext) {
        eventReporter.onLoadStarted(initializedViaCompose)

        val elementsSessionResult = retrieveElementsSession(
            initializationMode = initializationMode,
            customer = paymentSheetConfiguration.customer,
            externalPaymentMethods = paymentSheetConfiguration.externalPaymentMethods,
        )

        elementsSessionResult.mapCatching { elementsSession ->
            val billingDetailsCollectionConfig =
                paymentSheetConfiguration.billingDetailsCollectionConfiguration

            val cbcEligibility = CardBrandChoiceEligibility.create(
                isEligible = elementsSession.isEligibleForCardBrandChoice,
                preferredNetworks = paymentSheetConfiguration.preferredNetworks,
            )

            val sharedDataSpecsResult = lpmRepository.getSharedDataSpecs(
                stripeIntent = elementsSession.stripeIntent,
                serverLpmSpecs = elementsSession.paymentMethodSpecs,
            )
            val externalPaymentMethodSpecs = externalPaymentMethodsRepository.getExternalPaymentMethodSpecs(
                elementsSession.externalPaymentMethodData
            )
            logIfMissingExternalPaymentMethods(
                requestedExternalPaymentMethods = paymentSheetConfiguration.externalPaymentMethods,
                actualExternalPaymentMethods = externalPaymentMethodSpecs
            )
            val metadata = PaymentMethodMetadata(
                stripeIntent = elementsSession.stripeIntent,
                billingDetailsCollectionConfiguration = billingDetailsCollectionConfig,
                allowsDelayedPaymentMethods = paymentSheetConfiguration.allowsDelayedPaymentMethods,
                allowsPaymentMethodsRequiringShippingAddress = paymentSheetConfiguration
                    .allowsPaymentMethodsRequiringShippingAddress,
                paymentMethodOrder = paymentSheetConfiguration.paymentMethodOrder,
                cbcEligibility = cbcEligibility,
                merchantName = paymentSheetConfiguration.merchantDisplayName,
                defaultBillingDetails = paymentSheetConfiguration.defaultBillingDetails,
                shippingDetails = paymentSheetConfiguration.shippingDetails,
                hasCustomerConfiguration = paymentSheetConfiguration.customer != null,
                sharedDataSpecs = sharedDataSpecsResult.sharedDataSpecs,
                externalPaymentMethodSpecs = externalPaymentMethodSpecs
            )

            if (sharedDataSpecsResult.failedToParseServerResponse) {
                eventReporter.onLpmSpecFailure(sharedDataSpecsResult.failedToParseServerErrorMessage)
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

        val customerConfig = config.customer

        val savedSelection = async {
            retrieveSavedSelection(
                config = config,
                isGooglePayReady = isGooglePayReady.await(),
                elementsSession = elementsSession
            )
        }

        val customer = async {
            val customerState = when (customerConfig?.accessType) {
                is PaymentSheet.CustomerAccessType.CustomerSession ->
                    elementsSession.toCustomerState()
                is PaymentSheet.CustomerAccessType.LegacyCustomerEphemeralKey ->
                    customerConfig.toCustomerState(metadata)
                else -> null
            }

            customerState?.let { state ->
                state.copy(
                    paymentMethods = state.paymentMethods
                        .withLastUsedPaymentMethodFirst(savedSelection.await())
                )
            }
        }

        val initialPaymentSelection = async {
            retrieveInitialPaymentSelection(savedSelection, customer)
                ?: customer.await()?.paymentMethods?.firstOrNull()?.toPaymentSelection()
        }

        val linkState = async {
            if (elementsSession.isLinkEnabled && !config.billingDetailsCollectionConfiguration.collectsAnything) {
                loadLinkState(
                    config = config,
                    customer = customer.await(),
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
                customer = customer.await(),
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

    private suspend fun retrieveElementsSession(
        initializationMode: PaymentSheet.InitializationMode,
        customer: PaymentSheet.CustomerConfiguration?,
        externalPaymentMethods: List<String>,
    ): Result<ElementsSession> {
        return elementsSessionRepository.get(initializationMode, customer, externalPaymentMethods)
    }

    private suspend fun loadLinkState(
        config: PaymentSheet.Configuration,
        customer: CustomerState?,
        metadata: PaymentMethodMetadata,
        merchantCountry: String?,
        passthroughModeEnabled: Boolean,
        linkSignUpDisabled: Boolean,
        flags: Map<String, Boolean>,
    ): LinkState {
        val linkConfig = createLinkConfiguration(
            config = config,
            customer = customer,
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
        customer: CustomerState?,
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
            metadata = metadata,
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
        customer: Deferred<CustomerState?>
    ): PaymentSelection? {
        return when (val selection = savedSelection.await()) {
            is SavedSelection.GooglePay -> PaymentSelection.GooglePay
            is SavedSelection.Link -> PaymentSelection.Link
            is SavedSelection.PaymentMethod -> {
                customer.await()?.paymentMethods?.find { it.id == selection.id }?.toPaymentSelection()
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
        return metadata.supportedPaymentMethodTypes().isNotEmpty()
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
                logger.warning(
                    "Requested external payment method $requestedExternalPaymentMethod is not supported."
                )
            }
        }
    }

    private fun ElementsSession.toCustomerState(): CustomerState? {
        return customer?.let { customer ->
            CustomerState(
                id = customer.session.customerId,
                ephemeralKeySecret = customer.session.apiKey,
                paymentMethods = customer.paymentMethods,
            )
        } ?: run {
            val exception = IllegalStateException(
                "Excepted 'customer' attribute as part of 'elements_session' response!"
            )

            errorReporter.report(
                ErrorReporter
                    .UnexpectedErrorEvent
                    .PAYMENT_SHEET_LOADER_ELEMENTS_SESSION_CUSTOMER_NOT_FOUND,
                StripeException.create(exception)
            )

            if (!stripeIntent.isLiveMode) {
                throw exception
            }

            null
        }
    }

    private suspend fun PaymentSheet.CustomerConfiguration.toCustomerState(
        metadata: PaymentMethodMetadata,
    ): CustomerState {
        return CustomerState(
            id = id,
            ephemeralKeySecret = ephemeralKeySecret,
            paymentMethods = retrieveCustomerPaymentMethods(
                metadata = metadata,
                customerConfig = this,
            )
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
