package com.stripe.android.common.taptoadd

import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentelement.TapToAddPreview
import com.stripe.android.paymentsheet.CreateIntentResult

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
            connectionManager: TapToAddConnectionManager,
            createCardPresentSetupIntentCallbackRetriever: CreateCardPresentSetupIntentCallbackRetriever,
        ): TapToAddCollectionHandler {
            return if (isStripeTerminalSdkAvailable()) {
                DefaultTapToAddCollectionHandler(connectionManager, createCardPresentSetupIntentCallbackRetriever)
            } else {
                UnsupportedTapToAddCollectionHandler()
            }
        }
    }
}

@OptIn(TapToAddPreview::class)
internal class DefaultTapToAddCollectionHandler(
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
        } catch (error: CreateCardPresentSetupIntentCallbackNotFoundException) {
            return@runCatching TapToAddCollectionHandler.CollectionState.FailedCollection(
                error = error,
                displayMessage = error.resolvableError,
            )
        }

        when (val result = callback.createCardPresentSetupIntent()) {
            is CreateIntentResult.Success -> TapToAddCollectionHandler.CollectionState.Collected
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
}

internal class UnsupportedTapToAddCollectionHandler : TapToAddCollectionHandler {
    override suspend fun collect(metadata: PaymentMethodMetadata): TapToAddCollectionHandler.CollectionState {
        return TapToAddCollectionHandler.CollectionState.FailedCollection(
            error = IllegalStateException("Not handled!"),
            displayMessage = null,
        )
    }
}
