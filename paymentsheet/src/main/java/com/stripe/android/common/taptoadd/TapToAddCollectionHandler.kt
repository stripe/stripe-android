package com.stripe.android.common.taptoadd

import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentelement.TapToAddPreview

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
        ): TapToAddCollectionHandler {
            return if (isStripeTerminalSdkAvailable()) {
                DefaultTapToAddCollectionHandler(connectionManager)
            } else {
                UnsupportedTapToAddCollectionHandler()
            }
        }
    }
}

@OptIn(TapToAddPreview::class)
internal class DefaultTapToAddCollectionHandler(
    private val connectionManager: TapToAddConnectionManager,
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
    }.fold(
        onSuccess = { TapToAddCollectionHandler.CollectionState.Collected },
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
