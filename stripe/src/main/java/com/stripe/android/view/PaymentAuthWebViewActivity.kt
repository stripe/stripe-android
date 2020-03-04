package com.stripe.android.view

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.stripe.android.Logger
import com.stripe.android.PaymentAuthWebViewStarter
import com.stripe.android.R
import com.stripe.android.databinding.PaymentAuthWebViewActivityBinding
import com.stripe.android.stripe3ds2.utils.CustomizeUtils
import com.ults.listeners.SdkChallengeInterface.UL_HANDLE_CHALLENGE_ACTION

class PaymentAuthWebViewActivity : AppCompatActivity() {

    private val viewBinding: PaymentAuthWebViewActivityBinding by lazy {
        PaymentAuthWebViewActivityBinding.inflate(layoutInflater)
    }

    private lateinit var logger: Logger
    private lateinit var viewModel: PaymentAuthWebViewActivityViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val args: PaymentAuthWebViewStarter.Args? =
            intent.getParcelableExtra(PaymentAuthWebViewStarter.EXTRA_ARGS)
        if (args == null) {
            setResult(Activity.RESULT_CANCELED)
            finish()
            return
        }

        viewModel = ViewModelProvider(
            this, PaymentAuthWebViewActivityViewModel.Factory(args)
        )[PaymentAuthWebViewActivityViewModel::class.java]

        logger = Logger.getInstance(args.enableLogging)
        logger.debug("PaymentAuthWebViewActivity#onCreate()")

        LocalBroadcastManager.getInstance(this)
            .sendBroadcast(Intent().setAction(UL_HANDLE_CHALLENGE_ACTION))

        setContentView(viewBinding.root)

        setSupportActionBar(viewBinding.toolbar)
        customizeToolbar()

        val clientSecret = args.clientSecret
        setResult(Activity.RESULT_OK, Intent().putExtras(viewModel.paymentResult.toBundle()))

        if (clientSecret.isBlank()) {
            logger.debug("PaymentAuthWebViewActivity#onCreate() - clientSecret is blank")
            finish()
            return
        }

        logger.debug("PaymentAuthWebViewActivity#onCreate() - PaymentAuthWebView init and loadUrl")
        viewBinding.authWebView.init(
            this,
            logger,
            viewBinding.authWebViewProgressBar,
            clientSecret,
            args.returnUrl
        )
        viewBinding.authWebView.loadUrl(args.url)
    }

    override fun onDestroy() {
        viewBinding.authWebViewContainer.removeAllViews()
        viewBinding.authWebView.destroy()
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        logger.debug("PaymentAuthWebViewActivity#onCreateOptionsMenu()")
        menuInflater.inflate(R.menu.payment_auth_web_view_menu, menu)

        viewModel.buttonText?.let {
            logger.debug("PaymentAuthWebViewActivity#customizeToolbar() - updating close button text")
            menu.findItem(R.id.action_close).title = it
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
        viewModel.cancelIntentSource().observe(this, Observer { intent ->
            setResult(Activity.RESULT_OK, intent)
            finish()
        })
    }

    private fun customizeToolbar() {
        logger.debug("PaymentAuthWebViewActivity#customizeToolbar()")

        viewModel.toolbarTitle?.let {
            logger.debug("PaymentAuthWebViewActivity#customizeToolbar() - updating toolbar title")
            viewBinding.toolbar.title = CustomizeUtils.buildStyledText(
                this,
                it.text,
                it.toolbarCustomization
            )
        }

        viewModel.toolbarBackgroundColor?.let { backgroundColor ->
            logger.debug("PaymentAuthWebViewActivity#customizeToolbar() - updating toolbar background color")
            @ColorInt val backgroundColorInt = Color.parseColor(backgroundColor)
            viewBinding.toolbar.setBackgroundColor(backgroundColorInt)
            CustomizeUtils.setStatusBarColor(this, backgroundColorInt)
        }
    }
}
