package com.stripe.android.paymentsheet.example.samples.ui.paymentsheet.complete_flow

import android.graphics.Color
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.google.android.material.snackbar.Snackbar
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.samples.ui.shared.BuyButton
import com.stripe.android.paymentsheet.example.samples.ui.shared.CompletedPaymentAlertDialog
import com.stripe.android.paymentsheet.example.samples.ui.shared.PaymentSheetExampleTheme
import com.stripe.android.paymentsheet.example.samples.ui.shared.Receipt
import com.stripe.android.paymentsheet.rememberPaymentSheet
import com.stripe.android.uicore.image.StripeImage
import com.stripe.android.uicore.image.StripeImageLoader

internal class CompleteFlowActivity : AppCompatActivity() {

    private val snackbar by lazy {
        Snackbar.make(findViewById(android.R.id.content), "", Snackbar.LENGTH_SHORT)
            .setBackgroundTint(Color.BLACK)
            .setTextColor(Color.WHITE)
    }

    private val viewModel by viewModels<CompleteFlowViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val paymentSheet = rememberPaymentSheet(
                paymentResultCallback = viewModel::handlePaymentSheetResult,
            )

            PaymentSheetExampleTheme {
                val uiState by viewModel.state.collectAsState()

                uiState.paymentInfo?.let { paymentInfo ->
                    LaunchedEffect(paymentInfo) {
                        presentPaymentSheet(paymentSheet, paymentInfo)
                    }
                }

                uiState.status?.let { status ->
                    if (uiState.didComplete) {
                        CompletedPaymentAlertDialog(
                            onDismiss = ::finish
                        )
                    } else {
                        LaunchedEffect(status) {
                            snackbar.setText(status).show()
                            viewModel.statusDisplayed()
                        }
                    }
                }

                Receipt(
                    modifier = Modifier
                        .padding(
                            paddingValues = WindowInsets.systemBars.only(
                                WindowInsetsSides.Horizontal + WindowInsetsSides.Top
                            ).asPaddingValues()
                        ),
                    isLoading = uiState.isProcessing,
                    cartState = uiState.cartState,
                ) {

                    TextWithLogo("logo_here helps you pay")
                    TextWithLogo("Pay with logo_here")
                    TextWithLogo("Use logo_here to pay")

                    BuyButton(
                        buyButtonEnabled = !uiState.isProcessing,
                        onClick = viewModel::checkout,
                    )
                }
            }
        }
    }

    @Composable
    fun TextWithLogo(label: String) {
        val context = LocalContext.current
        val imageLoader = remember {
            StripeImageLoader(context.applicationContext)
        }
        val style = TextStyle(
            fontSize = 16.sp
        )
        Text(
            text = label.buildLogoAnnotatedString(),
            style = style,
            inlineContent = mapOf(
                "logo_here" to InlineTextContent(
                    placeholder = Placeholder(
                        width = style.fontSize,
                        height = style.fontSize,
                        placeholderVerticalAlign = PlaceholderVerticalAlign.Center
                    )
                ) {
                    StripeImage(
                        url = "https://js.stripe.com/v3/fingerprinted/img/payment-methods/icon-pm-klarna@3x-cbd108f6432733bea9ef16827d10f5c5.png",
                        imageLoader = imageLoader,
                        contentDescription = "",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            )
        )
    }

    @Composable
    fun String.buildLogoAnnotatedString(): AnnotatedString = buildAnnotatedString {
        val parts = split("logo_here")
        val preLogoString = parts.getOrNull(0)
        val postLogoString = parts.getOrNull(1)
        if (preLogoString == null || postLogoString == null) {
            // logo_here not found, just show label
            append(this@buildLogoAnnotatedString)
        } else {
            append(preLogoString)
            appendInlineContent(id = "logo_here")
            append(postLogoString)
        }
    }

    private fun presentPaymentSheet(
        paymentSheet: PaymentSheet,
        paymentInfo: CompleteFlowViewState.PaymentInfo,
    ) {
        if (!paymentInfo.shouldPresent) {
            return
        }

        paymentSheet.presentWithPaymentIntent(
            paymentIntentClientSecret = paymentInfo.clientSecret,
            configuration = paymentInfo.paymentSheetConfig,
        )

        viewModel.paymentSheetPresented()
    }
}
