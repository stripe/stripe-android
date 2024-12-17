package com.stripe.android.paymentelement.embedded

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

@AssistedFactory
@ExperimentalEmbeddedPaymentElementApi
internal fun interface EmbeddedConfirmationStateHolderFactory {
    fun create(coroutineScope: CoroutineScope): EmbeddedConfirmationStateHolder
}

@ExperimentalEmbeddedPaymentElementApi
internal class EmbeddedConfirmationStateHolder @AssistedInject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val selectionHolder: EmbeddedSelectionHolder,
    @Assisted coroutineScope: CoroutineScope,
) {
    var state: State?
        get() = savedStateHandle[CONFIRMATION_STATE_KEY]
        set(value) {
            savedStateHandle[CONFIRMATION_STATE_KEY] = value
        }

    init {
        coroutineScope.launch {
            selectionHolder.selection.collect { selection ->
                state = state?.copy(selection = selection)
            }
        }
    }

    @Parcelize
    data class State(
        val paymentMethodMetadata: PaymentMethodMetadata,
        val selection: PaymentSelection?,
        val initializationMode: PaymentElementLoader.InitializationMode,
        val configuration: EmbeddedPaymentElement.Configuration,
    ) : Parcelable

    companion object {
        const val CONFIRMATION_STATE_KEY = "CONFIRMATION_STATE_KEY"
    }
}
