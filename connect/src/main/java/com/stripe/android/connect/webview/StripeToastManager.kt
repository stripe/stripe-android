package com.stripe.android.connect.webview

import android.content.Context
import android.widget.Toast

/**
 * Provides an interface for various download and file operations. Useful for mocking in tests.
 */
interface StripeToastManager {
    fun showToast(toastString: String)
}

class StripeToastManagerImpl(private val context: Context) : StripeToastManager {
    override fun showToast(toastString: String) {
        Toast.makeText(context, toastString, Toast.LENGTH_LONG).show()
    }
}
