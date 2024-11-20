package com.stripe.android.connect.webview

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import com.stripe.android.connect.R
import com.stripe.android.core.BuildConfig
import com.stripe.android.core.Logger

internal interface StripeIntentLauncher {
    /**
     * Launches [uri] in a secure external Android Custom Tab.
     */
    fun launchSecureExternalWebTab(context: Context, uri: Uri)

    /**
     * Launches a uri with a mailto scheme.
     */
    fun launchEmailLink(context: Context, uri: Uri)

    /**
     * Launches [uri] with the system handler, allowing the system to choose how to open it.
     */
    fun launchUrlWithSystemHandler(context: Context, uri: Uri)
}

internal class StripeIntentLauncherImpl(
    private val toastManagerImpl: StripeToastManagerImpl = StripeToastManagerImpl(),
    private val logger: Logger = Logger.getInstance(enableLogging = BuildConfig.DEBUG)
) : StripeIntentLauncher {

    override fun launchSecureExternalWebTab(context: Context, uri: Uri) {
        val customTabsIntent = CustomTabsIntent.Builder().build()
        customTabsIntent.launchUrl(context, uri)
    }

    override fun launchUrlWithSystemHandler(context: Context, uri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            logger.error("Failed to open URL with system handler: ${e.message}")
            toastManagerImpl.showToast(context, context.getString(R.string.stripe_failed_to_open_url, uri.toString()))
        }
    }

    override fun launchEmailLink(context: Context, uri: Uri) {
        val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // log an error and fall back to a generic system handler
            logger.error("Failed to open URL with email handler: ${e.message}")
            launchUrlWithSystemHandler(context, uri)
        }
    }
}
