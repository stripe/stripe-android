package com.stripe.android.common.taptoadd.ui

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.link.ui.inline.UserInput
import com.stripe.android.model.PaymentMethod
import kotlinx.parcelize.Parcelize
import javax.inject.Inject
import javax.inject.Singleton

internal interface TapToAddStateHolder {
    val state: State?

    fun setState(state: State?)

    sealed interface State : Parcelable {
        @Parcelize
        data class CardAdded(val paymentMethod: PaymentMethod) : State

        @Parcelize
        data class Confirmation(
            val paymentMethod: PaymentMethod,
            val linkInput: UserInput?,
        ) : State
    }
}

@Singleton
internal class DefaultTapToAddStateHolder @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
) : TapToAddStateHolder {
    override val state: TapToAddStateHolder.State?
        get() = savedStateHandle[TAP_TO_ADD_STATE_KEY]

    override fun setState(state: TapToAddStateHolder.State?) {
        savedStateHandle[TAP_TO_ADD_STATE_KEY] = state
    }

    private companion object {
        const val TAP_TO_ADD_STATE_KEY = "TAP_TO_ADD_STATE"
    }
}
