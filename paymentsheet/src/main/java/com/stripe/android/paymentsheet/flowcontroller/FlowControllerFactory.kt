package com.stripe.android.paymentsheet.flowcontroller

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import com.stripe.android.common.ui.PaymentElementActivityResultCaller
import com.stripe.android.core.Identifiable
import com.stripe.android.core.utils.StatusBarCompat
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackReferences
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbacks
import com.stripe.android.paymentsheet.PaymentOptionResultCallback
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResultCallback

internal class FlowControllerFactory(
    private val viewModelStoreOwner: ViewModelStoreOwner,
    private val lifecycleOwner: LifecycleOwner,
    private val activityResultRegistryOwner: ActivityResultRegistryOwner,
    private val statusBarColor: () -> Int?,
    private val paymentOptionResultCallback: PaymentOptionResultCallback,
    private val paymentResultCallback: PaymentSheetResultCallback,
    private val initializedViaCompose: Boolean = false,
) {
    constructor(
        activity: ComponentActivity,
        paymentOptionResultCallback: PaymentOptionResultCallback,
        paymentResultCallback: PaymentSheetResultCallback
    ) : this(
        viewModelStoreOwner = activity,
        lifecycleOwner = activity,
        activityResultRegistryOwner = activity,
        statusBarColor = { StatusBarCompat.color(activity) },
        paymentOptionResultCallback = paymentOptionResultCallback,
        paymentResultCallback = paymentResultCallback,
    )

    constructor(
        fragment: Fragment,
        paymentOptionResultCallback: PaymentOptionResultCallback,
        paymentResultCallback: PaymentSheetResultCallback
    ) : this(
        viewModelStoreOwner = fragment,
        lifecycleOwner = fragment,
        activityResultRegistryOwner = (fragment.host as? ActivityResultRegistryOwner) ?: fragment.requireActivity(),
        statusBarColor = { StatusBarCompat.color(fragment.requireActivity()) },
        paymentOptionResultCallback = paymentOptionResultCallback,
        paymentResultCallback = paymentResultCallback,
    )

    fun create(callbacks: PaymentElementCallbacks): PaymentSheet.FlowController {
        val id = Identifiable()
        PaymentElementCallbackReferences[id] = callbacks
        return create(id)
    }

    fun create(id: Identifiable): PaymentSheet.FlowController =
        DefaultFlowController.getInstance(
            viewModelStoreOwner = viewModelStoreOwner,
            lifecycleOwner = lifecycleOwner,
            activityResultCaller = PaymentElementActivityResultCaller(
                key = "FlowController(instance = $id)",
                registryOwner = activityResultRegistryOwner,
            ),
            activityResultRegistryOwner = activityResultRegistryOwner,
            statusBarColor = statusBarColor,
            paymentOptionResultCallback = paymentOptionResultCallback,
            paymentResultCallback = paymentResultCallback,
            paymentElementCallbackIdentifier = id,
            initializedViaCompose = initializedViaCompose,
        )
}
