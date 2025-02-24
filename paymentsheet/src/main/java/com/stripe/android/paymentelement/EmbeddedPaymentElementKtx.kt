package com.stripe.android.paymentelement

import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.annotation.RestrictTo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.stripe.android.common.ui.PaymentElementActivityResultCaller
import com.stripe.android.common.ui.UpdateExternalPaymentMethodConfirmHandler
import com.stripe.android.common.ui.UpdateIntentConfirmationInterceptor
import com.stripe.android.utils.rememberActivity

/**
 * Creates an [EmbeddedPaymentElement] that is remembered across compositions.
 *
 * This *must* be called unconditionally as part of the initialization path.
 */
@ExperimentalEmbeddedPaymentElementApi
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
fun rememberEmbeddedPaymentElement(
    builder: EmbeddedPaymentElement.Builder
): EmbeddedPaymentElement {
    val viewModelStoreOwner = requireNotNull(LocalViewModelStoreOwner.current) {
        "EmbeddedPaymentElement must have a ViewModelStoreOwner."
    }

    UpdateExternalPaymentMethodConfirmHandler(builder.externalPaymentMethodConfirmHandler)
    UpdateIntentConfirmationInterceptor(builder.createIntentCallback)

    val lifecycleOwner = LocalLifecycleOwner.current
    val activityResultRegistryOwner = requireNotNull(LocalActivityResultRegistryOwner.current) {
        "EmbeddedPaymentElement must have an ActivityResultRegistryOwner."
    }

    val onResult by rememberUpdatedState(newValue = builder.resultCallback::onResult)

    val activity = rememberActivity {
        "EmbeddedPaymentElement must be created in the context of an Activity."
    }

    return remember {
        EmbeddedPaymentElement.create(
            statusBarColor = activity.window?.statusBarColor,
            activityResultCaller = PaymentElementActivityResultCaller(
                key = "Embedded",
                registryOwner = activityResultRegistryOwner,
            ),
            lifecycleOwner = lifecycleOwner,
            viewModelStoreOwner = viewModelStoreOwner,
            resultCallback = onResult,
        )
    }
}
