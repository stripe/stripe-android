package com.stripe.android.paymentsheet.flowcontroller

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultCaller
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import com.stripe.android.paymentsheet.PaymentOptionCallback
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResultCallback
import com.stripe.android.paymentsheet.model.PaymentOptionFactory
import com.stripe.android.view.AuthActivityStarterHost
import kotlinx.coroutines.CoroutineScope

internal class FlowControllerFactory(
    private val viewModelStoreOwner: ViewModelStoreOwner,
    private val lifecycleScope: CoroutineScope,
    private val lifecycleOwner: LifecycleOwner,
    private val appContext: Context,
    private val activityResultCaller: ActivityResultCaller,
    private val statusBarColor: () -> Int?,
    private val authHostSupplier: () -> AuthActivityStarterHost,
    private val paymentOptionFactory: PaymentOptionFactory,
    private val paymentOptionCallback: PaymentOptionCallback,
    private val paymentResultCallback: PaymentSheetResultCallback
) {
    constructor(
        activity: ComponentActivity,
        paymentOptionCallback: PaymentOptionCallback,
        paymentResultCallback: PaymentSheetResultCallback
    ) : this(
        activity,
        activity.lifecycleScope,
        activity,
        activity.applicationContext,
        activity,
        { activity.window.statusBarColor },
        { AuthActivityStarterHost.create(activity) },
        PaymentOptionFactory(activity.resources),
        paymentOptionCallback,
        paymentResultCallback
    )

    constructor(
        fragment: Fragment,
        paymentOptionCallback: PaymentOptionCallback,
        paymentResultCallback: PaymentSheetResultCallback
    ) : this(
        fragment,
        fragment.lifecycleScope,
        fragment,
        fragment.requireContext(),
        fragment,
        { fragment.activity?.window?.statusBarColor },
        { AuthActivityStarterHost.create(fragment) },
        PaymentOptionFactory(fragment.resources),
        paymentOptionCallback,
        paymentResultCallback
    )

    fun create(): PaymentSheet.FlowController =
        DefaultFlowController.getInstance(
            appContext = appContext,
            viewModelStoreOwner = viewModelStoreOwner,
            lifecycleScope = lifecycleScope,
            lifecycleOwner = lifecycleOwner,
            activityResultCaller = activityResultCaller,
            statusBarColor = statusBarColor,
            authHostSupplier = authHostSupplier,
            paymentOptionFactory = paymentOptionFactory,
            paymentOptionCallback = paymentOptionCallback,
            paymentResultCallback = paymentResultCallback
        )
}
