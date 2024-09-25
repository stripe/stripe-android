package com.stripe.android.paymentsheet

import android.app.Application
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.stripe.android.utils.rememberActivity

/**
 * Creates a [PaymentSheet] that is remembered across compositions.
 *
 * This *must* be called unconditionally, as part of the initialization path.
 *
 * @param paymentResultCallback Called with the result of the payment after [PaymentSheet] is dismissed.
 */
@Composable
fun rememberPaymentSheet(
    paymentResultCallback: PaymentSheetResultCallback,
): PaymentSheet {
    val onResult by rememberUpdatedState(newValue = paymentResultCallback::onPaymentSheetResult)

    val activityResultLauncher = rememberLauncherForActivityResult(
        contract = PaymentSheetContractV2(),
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
            initializedViaCompose = true,
        )
        PaymentSheet(launcher)
    }
}

/**
 * Creates a [PaymentSheet] that is remembered across compositions. Use this method when you intend
 * to create the [com.stripe.android.model.PaymentIntent] or [com.stripe.android.model.SetupIntent]
 * on your server.
 *
 * This *must* be called unconditionally, as part of the initialization path.
 *
 * @param createIntentCallback Called when the customer confirms the payment or setup.
 * @param paymentResultCallback Called with the result of the payment after [PaymentSheet] is dismissed.
 */
@Composable
fun rememberPaymentSheet(
    createIntentCallback: CreateIntentCallback,
    paymentResultCallback: PaymentSheetResultCallback,
): PaymentSheet {
    UpdateIntentConfirmationInterceptor(createIntentCallback)
    return rememberPaymentSheet(paymentResultCallback)
}

/**
 * Creates a [PaymentSheet] that is remembered across compositions. Use this method if you implement any external
 * payment methods, as specified in your [PaymentSheet.Configuration].
 *
 * This *must* be called unconditionally, as part of the initialization path.
 *
 * If you intend to create the [com.stripe.android.model.PaymentIntent] or [com.stripe.android.model.SetupIntent] on
 * your server, include a [createIntentCallback].
 *
 * @param createIntentCallback If specified, called when the customer confirms the payment or setup.
 * @param paymentResultCallback Called with the result of the payment after [PaymentSheet] is dismissed.
 * @param externalPaymentMethodConfirmHandler Called when a user confirms payment for an external payment method.
 */
@Composable
fun rememberPaymentSheet(
    createIntentCallback: CreateIntentCallback? = null,
    externalPaymentMethodConfirmHandler: ExternalPaymentMethodConfirmHandler,
    paymentResultCallback: PaymentSheetResultCallback,
): PaymentSheet {
    UpdateExternalPaymentMethodConfirmHandler(externalPaymentMethodConfirmHandler)
    return if (createIntentCallback == null) {
        rememberPaymentSheet(paymentResultCallback)
    } else {
        rememberPaymentSheet(createIntentCallback, paymentResultCallback)
    }
}

@Composable
private fun UpdateIntentConfirmationInterceptor(
    createIntentCallback: CreateIntentCallback,
) {
    LaunchedEffect(createIntentCallback) {
        IntentConfirmationInterceptor.createIntentCallback = createIntentCallback
    }
}

@Composable
private fun UpdateExternalPaymentMethodConfirmHandler(
    externalPaymentMethodConfirmHandler: ExternalPaymentMethodConfirmHandler,
) {
    LaunchedEffect(externalPaymentMethodConfirmHandler) {
        ExternalPaymentMethodInterceptor.externalPaymentMethodConfirmHandler = externalPaymentMethodConfirmHandler
    }
}
