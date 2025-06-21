package com.stripe.onramp

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner

internal class OnRampCoordinatorFactory(
    private val viewModelStoreOwner: ViewModelStoreOwner,
    private val lifecycleOwner: LifecycleOwner,
    private val activityResultRegistryOwner: ActivityResultRegistryOwner,
    private val onRampCallbacks: OnRampCoordinator.OnRampCallbacks,
    private val linkElementCallbackIdentifier: String = "LinkCoordinator",
) {
    constructor(
        activity: ComponentActivity,
        onRampCallbacks: OnRampCoordinator.OnRampCallbacks
    ) : this(
        viewModelStoreOwner = activity,
        lifecycleOwner = activity,
        activityResultRegistryOwner = activity,
        onRampCallbacks = onRampCallbacks,
    )

    constructor(
        fragment: Fragment,
        onRampCallbacks: OnRampCoordinator.OnRampCallbacks
    ) : this(
        viewModelStoreOwner = fragment,
        lifecycleOwner = fragment,
        activityResultRegistryOwner = (fragment.host as? ActivityResultRegistryOwner) ?: fragment.requireActivity(),
        onRampCallbacks = onRampCallbacks,
    )

    fun create(): OnRampCoordinator =
        DefaultOnRampCoordinator.getInstance(
            viewModelStoreOwner = viewModelStoreOwner,
            lifecycleOwner = lifecycleOwner,
            activityResultRegistryOwner = activityResultRegistryOwner,
            onRampCallbacks = onRampCallbacks,
            linkElementCallbackIdentifier = linkElementCallbackIdentifier,
        )
}
