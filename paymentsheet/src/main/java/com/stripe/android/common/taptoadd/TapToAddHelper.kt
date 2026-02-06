package com.stripe.android.common.taptoadd

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

internal interface TapToAddHelper {
    val hasPreviouslyAttemptedCollection: Boolean

    /**
     * Begins collection of payment method from the Tap to Add flow. Calling this method should show a screen that
     * indicates where to tap your card on your device.
     */
    fun startPaymentMethodCollection(paymentMethodMetadata: PaymentMethodMetadata)
}

internal class DefaultTapToAddHelper @Inject constructor(
    @ViewModelScope private val coroutineScope: CoroutineScope,
    private val tapToAddCollectionHandler: TapToAddCollectionHandler,
    private val savedStateHandle: SavedStateHandle,
) : TapToAddHelper {
    constructor(
        viewModel: BaseSheetViewModel
    ) : this(
        coroutineScope = viewModel.viewModelScope,
        tapToAddCollectionHandler = viewModel.tapToAddCollectionHandler,
        savedStateHandle = viewModel.savedStateHandle,
    )

    private var _hasPreviouslyAttemptedCollection
        get() = savedStateHandle.get<Boolean>(PREVIOUSLY_COLLECTED_WITH_TAP_TO_ADD_KEY) == true
        set(value) {
            savedStateHandle[PREVIOUSLY_COLLECTED_WITH_TAP_TO_ADD_KEY] = value
        }

    override val hasPreviouslyAttemptedCollection: Boolean
        get() = _hasPreviouslyAttemptedCollection

    override fun startPaymentMethodCollection(paymentMethodMetadata: PaymentMethodMetadata) {
        _hasPreviouslyAttemptedCollection = true

        coroutineScope.launch {
            tapToAddCollectionHandler.collect(paymentMethodMetadata)
        }
    }

    private companion object {
        const val PREVIOUSLY_COLLECTED_WITH_TAP_TO_ADD_KEY = "PREVIOUSLY_COLLECTED_WITH_TAP_TO_ADD"
    }
}
