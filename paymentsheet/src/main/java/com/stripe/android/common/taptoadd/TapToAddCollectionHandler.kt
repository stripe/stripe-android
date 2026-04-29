package com.stripe.android.common.taptoadd

import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.exception.StripeException
import com.stripe.android.core.exception.safeAnalyticsMessage
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.core.utils.UserFacingLogger
import com.stripe.android.lpmfoundations.paymentmethod.CustomerMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodUpdateParams
import com.stripe.android.networking.StripeRepository
import com.stripe.android.paymentelement.TapToAddPreview
import com.stripe.android.paymentelement.confirmation.intent.CallbackNotFoundException
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.CreateIntentResult
import com.stripe.android.paymentsheet.paymentdatacollection.ach.asAddressModel
import com.stripe.stripeterminal.external.callable.Callback
import com.stripe.stripeterminal.external.callable.Cancelable
import com.stripe.stripeterminal.external.callable.SetupIntentCallback
import com.stripe.stripeterminal.external.models.AllowRedisplay
import com.stripe.stripeterminal.external.models.CollectSetupIntentConfiguration
import com.stripe.stripeterminal.external.models.PaymentMethodType
import com.stripe.stripeterminal.external.models.SetupIntent
import com.stripe.stripeterminal.external.models.TapToPayUxConfiguration
import com.stripe.stripeterminal.external.models.TerminalErrorCode
import com.stripe.stripeterminal.external.models.TerminalException
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.Continuation
import kotlin.coroutines.suspendCoroutine

internal interface TapToAddCollectionHandler {
    suspend fun collect(metadata: PaymentMethodMetadata): CollectionState

    sealed interface CollectionState {
        data class Collected(val paymentMethod: PaymentMethod) : CollectionState

        data class FailedCollection(
            val error: Throwable,
            val errorCode: ErrorCode,
            val errorMessage: TapToAddErrorMessage,
        ) : CollectionState

        data class UnsupportedDevice(
            val error: Throwable,
            val errorMessage: TapToAddErrorMessage,
        ) : CollectionState

        data object Canceled : CollectionState
    }

    interface ErrorCode {
        val value: String
    }

    companion object {
        @OptIn(TapToAddPreview::class)
        fun create(
            isStripeTerminalSdkAvailable: IsStripeTerminalSdkAvailable,
            terminalWrapper: TerminalWrapper,
            stripeRepository: StripeRepository,
            paymentConfiguration: PaymentConfiguration,
            connectionManager: TapToAddConnectionManager,
            tapToPayUxConfiguration: TapToPayUxConfiguration,
            isSimulatedProvider: TapToAddIsSimulatedProvider,
            userFacingLogger: UserFacingLogger,
            errorReporter: ErrorReporter,
            createCardPresentSetupIntentCallbackRetriever: CreateCardPresentSetupIntentCallbackRetriever,
        ): TapToAddCollectionHandler {
            return if (isStripeTerminalSdkAvailable()) {
                DefaultTapToAddCollectionHandler(
                    terminalWrapper = terminalWrapper,
                    connectionManager = connectionManager,
                    stripeRepository = stripeRepository,
                    paymentConfiguration = paymentConfiguration,
                    tapToPayUxConfiguration = tapToPayUxConfiguration,
                    isSimulatedProvider = isSimulatedProvider,
                    userFacingLogger = userFacingLogger,
                    errorReporter = errorReporter,
                    createCardPresentSetupIntentCallbackRetriever = createCardPresentSetupIntentCallbackRetriever,
                )
            } else {
                UnsupportedTapToAddCollectionHandler()
            }
        }
    }
}

