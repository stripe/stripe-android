package com.stripe.android.paymentsheet.flowcontroller

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import com.stripe.android.common.ui.PaymentElementActivityResultCaller
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
    private val initializedViaCompose: Boolean = false,
) {
    constructor(
        activity: ComponentActivity,
        paymentOptionCallback: PaymentOptionCallback,
        paymentResultCallback: PaymentSheetResultCallback
    ) : this(
        viewModelStoreOwner = activity,
        lifecycleOwner = activity,
        activityResultRegistryOwner = activity,
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
        activityResultRegistryOwner = (fragment.host as? ActivityResultRegistryOwner) ?: fragment.requireActivity(),
        statusBarColor = { fragment.activity?.window?.statusBarColor },
        paymentOptionCallback = paymentOptionCallback,
        paymentResultCallback = paymentResultCallback,
    )

    fun create(): PaymentSheet.FlowController =
        DefaultFlowController.getInstance(
            viewModelStoreOwner = viewModelStoreOwner,
            lifecycleOwner = lifecycleOwner,
            activityResultCaller = PaymentElementActivityResultCaller(
                key = "FlowController",
                registryOwner = activityResultRegistryOwner,
            ),
            statusBarColor = statusBarColor,
            paymentOptionCallback = paymentOptionCallback,
            paymentResultCallback = paymentResultCallback,
            initializedViaCompose = initializedViaCompose,
        )
}
