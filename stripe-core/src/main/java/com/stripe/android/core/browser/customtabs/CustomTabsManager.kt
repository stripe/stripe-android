package com.stripe.android.core.browser.customtabs

import android.app.Activity
import android.content.Context
import android.net.Uri
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

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface CustomTabsManager : DefaultLifecycleObserver {

    /**
     * Opens the URL on a Custom Tab if possible. Otherwise fallsback to opening it on a WebView.
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

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {
        operator fun invoke(logger: Logger): CustomTabsManager {
            return CustomTabsManagerImpl(
                logger,
                GetCustomTabsPackage(logger)
            )
        }
    }
}

/**
 * Manages the connection to the CustomTabsService.
 *
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
private class CustomTabsManagerImpl @Inject constructor(
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
                .onSuccess { }
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
    private fun getSession(): CustomTabsSession? {
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
            val packageName: String? = getCustomTabsPackage(activity)
            val customTabsIntent = buildCustomTabsIntent()

            // If we cant find a package name no browser that supports Custom Tabs is installed.
            if (packageName == null) {
                fallback.invoke()
            } else {
                customTabsIntent.intent.setPackage(packageName)
                customTabsIntent.launchUrl(activity, uri)
            }
        }.onFailure {
            fallback.invoke()
        }
    }

    private fun buildCustomTabsIntent() = CustomTabsIntent.Builder(getSession())
        .setShareState(CustomTabsIntent.SHARE_STATE_OFF)
        .build()

    override fun mayLaunchUrl(url: String): Boolean = client
        ?.let { getSession() }
        ?.mayLaunchUrl(Uri.parse(url), null, null)?.also {
            log("URL prefetch: $url, Result: $it")
        } ?: run {
        log("URL not prefetched, ${if (client == null) "null client" else "null session"}")
        false
    }

    private fun bindCustomTabService(context: Context) {
        // Check for an existing connection
        if (client != null) {
            log("Bind unnecessary: Client already exists")
            return
        }

        val packageName = getCustomTabsPackage(context)
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
