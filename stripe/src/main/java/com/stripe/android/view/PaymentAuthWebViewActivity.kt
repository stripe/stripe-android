package com.stripe.android.view

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.stripe.android.Logger
import com.stripe.android.PaymentAuthWebViewStarter
import com.stripe.android.R
import com.stripe.android.stripe3ds2.utils.CustomizeUtils
import com.ults.listeners.SdkChallengeInterface.UL_HANDLE_CHALLENGE_ACTION
import kotlinx.android.synthetic.main.payment_auth_web_view_layout.*

class PaymentAuthWebViewActivity : AppCompatActivity() {

    private lateinit var logger: Logger
    private lateinit var args: PaymentAuthWebViewStarter.Args

    private val resultIntent: Intent
        get() {
            return Intent()
                .putExtra(StripeIntentResultExtras.CLIENT_SECRET, args.clientSecret)
                .putExtra(StripeIntentResultExtras.SOURCE_ID,
                    Uri.parse(args.url).lastPathSegment.orEmpty()
                )
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val args: PaymentAuthWebViewStarter.Args? =
            intent.getParcelableExtra(PaymentAuthWebViewStarter.EXTRA_ARGS)
        if (args == null) {
            setResult(Activity.RESULT_CANCELED)
            finish()
            return
        }

        this.args = args

        logger = Logger.getInstance(args.enableLogging)
        logger.debug("PaymentAuthWebViewActivity#onCreate()")

        LocalBroadcastManager.getInstance(this)
            .sendBroadcast(Intent().setAction(UL_HANDLE_CHALLENGE_ACTION))

        setContentView(R.layout.payment_auth_web_view_layout)

        val toolbar: Toolbar = findViewById(R.id.payment_auth_web_view_toolbar)
        setSupportActionBar(toolbar)

        customizeToolbar(toolbar)

        val clientSecret = args.clientSecret
        setResult(Activity.RESULT_OK, resultIntent)

        if (clientSecret.isBlank()) {
            logger.debug("PaymentAuthWebViewActivity#onCreate() - clientSecret is blank")
            finish()
            return
        }

        logger.debug("PaymentAuthWebViewActivity#onCreate() - PaymentAuthWebView init and loadUrl")
        auth_web_view.init(this, logger, auth_web_view_progress_bar, clientSecret, args.returnUrl)
        auth_web_view.loadUrl(args.url)
    }

    override fun onDestroy() {
        auth_web_view_container.removeAllViews()
        auth_web_view.destroy()
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        logger.debug("PaymentAuthWebViewActivity#onCreateOptionsMenu()")
        menuInflater.inflate(R.menu.payment_auth_web_view_menu, menu)

        args.toolbarCustomization?.let {
            val buttonText = it.buttonText
            if (!buttonText.isNullOrBlank()) {
                logger.debug("PaymentAuthWebViewActivity#customizeToolbar() - updating close button text")
                val closeMenuItem = menu.findItem(R.id.action_close)
                closeMenuItem.title = buttonText
            }
        }

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        logger.debug("PaymentAuthWebViewActivity#onOptionsItemSelected()")
        if (item.itemId == R.id.action_close) {
            cancelIntentSource()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        cancelIntentSource()
    }

    private fun cancelIntentSource() {
        setResult(Activity.RESULT_OK,
            resultIntent
                .putExtra(StripeIntentResultExtras.SHOULD_CANCEL_SOURCE, true)
        )
        finish()
    }

    private fun customizeToolbar(toolbar: Toolbar) {
        logger.debug("PaymentAuthWebViewActivity#customizeToolbar()")
        args.toolbarCustomization?.let { toolbarCustomization ->
            val headerText = toolbarCustomization.headerText
            if (!headerText.isNullOrBlank()) {
                logger.debug("PaymentAuthWebViewActivity#customizeToolbar() - updating toolbar title")
                toolbar.title = CustomizeUtils.buildStyledText(this, headerText, toolbarCustomization)
            }

            toolbarCustomization.backgroundColor?.let { backgroundColor ->
                logger.debug("PaymentAuthWebViewActivity#customizeToolbar() - updating toolbar background color")
                @ColorInt val backgroundColorInt = Color.parseColor(backgroundColor)
                toolbar.setBackgroundColor(backgroundColorInt)
                CustomizeUtils.setStatusBarColor(this, backgroundColorInt)
            }
        }
    }
}
