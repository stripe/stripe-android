package com.stripe.android.view

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.ColorInt
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isGone
import androidx.lifecycle.lifecycleScope
import com.stripe.android.R
import com.stripe.android.StripeIntentResult
import com.stripe.android.auth.PaymentBrowserAuthContract
import com.stripe.android.core.Logger
import com.stripe.android.core.exception.StripeException
import com.stripe.android.databinding.StripePaymentAuthWebViewActivityBinding
import com.stripe.android.payments.PaymentFlowResult
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.stripe3ds2.utils.CustomizeUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class PaymentAuthWebViewActivity : AppCompatActivity() {

    private val viewBinding: StripePaymentAuthWebViewActivityBinding by lazy {
        StripePaymentAuthWebViewActivityBinding.inflate(layoutInflater)
    }

    private val _args: PaymentBrowserAuthContract.Args? by lazy {
        PaymentBrowserAuthContract.parseArgs(intent)
    }

    private val logger: Logger by lazy {
        Logger.getInstance(_args?.enableLogging == true)
    }
    private val viewModel: PaymentAuthWebViewActivityViewModel by viewModels {
        PaymentAuthWebViewActivityViewModel.Factory(
            application,
            logger,
            requireNotNull(_args)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val args = _args
        if (args == null) {
            setResult(Activity.RESULT_CANCELED)
            finish()
            ErrorReporter.createFallbackInstance(applicationContext)
                .report(
                    errorEvent = ErrorReporter.ExpectedErrorEvent.AUTH_WEB_VIEW_NULL_ARGS,
                )
            return
        }

        logger.debug("PaymentAuthWebViewActivity#onCreate()")

        setContentView(viewBinding.root)

        setSupportActionBar(viewBinding.toolbar)
        customizeToolbar()

        onBackPressedDispatcher.addCallback {
            if (viewBinding.webView.canGoBack()) {
                viewBinding.webView.goBack()
            } else {
                cancelIntentSource()
            }
        }

        val clientSecret = args.clientSecret
        setResult(Activity.RESULT_OK, createResultIntent(viewModel.paymentResult))

        if (clientSecret.isBlank()) {
            logger.debug("PaymentAuthWebViewActivity#onCreate() - clientSecret is blank")
            finish()
            ErrorReporter.createFallbackInstance(applicationContext)
                .report(
                    errorEvent = ErrorReporter.UnexpectedErrorEvent.AUTH_WEB_VIEW_BLANK_CLIENT_SECRET,
                )
            return
        }

        logger.debug("PaymentAuthWebViewActivity#onCreate() - PaymentAuthWebView init and loadUrl")

        val isPagedLoaded = MutableStateFlow(false)
        lifecycleScope.launch {
            isPagedLoaded.collect { shouldHide ->
                if (shouldHide) {
                    viewBinding.progressBar.isGone = true
                }
            }
        }

        val webViewClient = PaymentAuthWebViewClient(
            logger,
            isPagedLoaded,
            clientSecret,
            args.returnUrl,
            ::startActivity,
            ::onAuthComplete
        )
        viewBinding.webView.onLoadBlank = {
            webViewClient.hasLoadedBlank = true
        }
        viewBinding.webView.webViewClient = webViewClient
        viewBinding.webView.webChromeClient = PaymentAuthWebChromeClient(this, logger)

        viewModel.logStart()
        viewBinding.webView.loadUrl(
            args.url,
            viewModel.extraHeaders
        )
    }

    @VisibleForTesting
    internal fun onAuthComplete(
        error: Throwable?
    ) {
        if (error != null) {
            ErrorReporter.createFallbackInstance(applicationContext)
                .report(
                    errorEvent = ErrorReporter.ExpectedErrorEvent.AUTH_WEB_VIEW_FAILURE,
                    stripeException = StripeException.create(error),
                )
            viewModel.logError()
            setResult(
                Activity.RESULT_OK,
                createResultIntent(
                    viewModel.paymentResult
                        .copy(
                            exception = StripeException.create(error),
                            flowOutcome = StripeIntentResult.Outcome.FAILED,
                            canCancelSource = true
                        )
                )
            )
        } else {
            viewModel.logComplete()
        }
        finish()
    }

    override fun onDestroy() {
        viewBinding.webViewContainer.removeAllViews()
        viewBinding.webView.destroy()
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        logger.debug("PaymentAuthWebViewActivity#onCreateOptionsMenu()")
        menuInflater.inflate(R.menu.stripe_payment_auth_web_view_menu, menu)

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

    private fun cancelIntentSource() {
        setResult(Activity.RESULT_OK, viewModel.cancellationResult)
        finish()
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

    private fun createResultIntent(
        paymentFlowResult: PaymentFlowResult.Unvalidated
    ) = Intent().putExtras(paymentFlowResult.toBundle())
}
