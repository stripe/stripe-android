package com.stripe.android.paymentelement

import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.annotation.RestrictTo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.stripe.android.common.ui.PaymentElementActivityResultCaller
import com.stripe.android.common.ui.UpdateCallbacks
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbacks
import com.stripe.android.utils.rememberActivity
import java.util.UUID

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

    val paymentElementCallbackIdentifier = rememberSaveable {
        UUID.randomUUID().toString()
    }

    val callbacks = remember(builder) {
        @OptIn(ExperimentalCustomPaymentMethodsApi::class)
        PaymentElementCallbacks(
            createIntentCallback = builder.createIntentCallback,
            customPaymentMethodConfirmHandler = builder.customPaymentMethodConfirmHandler,
            externalPaymentMethodConfirmHandler = builder.externalPaymentMethodConfirmHandler,
        )
    }

    UpdateCallbacks(paymentElementCallbackIdentifier, callbacks)

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
            activity = activity,
            activityResultCaller = PaymentElementActivityResultCaller(
                key = "EmbeddedPaymentElement(instance = $paymentElementCallbackIdentifier)",
                registryOwner = activityResultRegistryOwner,
            ),
            paymentElementCallbackIdentifier = paymentElementCallbackIdentifier,
            lifecycleOwner = lifecycleOwner,
            viewModelStoreOwner = viewModelStoreOwner,
            resultCallback = onResult,
        )
    }
}