@OptIn(TapToAddPreview::class)
internal class DefaultTapToAddCollectionHandler(
    private val terminalWrapper: TerminalWrapper,
    private val stripeRepository: StripeRepository,
    private val paymentConfiguration: PaymentConfiguration,
    private val connectionManager: TapToAddConnectionManager,
    private val errorReporter: ErrorReporter,
    private val isSimulatedProvider: TapToAddIsSimulatedProvider,
    private val userFacingLogger: UserFacingLogger,
    private val tapToPayUxConfiguration: TapToPayUxConfiguration,
    private val createCardPresentSetupIntentCallbackRetriever: CreateCardPresentSetupIntentCallbackRetriever,
) : TapToAddCollectionHandler {
    override suspend fun collect(
        metadata: PaymentMethodMetadata
    ): TapToAddCollectionHandler.CollectionState = runCatching {
        val customerMetadata = metadata.customerMetadata

        if (customerMetadata == null) {
            val message = "Internal Stripe Error: Attempted to collect with tap to add without a customer"
            val exception = IllegalStateException(message)

            userFacingLogger.logWarningWithoutPii(message)

            return@runCatching TapToAddCollectionHandler.CollectionState.FailedCollection(
                error = exception,
                errorCode = DefaultErrorCode.Internal.NoCustomer,
                errorMessage = TapToAddErrorMessageBuilder.build(exception),
            )
        }

        connectionManager.connect(
            config = TapToAddConnectionManager.ConnectionConfig(
                merchantDisplayName = metadata.merchantName,
            ),
        )

        val callback = try {
            createCardPresentSetupIntentCallbackRetriever.waitForCallback()
        } catch (error: CallbackNotFoundException) {
            userFacingLogger.logWarningWithoutPii(
                "createCardPresentSetupIntentCallback was not defined! Please provide the callback to use tap to add!"
            )

            return@runCatching TapToAddCollectionHandler.CollectionState.FailedCollection(
                error = error,
                errorCode = DefaultErrorCode.Internal.NoCardPresentCallbackFailure,
                errorMessage = TapToAddErrorMessageBuilder.build(error),
            )
        }

        when (val result = callback.createCardPresentSetupIntent()) {
            is CreateIntentResult.Success -> {
                setUxConfiguration()
                collectWithIntent(result.clientSecret, metadata, customerMetadata)
            }
            is CreateIntentResult.Failure -> {
                userFacingLogger.logWarningWithoutPii(
                    result.displayMessage
                        ?: result.cause.message
                        ?: "Unknown error occurred while creating card_present setup intent"
                )

                TapToAddCollectionHandler.CollectionState.FailedCollection(
                    error = result.cause,
                    errorCode = DefaultErrorCode.Internal.FailureFromMerchantCardPresentCallback,
                    errorMessage = TapToAddErrorMessageBuilder.build(result.cause)
                )
            }
        }
    }.fold(
        onSuccess = { it },
        onFailure = ::handleFailedCollection,
    )

    private suspend fun collectWithIntent(
        clientSecret: String,
        metadata: PaymentMethodMetadata,
        customerMetadata: CustomerMetadata,
    ): TapToAddCollectionHandler.CollectionState {
        val setupIntent = retrieveSetupIntent(clientSecret)

        validateSetupIntent(customerMetadata, setupIntent)?.let { failedValidationResult ->
            return failedValidationResult
        }

        val setupIntentWithAttachedPaymentMethod = collectPaymentMethod(setupIntent)
        val confirmedIntent = confirmSetupIntent(setupIntentWithAttachedPaymentMethod)
        val paymentMethod = fetchPaymentMethod(confirmedIntent, customerMetadata)
        val updatedPaymentMethod = updatePaymentMethod(paymentMethod, customerMetadata, metadata)

        return validatePaymentMethod(updatedPaymentMethod, metadata)
    }

    private fun setUxConfiguration() {
        terminal().setTapToPayUxConfiguration(tapToPayUxConfiguration)
    }

    private suspend fun retrieveSetupIntent(clientSecret: String) = suspendCoroutine { continuation ->
        terminal().retrieveSetupIntent(
            clientSecret = clientSecret,
            callback = continuation.createSetupIntentCallback(),
        )
    }

    private suspend fun collectPaymentMethod(
        intent: SetupIntent,
    ) = suspendCancellableCoroutine { continuation ->
        val cancellable = terminal().collectSetupIntentPaymentMethod(
            intent = intent,
            allowRedisplay = AllowRedisplay.ALWAYS,
            config = CollectSetupIntentConfiguration.Builder().build(),
            callback = continuation.createSetupIntentCallback(),
        )

        continuation.handleCancellation(
            cancelable = cancellable,
            errorEvent = ErrorReporter.UnexpectedErrorEvent.TAP_TO_ADD_COLLECT_SETUP_INTENT_CANCEL_FAILURE,
        )
    }

    private suspend fun updatePaymentMethod(
        paymentMethod: PaymentMethod,
        customerMetadata: CustomerMetadata,
        metadata: PaymentMethodMetadata,
    ): PaymentMethod {
        return metadata.defaultBillingDetails?.takeIf {
            metadata.billingDetailsCollectionConfiguration.attachDefaultsToPaymentMethod
        }?.let { billingDetails ->
            stripeRepository.updatePaymentMethod(
                paymentMethodId = paymentMethod.id,
                paymentMethodUpdateParams = PaymentMethodUpdateParams.createCard(
                    billingDetails = PaymentMethod.BillingDetails(
                        name = billingDetails.name,
                        email = billingDetails.email,
                        phone = billingDetails.phone,
                        address = billingDetails.address?.asAddressModel()
                    )
                ),
                options = getApiOptions(customerMetadata),
            ).getOrThrow()
        } ?: paymentMethod
    }

    private fun validateSetupIntent(
        customerMetadata: CustomerMetadata,
        setupIntent: SetupIntent,
    ): TapToAddCollectionHandler.CollectionState.FailedCollection? {
        val isSimulated = isSimulatedProvider.get()

        if (!setupIntent.paymentMethodTypes.contains(PaymentMethodType.CARD_PRESENT.typeName)) {
            val error = IllegalStateException("Missing 'card_present' payment method type on setup intent!")

            return TapToAddCollectionHandler.CollectionState.FailedCollection(
                error = error,
                errorCode = DefaultErrorCode.Internal.MissingCardPresentPaymentMethodType,
                errorMessage = if (isSimulated) {
                    TapToAddErrorMessage(
                        title = INTEGRATION_ERROR_TITLE.resolvableString,
                        action = MISSING_CARD_PRESENT_ACTION.resolvableString,
                    )
                } else {
                    TapToAddErrorMessageBuilder.build(error)
                }
            )
        }

        val setupIntentCustomer = setupIntent.customerId
        val expectedCustomer = getCustomerId(customerMetadata)

        if (setupIntentCustomer != expectedCustomer) {
            val error = IllegalStateException("Incorrect customer attached to setup intent!")

            return TapToAddCollectionHandler.CollectionState.FailedCollection(
                error = error,
                errorCode = DefaultErrorCode.Internal.IncorrectCustomerOnSetupIntent,
                errorMessage = if (isSimulated) {
                    TapToAddErrorMessage(
                        title = INTEGRATION_ERROR_TITLE.resolvableString,
                        action = createIncorrectCustomerActionMessage(
                            actualCustomer = setupIntentCustomer,
                            expectedCustomer = expectedCustomer,
                        ).resolvableString,
                    )
                } else {
                    TapToAddErrorMessageBuilder.build(error)
                }
            )
        }

        return null
    }

    private fun validatePaymentMethod(
        paymentMethod: PaymentMethod,
        metadata: PaymentMethodMetadata,
    ): TapToAddCollectionHandler.CollectionState {
        if (!metadata.cardBrandFilter.isAccepted(paymentMethod)) {
            val exception = TapToAddCardNotSupportedException()

            return TapToAddCollectionHandler.CollectionState.FailedCollection(
                error = exception,
                errorCode = DefaultErrorCode.Internal.CardBrandNotSupportedByMerchant,
                errorMessage = TapToAddErrorMessageBuilder.build(exception),
            )
        }

        return TapToAddCollectionHandler.CollectionState.Collected(paymentMethod)
    }

    private suspend fun confirmSetupIntent(
        intent: SetupIntent,
    ) = suspendCancellableCoroutine { continuation ->
        val cancellable = terminal().confirmSetupIntent(
            intent = intent,
            callback = continuation.createSetupIntentCallback(),
        )

        continuation.handleCancellation(
            cancelable = cancellable,
            errorEvent = ErrorReporter.UnexpectedErrorEvent.TAP_TO_ADD_CONFIRM_SETUP_INTENT_CANCEL_FAILURE,
        )
    }

    private fun Continuation<SetupIntent>.createSetupIntentCallback(): SetupIntentCallback {
        return object : SetupIntentCallback {
            override fun onSuccess(setupIntent: SetupIntent) {
                resumeWith(Result.success(setupIntent))
            }

            override fun onFailure(e: TerminalException) {
                resumeWith(Result.failure(e))
            }
        }
    }

    private fun CancellableContinuation<SetupIntent>.handleCancellation(
        cancelable: Cancelable,
        errorEvent: ErrorReporter.UnexpectedErrorEvent,
    ) {
        invokeOnCancellation {
            cancelable.cancel(
                object : Callback {
                    override fun onSuccess() {
                        // No-op
                    }

                    override fun onFailure(e: TerminalException) {
                        errorReporter.report(
                            errorEvent = errorEvent,
                            stripeException = StripeException.create(e),
                            additionalNonPiiParams = mapOf(
                                TERMINAL_ERROR_CODE_KEY to e.errorCode.toLogString()
                            ),
                        )
                    }
                }
            )
        }
    }

    private suspend fun fetchPaymentMethod(
        intent: SetupIntent,
        customerMetadata: CustomerMetadata,
    ): PaymentMethod {
        val paymentMethodId = intent.paymentMethodId
            ?: run {
                errorReporter.report(
                    ErrorReporter
                        .UnexpectedErrorEvent
                        .TAP_TO_ADD_NO_GENERATED_CARD_AFTER_SUCCESSFUL_INTENT_CONFIRMATION
                )

                throw IllegalStateException(
                    "No card payment method after collecting through tap!"
                )
            }

        return stripeRepository.retrieveSavedPaymentMethodFromCardPresentPaymentMethod(
            cardPresentPaymentMethodId = paymentMethodId,
            customerId = getCustomerId(customerMetadata),
            options = getApiOptions(customerMetadata)
        ).getOrThrow()
    }

    private fun handleFailedCollection(error: Throwable): TapToAddCollectionHandler.CollectionState {
        return when (error) {
            is TerminalException if error.errorCode == TerminalErrorCode.CANCELED -> {
                TapToAddCollectionHandler.CollectionState.Canceled
            }
            is TerminalException if unsupportedDeviceErrorCodes.contains(error.errorCode) -> {
                TapToAddCollectionHandler.CollectionState.UnsupportedDevice(
                    error = error,
                    errorMessage = TapToAddErrorMessageBuilder.build(error),
                )
            }
            else -> {
                TapToAddCollectionHandler.CollectionState.FailedCollection(
                    error = error,
                    errorCode = when (error) {
                        is TerminalException -> DefaultErrorCode.Terminal(error)
                        else -> DefaultErrorCode.Exception(error)
                    },
                    errorMessage = TapToAddErrorMessageBuilder.build(error)
                )
            }
        }
    }

    private fun getCustomerId(customerMetadata: CustomerMetadata): String {
        return when (customerMetadata) {
            is CustomerMetadata.CustomerSession -> customerMetadata.id
            is CustomerMetadata.LegacyEphemeralKey -> customerMetadata.id
            is CustomerMetadata.CheckoutSession -> customerMetadata.customerId
        }
    }

    private fun getApiOptions(customerMetadata: CustomerMetadata): ApiRequest.Options {
        val ephemeralKeySecret = when (customerMetadata) {
            is CustomerMetadata.CustomerSession -> customerMetadata.ephemeralKeySecret
            is CustomerMetadata.LegacyEphemeralKey -> customerMetadata.ephemeralKeySecret
            is CustomerMetadata.CheckoutSession -> {
                throw NotImplementedError("Checkout sessions does not support Tap to Add!")
            }
        }

        return ApiRequest.Options(
            apiKey = ephemeralKeySecret,
            stripeAccount = paymentConfiguration.stripeAccountId,
        )
    }

    private fun terminal() = terminalWrapper.getInstance()

    private sealed interface DefaultErrorCode : TapToAddCollectionHandler.ErrorCode {
        enum class Internal(override val value: String) : DefaultErrorCode {
            NoCustomer("noCustomer"),
            NoCardPresentCallbackFailure("noCardPresentCallbackFailure"),
            FailureFromMerchantCardPresentCallback("failureFromMerchantCardPresentCallback"),
            CardBrandNotSupportedByMerchant("cardBrandNotSupportedByMerchant"),
            MissingCardPresentPaymentMethodType("missingCardPresentPaymentMethodType"),
            IncorrectCustomerOnSetupIntent("incorrectCustomerOnSetupIntent")
        }

        class Terminal(exception: TerminalException) : DefaultErrorCode {
            override val value: String = exception.errorCode.toLogString()
        }

        class Exception(error: Throwable) : DefaultErrorCode {
            override val value: String = error.safeAnalyticsMessage
        }
    }

    private fun createIncorrectCustomerActionMessage(
        expectedCustomer: String,
        actualCustomer: String?,
    ): String {
        return when (actualCustomer) {
            null ->
                "Please attach the customer ($expectedCustomer) to the setup intent that was used to" +
                " when initializing Payment Element"
            else ->
                "Setup intent had a different customer ($actualCustomer) than the expected customer " +
                "initialized with Payment Element ($expectedCustomer). Please attach the expected customer."
        }
    }

    private companion object {
        const val INTEGRATION_ERROR_TITLE = "Integration error"
        const val MISSING_CARD_PRESENT_ACTION = "Ensure the setup intent allows a `card_present` payment method " +
            "type on it to allow for tapping cards"
    }
}

internal class UnsupportedTapToAddCollectionHandler : TapToAddCollectionHandler {
    override suspend fun collect(metadata: PaymentMethodMetadata): TapToAddCollectionHandler.CollectionState {
        val exception = IllegalStateException("Not handled!")

        return TapToAddCollectionHandler.CollectionState.FailedCollection(
            error = exception,
            errorCode = UnsupportedErrorCode,
            errorMessage = TapToAddErrorMessageBuilder.build(exception),
        )
    }

    private object UnsupportedErrorCode : TapToAddCollectionHandler.ErrorCode {
        override val value: String = "attemptedTapToAddWhenUnsupported"
    }
}

private const val TERMINAL_ERROR_CODE_KEY = "terminalErrorCode"

private val unsupportedDeviceErrorCodes = listOf(
    TerminalErrorCode.TAP_TO_PAY_DEVICE_TAMPERED,
    TerminalErrorCode.TAP_TO_PAY_UNSUPPORTED_DEVICE,
)
