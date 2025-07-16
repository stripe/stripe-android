package com.stripe.android.crypto.onramp

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.stripe.android.crypto.onramp.model.ConfigurationCallback
import com.stripe.android.crypto.onramp.model.LinkUserInfo
import com.stripe.android.crypto.onramp.model.OnrampConfiguration
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
}

/**
 * ViewModel that stores Onramp configuration in a SavedStateHandle for
 * process death restoration.
 *
 * @property handle SavedStateHandle backing persistent state.
 */
internal class OnrampCoordinatorViewModel(
    private val handle: SavedStateHandle
) : ViewModel() {

    /**
     * The current OnrampConfiguration, persisted across process restarts.
     */
    var onRampConfiguration: OnrampConfiguration?
        get() = handle["configuration"]
        set(value) = handle.set("configuration", value)
}
