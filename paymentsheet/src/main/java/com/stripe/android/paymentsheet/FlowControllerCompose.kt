package com.stripe.android.paymentsheet

import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.stripe.android.common.ui.UpdateCallbacks
import com.stripe.android.core.utils.StatusBarCompat
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbacks
import com.stripe.android.paymentsheet.flowcontroller.FlowControllerFactory
import com.stripe.android.utils.rememberActivity
import java.util.UUID

/**
 * Creates a [PaymentSheet.FlowController] that is remembered across compositions.
 *
 * This *must* be called unconditionally, as part of the initialization path.
 *
 * @param paymentOptionCallback Called when the customer's desired payment method changes.
 * @param paymentResultCallback Called when a [PaymentSheetResult] is available.
 */
@Composable
@Deprecated(
    message = "This will be removed in a future release. Use FlowController.Builder instead.",
    replaceWith = ReplaceWith(
        "remember(paymentOptionCallback, paymentResultCallback) { " +
            "PaymentSheet.FlowController.Builder(paymentResultCallback, paymentOptionCallback) " +
            "}.build()"
    )
)
fun rememberPaymentSheetFlowController(
    paymentOptionCallback: PaymentOptionCallback,
    paymentResultCallback: PaymentSheetResultCallback,
): PaymentSheet.FlowController {
    val callbacks = remember {
        PaymentElementCallbacks.Builder()
            .build()
    }

    return internalRememberPaymentSheetFlowController(
        paymentOptionCallback = paymentOptionCallback,
        paymentResultCallback = paymentResultCallback,
        callbacks = callbacks,
    )
}

/**
 * Creates a [PaymentSheet.FlowController] that is remembered across compositions. Use this method
 * when you intend to create the [com.stripe.android.model.PaymentIntent] or
 * [com.stripe.android.model.SetupIntent] on your server.
 *
 * This *must* be called unconditionally, as part of the initialization path.
 *
 * @param createIntentCallback Called when the customer confirms the payment or setup.
 * @param paymentOptionCallback Called when the customer's desired payment method changes.
 * @param paymentResultCallback Called when a [PaymentSheetResult] is available.
 */
@Composable
@Deprecated(
    message = "This will be removed in a future release. Use FlowController.Builder instead.",
    replaceWith = ReplaceWith(
        "remember(createIntentCallback, paymentOptionCallback, paymentResultCallback) { " +
            "PaymentSheet.FlowController.Builder(paymentResultCallback, paymentOptionCallback)" +
            ".createIntentCallback(createIntentCallback) " +
            "}.build()"
    )
)
fun rememberPaymentSheetFlowController(
    createIntentCallback: CreateIntentCallback,
    paymentOptionCallback: PaymentOptionCallback,
    paymentResultCallback: PaymentSheetResultCallback,
): PaymentSheet.FlowController {
    val callbacks = remember(createIntentCallback) {
        PaymentElementCallbacks.Builder()
            .createIntentCallback(createIntentCallback)
            .build()
    }

    return internalRememberPaymentSheetFlowController(
        paymentOptionCallback = paymentOptionCallback,
        paymentResultCallback = paymentResultCallback,
        callbacks = callbacks,
    )
}

/**
 * Creates a [PaymentSheet.FlowController] that is remembered across compositions. Use this method if you implement any
 * external payment methods, as specified in your [PaymentSheet.Configuration].
 *
 * This *must* be called unconditionally, as part of the initialization path.
 *
 * If you intend to create the [com.stripe.android.model.PaymentIntent] or [com.stripe.android.model.SetupIntent] on
 * your server, include a [createIntentCallback].
 *
 * @param createIntentCallback If specified, called when the customer confirms the payment or setup.
 * @param externalPaymentMethodConfirmHandler Called when a user confirms payment for an external payment method.
 * @param paymentOptionCallback Called when the customer's desired payment method changes.
 * @param paymentResultCallback Called when a [PaymentSheetResult] is available.
 */
@Composable
@Deprecated(
    message = "This will be removed in a future release. Use FlowController.Builder instead.",
    replaceWith = ReplaceWith(
        "remember(" +
            "createIntentCallback, " +
            "externalPaymentMethodConfirmHandler, " +
            "paymentOptionCallback, " +
            "paymentResultCallback" +
            ") { " +
            "PaymentSheet.FlowController.Builder(paymentResultCallback, paymentOptionCallback)" +
            ".createIntentCallback(createIntentCallback)" +
            ".externalPaymentMethodConfirmHandler(externalPaymentMethodConfirmHandler) " +
            "}.build()"
    )
)
fun rememberPaymentSheetFlowController(
    createIntentCallback: CreateIntentCallback? = null,
    externalPaymentMethodConfirmHandler: ExternalPaymentMethodConfirmHandler,
    paymentOptionCallback: PaymentOptionCallback,
    paymentResultCallback: PaymentSheetResultCallback,
): PaymentSheet.FlowController {
    val callbacks = remember(createIntentCallback, externalPaymentMethodConfirmHandler) {
        PaymentElementCallbacks.Builder()
            .createIntentCallback(createIntentCallback)
            .externalPaymentMethodConfirmHandler(externalPaymentMethodConfirmHandler)
            .build()
    }

    return internalRememberPaymentSheetFlowController(
        paymentOptionCallback = paymentOptionCallback,
        paymentResultCallback = paymentResultCallback,
        callbacks = callbacks,
    )
}

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
            statusBarColor = { StatusBarCompat.color(activity) },
            paymentOptionCallback = paymentOptionCallback,
            paymentResultCallback = paymentResultCallback,
            paymentElementCallbackIdentifier = paymentElementCallbackIdentifier,
            initializedViaCompose = true,
        ).create()
    }
}
