package com.stripe.android.view

import android.app.Activity
import android.webkit.ConsoleMessage
import android.webkit.JsResult
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.appcompat.app.AlertDialog
import com.stripe.android.R
import com.stripe.android.core.Logger

internal class PaymentAuthWebChromeClient(
    private val activity: Activity,
    private val logger: Logger
) : WebChromeClient() {
    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
        consoleMessage?.message()?.let(logger::debug)
        return super.onConsoleMessage(consoleMessage)
    }

    override fun onJsConfirm(
        view: WebView?,
        url: String?,
        message: String?,
        result: JsResult?
    ): Boolean {
        AlertDialog.Builder(activity, R.style.StripeAlertDialogStyle)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok) { _, _ -> result?.confirm() }
            .setNegativeButton(android.R.string.cancel) { _, _ -> result?.cancel() }
            .create()
            .show()
        return true
    }
}
