package com.stripe.android.shoppay

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.os.BundleCompat
import androidx.core.os.bundleOf
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stripe.android.common.ui.BottomSheetScaffold
import com.stripe.android.common.ui.ElementsBottomSheetLayout
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.WalletConfiguration
import com.stripe.android.shoppay.webview.MainWebView
import com.stripe.android.shoppay.webview.WebViewModel
import com.stripe.android.uicore.elements.bottomsheet.rememberStripeBottomSheetState
import com.stripe.android.uicore.utils.collectAsState

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
                }
            )
        }
    }

    @Composable
    private fun ComposeWebView() {
        val viewModel: WebViewModel = viewModel(
            factory = WebViewModel.Factory(
                walletHandlers = WalletConfiguration.Handlers()
            )
        )
        val showPopup by viewModel.showPopup.collectAsState()

        Column(modifier = Modifier.fillMaxSize()) {
            val canGoBack = remember { mutableStateOf(false) }
            val canGoForward = remember { mutableStateOf(false) }

            TopAppBar(
                title = { Text("Stripe Checkout Bridge") },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            viewModel.webView.value?.goBack()
                        },
                        enabled = canGoBack.value
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.webView.value?.goForward()
                        },
                        enabled = canGoForward.value
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Forward"
                        )
                    }
                    IconButton(
                        onClick = {
                            viewModel.webView.value?.reload()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh"
                        )
                    }
                }
            )

            Box(modifier = Modifier.fillMaxSize()) {
                MainWebView(
                    viewModel = viewModel,
                    onNavigationStateChange = { back, forward ->
                        canGoBack.value = back
                        canGoForward.value = forward
                    }
                )

                if (showPopup) {
                    PopupWebViewDialog(viewModel = viewModel)
                }
            }
        }
    }

    @Composable
    fun PopupWebViewDialog(viewModel: WebViewModel) {
        val popupWebView by viewModel.popupWebView.collectAsState()

        popupWebView?.let { webView ->
            AndroidView(
                factory = {
                    webView.apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()// Use a fraction of the dialog's max size
            )
        }
    }

    private fun dismissWithResult(result: ShopPayActivityResult) {
        val bundle = bundleOf(
            ShopPayActivityContract.EXTRA_RESULT to result
        )
        setResult(RESULT_COMPLETE, Intent().putExtras(bundle))
        finish()
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
