package com.stripe.android.paymentsheet.addresselement

import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

internal class AddressElementActivityCoordinator(
    initialArgs: AddressElementActivityContract.Args,
) {
    var args by mutableStateOf(initialArgs)
        private set

    fun handleNewIntent(intent: Intent, isFinishing: Boolean): Boolean {
        val updatedArgs = AddressElementActivityContract.Args.fromIntent(intent) ?: return false
        val isValidTransition =
            !isFinishing &&
                args is AddressElementActivityContract.Args.CheckoutShipping.Loading &&
                updatedArgs is AddressElementActivityContract.Args.CheckoutShipping.Ready
        if (!isValidTransition) return false

        args = updatedArgs
        return true
    }
}
