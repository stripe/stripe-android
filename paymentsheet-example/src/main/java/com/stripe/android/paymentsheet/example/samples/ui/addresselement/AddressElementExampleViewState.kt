package com.stripe.android.paymentsheet.example.samples.ui.addresselement

import com.stripe.android.paymentsheet.addresselement.AddressDetails

sealed interface AddressElementExampleViewState {

    object Loading : AddressElementExampleViewState

    data class Content(
        val publishableKey: String,
        val address: AddressDetails? = null,
        val status: String? = null,
    ) : AddressElementExampleViewState

    data class Error(
        val message: String,
    ) : AddressElementExampleViewState
}
