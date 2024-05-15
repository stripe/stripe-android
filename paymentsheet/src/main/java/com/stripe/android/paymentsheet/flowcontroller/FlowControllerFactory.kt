package com.stripe.android.paymentsheet.flowcontroller

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import com.stripe.android.paymentsheet.CreateIntentCallback
import com.stripe.android.paymentsheet.ExternalPaymentMethodConfirmHandler
import com.stripe.android.paymentsheet.ExternalPaymentMethodInterceptor
import com.stripe.android.paymentsheet.IntentConfirmationInterceptor
import com.stripe.android.paymentsheet.PaymentOptionCallback
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResultCallback

internal class FlowControllerFactory(
    private val viewModelStoreOwner: ViewModelStoreOwner,
    private val lifecycleOwner: LifecycleOwner,
    private val activityResultRegistryOwner: ActivityResultRegistryOwner,
    private val statusBarColor: () -> Int?,
    private val paymentOptionCallback: PaymentOptionCallback,
    private val paymentResultCallback: PaymentSheetResultCallback,
    private val externalPaymentMethodConfirmHandler: ExternalPaymentMethodConfirmHandler?,
    private val createIntentCallback: CreateIntentCallback?,
) {
    constructor(
        activity: ComponentActivity,
        paymentOptionCallback: PaymentOptionCallback,
        paymentResultCallback: PaymentSheetResultCallback,
        externalPaymentMethodConfirmHandler: ExternalPaymentMethodConfirmHandler? = null,
        createIntentCallback: CreateIntentCallback? = null,
    ) : this(
        viewModelStoreOwner = activity,
        lifecycleOwner = activity,
        activityResultRegistryOwner = activity,
        statusBarColor = { activity.window.statusBarColor },
        paymentOptionCallback = paymentOptionCallback,
        paymentResultCallback = paymentResultCallback,
        externalPaymentMethodConfirmHandler = externalPaymentMethodConfirmHandler,
        createIntentCallback = createIntentCallback,
    )

    constructor(
        fragment: Fragment,
        paymentOptionCallback: PaymentOptionCallback,
        paymentResultCallback: PaymentSheetResultCallback,
        externalPaymentMethodConfirmHandler: ExternalPaymentMethodConfirmHandler? = null,
        createIntentCallback: CreateIntentCallback? = null,
    ) : this(
        viewModelStoreOwner = fragment,
        lifecycleOwner = fragment,
        activityResultRegistryOwner = (fragment.host as? ActivityResultRegistryOwner) ?: fragment.requireActivity(),
        statusBarColor = { fragment.activity?.window?.statusBarColor },
        paymentOptionCallback = paymentOptionCallback,
        paymentResultCallback = paymentResultCallback,
        externalPaymentMethodConfirmHandler = externalPaymentMethodConfirmHandler,
        createIntentCallback = createIntentCallback,
    )

    fun create(): PaymentSheet.FlowController {
        IntentConfirmationInterceptor.createIntentCallback = createIntentCallback
        ExternalPaymentMethodInterceptor.externalPaymentMethodConfirmHandler = externalPaymentMethodConfirmHandler
        return DefaultFlowController.getInstance(
            viewModelStoreOwner = viewModelStoreOwner,
            lifecycleOwner = lifecycleOwner,
            activityResultRegistryOwner = activityResultRegistryOwner,
            statusBarColor = statusBarColor,
            paymentOptionCallback = paymentOptionCallback,
            paymentResultCallback = paymentResultCallback
        )
    }
}
