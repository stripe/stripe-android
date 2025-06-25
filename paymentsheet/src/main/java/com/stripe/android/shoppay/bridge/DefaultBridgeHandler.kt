package com.stripe.android.shoppay.bridge

import android.webkit.JavascriptInterface
import com.stripe.android.core.BuildConfig
import com.stripe.android.core.Logger
import com.stripe.android.core.injection.PUBLISHABLE_KEY
import javax.inject.Inject
import javax.inject.Named

internal class DefaultBridgeHandler @Inject constructor(
    @Named(PUBLISHABLE_KEY) private val publishableKey: String
) : BridgeHandler {
    private val logger = Logger.getInstance(BuildConfig.DEBUG)

    @JavascriptInterface
    override fun consoleLog(level: String, message: String, origin: String, url: String) {
        val emoji = when (level.lowercase()) {
            "error" -> "❌"
            "warn" -> "⚠️"
            else -> "📝"
        }
        val frameInfo = if (origin != url) "[$origin]" else ""
        logMessage(emoji, "Console ${level.uppercase()}$frameInfo: $message")
    }

    @JavascriptInterface
    override fun getStripePublishableKey() = publishableKey

    @JavascriptInterface
    override fun ready(message: String) {
        logMessage(message = message)
    }

    private fun logMessage(emoji: String = "📝", message: String) {
        logger.debug("$emoji $message")
    }
}
