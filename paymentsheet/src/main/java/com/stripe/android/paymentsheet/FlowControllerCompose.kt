package com.stripe.android.paymentsheet

import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.stripe.android.paymentsheet.confirmation.IntentConfirmationInterceptor
import com.stripe.android.paymentsheet.flowcontroller.FlowControllerFactory
import com.stripe.android.utils.rememberActivity

/**
 * Creates a [PaymentSheet.FlowController] that is remembered across compositions.
 *
 * This *must* be called unconditionally, as part of the initialization path.
 *
 * @param paymentOptionCallback Called when the customer's desired payment method changes.
 * @param paymentResultCallback Called when a [PaymentSheetResult] is available.
 */
@Composable
fun rememberPaymentSheetFlowController(
    paymentOptionCallback: PaymentOptionCallback,
    paymentResultCallback: PaymentSheetResultCallback,
): PaymentSheet.FlowController {
    return internalRememberPaymentSheetFlowController(
        paymentOptionCallback = paymentOptionCallback,
        paymentResultCallback = paymentResultCallback,
        createIntentCallback = null,
        externalPaymentMethodConfirmHandler = null,
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
fun rememberPaymentSheetFlowController(
    createIntentCallback: CreateIntentCallback,
    paymentOptionCallback: PaymentOptionCallback,
    paymentResultCallback: PaymentSheetResultCallback,
): PaymentSheet.FlowController {
    return internalRememberPaymentSheetFlowController(
        paymentOptionCallback = paymentOptionCallback,
        paymentResultCallback = paymentResultCallback,
        createIntentCallback = createIntentCallback,
        externalPaymentMethodConfirmHandler = null,
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
fun rememberPaymentSheetFlowController(
    createIntentCallback: CreateIntentCallback? = null,
    externalPaymentMethodConfirmHandler: ExternalPaymentMethodConfirmHandler,
    paymentOptionCallback: PaymentOptionCallback,
    paymentResultCallback: PaymentSheetResultCallback,
): PaymentSheet.FlowController {
    return internalRememberPaymentSheetFlowController(
        paymentOptionCallback = paymentOptionCallback,
        paymentResultCallback = paymentResultCallback,
        createIntentCallback = createIntentCallback,
        externalPaymentMethodConfirmHandler = externalPaymentMethodConfirmHandler
    )
}

/**
 * Creates a [PaymentSheet.FlowController] that is remembered across compositions.
 *
 * @param builder which contains required [PaymentOptionCallback] and [PaymentSheetResultCallback] as well as
 * other optional callbacks.
 */
@Composable
internal fun rememberPaymentSheetFlowController(
    builder: PaymentSheet.FlowController.Builder
): PaymentSheet.FlowController {
    return internalRememberPaymentSheetFlowController(
        paymentOptionCallback = builder.paymentOptionCallback,
        paymentResultCallback = builder.resultCallback
    )
}

@Composable
private fun internalRememberPaymentSheetFlowController(
    paymentOptionCallback: PaymentOptionCallback,
    paymentResultCallback: PaymentSheetResultCallback,
): PaymentSheet.FlowController {
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
        ).create()
    }
}

@Composable
private fun internalRememberPaymentSheetFlowController(
    createIntentCallback: CreateIntentCallback?,
    externalPaymentMethodConfirmHandler: ExternalPaymentMethodConfirmHandler?,
    paymentOptionCallback: PaymentOptionCallback,
    paymentResultCallback: PaymentSheetResultCallback,
): PaymentSheet.FlowController {
    UpdateIntentConfirmationInterceptor(createIntentCallback)
    UpdateExternalPaymentMethodConfirmHandler(externalPaymentMethodConfirmHandler)

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
            initializedViaCompose = true,
        ).create()
    }
}

@Composable
private fun UpdateIntentConfirmationInterceptor(
    createIntentCallback: CreateIntentCallback?,
) {
    LaunchedEffect(createIntentCallback) {
        IntentConfirmationInterceptor.createIntentCallback = createIntentCallback
    }
}

@Composable
private fun UpdateExternalPaymentMethodConfirmHandler(
    externalPaymentMethodConfirmHandler: ExternalPaymentMethodConfirmHandler?,
) {
    LaunchedEffect(externalPaymentMethodConfirmHandler) {
        ExternalPaymentMethodInterceptor.externalPaymentMethodConfirmHandler = externalPaymentMethodConfirmHandler
    }
}
