package com.stripe.android.paymentsheet.flowcontroller

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultCaller
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import com.stripe.android.ConfirmCallback
import com.stripe.android.paymentsheet.PaymentOptionCallback
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResultCallback

internal class FlowControllerFactory(
    private val viewModelStoreOwner: ViewModelStoreOwner,
    private val lifecycleOwner: LifecycleOwner,
    private val appContext: Context,
    private val activityResultCaller: ActivityResultCaller,
    private val statusBarColor: () -> Int?,
    private val paymentOptionCallback: PaymentOptionCallback,
    private val paymentResultCallback: PaymentSheetResultCallback,
    private val confirmCallback: ConfirmCallback?,
) {
    constructor(
        activity: ComponentActivity,
        paymentOptionCallback: PaymentOptionCallback,
        paymentResultCallback: PaymentSheetResultCallback,
        confirmCallback: ConfirmCallback?
    ) : this(
        viewModelStoreOwner = activity,
        lifecycleOwner = activity,
        appContext = activity.applicationContext,
        activityResultCaller = activity,
        statusBarColor = { activity.window.statusBarColor },
        paymentOptionCallback = paymentOptionCallback,
        paymentResultCallback = paymentResultCallback,
        confirmCallback = confirmCallback,
    )

    constructor(
        fragment: Fragment,
        paymentOptionCallback: PaymentOptionCallback,
        paymentResultCallback: PaymentSheetResultCallback,
        confirmCallback: ConfirmCallback?
    ) : this(
        viewModelStoreOwner = fragment,
        lifecycleOwner = fragment,
        appContext = fragment.requireContext().applicationContext,
        activityResultCaller = fragment,
        statusBarColor = { fragment.activity?.window?.statusBarColor },
        paymentOptionCallback = paymentOptionCallback,
        paymentResultCallback = paymentResultCallback,
        confirmCallback = confirmCallback,
    )

    fun create(): PaymentSheet.FlowController =
        DefaultFlowController.getInstance(
            appContext = appContext,
            viewModelStoreOwner = viewModelStoreOwner,
            lifecycleOwner = lifecycleOwner,
            activityResultCaller = activityResultCaller,
            statusBarColor = statusBarColor,
            paymentOptionCallback = paymentOptionCallback,
            paymentResultCallback = paymentResultCallback,
            confirmCallback = confirmCallback,
        )
}
