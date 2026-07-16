package com.stripe.android.paymentelement.embedded.content

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
 * Supplies the confirmation-launch state the reused embedded UI reads to build its launch args — the
 * sheet launcher (via [DefaultEmbeddedContentHelper]) and the wallet buttons. Each integration owns
 * its own source: [EmbeddedPaymentElement][com.stripe.android.paymentelement.EmbeddedPaymentElement]
 * uses [EmbeddedConfirmationStateHolder] (written imperatively as it loads), while checkout has
 * [com.stripe.android.checkout.CheckoutControllerStateHolder] project its already-owned state.
 */
internal interface EmbeddedConfirmationStateDataSource {
    val embeddedConfirmationState: StateFlow<EmbeddedConfirmationStateHolder.State?>
}

@Singleton
internal class EmbeddedConfirmationStateHolder @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val selectionHolder: EmbeddedSelectionHolder,
    @ViewModelScope private val coroutineScope: CoroutineScope,
) : EmbeddedConfirmationStateDataSource {
    var state: State?
        get() = savedStateHandle[CONFIRMATION_STATE_KEY]
        set(value) {
            savedStateHandle[CONFIRMATION_STATE_KEY] = value
        }

    val stateFlow: StateFlow<State?>
        get() = savedStateHandle.getStateFlow(CONFIRMATION_STATE_KEY, null)

    override val embeddedConfirmationState: StateFlow<State?>
        get() = stateFlow

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
        const val CONFIRMATION_STATE_KEY = "CONFIRMATION_STATE_KEY"
    }
}
