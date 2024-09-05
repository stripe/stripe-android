package com.stripe.android.paymentsheet.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner

@Composable
fun ViewModelStoreOwnerContext(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalViewModelStoreOwner provides object : ViewModelStoreOwner {
            override val viewModelStore: ViewModelStore = ViewModelStore()
        }
    ) {
        content()
    }
}
