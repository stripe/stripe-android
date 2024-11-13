package com.stripe.android.connect.webview

import android.content.Context
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

/**
 * Provides an interface for various download and file operations. Useful for mocking in tests.
 */
internal interface StripeToastManager {
    fun showToast(toastString: String)
}

internal class StripeToastManagerImpl(
    private val context: Context,
    private val scope: CoroutineScope = MainScope()
) : StripeToastManager {
    override fun showToast(toastString: String) {
        scope.launch {
            Toast.makeText(context, toastString, Toast.LENGTH_LONG).show()
        }
    }
}
