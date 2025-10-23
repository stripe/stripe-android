package com.stripe.android.paymentmethodmessaging.view.messagingelement

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.os.BundleCompat
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.elements.bottomsheet.StripeBottomSheetLayout
import com.stripe.android.uicore.elements.bottomsheet.rememberStripeBottomSheetLayoutInfo
import com.stripe.android.uicore.elements.bottomsheet.rememberStripeBottomSheetState
import kotlinx.parcelize.Parcelize

internal class TermsActivity : AppCompatActivity() {
    private val args: TermsActivityArgs? by lazy {
        TermsActivityArgs.fromIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val termsArgs = args
        if (termsArgs == null) {
            finish()
            return
        }

        renderE

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
                    AndroidView(factory = { context ->
                        WebView(context).apply {
                            webViewClient = WebViewClient() // Ensure links open inside the WebView
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
internal data class TermsActivityArgs(
    val learnMoreUrl: String,
    val theme: PaymentMethodMessagingElement.Appearance.Theme
) : Parcelable {
    companion object {
        internal const val TERMS_ARGS: String = "terms_args"
        fun createIntent(context: Context, args: TermsActivityArgs): Intent {
            return Intent(context, TermsActivity::class.java)
                .putExtra(TERMS_ARGS, args)
        }
        fun fromIntent(intent: Intent): TermsActivityArgs? {
            return intent.extras?.let { bundle ->
                BundleCompat.getParcelable(bundle, TERMS_ARGS, TermsActivityArgs::class.java)
            }
        }
    }
}