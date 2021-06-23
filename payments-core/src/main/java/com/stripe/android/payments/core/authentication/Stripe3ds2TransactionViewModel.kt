package com.stripe.android.payments.core.authentication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

internal class Stripe3ds2TransactionViewModel : ViewModel() {
    // TODO(mshafrir-stripe): move logic from Stripe3ds2Authenticator into ViewModel
}

internal class Stripe3ds2TransactionViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return Stripe3ds2TransactionViewModel() as T
    }
}
