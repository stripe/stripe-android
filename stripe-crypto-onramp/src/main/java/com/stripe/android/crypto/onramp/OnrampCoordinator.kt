package com.stripe.android.crypto.onramp

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.stripe.android.crypto.onramp.di.DaggerOnrampComponent
import com.stripe.android.crypto.onramp.di.OnrampComponent
import com.stripe.android.crypto.onramp.model.ConfigurationCallback
import com.stripe.android.crypto.onramp.model.LinkUserInfo
import com.stripe.android.crypto.onramp.model.OnrampConfiguration
import com.stripe.android.crypto.onramp.viewmodels.OnrampCoordinatorViewModel
import com.stripe.android.link.LinkController
import javax.inject.Inject

/**
 * Coordinator interface for managing the Onramp lifecycle, Link user checks,
 * and authentication flows.
 *
 * @param viewModel The ViewModel that persists configuration state across process restarts.
 * @param activityResultRegistryOwner Host providing ActivityResultRegistry for LinkController.
 * @param isLinkUserCallback Callback invoked with the result of determining if a
 *                           provided email is associated with a Link user.
 */
internal class OnrampCoordinator @Inject internal constructor(
    private val viewModel: OnrampCoordinatorViewModel,
    private val activityResultRegistryOwner: ActivityResultRegistryOwner,
    private val isLinkUserCallback: (Boolean) -> Unit
) {

    private val linkController: LinkController by lazy {
        // Resolve the hosting activity, fail fast if incorrect type
        val activity: ComponentActivity =
            (activityResultRegistryOwner as? ComponentActivity)
                ?: throw IllegalStateException(
                    "Expected a ComponentActivity, got ${activityResultRegistryOwner::class}"
                )

        LinkController.create(
            activity = activity,
            presentPaymentMethodsCallback = { /* No-op for now */ },
            lookupConsumerCallback = { result ->
                when (result) {
                    is LinkController.LookupConsumerResult.Success -> isLinkUserCallback(result.isConsumer)
                    is LinkController.LookupConsumerResult.Failed -> isLinkUserCallback(false)
                }
            },
            createPaymentMethodCallback = { /* No-op for now */ },
            presentForAuthenticationCallback = { /* No-op for now */ }
        )
    }

    /**
     * Initialize the coordinator with the provided configuration.
     *
     * @param configuration The OnrampConfiguration to apply.
     * @param callback Callback receiving success or failure.
     */
    fun configure(
        configuration: OnrampConfiguration,
        callback: ConfigurationCallback
    ) {
        viewModel.onRampConfiguration = configuration

        callback.onConfigured(success = true, error = null)
    }

    /**
     * Check if the given email corresponds to an existing Link user.
     *
     * @param email The email address to look up.
     */
    fun isLinkUser(email: String) {
        linkController.lookupConsumer(email)
    }

    /**
     * Given the required information, registers a new Link user.
     *
     * @param info The LinkInfo for the new user.
     */
    @Suppress("UnusedParameter")
    fun registerNewLinkUser(info: LinkUserInfo) {
        TODO("Not yet implemented")
    }

    /**
     * Authenticate an existing Link user via email.
     *
     * @param email The email address of the existing user.
     */
    @Suppress("UnusedParameter")
    fun authenticateExistingLinkUser(email: String) {
        TODO("Not yet implemented")
    }

    /**
     * Present UI to authenticate a Link user.
     *
     * @param email The email address to authenticate.
     */
    @Suppress("UnusedParameter")
    fun presentForAuthentication(email: String) {
        TODO("Not yet implemented")
    }

    /**
     * A Builder utility type to create an [OnrampCoordinator] with appropriate parameters.
     *
     * @param isLinkUserCallback A callback for handling if a given user has a link account.
     */
    class Builder(
        private val isLinkUserCallback: (Boolean) -> Unit
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
                factory = OnrampCoordinatorViewModel.Factory()
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
                    .isLinkUserCallback(isLinkUserCallback)
                    .build()

            return onrampComponent.onrampCoordinator
        }
    }
}
