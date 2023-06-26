package com.stripe.android.paymentsheet.example.samples.ui.paymentsheet.complete_flow

import android.graphics.Color
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.google.android.material.snackbar.Snackbar
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.samples.ui.shared.BuyButton
import com.stripe.android.paymentsheet.example.samples.ui.shared.CompletedPaymentAlertDialog
import com.stripe.android.paymentsheet.example.samples.ui.shared.PaymentSheetExampleTheme
import com.stripe.android.paymentsheet.example.samples.ui.shared.Receipt
import com.stripe.android.paymentsheet.rememberPaymentSheet

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
                    isLoading = uiState.isProcessing,
                    cartState = uiState.cartState,
                ) {
                    BuyButton(
                        buyButtonEnabled = !uiState.isProcessing,
                        onClick = viewModel::checkout,
                    )
                }
            }
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
