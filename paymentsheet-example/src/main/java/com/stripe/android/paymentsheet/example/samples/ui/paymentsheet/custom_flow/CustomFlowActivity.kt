package com.stripe.android.paymentsheet.example.samples.ui.paymentsheet.custom_flow

import android.graphics.Color
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.google.android.material.snackbar.Snackbar
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.R
import com.stripe.android.paymentsheet.example.samples.ui.shared.BuyButton
import com.stripe.android.paymentsheet.example.samples.ui.shared.CompletedPaymentAlertDialog
import com.stripe.android.paymentsheet.example.samples.ui.shared.ErrorScreen
import com.stripe.android.paymentsheet.example.samples.ui.shared.PaymentMethodSelector
import com.stripe.android.paymentsheet.example.samples.ui.shared.PaymentSheetExampleTheme
import com.stripe.android.paymentsheet.example.samples.ui.shared.Receipt
import com.stripe.android.paymentsheet.rememberPaymentSheetFlowController

internal class CustomFlowActivity : AppCompatActivity() {

    private val snackbar by lazy {
        Snackbar.make(findViewById(android.R.id.content), "", Snackbar.LENGTH_SHORT)
            .setBackgroundTint(Color.BLACK)
            .setTextColor(Color.WHITE)
    }

    private val viewModel by viewModels<CustomFlowViewModel>()

    @SuppressWarnings("LongMethod")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val flowController = rememberPaymentSheetFlowController(
                paymentOptionCallback = viewModel::handlePaymentOptionChanged,
                paymentResultCallback = viewModel::handlePaymentSheetResult,
            )

            PaymentSheetExampleTheme {
                val uiState by viewModel.state.collectAsState()
                val paymentMethodLabel = determinePaymentMethodLabel(uiState)

                uiState.paymentInfo?.let { paymentInfo ->
                    LaunchedEffect(paymentInfo) {
                        configureFlowController(flowController, paymentInfo)
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

                Box(
                    modifier = Modifier
                        .padding(
                            paddingValues = WindowInsets.systemBars.only(
                                WindowInsetsSides.Horizontal + WindowInsetsSides.Top
                            ).asPaddingValues()
                        ),
                ) {
                    if (uiState.isError) {
                        ErrorScreen(onRetry = viewModel::retry)
                    } else {
                        Receipt(
                            isLoading = uiState.isProcessing,
                            cartState = uiState.cartState,
                        ) {
                            PaymentMethodSelector(
                                isEnabled = uiState.isPaymentMethodButtonEnabled,
                                paymentMethodLabel = paymentMethodLabel,
                                paymentMethodPainter = uiState.paymentOption?.iconPainter,
                                onClick = flowController::presentPaymentOptions,
                            )
                            BuyButton(
                                buyButtonEnabled = uiState.isBuyButtonEnabled,
                                onClick = {
                                    viewModel.handleBuyButtonPressed()
                                    flowController.confirm()
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun configureFlowController(
        flowController: PaymentSheet.FlowController,
        paymentInfo: CustomFlowViewState.PaymentInfo,
    ) {
        flowController.configureWithPaymentIntent(
            paymentIntentClientSecret = paymentInfo.clientSecret,
            configuration = paymentInfo.paymentSheetConfig,
            callback = viewModel::handleFlowControllerConfigured,
        )
    }
}

@Composable
private fun determinePaymentMethodLabel(uiState: CustomFlowViewState): String {
    val context = LocalContext.current
    return remember(uiState) {
        if (uiState.paymentOption?.label != null) {
            uiState.paymentOption.label
        } else if (!uiState.isProcessing) {
            context.getString(R.string.select)
        } else {
            context.getString(R.string.loading)
        }
    }
}
