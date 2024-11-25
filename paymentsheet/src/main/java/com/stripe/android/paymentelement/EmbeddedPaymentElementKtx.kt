package com.stripe.android.paymentelement

import androidx.annotation.RestrictTo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner

@ExperimentalEmbeddedPaymentElementApi
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
fun rememberEmbeddedPaymentElement(): EmbeddedPaymentElement {
    val viewModelStoreOwner = requireNotNull(LocalViewModelStoreOwner.current) {
        "EmbeddedPaymentElement must have a ViewModelStoreOwner."
    }
    return remember {
        EmbeddedPaymentElement.create(
            viewModelStoreOwner = viewModelStoreOwner,
        )
    }
}
