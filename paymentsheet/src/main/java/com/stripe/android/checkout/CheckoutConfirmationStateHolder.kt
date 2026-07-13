package com.stripe.android.checkout

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentsheet.model.PaymentSelection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns the confirmation [State] for [CheckoutController], persisting it in [SavedStateHandle] so it
 * survives process death. Kept separate from the controller so the confirmation flow can depend on
 * this holder directly rather than reaching back into the controller.
 */
@Singleton
internal class CheckoutConfirmationStateHolder @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val selectionHolder: EmbeddedSelectionHolder,
    @ViewModelScope private val coroutineScope: CoroutineScope,
) {
    var state: State?
        get() = savedStateHandle[CONFIRMATION_STATE_KEY]
        set(value) {
            savedStateHandle[CONFIRMATION_STATE_KEY] = value
        }

    val stateFlow: StateFlow<State?>
        get() = savedStateHandle.getStateFlow(CONFIRMATION_STATE_KEY, null)

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
        val configuration: EmbeddedPaymentElement.Configuration,
    ) : Parcelable

    companion object {
        const val CONFIRMATION_STATE_KEY = "CheckoutController_ConfirmationState"
    }
}
