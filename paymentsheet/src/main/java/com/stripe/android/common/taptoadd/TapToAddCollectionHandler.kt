package com.stripe.android.common.taptoadd

import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentelement.TapToAddPreview
import com.stripe.android.paymentelement.confirmation.intent.CallbackNotFoundException
import com.stripe.android.paymentsheet.CreateIntentResult
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.stripeterminal.external.callable.Callback
import com.stripe.stripeterminal.external.callable.Cancelable
import com.stripe.stripeterminal.external.callable.SetupIntentCallback
import com.stripe.stripeterminal.external.models.AllowRedisplay
import com.stripe.stripeterminal.external.models.SetupIntent
import com.stripe.stripeterminal.external.models.SetupIntentConfiguration
import com.stripe.stripeterminal.external.models.TerminalException
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.Continuation
import kotlin.coroutines.suspendCoroutine

internal interface TapToAddCollectionHandler {
    suspend fun collect(metadata: PaymentMethodMetadata): CollectionState

    sealed interface CollectionState {
        data object Collected : CollectionState

        data class FailedCollection(
            val error: Throwable,
            val displayMessage: ResolvableString?,
        ) : CollectionState
    }

    companion object {
        @OptIn(TapToAddPreview::class)
        fun create(
            isStripeTerminalSdkAvailable: IsStripeTerminalSdkAvailable,
            terminalWrapper: TerminalWrapper,
            connectionManager: TapToAddConnectionManager,
            createCardPresentSetupIntentCallbackRetriever: CreateCardPresentSetupIntentCallbackRetriever,
        ): TapToAddCollectionHandler {
            return if (isStripeTerminalSdkAvailable()) {
                DefaultTapToAddCollectionHandler(
                    terminalWrapper = terminalWrapper,
                    connectionManager = connectionManager,
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
    private val connectionManager: TapToAddConnectionManager,
    private val createCardPresentSetupIntentCallbackRetriever: CreateCardPresentSetupIntentCallbackRetriever,
) : TapToAddCollectionHandler {
    override suspend fun collect(
        metadata: PaymentMethodMetadata
    ): TapToAddCollectionHandler.CollectionState = runCatching {
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
            is CreateIntentResult.Success -> collectWithIntent(result.clientSecret, metadata)
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
            TapToAddCollectionHandler.CollectionState.FailedCollection(
                error = error,
                displayMessage = error.stripeErrorMessage()
            )
        }
    )

    private suspend fun collectWithIntent(
        clientSecret: String,
        metadata: PaymentMethodMetadata,
    ): TapToAddCollectionHandler.CollectionState {
        val setupIntent = retrieveSetupIntent(clientSecret)
        val setupIntentWithAttachedPaymentMethod = collectPaymentMethod(setupIntent, metadata)

        confirmSetupIntent(setupIntentWithAttachedPaymentMethod)

        return TapToAddCollectionHandler.CollectionState.Collected
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
