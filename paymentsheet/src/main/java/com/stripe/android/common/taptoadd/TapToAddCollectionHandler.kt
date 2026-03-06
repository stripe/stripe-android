package com.stripe.android.common.taptoadd

import com.stripe.android.R as StripeR
import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.lpmfoundations.paymentmethod.CustomerMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentelement.TapToAddPreview
import com.stripe.android.paymentelement.confirmation.intent.CallbackNotFoundException
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.CreateIntentResult
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.repositories.SavedPaymentMethodRepository
import com.stripe.stripeterminal.external.callable.Callback
import com.stripe.stripeterminal.external.callable.Cancelable
import com.stripe.stripeterminal.external.callable.SetupIntentCallback
import com.stripe.stripeterminal.external.models.AllowRedisplay
import com.stripe.stripeterminal.external.models.SetupIntent
import com.stripe.stripeterminal.external.models.SetupIntentConfiguration
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
            val displayMessage: ResolvableString?,
        ) : CollectionState

        data object Canceled : CollectionState
    }

    companion object {
        @OptIn(TapToAddPreview::class)
        fun create(
            isStripeTerminalSdkAvailable: IsStripeTerminalSdkAvailable,
            terminalWrapper: TerminalWrapper,
            savedPaymentMethodRepository: SavedPaymentMethodRepository,
            connectionManager: TapToAddConnectionManager,
            tapToPayUxConfiguration: TapToPayUxConfiguration,
            errorReporter: ErrorReporter,
            createCardPresentSetupIntentCallbackRetriever: CreateCardPresentSetupIntentCallbackRetriever,
        ): TapToAddCollectionHandler {
            return if (isStripeTerminalSdkAvailable()) {
                DefaultTapToAddCollectionHandler(
                    terminalWrapper = terminalWrapper,
                    connectionManager = connectionManager,
                    savedPaymentMethodRepository = savedPaymentMethodRepository,
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
    private val savedPaymentMethodRepository: SavedPaymentMethodRepository,
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
                displayMessage = exception.stripeErrorMessage(),
            )
        }

        if (!connectionManager.isConnected) {
            connectionManager.connect()

            connectionManager
                .awaitConnection()
                .onFailure { exception ->
                    throw exception
                }
        }

        val callback = try {
            createCardPresentSetupIntentCallbackRetriever.waitForCallback()
        } catch (error: CallbackNotFoundException) {
            return@runCatching TapToAddCollectionHandler.CollectionState.FailedCollection(
                error = error,
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
                    displayMessage = result.displayMessage?.resolvableString ?: result.cause.stripeErrorMessage()
                )
            }
        }
    }.fold(
        onSuccess = { it },
        onFailure = { error ->
            if (error is TerminalException && error.errorCode == TerminalErrorCode.CANCELED) {
                TapToAddCollectionHandler.CollectionState.Canceled
            } else {
                TapToAddCollectionHandler.CollectionState.FailedCollection(
                    error = error,
                    displayMessage = error.stripeErrorMessage()
                )
            }
        }
    )

    private suspend fun collectWithIntent(
        clientSecret: String,
        metadata: PaymentMethodMetadata,
        customerMetadata: CustomerMetadata,
    ): TapToAddCollectionHandler.CollectionState {
        val setupIntent = retrieveSetupIntent(clientSecret)
        val setupIntentWithAttachedPaymentMethod = collectPaymentMethod(setupIntent, metadata)
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
        metadata: PaymentMethodMetadata,
    ) = suspendCancellableCoroutine { continuation ->
        val allowRedisplay = metadata.allowRedisplay(
            code = PaymentMethod.Type.Card.code,
            customerRequestedSave = PaymentSelection.CustomerRequestedSave.RequestReuse,
        )

        val cancellable = terminal().collectSetupIntentPaymentMethod(
            intent = intent,
            allowRedisplay = allowRedisplay.toTerminalAllowRedisplay(),
            config = SetupIntentConfiguration.Builder().build(),
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

    private fun PaymentMethod.AllowRedisplay.toTerminalAllowRedisplay(): AllowRedisplay {
        return when (this) {
            PaymentMethod.AllowRedisplay.UNSPECIFIED -> AllowRedisplay.UNSPECIFIED
            PaymentMethod.AllowRedisplay.LIMITED -> AllowRedisplay.LIMITED
            PaymentMethod.AllowRedisplay.ALWAYS -> AllowRedisplay.ALWAYS
        }
    }

    private suspend fun fetchPaymentMethod(
        intent: SetupIntent,
        customerMetadata: CustomerMetadata,
    ): PaymentMethod {
        val paymentMethodDetails = intent.latestAttempt?.paymentMethodDetails
        val presentDetails = paymentMethodDetails?.cardPresentDetails
            ?: paymentMethodDetails?.interacPresentDetails
        val generatedCardId = presentDetails?.generatedCard
            ?: run {
                errorReporter.report(
                    ErrorReporter
                        .UnexpectedErrorEvent
                        .TAP_TO_ADD_NO_GENERATED_CARD_AFTER_SUCCESSFUL_INTENT_CONFIRMATION
                )

                throw IllegalStateException(
                    "No generated card payment method after collecting through tap!"
                )
            }

        return savedPaymentMethodRepository.retrievePaymentMethod(
            customerMetadata = customerMetadata,
            paymentMethodId = generatedCardId,
        ).getOrThrow()
    }

    private fun terminal() = terminalWrapper.getInstance()
}

internal class UnsupportedTapToAddCollectionHandler : TapToAddCollectionHandler {
    override suspend fun collect(metadata: PaymentMethodMetadata): TapToAddCollectionHandler.CollectionState {
        return TapToAddCollectionHandler.CollectionState.FailedCollection(
            error = IllegalStateException("Not handled!"),
            displayMessage = null,
        )
    }
}
