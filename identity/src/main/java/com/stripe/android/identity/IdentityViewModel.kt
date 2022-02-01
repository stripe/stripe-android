package com.stripe.android.identity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.stripe.android.identity.camera.IdentityScanFlow

internal class IdentityViewModel : ViewModel() {
    internal val identityScanFlow = IdentityScanFlow()

    internal class IdentityViewModelFactory :
        ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return IdentityViewModel() as T
        }
    }
}
