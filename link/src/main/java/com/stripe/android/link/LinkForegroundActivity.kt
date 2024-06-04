package com.stripe.android.link

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri

internal class LinkForegroundActivity : AppCompatActivity() {
    companion object {
        const val ACTION_REDIRECT = "LinkForegroundActivity.redirect"
        const val EXTRA_POPUP_URL = "LinkPopupUrl"
        const val EXTRA_FAILURE = "LinkFailure"
        const val RESULT_COMPLETE = 49871
        const val RESULT_FAILURE = 91367

        private const val SAVED_STATE_HAS_LAUNCHED_POPUP = "LinkHasLaunchedPopup"

        fun createIntent(context: Context, popupUrl: String): Intent {
            return Intent(context, LinkForegroundActivity::class.java)
                .putExtra(EXTRA_POPUP_URL, popupUrl)
        }

        fun redirectIntent(context: Context, uri: Uri?): Intent {
            val intent = Intent(context, LinkForegroundActivity::class.java)
            intent.action = ACTION_REDIRECT
            intent.data = uri
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            return intent
        }
    }

    private var hasLaunchedPopup: Boolean = false

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        hasLaunchedPopup = savedInstanceState?.getBoolean(SAVED_STATE_HAS_LAUNCHED_POPUP) ?: false

        handleRedirectIfAvailable(intent)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(SAVED_STATE_HAS_LAUNCHED_POPUP, hasLaunchedPopup)
    }

    public override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleRedirectIfAvailable(intent)
    }

    override fun onResume() {
        super.onResume()

        if (!isFinishing) {
            if (hasLaunchedPopup) {
                setResult(Activity.RESULT_CANCELED)
                finish()
            } else {
                launchPopup()
            }
        }
    }

    private fun handleRedirectIfAvailable(intent: Intent) {
        if (intent.action == ACTION_REDIRECT) {
            setResult(RESULT_COMPLETE, intent)
            finish()
        }
    }

    private fun launchPopup() {
        hasLaunchedPopup = true

        val popupUri = intent.extras?.getString(EXTRA_POPUP_URL)?.toUri()
        if (popupUri == null) {
            setResult(Activity.RESULT_CANCELED)
            finish()
            return
        }
        try {
            CustomTabsIntent.Builder()
                .setShareState(CustomTabsIntent.SHARE_STATE_OFF)
                .build()
                .launchUrl(this, popupUri)
        } catch (e: ActivityNotFoundException) {
            setResult(RESULT_FAILURE, Intent().putExtra(EXTRA_FAILURE, e))
            finish()
        }
    }
}
