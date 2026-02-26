package com.stripe.android.paymentmethodmessaging.element

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.stripe.android.ui.core.CircularProgressIndicator
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.elements.bottomsheet.StripeBottomSheetLayout
import com.stripe.android.uicore.elements.bottomsheet.StripeBottomSheetState
import com.stripe.android.uicore.elements.bottomsheet.rememberStripeBottomSheetLayoutInfo
import com.stripe.android.uicore.elements.bottomsheet.rememberStripeBottomSheetState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import com.stripe.android.uicore.R as UiCoreR

@OptIn(PaymentMethodMessagingElementPreview::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
internal class LearnMoreActivity : AppCompatActivity() {
    private val args: LearnMoreActivityArgs? by lazy {
        LearnMoreActivityArgs.fromIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val termsArgs = args
        if (termsArgs == null || termsArgs.learnMoreUrl.isBlank()) {
            finish()
            return
        }

        setContent {
            StripeTheme {
                val bottomSheetState = rememberStripeBottomSheetState()
                val layoutInfo = rememberStripeBottomSheetLayoutInfo()
                val scope = rememberCoroutineScope()
                StripeBottomSheetLayout(
                    state = bottomSheetState,
                    layoutInfo = layoutInfo,
                    modifier = Modifier,
                    onDismissed = { dismiss(scope, bottomSheetState) }
                ) {
                    Box(
                        Modifier
                            .background(getBackgroundColor(termsArgs.theme))
                            .animateContentSize(tween())
                    ) {
                        LearnMoreWebView(termsArgs.learnMoreUrl, termsArgs.theme) {
                            dismiss(scope, bottomSheetState)
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Composable
    private fun LearnMoreWebView(
        url: String,
        theme: PaymentMethodMessagingElement.Appearance.Theme,
        onDismiss: () -> Unit
    ) {
        val context = LocalContext.current
        val webView = remember { WebView(context) }
        var isLoading by remember { mutableStateOf(true) }
        val density = LocalDensity.current
        var contentHeight by remember { mutableStateOf(0.dp) }
        val configuration = LocalConfiguration.current
        val screenHeightDp = configuration.screenHeightDp.dp
        val minContentHeight = screenHeightDp / SCREEN_HEIGHT_DIVIDER
        val iconColor = if (theme == PaymentMethodMessagingElement.Appearance.Theme.DARK) {
            Color.White
        } else {
            Color.Black
        }

        LaunchedEffect(url) {
            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    isLoading = false
                }
            }
            webView.settings.javaScriptEnabled = true
            webView.loadUrl(url)
        }

        Box(
            Modifier
                .onGloballyPositioned {
                    contentHeight = with(density) { it.size.height.toDp() }
                }
                .defaultMinSize(minHeight = minContentHeight)
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.Start
                ) {
                    Icon(
                        painter = painterResource(UiCoreR.drawable.stripe_ic_material_close),
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier
                            .size(40.dp)
                            .padding(start = 8.dp, top = 12.dp)
                            .clickable {
                                onDismiss()
                            }
                            .testTag(CLOSE_BUTTON_TEST_TAG)
                    )
                }
                AndroidView(
                    factory = { webView },
                )
            }
        }
        if (isLoading) {
            LoadingIndicator(
                contentHeight = contentHeight,
                theme = theme
            )
        }
    }

    @Composable
    private fun LoadingIndicator(contentHeight: Dp, theme: PaymentMethodMessagingElement.Appearance.Theme) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .background(getBackgroundColor(theme))
                .requiredHeight(contentHeight)
                .fillMaxWidth()
        ) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 2.dp,
                modifier = Modifier.requiredSize(48.dp),
            )
        }
    }

    private fun dismiss(scope: CoroutineScope, state: StripeBottomSheetState) {
        scope.launch {
            state.hide()
            finish()
        }
    }

    private fun getBackgroundColor(theme: PaymentMethodMessagingElement.Appearance.Theme): Color {
        return when (theme) {
            PaymentMethodMessagingElement.Appearance.Theme.DARK -> darkBackgroundColor
            PaymentMethodMessagingElement.Appearance.Theme.FLAT -> flatBackgroundColor
            PaymentMethodMessagingElement.Appearance.Theme.LIGHT -> Color.White
        }
    }

    private companion object {
        val darkBackgroundColor = Color(0xFF30313D)
        val flatBackgroundColor = Color(0xFFF1F1F1)
        const val SCREEN_HEIGHT_DIVIDER = 3
        const val CLOSE_BUTTON_TEST_TAG = "close_button"
    }
}
