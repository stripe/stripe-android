package com.stripe.android.common.taptoadd

import com.stripe.android.PaymentConfiguration
import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.core.exception.safeAnalyticsMessage
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.lpmfoundations.paymentmethod.CustomerMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.networking.StripeRepository
import com.stripe.android.paymentelement.TapToAddPreview
import com.stripe.android.paymentelement.confirmation.intent.CallbackNotFoundException
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.CreateIntentResult
import com.stripe.stripeterminal.external.callable.Callback
import com.stripe.stripeterminal.external.callable.Cancelable
import com.stripe.stripeterminal.external.callable.SetupIntentCallback
import com.stripe.stripeterminal.external.models.AllowRedisplay
import com.stripe.stripeterminal.external.models.CollectSetupIntentConfiguration
import com.stripe.stripeterminal.external.models.SetupIntent
import com.stripe.stripeterminal.external.models.TapToPayUxConfiguration
import com.stripe.stripeterminal.external.models.TerminalErrorCode
import com.stripe.stripeterminal.external.models.TerminalException
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.Continuation
import kotlin.coroutines.suspendCoroutine
import com.stripe.android.R as StripeR

internal interface TapToAddCollectionHandler {
    suspend fun collect(metadata: PaymentMethodMetadata): CollectionState

    sealed interface CollectionState {
        data class Collected(val paymentMethod: PaymentMethod) : CollectionState

        data class FailedCollection(
            val error: Throwable,
            val errorCode: ErrorCode,
            val displayMessage: ResolvableString?,
        ) : CollectionState

