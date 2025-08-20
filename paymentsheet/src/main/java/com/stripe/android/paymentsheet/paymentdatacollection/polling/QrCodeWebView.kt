package com.stripe.android.paymentsheet.paymentdatacollection.polling

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidViewBinding
import androidx.core.view.isGone
import com.stripe.android.BuildConfig
import com.stripe.android.core.Logger
import com.stripe.android.databinding.StripePaymentAuthWebViewActivityBinding
import com.stripe.android.uicore.utils.collectAsState
import com.stripe.android.view.PaymentAuthWebViewClient
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
fun QrCodeWebView(
    url: String,
    clientSecret: String,
    onClose: () -> Unit,
) {
    val isPageLoaded = MutableStateFlow(false)
    val isPageLoadedState by isPageLoaded.collectAsState()
    var binding : StripePaymentAuthWebViewActivityBinding? = null
    
    AndroidViewBinding(
        factory = { layoutInflater, _, _ ->
            val localBinding = StripePaymentAuthWebViewActivityBinding.inflate(layoutInflater)

            localBinding.toolbar.setNavigationIcon(
                com.stripe.android.paymentsheet.R.drawable.stripe_ic_paymentsheet_close
            )
            localBinding.toolbar.setNavigationOnClickListener { onClose() }

            val webViewClient = PaymentAuthWebViewClient(
                logger = Logger.getInstance(BuildConfig.DEBUG),
                isPageLoaded = isPageLoaded,
                clientSecret = clientSecret,
                returnUrl = null,
                activityStarter = {},
                activityFinisher = { _ -> onClose() }
            )

            localBinding.webView.webViewClient = webViewClient
            localBinding.webView.loadUrl(url)

            binding = localBinding
            localBinding
        },
        modifier = Modifier.fillMaxSize(),
        update = {
            if (isPageLoadedState) {
                binding?.progressBar?.isGone = true
            }
        })
}
