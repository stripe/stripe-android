package com.stripe.android.connect.webview

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast

/**
 * Provides an interface for various download and file operations. Useful for mocking in tests.
 */
internal interface StripeToastManager {
    fun showToast(context: Context, toastString: String)
}

internal class StripeToastManagerImpl : StripeToastManager {
    override fun showToast(context: Context, toastString: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, toastString, Toast.LENGTH_LONG).show()
        }
    }
}
