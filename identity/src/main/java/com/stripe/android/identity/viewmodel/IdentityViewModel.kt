package com.stripe.android.identity.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.stripe.android.identity.IdentityVerificationSheetContract

internal class IdentityViewModel(
    val args: IdentityVerificationSheetContract.Args
) : ViewModel() {
    internal class IdentityViewModelFactory(
        private val args: IdentityVerificationSheetContract.Args
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return IdentityViewModel(args) as T
        }
    }
}
