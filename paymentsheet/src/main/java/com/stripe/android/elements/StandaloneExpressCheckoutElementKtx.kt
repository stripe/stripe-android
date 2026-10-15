package com.stripe.android.elements

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
import com.stripe.android.elements.ece.callbacks
import com.stripe.android.elements.ece.createStandaloneExpressCheckoutElement
import com.stripe.android.paymentsheet.PaymentSheetResultCallback
import com.stripe.android.utils.rememberActivity
import java.util.UUID

/**
 * Creates a [StandaloneExpressCheckoutElement], remembered across compositions.
 * Call unconditionally during initialization, before the lifecycle owner is started.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
fun rememberStandaloneExpressCheckoutElement(
    builder: StandaloneExpressCheckoutElement.Builder,
): StandaloneExpressCheckoutElement {
    val identifier = rememberSaveable { UUID.randomUUID().toString() }
    val lifecycleOwner = LocalLifecycleOwner.current
    val viewModelStoreOwner = requireNotNull(LocalViewModelStoreOwner.current) {
        "StandaloneExpressCheckoutElement requires a ViewModelStoreOwner."
    }
    val registryOwner = requireNotNull(LocalActivityResultRegistryOwner.current) {
        "StandaloneExpressCheckoutElement requires an ActivityResultRegistryOwner."
    }
    val activity = rememberActivity { "StandaloneExpressCheckoutElement requires an Activity." }
    val onResult by rememberUpdatedState(builder.resultCallback)
    val element = remember(lifecycleOwner, viewModelStoreOwner, registryOwner) {
        createStandaloneExpressCheckoutElement(
            activity = activity,
            activityResultCaller = PaymentElementActivityResultCaller(
                key = "ExpressCheckoutElement(instance = $identifier)",
                registryOwner = registryOwner,
            ),
            lifecycleOwner = lifecycleOwner,
            viewModelStoreOwner = viewModelStoreOwner,
            callbackIdentifier = identifier,
            initializedViaCompose = true,
            resultCallback = PaymentSheetResultCallback { onResult.onPaymentSheetResult(it) },
        )
    }
    UpdateCallbacks(identifier, builder.callbacks())
    return element
}
