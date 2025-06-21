package com.stripe.onramp

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.onramp.model.OnRampConfiguration

internal class OnRampCoordinatorViewModel(
    val handle: SavedStateHandle,
) : ViewModel() {

    var paymentSelection: PaymentSelection?
        get() = handle["paymentSelection"]
        set(value) = handle.set("paymentSelection", value)

    var onRampConfiguration: OnRampConfiguration?
        get() = handle["configuration"]
        set(value) = handle.set("configuration", value)

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            return OnRampCoordinatorViewModel(
                handle = extras.createSavedStateHandle(),
            ) as T
        }
    }
}
