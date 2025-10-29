@file:OptIn(PaymentMethodMessagingElementPreview::class)

package com.stripe.android.paymentmethodmessaging.element

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.os.BundleCompat
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.elements.bottomsheet.StripeBottomSheetLayout
import com.stripe.android.uicore.elements.bottomsheet.rememberStripeBottomSheetLayoutInfo
import com.stripe.android.uicore.elements.bottomsheet.rememberStripeBottomSheetState
import kotlinx.parcelize.Parcelize

internal class LearnMoreActivity : AppCompatActivity() {
    private val args: LearnMoreActivityArgs? by lazy {
        LearnMoreActivityArgs.fromIntent(intent)
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val termsArgs = args
        if (termsArgs == null) {
            finish()
            return
        }

        setContent {
            StripeTheme {
                val bottomSheetState = rememberStripeBottomSheetState()
                val layoutInfo = rememberStripeBottomSheetLayoutInfo()
                StripeBottomSheetLayout(
                    state = bottomSheetState,
                    layoutInfo = layoutInfo,
                    modifier = Modifier,
                    onDismissed = {
                        finish()
                    }
                ) {
                    AndroidView(
                        factory = { context ->
                            WebView(context).apply {
                                webViewClient = WebViewClient()
                                settings.javaScriptEnabled = true
                                loadUrl(termsArgs.learnMoreUrl + termsArgs.theme.toQueryParam())
                            }
                        })
                }
            }
        }
    }

    private fun PaymentMethodMessagingElement.Appearance.Theme.toQueryParam(): String {
        return when (this) {
            PaymentMethodMessagingElement.Appearance.Theme.LIGHT -> "&theme=stripe"
            PaymentMethodMessagingElement.Appearance.Theme.DARK -> "&theme=night"
            PaymentMethodMessagingElement.Appearance.Theme.FLAT -> "&theme=flat"
        }
    }
}

@Parcelize
internal data class LearnMoreActivityArgs(
    val learnMoreUrl: String,
    val theme: PaymentMethodMessagingElement.Appearance.Theme
) : Parcelable {
    companion object {
        private const val LEARN_MORE_ARGS: String = "learn_more_args"
        fun createIntent(context: Context, args: LearnMoreActivityArgs): Intent {
            return Intent(context, LearnMoreActivity::class.java)
                .putExtra(LEARN_MORE_ARGS, args)
        }
        fun fromIntent(intent: Intent): LearnMoreActivityArgs? {
            return intent.extras?.let { bundle ->
                BundleCompat.getParcelable(bundle, LEARN_MORE_ARGS, LearnMoreActivityArgs::class.java)
            }
        }
    }
}