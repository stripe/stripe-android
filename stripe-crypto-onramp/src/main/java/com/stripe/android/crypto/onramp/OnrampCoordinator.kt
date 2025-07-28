package com.stripe.android.crypto.onramp

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.annotation.RestrictTo
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.stripe.android.crypto.onramp.di.DaggerOnrampComponent
import com.stripe.android.crypto.onramp.di.OnrampComponent
import com.stripe.android.crypto.onramp.model.LinkUserInfo
import com.stripe.android.crypto.onramp.model.OnrampCallbacks
import com.stripe.android.crypto.onramp.model.OnrampConfiguration
import com.stripe.android.crypto.onramp.viewmodels.OnrampCoordinatorViewModel
import javax.inject.Inject

/**
 * Coordinator interface for managing the Onramp lifecycle, Link user checks,
 * and authentication flows.
 *
 * @param viewModel The ViewModel that persists configuration state across process restarts.
 * @param activityResultRegistryOwner Host providing ActivityResultRegistry for LinkController.
 * @param onrampCallbacks Callback structure that manages the results of asynchronous requests
 *                        made by the coordinator.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class OnrampCoordinator @Inject internal constructor(
    private val viewModel: OnrampCoordinatorViewModel,
    private val onrampController: OnrampController,
    private val activityResultRegistryOwner: ActivityResultRegistryOwner,
    private val onrampCallbacks: OnrampCallbacks
) {

    /**
     * Initialize the coordinator with the provided configuration.
     *
     * @param configuration The OnrampConfiguration to apply.
     */
    fun configure(
        configuration: OnrampConfiguration,
    ) {
        viewModel.configure(configuration)
    }

    /**
     * Check if the given email corresponds to an existing Link user.
     *
     * @param email The email address to look up.
     */
    fun isLinkUser(email: String) {
        onrampController.isLinkUser(email)
    }

    /**
     * Given the required information, registers a new Link user.
     *
     * @param info The LinkInfo for the new user.
     */
    fun registerNewLinkUser(info: LinkUserInfo) {
        onrampController.registerNewUser(info)
    }

    /**
     * Authenticate an existing Link user via email.
     *
     * @param email The email address of the existing user.
     */
    fun authenticateExistingLinkUser(email: String) {
        onrampController.authenticateExistingUser(email)
    }

    /**
     * A Builder utility type to create an [OnrampCoordinator] with appropriate parameters.
     *
     * @param onrampCallbacks Callbacks for handling asynchronous responses from the coordinator.
     */
    class Builder(
        private val onrampCallbacks: OnrampCallbacks
    ) {
        /**
         * Constructs an [OnrampCoordinator] for the given parameters.
         *
         * @param activity The Activity that is using the [OnrampCoordinator].
         */
        fun build(activity: ComponentActivity): OnrampCoordinator {
            return create(
                viewModelStoreOwner = activity,
                lifecycleOwner = activity,
                activityResultRegistryOwner = activity
            )
        }

        /**
         * Constructs an [OnrampCoordinator] for the given parameters.
         *
         * @param fragment The Fragment that is using the [OnrampCoordinator].
         */
        fun build(fragment: Fragment): OnrampCoordinator {
            return create(
                viewModelStoreOwner = fragment,
                lifecycleOwner = fragment,
                activityResultRegistryOwner = (fragment.host as? ActivityResultRegistryOwner)
                    ?: fragment.requireActivity()
            )
        }

        private fun create(
            viewModelStoreOwner: ViewModelStoreOwner,
            lifecycleOwner: LifecycleOwner,
            activityResultRegistryOwner: ActivityResultRegistryOwner
        ): OnrampCoordinator {
            val linkElementCallbackIdentifier = "OnrampCoordinator"

            val viewModel = ViewModelProvider(
                owner = viewModelStoreOwner,
                factory = OnrampCoordinatorViewModel.Factory(
                    onrampCallbacks = onrampCallbacks
                )
            ).get(
                key = "OnRampCoordinatorViewModel(instance = $linkElementCallbackIdentifier)",
                modelClass = OnrampCoordinatorViewModel::class.java
            )

            val application = when (lifecycleOwner) {
                is Fragment -> lifecycleOwner.requireActivity().application
                is ComponentActivity -> lifecycleOwner.application
                else -> throw IllegalArgumentException("LifecycleOwner must be an Activity or Fragment")
            }

            val onrampComponent: OnrampComponent =
                DaggerOnrampComponent
                    .builder()
                    .application(application)
                    .onRampCoordinatorViewModel(viewModel)
                    .linkElementCallbackIdentifier(linkElementCallbackIdentifier)
                    .activityResultRegistryOwner(activityResultRegistryOwner)
                    .onrampCallbacks(onrampCallbacks)
                    .build()

            return onrampComponent.onrampCoordinator
        }
    }
}
