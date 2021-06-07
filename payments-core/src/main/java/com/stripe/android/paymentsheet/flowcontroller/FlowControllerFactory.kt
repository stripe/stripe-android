package com.stripe.android.paymentsheet.flowcontroller

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import com.stripe.android.paymentsheet.PaymentOptionCallback
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResultCallback
import com.stripe.android.paymentsheet.model.PaymentOptionFactory
import com.stripe.android.view.AuthActivityStarter
import kotlinx.coroutines.CoroutineScope

internal class FlowControllerFactory(
    private val viewModelStoreOwner: ViewModelStoreOwner,
    private val lifecycleScope: CoroutineScope,
    private val appContext: Context,
    private val activityLauncherFactory: ActivityLauncherFactory,
    private val statusBarColor: () -> Int?,
    private val authHostSupplier: () -> AuthActivityStarter.Host,
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
        activity.applicationContext,
        ActivityLauncherFactory.ActivityHost(activity),
        { activity.window.statusBarColor },
        { AuthActivityStarter.Host.create(activity) },
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
        fragment.requireContext(),
        ActivityLauncherFactory.FragmentHost(fragment),
        { fragment.activity?.window?.statusBarColor },
        { AuthActivityStarter.Host.create(fragment) },
        PaymentOptionFactory(fragment.resources),
        paymentOptionCallback,
        paymentResultCallback
    )

    fun create(): PaymentSheet.FlowController =
        DefaultFlowController.getInstance(
            appContext = appContext,
            viewModelStoreOwner = viewModelStoreOwner,
            lifecycleScope = lifecycleScope,
            activityLauncherFactory = activityLauncherFactory,
            statusBarColor = statusBarColor,
            authHostSupplier = authHostSupplier,
            paymentOptionFactory = paymentOptionFactory,
            paymentOptionCallback = paymentOptionCallback,
            paymentResultCallback = paymentResultCallback
        )
}
