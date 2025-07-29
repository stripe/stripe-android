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
import com.stripe.android.crypto.onramp.model.OnrampConfiguration
import com.stripe.android.crypto.onramp.model.OnrampConfigurationResult
import com.stripe.android.crypto.onramp.model.OnrampContinuations
import com.stripe.android.crypto.onramp.model.OnrampLinkLookupResult
import com.stripe.android.crypto.onramp.model.OnrampRegisterUserResult
import com.stripe.android.crypto.onramp.model.OnrampVerificationResult
import com.stripe.android.crypto.onramp.viewmodels.OnrampCoordinatorViewModel
import javax.inject.Inject

/**
 * Coordinator interface for managing the Onramp lifecycle, Link user checks,
 * and authentication flows.
 *
 * @param viewModel The ViewModel that persists configuration state across process restarts.
 * @param activityResultRegistryOwner Host providing ActivityResultRegistry for LinkController.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class OnrampCoordinator @Inject internal constructor(
    private val viewModel: OnrampCoordinatorViewModel,
    private val onrampLinkController: OnrampLinkController,
    private val activityResultRegistryOwner: ActivityResultRegistryOwner
) {

    /**
     * Initialize the coordinator with the provided configuration.
     *
     * @param configuration The OnrampConfiguration to apply.
     * @return The result of the configuration operation.
     */
    suspend fun configure(
        configuration: OnrampConfiguration,
    ): OnrampConfigurationResult {
        return viewModel.configure(configuration)
    }

    /**
     * Check if the given email corresponds to an existing Link user.
     *
     * @param email The email address to look up.
     * @return The result of the lookup operation.
     */
    suspend fun isLinkUser(email: String): OnrampLinkLookupResult {
        return onrampLinkController.isLinkUser(email)
    }

    /**
     * Given the required information, registers a new Link user.
     *
     * @param info The LinkInfo for the new user.
     * @return The result of the registration operation.
     */
    suspend fun registerNewLinkUser(info: LinkUserInfo): OnrampRegisterUserResult {
        return onrampLinkController.registerNewUser(info)
    }

    /**
     * Authenticate an existing Link user via email.
     *
     * @param email The email address of the existing user.
     * @return The result of the authentication operation.
     */
    suspend fun authenticateExistingLinkUser(email: String): OnrampVerificationResult {
        return onrampLinkController.authenticateExistingUser(email)
    }

    /**
     * A Builder utility type to create an [OnrampCoordinator] with appropriate parameters.
     */
    class Builder {
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

            // Create continuations instance to be shared across components
            val continuations = OnrampContinuations()

            val viewModel = ViewModelProvider(
                owner = viewModelStoreOwner,
                factory = OnrampCoordinatorViewModel.Factory(continuations)
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
                    .continuations(continuations)
                    .linkElementCallbackIdentifier(linkElementCallbackIdentifier)
                    .activityResultRegistryOwner(activityResultRegistryOwner)
                    .lifecycleOwner(lifecycleOwner)
                    .build()

            return onrampComponent.onrampCoordinator
        }
    }
}
