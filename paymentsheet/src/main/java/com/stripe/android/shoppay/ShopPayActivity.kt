package com.stripe.android.shoppay

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.os.bundleOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import com.stripe.android.common.ui.BottomSheetScaffold
import com.stripe.android.common.ui.ElementsBottomSheetLayout
import com.stripe.android.core.Logger
import com.stripe.android.paymentsheet.BuildConfig
import com.stripe.android.paymentsheet.R
import com.stripe.android.shoppay.webview.MainWebView
import com.stripe.android.ui.core.CircularProgressIndicator
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.elements.bottomsheet.rememberStripeBottomSheetState
import com.stripe.android.uicore.utils.collectAsState

internal class ShopPayActivity : ComponentActivity() {
    @VisibleForTesting
    internal var viewModelFactory: ViewModelProvider.Factory = ShopPayViewModel.factory()

    internal var viewModel: ShopPayViewModel? = null

    @SuppressWarnings("TooGenericExceptionCaught")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            viewModel = ViewModelProvider(this, viewModelFactory)[ShopPayViewModel::class.java]
        } catch (e: Throwable) {
            Logger.getInstance(BuildConfig.DEBUG).error("Failed to create ShopPayViewModel", e)
            dismissWithResult(ShopPayActivityResult.Failed(Throwable("Failed to create ShopPayViewModel")))
        }

        val vm = viewModel ?: return
        setContent {
            Content(vm)
        }
    }

    @Composable
    private fun Content(viewModel: ShopPayViewModel) {
        val bottomSheetState = rememberStripeBottomSheetState()

        LaunchedEffect(Unit) {
            viewModel.paymentResult.collect { result ->
                dismissWithResult(result)
            }
        }

        StripeTheme {
            ElementsBottomSheetLayout(
                state = bottomSheetState,
                onDismissed = {
                    dismissWithResult(ShopPayActivityResult.Canceled)
                }
            ) {
                BottomSheetScaffold(
                    modifier = Modifier
                        .fillMaxHeight(SHOP_PAY_SHEET_HEIGHT_RATIO),
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
                        ComposeWebView(viewModel)
                    }
                )
            }
        }
    }

    @Composable
    private fun ComposeWebView(viewModel: ShopPayViewModel) {
        val showPopup by viewModel.showPopup.collectAsState()

        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize()) {
                MainWebView(
                    viewModel = viewModel,
                )

                if (showPopup) {
                    PopupWebViewDialog(viewModel = viewModel)
                } else {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }

    @Composable
    private fun PopupWebViewDialog(viewModel: ShopPayViewModel) {
        val popupWebView by viewModel.popupWebView.collectAsState()

        popupWebView?.let { webView ->
            AndroidView(
                factory = {
                    webView.apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
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

        internal fun getArgs(savedStateHandle: SavedStateHandle): ShopPayArgs? {
            return savedStateHandle.get<ShopPayArgs>(EXTRA_ARGS)
        }
    }
}

private const val SHOP_PAY_SHEET_HEIGHT_RATIO = .75f
