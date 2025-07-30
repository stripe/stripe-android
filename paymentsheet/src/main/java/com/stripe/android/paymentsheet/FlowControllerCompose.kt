package com.stripe.android.paymentsheet

import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.stripe.android.common.ui.UpdateCallbacks
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbacks
import com.stripe.android.paymentsheet.flowcontroller.FlowControllerFactory
import com.stripe.android.utils.rememberActivity
import java.util.UUID

@Composable
internal fun internalRememberPaymentSheetFlowController(
    callbacks: PaymentElementCallbacks,
    paymentOptionCallback: PaymentOptionCallback,
    paymentResultCallback: PaymentSheetResultCallback,
): PaymentSheet.FlowController {
    val paymentElementCallbackIdentifier = rememberSaveable {
        UUID.randomUUID().toString()
    }

    UpdateCallbacks(paymentElementCallbackIdentifier, callbacks)

    val viewModelStoreOwner = requireNotNull(LocalViewModelStoreOwner.current) {
        "PaymentSheet.FlowController must be created with access to a ViewModelStoreOwner"
    }

    val activityResultRegistryOwner = requireNotNull(LocalActivityResultRegistryOwner.current) {
        "PaymentSheet.FlowController must be created with access to a ActivityResultRegistryOwner"
    }

    val lifecycleOwner = LocalLifecycleOwner.current

    val activity = rememberActivity {
        "PaymentSheet.FlowController must be created in the context of an Activity"
    }

    return remember(paymentOptionCallback, paymentResultCallback) {
        FlowControllerFactory(
            viewModelStoreOwner = viewModelStoreOwner,
            lifecycleOwner = lifecycleOwner,
            activityResultRegistryOwner = activityResultRegistryOwner,
            statusBarColor = { activity.window?.statusBarColor },
            paymentOptionCallback = paymentOptionCallback,
            paymentResultCallback = paymentResultCallback,
            paymentElementCallbackIdentifier = paymentElementCallbackIdentifier,
            initializedViaCompose = true,
        ).create()
    }
}
