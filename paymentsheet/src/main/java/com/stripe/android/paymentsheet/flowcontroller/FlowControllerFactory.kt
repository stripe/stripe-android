package com.stripe.android.paymentsheet.flowcontroller

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultCaller
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import com.stripe.android.paymentsheet.PaymentOptionCallback
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResultCallback

internal class FlowControllerFactory(
    private val viewModelStoreOwner: ViewModelStoreOwner,
    private val lifecycleOwner: LifecycleOwner,
    private val activityResultCaller: ActivityResultCaller,
    private val statusBarColor: () -> Int?,
    private val paymentOptionCallback: PaymentOptionCallback,
    private val paymentResultCallback: PaymentSheetResultCallback
) {
    constructor(
        activity: ComponentActivity,
        paymentOptionCallback: PaymentOptionCallback,
        paymentResultCallback: PaymentSheetResultCallback
    ) : this(
        viewModelStoreOwner = activity,
        lifecycleOwner = activity,
        activityResultCaller = activity,
        statusBarColor = { activity.window.statusBarColor },
        paymentOptionCallback = paymentOptionCallback,
        paymentResultCallback = paymentResultCallback,
    )

    constructor(
        fragment: Fragment,
        paymentOptionCallback: PaymentOptionCallback,
        paymentResultCallback: PaymentSheetResultCallback
    ) : this(
        viewModelStoreOwner = fragment,
        lifecycleOwner = fragment,
        activityResultCaller = fragment,
        statusBarColor = { fragment.activity?.window?.statusBarColor },
        paymentOptionCallback = paymentOptionCallback,
        paymentResultCallback = paymentResultCallback,
    )

    fun create(): PaymentSheet.FlowController =
        DefaultFlowController.getInstance(
            viewModelStoreOwner = viewModelStoreOwner,
            lifecycleOwner = lifecycleOwner,
            activityResultCaller = activityResultCaller,
            statusBarColor = statusBarColor,
            paymentOptionCallback = paymentOptionCallback,
            paymentResultCallback = paymentResultCallback
        )
}
