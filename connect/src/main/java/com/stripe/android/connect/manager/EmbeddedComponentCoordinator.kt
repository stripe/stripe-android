package com.stripe.android.connect.manager

import android.Manifest
import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.VisibleForTesting
import androidx.core.content.ContextCompat.checkSelfPermission
import com.stripe.android.connect.BuildConfig
import com.stripe.android.connect.EmbeddedComponentManager.Configuration
import com.stripe.android.connect.FetchClientSecretCallback
import com.stripe.android.connect.PrivateBetaConnectSDK
import com.stripe.android.connect.StripeEmbeddedComponent
import com.stripe.android.connect.analytics.ComponentAnalyticsService
import com.stripe.android.connect.analytics.ConnectAnalyticsService
import com.stripe.android.connect.appearance.Appearance
import com.stripe.android.connect.appearance.fonts.CustomFontSource
import com.stripe.android.connect.di.StripeConnectComponent
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
import javax.inject.Inject
import kotlin.coroutines.resume

@OptIn(PrivateBetaConnectSDK::class)
@EmbeddedComponentManagerScope
internal class EmbeddedComponentCoordinator @Inject constructor(
    private val configuration: Configuration,
    private val fetchClientSecretCallback: FetchClientSecretCallback,
    private val logger: Logger,
    appearance: Appearance,
    internal val customFonts: List<CustomFontSource>,
) {
    private val _appearanceFlow = MutableStateFlow(appearance)
    internal val appearanceFlow: StateFlow<Appearance> get() = _appearanceFlow.asStateFlow()

    private val loggerTag = javaClass.simpleName
    private val isDebugBuild = BuildConfig.DEBUG

    fun update(appearance: Appearance) {
        _appearanceFlow.value = appearance
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

    internal fun checkContextDuringCreate(context: Context) {
        val activity = context.findActivityWithErrorHandling()
        checkNotNull(requestPermissionLaunchers[activity]) {
            "You must call EmbeddedComponentManager.onActivityCreate in your Activity.onCreate function"
        }
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

    companion object {
        private var connectAnalyticsService: ConnectAnalyticsService? = null

        @VisibleForTesting
        internal val permissionsFlow: MutableSharedFlow<Boolean> = MutableSharedFlow(extraBufferCapacity = 1)
        val requestPermissionLaunchers = mutableMapOf<Activity, ActivityResultLauncher<String>>()

        @VisibleForTesting
        internal val chooseFileResultFlow: MutableSharedFlow<ActivityResult<Array<Uri>?>> =
            MutableSharedFlow(extraBufferCapacity = 1)
        val chooseFileLaunchers = mutableMapOf<Activity, ActivityResultLauncher<Intent>>()

        @VisibleForTesting
        internal val financialConnectionsResults: MutableSharedFlow<ActivityResult<FinancialConnectionsSheetResult>> =
            MutableSharedFlow(extraBufferCapacity = 1)
        val financialConnectionsSheets = mutableMapOf<Activity, FinancialConnectionsSheet>()

        fun onActivityCreate(activity: ComponentActivity) {
            val application = activity.application

            if (connectAnalyticsService == null) {
                connectAnalyticsService = StripeConnectComponent.instance.analyticsServiceFactory.create(application)
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
