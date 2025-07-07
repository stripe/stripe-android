package com.stripe.android.shoppay

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.os.bundleOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import com.stripe.android.common.ui.BottomSheetScaffold
import com.stripe.android.common.ui.ElementsBottomSheetLayout
import com.stripe.android.core.Logger
import com.stripe.android.paymentsheet.BuildConfig
import com.stripe.android.shoppay.webview.EceWebView
import com.stripe.android.shoppay.webview.PopUpWebChromeClient
import com.stripe.android.shoppay.webview.PopUpWebViewClient
import com.stripe.android.ui.core.CircularProgressIndicator
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.elements.bottomsheet.rememberStripeBottomSheetState
import kotlinx.coroutines.launch

internal class ShopPayActivity : ComponentActivity() {
    @VisibleForTesting
    internal var viewModelFactory: ViewModelProvider.Factory = ShopPayViewModel.factory()

    private val viewModel: ShopPayViewModel by viewModels<ShopPayViewModel> {
        viewModelFactory
    }
    private val popupWebView = mutableStateOf<WebView?>(null)

    private val eceWebView by lazy {
        val assetLoader = viewModel.assetLoader(this)
        EceWebView(
            context = this,
            bridgeHandler = viewModel.bridgeHandler,
            webViewClient = PopUpWebViewClient(
                assetLoader = assetLoader,
                onPageLoaded = viewModel::onPageLoaded
            ),
            webChromeClient = PopUpWebChromeClient(
                context = this,
                bridgeHandler = viewModel.bridgeHandler,
                assetLoader = assetLoader,
                setPopUpView = { webView ->
                    popupWebView.value = webView
                },
                closeWebView = viewModel::closePopup,
                onPageLoaded = viewModel::onPageLoaded,
            )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            if (savedInstanceState != null) {
                eceWebView.restoreState(savedInstanceState)
            } else {
                viewModel.loadUrl(eceWebView)
            }
        } catch (e: ShopPayViewModel.NoArgsException) {
            Logger.getInstance(BuildConfig.DEBUG).error("Failed to create ShopPayViewModel", e)
            dismissWithResult(ShopPayActivityResult.Failed(Throwable("Failed to create ShopPayViewModel")))
        }
        setContent {
            Content()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        eceWebView.saveState(outState)
    }

    @Composable
    private fun Content() {
        val bottomSheetState = rememberStripeBottomSheetState()
        val scope = rememberCoroutineScope()

        fun dismiss(result: ShopPayActivityResult) {
            scope.launch {
                bottomSheetState.hide()
                dismissWithResult(result)
            }
        }

        LaunchedEffect(Unit) {
            viewModel.paymentResult.collect { result ->
                dismiss(result)
            }
        }

        StripeTheme(
            // Shop Pay doesn't support dark mode, so we will only be supporting light mode on this screen.
            colors = StripeTheme.getColors(isDark = false)
        ) {
            ElementsBottomSheetLayout(
                state = bottomSheetState,
                onDismissed = {
                    dismiss(ShopPayActivityResult.Canceled)
                }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight(viewModel.sheetHeightRatio),
                ) {
                    ShopPayWebView()
                }
            }
        }
    }

    @Composable
    private fun ShopPayWebView() {
        val popupWebView by remember { this.popupWebView }

        Column(
            modifier = Modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            popupWebView?.let {
                PopupWebViewDialog(it)
            } ?: run {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(64.dp),
                    color = ShopPayBackgroundColor,
                    strokeWidth = 4.dp
                )
            }
        }
    }

    @Composable
    private fun PopupWebViewDialog(webView: WebView) {
        val backgroundColor = MaterialTheme.colors.background.toArgb()

        AndroidView(
            factory = {
                webView.apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    setBackgroundColor(backgroundColor)
                }
            },
            modifier = Modifier
                .fillMaxSize()
        )
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

        internal fun getArgs(savedStateHandle: SavedStateHandle): ShopPayArgs? {
            return savedStateHandle.get<ShopPayArgs>(EXTRA_ARGS)
        }
    }
}
