package com.stripe.android.core.browser.customtabs

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION_CODES
import androidx.annotation.RestrictTo
import androidx.browser.customtabs.CustomTabsCallback
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsIntent
import androidx.browser.customtabs.CustomTabsServiceConnection
import androidx.browser.customtabs.CustomTabsSession
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.stripe.android.core.Logger
import javax.inject.Inject

/**
 * Custom Tabs Service Connection manager implementation.
 *
 * Usage:
 *
 * 1. Ensure you register the CustomTabsService in your SDK's AndroidManifest.
 * 2. Register the CustomTabsManager as a lifecycle observer in your activity.
 * 3. (Optional) Call [CustomTabsManager.mayLaunchUrl] to pre-fetch the url.
 * 4. Call [CustomTabsManager.openCustomTab] to open URLs.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface CustomTabsManager : DefaultLifecycleObserver {

    /**
     * Opens the URL on a Custom Tab if possible.
     *
     * @param activity The host activity.
     * @param uri the Uri to be opened.
     * @param fallback a CustomTabFallback to be used if Custom Tabs is not available.
     */
    fun openCustomTab(
        activity: Activity,
        uri: Uri,
        fallback: () -> Unit
    )

    /**
     * Warms up the browser for a given URL, so that it loads faster when launched.
     */
    fun mayLaunchUrl(url: String): Boolean
}

/**
 * Manages the connection to the CustomTabsService.
 *
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class CustomTabsManagerImpl @Inject constructor(
    private val logger: Logger,
    private val getCustomTabsPackage: GetCustomTabsPackage
) : CustomTabsManager {

    private var client: CustomTabsClient? = null
    private var connection: CustomTabsServiceConnection? = null
    private var session: CustomTabsSession? = null
    private var serviceBound = false

    /**
     * Binds the Activity to the CustomTabsService.
     */
    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        val context = owner as? Context ?: return
        if (client != null) return
        connection = ServiceConnection(object : ServiceConnectionCallback {
            override fun onServiceConnected(
                client: CustomTabsClient
            ) {
                log("Service connected")
                this@CustomTabsManagerImpl.client = client
                this@CustomTabsManagerImpl.client?.warmup(0)
                session = this@CustomTabsManagerImpl.client?.newSession(CustomTabsCallback())
            }

            override fun onServiceDisconnected() {
                log("Service disconnected")
                client = null
                session = null
            }
        })
        bindCustomTabService(context)
    }

    /**
     * Unbinds the Activity from the CustomTabsService.
     */
    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        val context = owner as? Context ?: return
        if (connection == null) return
        connection?.let {
            runCatching {
                if (serviceBound) {
                    context.unbindService(it)
                    serviceBound = false
                } else {
                    log("OnStop: service was not bound")
                }
            }
                .onFailure { log("OnStop: couldn't unbind, ${it.stackTraceToString()}") }
                .onSuccess { log("OnStop: service unbound") }
        }
        client = null
        connection = null
        session = null
    }

    /**
     * Creates or retrieves an exiting CustomTabsSession.
     *
     * @return a CustomTabsSession, or null if a connection to the service could not be established.
     */
    private fun getOrRetrieveSession(): CustomTabsSession? {
        when {
            client == null -> session = null
            session == null -> session = client?.newSession(null)
        }
        return session
    }

    override fun openCustomTab(
        activity: Activity,
        uri: Uri,
        fallback: () -> Unit
    ) {
        runCatching {
            val packageName: String? = getCustomTabsPackage(uri)
            val customTabsIntent = buildCustomTabsIntent()

            // If we cant find a package name no browser that supports Custom Tabs is installed.
            if (packageName == null) {
                if (attemptToLaunchInNativeApp(activity, uri)) {
                    log("uri launched on native app")
                } else {
                    log("Custom tabs unsupported, using fallback")
                    fallback.invoke()
                }
            } else {
                log("Opening Custom Tab with package: $packageName")
                customTabsIntent.intent.setPackage(packageName)
                customTabsIntent.launchUrl(activity, uri)
            }
        }.onFailure {
            log("Failed to open Custom Tab, using fallback: ${it.stackTraceToString()}")
            fallback.invoke()
        }
    }

    private fun attemptToLaunchInNativeApp(activity: Activity, uri: Uri) = when {
        /**
         * Android 11 introduces a new Intent flag, FLAG_ACTIVITY_REQUIRE_NON_BROWSER, which is the recommended way
         * to try opening a native app, as it does not require the app to declare any package manager queries.
         */
        Build.VERSION.SDK_INT >= VERSION_CODES.R -> try {
            activity.startActivity(
                Intent(Intent.ACTION_VIEW, uri)
                    .addCategory(Intent.CATEGORY_BROWSABLE)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REQUIRE_NON_BROWSER)
            )
            true
        } catch (ex: ActivityNotFoundException) {
            log("No native app found to launch $uri. ${ex.stackTraceToString()}")
            false
        }
        /**
         * Pre-Android 11, we need to query the package manager to see if the app is installed,
         * which we can't do as we'd need to register all bank apps supporting app2app.
         */
        else -> false
    }

    private fun buildCustomTabsIntent() = CustomTabsIntent.Builder(getOrRetrieveSession())
        .setSendToExternalDefaultHandlerEnabled(true)
        .setShareState(CustomTabsIntent.SHARE_STATE_OFF)
        .setDownloadButtonEnabled(false)
        .setBookmarksButtonEnabled(false)
        .build()

    override fun mayLaunchUrl(url: String): Boolean = when (val session = getOrRetrieveSession()) {
        null -> {
            log("Client or session is null, unable to prefetch")
            false
        }
        else -> {
            val prefetchResult = session.mayLaunchUrl(Uri.parse(url), null, null)
            log("URL prefetch: $url, Result: $prefetchResult")
            prefetchResult
        }
    }

    private fun bindCustomTabService(context: Context) {
        // Check for an existing connection
        if (client != null) {
            log("Bind unnecessary: Client already exists")
            return
        }

        val packageName = getCustomTabsPackage(Uri.parse("http://"))
        if (packageName == null) {
            log("Unable to bind: No Custom Tabs compatible browser found")
            return
        }
        connection?.let {
            if (CustomTabsClient.bindCustomTabsService(context, packageName, it)) {
                log("Bind successful")
                serviceBound = true
            } else {
                log("Bind failed")
            }
        }
    }

    private fun log(message: String) {
        logger.debug("CustomTabsManager: $message")
    }
}