        data class UnsupportedDevice(
            val error: Throwable,
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
    private val tapToPayUxConfiguration: TapToPayUxConfiguration,
    private val createCardPresentSetupIntentCallbackRetriever: CreateCardPresentSetupIntentCallbackRetriever,
) : TapToAddCollectionHandler {
    override suspend fun collect(
        metadata: PaymentMethodMetadata
    ): TapToAddCollectionHandler.CollectionState = runCatching {
        val customerMetadata = metadata.customerMetadata

        if (customerMetadata == null) {
            val exception = IllegalStateException("Attempted to collect with tap to add without a customer")

            return@runCatching TapToAddCollectionHandler.CollectionState.FailedCollection(
                error = exception,
                errorCode = DefaultErrorCode.Internal.NoCustomer,
                displayMessage = exception.stripeErrorMessage(),
            )
        }

        connectionManager.connect()

        val callback = try {
            createCardPresentSetupIntentCallbackRetriever.waitForCallback()
        } catch (error: CallbackNotFoundException) {
            return@runCatching TapToAddCollectionHandler.CollectionState.FailedCollection(
                error = error,
                errorCode = DefaultErrorCode.Internal.NoCardPresentCallbackFailure,
                displayMessage = error.resolvableError,
            )
        }

        when (val result = callback.createCardPresentSetupIntent()) {
            is CreateIntentResult.Success -> {
                setUxConfiguration()
                collectWithIntent(result.clientSecret, metadata, customerMetadata)
            }
            is CreateIntentResult.Failure -> {
                TapToAddCollectionHandler.CollectionState.FailedCollection(
                    error = result.cause,
                    errorCode = DefaultErrorCode.Internal.FailureFromMerchantCardPresentCallback,
                    displayMessage = result.displayMessage?.resolvableString ?: result.cause.stripeErrorMessage()
                )
            }
        }
    }.fold(
        onSuccess = { it },
        onFailure = { error ->
            when (error) {
                is TerminalException if error.errorCode == TerminalErrorCode.CANCELED -> {
                    TapToAddCollectionHandler.CollectionState.Canceled
                }
                is TerminalException if unsupportedDeviceErrorCodes.contains(error.errorCode) -> {
                    TapToAddCollectionHandler.CollectionState.UnsupportedDevice(error = error)
                }
                else -> {
                    TapToAddCollectionHandler.CollectionState.FailedCollection(
                        error = error,
                        errorCode = when (error) {
                            is TerminalException -> DefaultErrorCode.Terminal(error)
                            else -> DefaultErrorCode.Exception(error)
                        },
                        displayMessage = error.stripeErrorMessage()
                    )
                }
            }
        }
    )

    private suspend fun collectWithIntent(
        clientSecret: String,
        metadata: PaymentMethodMetadata,
        customerMetadata: CustomerMetadata,
    ): TapToAddCollectionHandler.CollectionState {
        val setupIntent = retrieveSetupIntent(clientSecret)
        val setupIntentWithAttachedPaymentMethod = collectPaymentMethod(setupIntent)
        val confirmedIntent = confirmSetupIntent(setupIntentWithAttachedPaymentMethod)
        val paymentMethod = fetchPaymentMethod(confirmedIntent, customerMetadata)

        return validatePaymentMethod(paymentMethod, metadata)
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

        continuation.handleCancellation(cancellable)
    }

    private fun validatePaymentMethod(
        paymentMethod: PaymentMethod,
        metadata: PaymentMethodMetadata,
    ): TapToAddCollectionHandler.CollectionState {
        if (!metadata.cardBrandFilter.isAccepted(paymentMethod)) {
            return TapToAddCollectionHandler.CollectionState.FailedCollection(
                error = IllegalStateException("Payment method is not supported by card brand filter!"),
                errorCode = DefaultErrorCode.Internal.CardBrandNotSupportedByMerchant,
                displayMessage = resolvableString(
                    StripeR.string.stripe_disallowed_card_brand,
                    paymentMethod.card?.brand ?: CardBrand.Unknown,
                )
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

        continuation.handleCancellation(cancellable)
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
        cancelable: Cancelable
    ) {
        invokeOnCancellation {
            cancelable.cancel(
                object : Callback {
                    override fun onSuccess() {
                        // No-op
                    }

                    override fun onFailure(e: TerminalException) {
                        // No-op
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

        val (customerId, ephemeralKeySecret) = when (customerMetadata) {
            is CustomerMetadata.CustomerSession -> customerMetadata.id to customerMetadata.ephemeralKeySecret
            is CustomerMetadata.LegacyEphemeralKey -> customerMetadata.id to customerMetadata.ephemeralKeySecret
            is CustomerMetadata.CheckoutSession -> {
                throw NotImplementedError("Checkout sessions do not support retrieving individual payment methods!")
            }
        }

        return stripeRepository.retrieveSavedPaymentMethodFromCardPresentPaymentMethod(
            cardPresentPaymentMethodId = paymentMethodId,
            customerId = customerId,
            options = ApiRequest.Options(
                apiKey = ephemeralKeySecret,
                stripeAccount = paymentConfiguration.stripeAccountId,
            )
        ).getOrThrow()
    }

    private fun terminal() = terminalWrapper.getInstance()

    private sealed interface DefaultErrorCode : TapToAddCollectionHandler.ErrorCode {
        enum class Internal(override val value: String) : DefaultErrorCode {
            NoCustomer("noCustomer"),
            NoCardPresentCallbackFailure("noCardPresentCallbackFailure"),
            FailureFromMerchantCardPresentCallback("failureFromMerchantCardPresentCallback"),
            CardBrandNotSupportedByMerchant("cardBrandNotSupportedByMerchant")
        }

        class Terminal(exception: TerminalException) : DefaultErrorCode {
            override val value: String = exception.errorCode.toLogString()
        }

        class Exception(error: Throwable) : DefaultErrorCode {
            override val value: String = error.safeAnalyticsMessage
        }
    }
}

internal class UnsupportedTapToAddCollectionHandler : TapToAddCollectionHandler {
    override suspend fun collect(metadata: PaymentMethodMetadata): TapToAddCollectionHandler.CollectionState {
        return TapToAddCollectionHandler.CollectionState.FailedCollection(
            error = IllegalStateException("Not handled!"),
            errorCode = UnsupportedErrorCode,
            displayMessage = null,
        )
    }

    private object UnsupportedErrorCode : TapToAddCollectionHandler.ErrorCode {
        override val value: String = "attemptedTapToAddWhenUnsupported"
    }
}

private val unsupportedDeviceErrorCodes = listOf(
    TerminalErrorCode.TAP_TO_PAY_DEVICE_TAMPERED,
    TerminalErrorCode.TAP_TO_PAY_UNSUPPORTED_DEVICE,
)
