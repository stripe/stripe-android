package com.stripe.android.shoppay

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.os.BundleCompat
import androidx.core.os.bundleOf
import com.stripe.android.common.ui.BottomSheetScaffold
import com.stripe.android.common.ui.ElementsBottomSheetLayout
import com.stripe.android.paymentsheet.R
import com.stripe.android.uicore.elements.bottomsheet.rememberStripeBottomSheetState

/**
 * Activity that handles Shop Pay authentication via WebView.
 */
internal class ShopPayActivity : ComponentActivity() {
    private var args: ShopPayArgs? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        args = intent.extras?.let { args ->
            BundleCompat.getParcelable(args, EXTRA_ARGS, ShopPayArgs::class.java)
        }

        if (args == null) {
            dismissWithResult(ShopPayActivityResult.Failed(Throwable("No args")))
            return
        }

        setContent {
            Content()
        }
    }

    @Composable
    private fun Content() {
        val bottomSheetState = rememberStripeBottomSheetState()

        ElementsBottomSheetLayout(
            state = bottomSheetState,
            onDismissed = {
                dismissWithResult(ShopPayActivityResult.Canceled)
            }
        ) {
            BottomSheetScaffold(
//                modifier = Modifier
//                    .fillMaxSize(),
                topBar = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        IconButton(
                            modifier = Modifier.align(Alignment.CenterStart),
                            onClick = {
                                dismissWithResult(ShopPayActivityResult.Canceled)
                            }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.stripe_ic_paymentsheet_close),
                                contentDescription = "Close"
                            )
                        }
                    }
                },
                content = {
                    ComposeWebView()
//                    Column(
//                        modifier = Modifier
//                            .fillMaxSize()
//                    ) {
//                        Button(
//                            onClick = {
//                                dismissWithResult(ShopPayActivityResult.Completed("pm_1234"))
//                            }
//                        ) {
//                            Text("Complete")
//                        }
//
//                        Button(
//                            onClick = {
//                                dismissWithResult(ShopPayActivityResult.Failed(Throwable("Failed")))
//                            }
//                        ) {
//                            Text("Fail")
//                        }
//                    }
                }
            )
        }
    }

    @Composable
    private fun ComposeWebView() {
        // Declare a string that contains a url
        val mUrl = "https://www.google.com"

        // Adding a WebView inside AndroidView
        // with layout as full screen
        AndroidView(
            modifier = Modifier
                .fillMaxSize(),
            factory = {
                WebView(it).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    settings.javaScriptEnabled = true
                }
            }, update = {
                it.webViewClient = CustomWebViewClient { id, addressLine1 ->
                    dismissWithResult(ShopPayActivityResult.Completed(id, addressLine1))
                }
                it.loadDataWithBaseURL(null, RAW_HTML, "text/html", "UTF-8", null)
//                it.loadUrl(mUrl)
            })
    }

    private fun dismissWithResult(result: ShopPayActivityResult) {
        val bundle = bundleOf(
            ShopPayActivityContract.EXTRA_RESULT to result
        )
        setResult(RESULT_COMPLETE, Intent().putExtras(bundle))
        finish()
    }

    private class CustomWebViewClient(
        private val onComplete: (String, String) -> Unit
    ): WebViewClient() {
        @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            return handleUrl(request.url.toString())
        }

        // For API below 21
        @Suppress("OverridingDeprecatedMember")
        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
            return handleUrl(url)
        }

        private fun handleUrl(url: String): Boolean {

            // Check if it's our stripe URL scheme
            if (url.startsWith("stripe://")) {
                // Parse the URL
                val uri = Uri.parse(url)
                val paymentMethodId = uri.getQueryParameter("paymentMethodId")
                    ?: throw IllegalStateException("No paymentMethodId")
                val addressLine1 = uri.getQueryParameter("address.line1")
                    ?: throw IllegalStateException("No address")

                // Process the payment
//                processPayment(paymentMethodId, addressLine1)
                onComplete(paymentMethodId, addressLine1)

                // Return true to indicate we handled the URL
                return true
            }

            // For all other URLs, let the WebView handle it normally
            return false
        }
    }

    companion object {
        internal const val EXTRA_ARGS = "shop_pay_args"
        internal const val RESULT_COMPLETE = 63636

        internal fun createIntent(
            context: Context,
            args: ShopPayArgs
        ): Intent {
            return Intent(context, ShopPayActivity::class.java)
                .putExtra(EXTRA_ARGS, args)
        }
    }
}

private const val RAW_HTML = """
    <!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Shop Pay</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            max-width: 600px;
            margin: 0 auto;
            padding: 20px;
            text-align: center;
        }
        h1 {
            color: #5A31F4;
            margin-bottom: 30px;
        }
        .checkout-button {
            background-color: #5A31F4;
            color: white;
            border: none;
            padding: 12px 24px;
            font-size: 16px;
            border-radius: 4px;
            cursor: pointer;
            transition: background-color 0.3s;
        }
        .checkout-button:hover {
            background-color: #4526C3;
        }
    </style>
</head>
<body>
    <h1>Welcome to Shop Pay</h1>
    <button class="checkout-button" onclick="window.location.href = 'stripe://action?paymentMethodId=pm_123456758&address.line1=123%20main';">Checkout</button>
</body>
</html>

"""
