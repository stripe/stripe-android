package com.stripe.android.common.taptoadd

import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal interface TapToAddHelper {
    fun collect()

    companion object {
        fun create(
            coroutineScope: CoroutineScope,
            tapToAddCollectionHandler: TapToAddCollectionHandler,
            paymentMethodMetadata: PaymentMethodMetadata,
            updateProcessing: (processing: Boolean) -> Unit,
            updateError: (ResolvableString) -> Unit,
        ): TapToAddHelper? {
            return if (paymentMethodMetadata.isTapToAddSupported) {
                DefaultTapToAddHelper(
                    coroutineScope = coroutineScope,
                    paymentMethodMetadata = paymentMethodMetadata,
                    tapToAddCollectionHandler = tapToAddCollectionHandler,
                    updateProcessing = updateProcessing,
                    updateError = updateError,
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
    private val updateProcessing: (processing: Boolean) -> Unit,
    private val updateError: (ResolvableString) -> Unit,
) : TapToAddHelper {
    override fun collect() {
        coroutineScope.launch {
            updateProcessing(true)

            when (val collectionState = tapToAddCollectionHandler.collect(paymentMethodMetadata)) {
                is TapToAddCollectionHandler.CollectionState.Collected -> Unit
                is TapToAddCollectionHandler.CollectionState.FailedCollection -> {
                    updateError(
                        collectionState.displayMessage ?: collectionState.error.stripeErrorMessage()
                    )
                }
            }

            updateProcessing(false)
        }
    }
}
