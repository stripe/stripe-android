package com.stripe.android.checkout

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

internal sealed interface CheckoutElementsControllerState : Parcelable {
    @Parcelize
    data class Loading(
        val state: CheckoutElementsControllerLoadingState,
    ) : CheckoutElementsControllerState

    @Parcelize
    data class Loaded(
        val state: CheckoutElementsControllerLoadedState,
    ) : CheckoutElementsControllerState
}
