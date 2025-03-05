package com.stripe.android.connect

import android.Manifest
import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.fragment.app.FragmentActivity
import com.stripe.android.connect.analytics.ComponentAnalyticsService
import com.stripe.android.connect.analytics.ConnectAnalyticsService
import com.stripe.android.connect.analytics.DefaultConnectAnalyticsService
import com.stripe.android.connect.appearance.Appearance
import com.stripe.android.connect.appearance.fonts.CustomFontSource
import com.stripe.android.connect.util.findActivity
import com.stripe.android.connect.webview.ChooseFileActivityResultContract
import com.stripe.android.connect.webview.serialization.ConnectInstanceJs
import com.stripe.android.connect.webview.serialization.toJs
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.FinancialConnectionsSheetResult
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
@SuppressWarnings("TooManyFunctions")
class EmbeddedComponentManager(
    private val configuration: Configuration,
    private val fetchClientSecretCallback: FetchClientSecretCallback,
    appearance: Appearance = Appearance(),
    private val customFonts: List<CustomFontSource> = emptyList(),
) {
    private val _appearanceFlow = MutableStateFlow(appearance)
    internal val appearanceFlow: StateFlow<Appearance> get() = _appearanceFlow.asStateFlow()

    private val logger = Logger.getInstance(enableLogging = BuildConfig.DEBUG)
    private val loggerTag = javaClass.simpleName

    private val isDebugBuild = BuildConfig.DEBUG

    // Public functions

    /**
     * Returns a controller for presenting the Account Onboarding component full screen.
     *
     * @param activity The [FragmentActivity] to present the component in.
     * @param title Optional title to display in the toolbar.
     * @param props Optional props to use for configuring the component.
     * @param cacheKey Key to use for caching the internal WebView across configuration changes.
     */
    @PrivateBetaConnectSDK
    fun createAccountOnboardingController(
        activity: FragmentActivity,
        title: String? = null,
        props: AccountOnboardingProps? = null,
        cacheKey: String? = null,
    ): AccountOnboardingController {
        return AccountOnboardingController(
            activity = activity,
            embeddedComponentManager = this,
            title = title,
            props = props,
            cacheKey = cacheKey,
        )
    }

    /**
     * Create a new [AccountOnboardingView] for inclusion in the view hierarchy.
     *
     * @param context The [Context] to use for creating the view.
     * @param listener Optional listener to use for handling events from the view.
     * @param props Optional props to use for configuring the view.
     * @param cacheKey Key to use for caching the internal WebView across configuration changes.
     */
    @PrivateBetaConnectSDK
    fun createAccountOnboardingView(
        context: Context,
        listener: AccountOnboardingListener? = null,
        props: AccountOnboardingProps? = null,
        cacheKey: String? = null,
    ): AccountOnboardingView {
        checkContextDuringCreate(context)
        return AccountOnboardingView(
            context = context,
            embeddedComponentManager = this,
            listener = listener,
            props = props,
            cacheKey = cacheKey,
        )
    }

    /**
     * Create a new [PayoutsView] for inclusion in the view hierarchy.
     *
     * @param context The [Context] to use for creating the view.
     * @param listener Optional [PayoutsListener] to use for handling events from the view.
     * @param cacheKey Key to use for caching the internal WebView within an Activity across configuration changes.
     */
    @PrivateBetaConnectSDK
    fun createPayoutsView(
        context: Context,
        listener: PayoutsListener? = null,
        cacheKey: String? = null,
    ): PayoutsView {
        checkContextDuringCreate(context)
        return PayoutsView(
            context = context,
            embeddedComponentManager = this,
            listener = listener,
            cacheKey = cacheKey,
        )
    }

    private fun checkContextDuringCreate(context: Context) {
        val activity = context.findActivityWithErrorHandling()
        checkNotNull(requestPermissionLaunchers[activity]) {
            "You must call EmbeddedComponentManager.onActivityCreate in your Activity.onCreate function"
        }
    }

    @PrivateBetaConnectSDK
    fun update(appearance: Appearance) {
        _appearanceFlow.value = appearance
    }

    @PrivateBetaConnectSDK
    fun logout() {
        throw NotImplementedError("Logout functionality is not yet implemented")
    }

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
    internal suspend fun requestCameraPermission(activity: Activity): Boolean? {
        if (checkSelfPermission(activity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            logger.debug("($loggerTag) Skipping permission request - CAMERA permission already granted")
            return true
        }

        val launcher = getLauncher(activity, requestPermissionLaunchers, "Error launching camera permission request")
            ?: return null
        launcher.launch(Manifest.permission.CAMERA)

        return permissionsFlow.first()
    }

    internal suspend fun chooseFile(activity: Activity, requestIntent: Intent): Array<Uri>? {
        val launcher = getLauncher(activity, chooseFileLaunchers, "Error choosing file")
            ?: return null
        launcher.launch(requestIntent)

        return chooseFileResultFlow
            .first { it.activity == activity }
            .result
    }

    internal suspend fun presentFinancialConnections(
        activity: Activity,
        clientSecret: String,
        connectedAccountId: String,
    ): FinancialConnectionsSheetResult? {
        val sheet = financialConnectionsSheets[activity]
        if (sheet == null) {
            logger.warning(
                buildString {
                    append("($loggerTag) Error presenting FinancialConnectionsSheet ")
                    append("Did you call EmbeddedComponentManager.onActivityCreate in your Activity.onCreate function?")
                }
            )
            if (isDebugBuild) {
                // crash if in debug mode so that developers are more likely to catch this error.
                error("You must create the EmbeddedComponent view from an Activity")
            }
            return null
        }
        sheet.present(
            FinancialConnectionsSheet.Configuration(
                financialConnectionsSessionClientSecret = clientSecret,
                publishableKey = configuration.publishableKey,
                stripeAccountId = connectedAccountId,
            )
        )
        return financialConnectionsResults
            .first { it.activity == activity }
            .result
    }

    private fun <I> getLauncher(
        activity: Activity,
        launchers: Map<Activity, ActivityResultLauncher<I>>,
        errorMessage: String,
    ): ActivityResultLauncher<I>? {
        val launcher = launchers[activity]
        if (launcher == null) {
            logger.warning(
                "($loggerTag) $errorMessage " +
                    "Did you call EmbeddedComponentManager.onActivityCreate in your Activity.onCreate function?"
            )
            return null
        }
        return launcher
    }

    private fun Context.findActivityWithErrorHandling(): Activity? {
        val activity = findActivity()
        if (activity == null) {
            logger.warning("($loggerTag) You must create the EmbeddedComponent view from an Activity")
            if (isDebugBuild) {
                // crash if in debug mode so that developers are more likely to catch this error.
                error("You must create the EmbeddedComponent view from an Activity")
            }
        }
        return activity
    }

    internal fun getComponentAnalyticsService(component: StripeEmbeddedComponent): ComponentAnalyticsService {
        val analyticsService = checkNotNull(connectAnalyticsService) {
            "ConnectAnalyticsService is not initialized"
        }
        val publishableKeyToLog = if (configuration.publishableKey.startsWith("uk_")) {
            null // don't log "uk_" keys
        } else {
            configuration.publishableKey
        }
        return ComponentAnalyticsService(
            analyticsService = analyticsService,
            component = component,
            publishableKey = publishableKeyToLog,
        )
    }

    // Configuration

    @PrivateBetaConnectSDK
    @Parcelize
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class Configuration(
        val publishableKey: String,
    ) : Parcelable

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {
        private var connectAnalyticsService: ConnectAnalyticsService? = null

        @VisibleForTesting
        internal val permissionsFlow: MutableSharedFlow<Boolean> = MutableSharedFlow(extraBufferCapacity = 1)
        private val requestPermissionLaunchers = mutableMapOf<Activity, ActivityResultLauncher<String>>()

        @VisibleForTesting
        internal val chooseFileResultFlow: MutableSharedFlow<ActivityResult<Array<Uri>?>> =
            MutableSharedFlow(extraBufferCapacity = 1)
        private val chooseFileLaunchers = mutableMapOf<Activity, ActivityResultLauncher<Intent>>()

        @VisibleForTesting
        internal val financialConnectionsResults: MutableSharedFlow<ActivityResult<FinancialConnectionsSheetResult>> =
            MutableSharedFlow(extraBufferCapacity = 1)
        private val financialConnectionsSheets = mutableMapOf<Activity, FinancialConnectionsSheet>()

        /**
         * Hooks the [EmbeddedComponentManager] into this activity's lifecycle.
         *
         * Must be called in [ComponentActivity.onCreate], passing in the instance of the
         * activity as [activity]. This must be called in all activities where an EmbeddedComponent
         * view is used.
         */
        fun onActivityCreate(activity: ComponentActivity) {
            val application = activity.application

            if (connectAnalyticsService == null) {
                connectAnalyticsService = DefaultConnectAnalyticsService(
                    application = application,
                )
            }

            application.registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
                override fun onActivityDestroyed(destroyedActivity: Activity) {
                    // ensure we remove the activity and its launcher from our map, and unregister
                    // this activity from future callbacks
                    requestPermissionLaunchers.remove(destroyedActivity)
                    chooseFileLaunchers.remove(destroyedActivity)
                    financialConnectionsSheets.remove(destroyedActivity)
                    if (destroyedActivity == activity) {
                        application.unregisterActivityLifecycleCallbacks(this)
                    }
                }

                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                    /* no-op */
                }

                override fun onActivityStarted(activity: Activity) {
                    /* no-op */
                }

                override fun onActivityResumed(activity: Activity) {
                    /* no-op */
                }

                override fun onActivityPaused(activity: Activity) {
                    /* no-op */
                }

                override fun onActivityStopped(activity: Activity) {
                    /* no-op */
                }

                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
                    /* no-op */
                }
            })

            requestPermissionLaunchers[activity] =
                activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                    permissionsFlow.tryEmit(isGranted)
                }

            chooseFileLaunchers[activity] =
                activity.registerForActivityResult(ChooseFileActivityResultContract()) { result ->
                    chooseFileResultFlow.tryEmit(ActivityResult(activity, result))
                }

            financialConnectionsSheets[activity] =
                // Using `FinancialConnectionsSheet.create()` here for both link account and manual entry flows.
                // Ideally, we'd use `createForBankAccountToken()` for manual entry flows, but we're currently unable
                // to determine which flow the user is on based on the JS message received.
                FinancialConnectionsSheet.create(activity) { result ->
                    financialConnectionsResults.tryEmit(ActivityResult(activity, result))
                }
        }
    }

    internal class ActivityResult<T>(val activity: Activity, val result: T)
}
