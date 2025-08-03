package com.stripe.android.iap

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri

internal class CheckoutForegroundActivity : AppCompatActivity() {
    companion object {
        const val ACTION_REDIRECT = "CheckoutForegroundActivity.redirect"
        const val EXTRA_CHECKOUT_URL = "CheckoutUrl"
        const val EXTRA_FAILURE = "CheckoutFailure"
        const val RESULT_COMPLETE = 49871
        const val RESULT_FAILURE = 91367

        private const val SAVED_STATE_HAS_LAUNCHED_URL = "CheckoutForegroundActivity_LaunchedUrl"

        fun createIntent(context: Context, checkoutUrl: String): Intent {
            return Intent(context, CheckoutForegroundActivity::class.java)
                .putExtra(EXTRA_CHECKOUT_URL, checkoutUrl)
        }

        fun redirectIntent(context: Context, uri: Uri?): Intent {
            val intent = Intent(context, CheckoutForegroundActivity::class.java)
            intent.action = ACTION_REDIRECT
            intent.data = uri
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            return intent
        }
    }

    private var hasLaunchedUrl: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        hasLaunchedUrl = savedInstanceState?.getBoolean(SAVED_STATE_HAS_LAUNCHED_URL) ?: false

        handleRedirectIfAvailable(intent)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(SAVED_STATE_HAS_LAUNCHED_URL, hasLaunchedUrl)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleRedirectIfAvailable(intent)
    }

    override fun onResume() {
        super.onResume()

        if (!isFinishing) {
            if (hasLaunchedUrl) {
                setResult(RESULT_CANCELED)
                finish()
            } else {
                launchCheckoutUrl()
            }
        }
    }

    private fun handleRedirectIfAvailable(intent: Intent) {
        if (intent.action == ACTION_REDIRECT) {
            setResult(RESULT_COMPLETE, intent)
            finish()
        }
    }

    private fun launchCheckoutUrl() {
        hasLaunchedUrl = true

        val checkoutUri = intent.extras?.getString(EXTRA_CHECKOUT_URL)?.toUri()
        if (checkoutUri == null) {
            setResult(RESULT_CANCELED)
            finish()
            return
        }
        try {
            CustomTabsIntent.Builder()
                .setShareState(CustomTabsIntent.SHARE_STATE_OFF)
                .build()
                .launchUrl(this, checkoutUri)
        } catch (e: ActivityNotFoundException) {
            setResult(RESULT_FAILURE, Intent().putExtra(EXTRA_FAILURE, e))
            finish()
        }
    }
}
