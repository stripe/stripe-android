package com.stripe.onramp

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import com.stripe.android.paymentsheet.model.PaymentOption

internal class OnRampCoordinatorFactory(
    private val viewModelStoreOwner: ViewModelStoreOwner,
    private val lifecycleOwner: LifecycleOwner,
    private val activityResultRegistryOwner: ActivityResultRegistryOwner,
    private val paymentOptionCallback: (PaymentOption?) -> Unit,
    private val userAuthenticatedCallback: ((OnRampCoordinator.User) -> Unit)? = null,
    private val linkElementCallbackIdentifier: String = "LinkCoordinator",
) {
    constructor(
        activity: ComponentActivity,
        paymentOptionCallback: (PaymentOption?) -> Unit,
        userAuthenticatedCallback: ((OnRampCoordinator.User) -> Unit)? = null
    ) : this(
        viewModelStoreOwner = activity,
        lifecycleOwner = activity,
        activityResultRegistryOwner = activity,
        paymentOptionCallback = paymentOptionCallback,
        userAuthenticatedCallback = userAuthenticatedCallback,
    )

    constructor(
        fragment: Fragment,
        paymentOptionCallback: (PaymentOption?) -> Unit,
        userAuthenticatedCallback: ((OnRampCoordinator.User) -> Unit)? = null
    ) : this(
        viewModelStoreOwner = fragment,
        lifecycleOwner = fragment,
        activityResultRegistryOwner = (fragment.host as? ActivityResultRegistryOwner) ?: fragment.requireActivity(),
        paymentOptionCallback = paymentOptionCallback,
        userAuthenticatedCallback = userAuthenticatedCallback,
    )

    fun create(): OnRampCoordinator =
        DefaultOnRampCoordinator.getInstance(
            viewModelStoreOwner = viewModelStoreOwner,
            lifecycleOwner = lifecycleOwner,
            activityResultRegistryOwner = activityResultRegistryOwner,
            paymentOptionCallback = paymentOptionCallback,
            userAuthenticatedCallback = userAuthenticatedCallback,
            linkElementCallbackIdentifier = linkElementCallbackIdentifier,
        )
}
