package com.stripe.android.paymentsheet.flowcontroller

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultCaller
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import com.stripe.android.paymentsheet.PaymentOptionCallback
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResultCallback
import com.stripe.android.paymentsheet.model.PaymentOptionFactory
import com.stripe.android.uicore.image.StripeImageLoader

internal class FlowControllerFactory(
    private val viewModelStoreOwner: ViewModelStoreOwner,
    private val lifecycleOwner: LifecycleOwner,
    private val appContext: Context,
    private val activityResultCaller: ActivityResultCaller,
    private val statusBarColor: () -> Int?,
    private val paymentOptionFactory: PaymentOptionFactory,
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
        appContext = activity.applicationContext,
        activityResultCaller = activity,
        statusBarColor = { activity.window.statusBarColor },
        paymentOptionFactory = PaymentOptionFactory(
            activity.resources,
            StripeImageLoader(activity.applicationContext)
        ),
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
        appContext = fragment.requireContext().applicationContext,
        activityResultCaller = fragment,
        statusBarColor = { fragment.activity?.window?.statusBarColor },
        paymentOptionFactory = PaymentOptionFactory(
            fragment.resources,
            StripeImageLoader(fragment.requireContext().applicationContext)
        ),
        paymentOptionCallback = paymentOptionCallback,
        paymentResultCallback = paymentResultCallback,
    )

    fun create(): PaymentSheet.FlowController =
        DefaultFlowController.getInstance(
            appContext = appContext,
            viewModelStoreOwner = viewModelStoreOwner,
            lifecycleOwner = lifecycleOwner,
            activityResultCaller = activityResultCaller,
            statusBarColor = statusBarColor,
            paymentOptionFactory = paymentOptionFactory,
            paymentOptionCallback = paymentOptionCallback,
            paymentResultCallback = paymentResultCallback
        )
}
