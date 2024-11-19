package com.stripe.android.connect.webview

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent

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

internal class StripeIntentLauncherImpl : StripeIntentLauncher {
    override fun launchSecureExternalWebTab(context: Context, uri: Uri) {
        val customTabsIntent = CustomTabsIntent.Builder().build()
        customTabsIntent.launchUrl(context, uri)
    }

    override fun launchUrlWithSystemHandler(context: Context, uri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
//            logger.error("Failed to open URL with system handler: ${e.message}")
            // Optionally, you could show a toast or snackbar to inform the user
            // that the URL couldn't be opened
        }
    }

    override fun launchEmailLink(context: Context, uri: Uri) {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = uri
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
//            logger.error("Failed to open mailto link: ${e.message}")
            // Fallback to system handler
            openUrlWithSystemHandler(context, uri)
        }
    }

    private fun openUrlWithSystemHandler(context: Context, uri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
//            logger.error("Failed to open URL with system handler: ${e.message}")
            // Optionally, you could show a toast or snackbar to inform the user
            // that the URL couldn't be opened
        }
    }
}
