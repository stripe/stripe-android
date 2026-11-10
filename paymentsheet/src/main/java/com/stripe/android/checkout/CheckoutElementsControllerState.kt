package com.stripe.android.checkout

import android.os.Parcelable
import com.stripe.android.paymentelement.CheckoutSessionPreview
import kotlinx.parcelize.Parcelize

@OptIn(CheckoutSessionPreview::class)
internal sealed interface CheckoutElementsControllerState : Parcelable {
    val configuration: CheckoutElementsController.Configuration.State
        get() = when (this) {
            is Loading -> state.configuration
            is Loaded -> state.configuration
        }

    @Parcelize
    data class Loading(
        val state: CheckoutElementsControllerLoadingState,
    ) : CheckoutElementsControllerState

    @Parcelize
    data class Loaded(
        val state: CheckoutElementsControllerLoadedState,
    ) : CheckoutElementsControllerState
}
