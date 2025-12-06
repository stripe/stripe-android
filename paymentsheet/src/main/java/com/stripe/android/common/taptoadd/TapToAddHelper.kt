package com.stripe.android.common.taptoadd

import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal interface TapToAddHelper {
    /**
     * Begins collection of payment method from the Tap to Add flow. Calling this method should show a screen that
     * indicates where to tap your card on your device.
     */
    fun startPaymentMethodCollection()

    companion object {
        fun create(
            coroutineScope: CoroutineScope,
            tapToAddCollectionHandler: TapToAddCollectionHandler,
            paymentMethodMetadata: PaymentMethodMetadata,
            onCollectingUpdated: (processing: Boolean) -> Unit,
            onError: (ResolvableString) -> Unit,
        ): TapToAddHelper? {
            return if (paymentMethodMetadata.isTapToAddSupported) {
                DefaultTapToAddHelper(
                    coroutineScope = coroutineScope,
                    paymentMethodMetadata = paymentMethodMetadata,
                    tapToAddCollectionHandler = tapToAddCollectionHandler,
                    onCollectingUpdated = onCollectingUpdated,
                    onError = onError,
                )
            } else {
                null
            }
        }
    }
}

internal class DefaultTapToAddHelper(
    private val coroutineScope: CoroutineScope,
    private val tapToAddCollectionHandler: TapToAddCollectionHandler,
    private val paymentMethodMetadata: PaymentMethodMetadata,
    private val onCollectingUpdated: (collecting: Boolean) -> Unit,
    private val onError: (ResolvableString) -> Unit,
) : TapToAddHelper {
    override fun startPaymentMethodCollection() {
        coroutineScope.launch {
            onCollectingUpdated(true)

            when (val collectionState = tapToAddCollectionHandler.collect(paymentMethodMetadata)) {
                is TapToAddCollectionHandler.CollectionState.Collected -> Unit
                is TapToAddCollectionHandler.CollectionState.FailedCollection -> {
                    onError(
                        collectionState.displayMessage ?: collectionState.error.stripeErrorMessage()
                    )
                }
            }

            onCollectingUpdated(false)
        }
    }
}
