package com.stripe.android.connect

import android.Manifest
import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.core.content.ContextCompat.checkSelfPermission
import com.stripe.android.connect.appearance.Appearance
import com.stripe.android.connect.appearance.fonts.CustomFontSource
import com.stripe.android.connect.util.findActivity
import com.stripe.android.connect.webview.serialization.ConnectInstanceJs
import com.stripe.android.connect.webview.serialization.toJs
import com.stripe.android.core.Logger
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.parcelize.Parcelize
import kotlin.coroutines.resume

@PrivateBetaConnectSDK
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class EmbeddedComponentManager(
    private val configuration: Configuration,
    private val fetchClientSecretCallback: FetchClientSecretCallback,
    appearance: Appearance = Appearance(),
    private val customFonts: List<CustomFontSource> = emptyList(),
) {
    private val _appearanceFlow = MutableStateFlow(appearance)
    internal val appearanceFlow: StateFlow<Appearance> get() = _appearanceFlow.asStateFlow()

    private val logger: Logger = Logger.getInstance(enableLogging = BuildConfig.DEBUG)
    private val isDebugBuild: Boolean = BuildConfig.DEBUG
    private val loggerTag = javaClass.simpleName

    // Public functions

    /**
     * Create a new [AccountOnboardingView] for inclusion in the view hierarchy.
     */
    @PrivateBetaConnectSDK
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun createAccountOnboardingView(
        context: Context,
        listener: AccountOnboardingListener? = null,
    ): AccountOnboardingView {
        val activity = checkNotNull(context.findActivity()) {
            "You must create an AccountOnboardingView from an Activity"
        }
        checkNotNull(launcherMap[activity]) {
            "You must call EmbeddedComponentManager.onActivityCreate in your Activity.onCreate function"
        }

        return AccountOnboardingView(
            context = context,
            embeddedComponentManager = this,
            listener = listener
        )
    }

    /**
     * Create a new [PayoutsView] for inclusion in the view hierarchy.
     */
    @PrivateBetaConnectSDK
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun createPayoutsView(
        context: Context,
        listener: PayoutsListener? = null,
    ): PayoutsView {
        val activity = checkNotNull(context.findActivity()) {
            "You must create a PayoutsView from an Activity"
        }
        checkNotNull(launcherMap[activity]) {
            "You must call EmbeddedComponentManager.onActivityCreate in your Activity.onCreate function"
        }

        return PayoutsView(
            context = context,
            embeddedComponentManager = this,
            listener = listener,
        )
    }

    @PrivateBetaConnectSDK
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun update(appearance: Appearance) {
        _appearanceFlow.value = appearance
    }

    @PrivateBetaConnectSDK
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun logout() {
        throw NotImplementedError("Logout functionality is not yet implemented")
    }

    // Internal functions (not for public consumption)

    internal fun getInitialParams(context: Context): ConnectInstanceJs {
        return ConnectInstanceJs(
            appearance = _appearanceFlow.value.toJs(),
            fonts = customFonts.map { it.toJs(context) },
        )
    }

    /**
     * Returns the Connect Embedded Component URL for the given [StripeEmbeddedComponent].
     */
    internal fun getStripeURL(component: StripeEmbeddedComponent): String {
        return buildString {
            append("https://connect-js.stripe.com/v1.0/android_webview.html")
            append("#component=${component.componentName}")
            append("&publicKey=${configuration.publishableKey}")
        }
    }

    /**
     * Fetch the client secret from the consumer of the SDK.
     */
    internal suspend fun fetchClientSecret(): String? {
        return suspendCancellableCoroutine { continuation ->
            val resultCallback = object : FetchClientSecretCallback.ClientSecretResultCallback {
                override fun onResult(secret: String?) {
                    continuation.resume(secret)
                }
            }
            fetchClientSecretCallback.fetchClientSecret(resultCallback)
        }
    }

    /**
     * Requests camera permissions for the EmbeddedComponents. Returns true if the user grants permission, false if the
     * user denies permission, and null if the request cannot be completed.
     *
     * This function may result in a permissions pop-up being shown to the user (although this may not always
     * happen, such as when the permission has already granted).
     */
    internal suspend fun requestCameraPermission(context: Context): Boolean? {
        if (checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            logger.debug("($loggerTag) Skipping permission request - CAMERA permission already granted")
            return true
        }

        val activity = context.findActivity()
        if (activity == null) {
            logger.warning("($loggerTag) You must create the EmbeddedComponent view from an Activity")
            if (isDebugBuild) {
                // crash if in debug mode so that developers are more likely to catch this error.
                error("You must create an AccountOnboardingView from an Activity")
            }
        }
        val launcher = launcherMap[activity]
        if (launcher == null) {
            logger.warning(
                "($loggerTag) Error launching camera permission request. " +
                    "Did you call EmbeddedComponentManager.onActivityCreate in your Activity.onCreate function?"
            )
            return null
        }
        launcher.launch(Manifest.permission.CAMERA)

        return permissionsFlow.first()
    }

    // Configuration

    @PrivateBetaConnectSDK
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Parcelize
    data class Configuration(
        val publishableKey: String,
    ) : Parcelable

    companion object {
        @VisibleForTesting
        internal val permissionsFlow: MutableSharedFlow<Boolean> = MutableSharedFlow(extraBufferCapacity = 1)
        private val launcherMap = mutableMapOf<Activity, ActivityResultLauncher<String>>()

        /**
         * Hooks the [EmbeddedComponentManager] into this activity's lifecycle.
         *
         * Must be called in [ComponentActivity.onCreate], passing in the instance of the
         * activity as [activity]. This must be called in all activities where an EmbeddedComponent
         * view is used.
         */
        fun onActivityCreate(activity: ComponentActivity) {
            val application = activity.application

            application.registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
                override fun onActivityDestroyed(destroyedActivity: Activity) {
                    // ensure we remove the activity and its launcher from our map, and unregister
                    // this activity from future callbacks
                    launcherMap.remove(destroyedActivity)
                    if (destroyedActivity == activity) {
                        application.unregisterActivityLifecycleCallbacks(this)
                    }
                }

                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) { /* no-op */ }

                override fun onActivityStarted(activity: Activity) { /* no-op */ }

                override fun onActivityResumed(activity: Activity) { /* no-op */ }

                override fun onActivityPaused(activity: Activity) { /* no-op */ }

                override fun onActivityStopped(activity: Activity) { /* no-op */ }

                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) { /* no-op */ }
            })

            launcherMap[activity] = activity.registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                permissionsFlow.tryEmit(isGranted)
            }
        }
    }
}
