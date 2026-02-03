package com.stripe.android.common.taptoadd

import androidx.lifecycle.SavedStateHandle
import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.verticalmode.toDisplayableSavedPaymentMethod
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal interface TapToAddHelper {
    val collectedPaymentMethod: StateFlow<DisplayableSavedPaymentMethod?>
    val hasPreviouslyAttemptedCollection: Boolean

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
                    savedStateHandle = SavedStateHandle(),
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
    private val savedStateHandle: SavedStateHandle,
    private val paymentMethodMetadata: PaymentMethodMetadata,
    private val onCollectingUpdated: (collecting: Boolean) -> Unit,
    private val onError: (ResolvableString) -> Unit,
) : TapToAddHelper {
    private val _collectedPaymentMethod = MutableStateFlow<DisplayableSavedPaymentMethod?>(null)
    override val collectedPaymentMethod = _collectedPaymentMethod.asStateFlow()

    private var _hasPreviouslyAttemptedCollection
        get() = savedStateHandle.get<Boolean>(PREVIOUSLY_COLLECTED_WITH_TAP_TO_AD_KEY) == true
        set(value) {
            savedStateHandle[PREVIOUSLY_COLLECTED_WITH_TAP_TO_AD_KEY] = value
        }

    override val hasPreviouslyAttemptedCollection: Boolean
        get() = _hasPreviouslyAttemptedCollection

    override fun startPaymentMethodCollection() {
        _hasPreviouslyAttemptedCollection = true

        coroutineScope.launch {
            onCollectingUpdated(true)

            when (val collectionState = tapToAddCollectionHandler.collect(paymentMethodMetadata)) {
                is TapToAddCollectionHandler.CollectionState.Collected -> {
                    _collectedPaymentMethod.value = collectionState.paymentMethod.toDisplayableSavedPaymentMethod(
                        paymentMethodMetadata = paymentMethodMetadata,
                        defaultPaymentMethodId = null,
                    )
                }
                is TapToAddCollectionHandler.CollectionState.FailedCollection -> {
                    onError(
                        collectionState.displayMessage ?: collectionState.error.stripeErrorMessage()
                    )
                }
            }

            onCollectingUpdated(false)
        }
    }

    private companion object {
        const val PREVIOUSLY_COLLECTED_WITH_TAP_TO_AD_KEY = "PREVIOUSLY_COLLECTED_WITH_TAP_TO_ADD"
    }
}
