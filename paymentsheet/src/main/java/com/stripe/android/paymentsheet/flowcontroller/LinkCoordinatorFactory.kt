package com.stripe.android.paymentsheet.flowcontroller

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import com.stripe.android.common.ui.PaymentElementActivityResultCaller
import com.stripe.android.paymentsheet.LinkCoordinator
import com.stripe.android.paymentsheet.model.PaymentOption

internal class LinkCoordinatorFactory(
    private val viewModelStoreOwner: ViewModelStoreOwner,
    private val lifecycleOwner: LifecycleOwner,
    private val activityResultRegistryOwner: ActivityResultRegistryOwner,
    private val paymentOptionCallback: (PaymentOption?) -> Unit,
    private val linkElementCallbackIdentifier: String = "LinkCoordinator",
) {
    constructor(
        activity: ComponentActivity,
        paymentOptionCallback: (PaymentOption?) -> Unit
    ) : this(
        viewModelStoreOwner = activity,
        lifecycleOwner = activity,
        activityResultRegistryOwner = activity,
        paymentOptionCallback = paymentOptionCallback,
    )

    constructor(
        fragment: Fragment,
        paymentOptionCallback: (PaymentOption?) -> Unit
    ) : this(
        viewModelStoreOwner = fragment,
        lifecycleOwner = fragment,
        activityResultRegistryOwner = (fragment.host as? ActivityResultRegistryOwner) ?: fragment.requireActivity(),
        paymentOptionCallback = paymentOptionCallback,
    )

    fun create(): LinkCoordinator =
        DefaultLinkCoordinator.getInstance(
            viewModelStoreOwner = viewModelStoreOwner,
            lifecycleOwner = lifecycleOwner,
            activityResultCaller = PaymentElementActivityResultCaller(
                key = "LinkCoordinator(instance = $linkElementCallbackIdentifier)",
                registryOwner = activityResultRegistryOwner,
            ),
            activityResultRegistryOwner = activityResultRegistryOwner,
            paymentOptionCallback = paymentOptionCallback,
            linkElementCallbackIdentifier = linkElementCallbackIdentifier,
        )
} 