package com.stripe.android.paymentsheet

import android.app.Application
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.stripe.android.common.ui.UpdateCallbacks
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbacks
import com.stripe.android.utils.rememberActivity
import java.util.UUID

@Composable
internal fun internalRememberPaymentSheet(
    callbacks: PaymentElementCallbacks,
    paymentResultCallback: PaymentSheetResultCallback,
): PaymentSheet {
    val paymentElementCallbackIdentifier = rememberSaveable {
        UUID.randomUUID().toString()
    }

    UpdateCallbacks(paymentElementCallbackIdentifier, callbacks)

    val onResult by rememberUpdatedState(newValue = paymentResultCallback::onPaymentSheetResult)

    val activityResultLauncher = rememberLauncherForActivityResult(
        contract = PaymentSheetContract(),
        onResult = onResult,
    )

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val activity = rememberActivity {
        "PaymentSheet must be created in the context of an Activity"
    }

    return remember(paymentResultCallback) {
        val launcher = DefaultPaymentSheetLauncher(
            activityResultLauncher = activityResultLauncher,
            activity = activity,
            application = context.applicationContext as Application,
            lifecycleOwner = lifecycleOwner,
            callback = paymentResultCallback,
            paymentElementCallbackIdentifier = paymentElementCallbackIdentifier,
            initializedViaCompose = true,
        )
        PaymentSheet(launcher)
    }
}
