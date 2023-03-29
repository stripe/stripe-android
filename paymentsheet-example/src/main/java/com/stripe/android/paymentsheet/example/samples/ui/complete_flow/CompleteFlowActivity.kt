package com.stripe.android.paymentsheet.example.samples.ui.complete_flow

import android.graphics.Color
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.google.android.material.snackbar.Snackbar
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.samples.ui.BuyButton
import com.stripe.android.paymentsheet.example.samples.ui.Receipt

internal class CompleteFlowActivity : AppCompatActivity() {

    private val snackbar by lazy {
        Snackbar.make(findViewById(android.R.id.content), "", Snackbar.LENGTH_SHORT)
            .setBackgroundTint(Color.BLACK)
            .setTextColor(Color.WHITE)
    }

    private val viewModel by viewModels<CompleteFlowViewModel>()

    private lateinit var paymentSheet: PaymentSheet

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        paymentSheet = PaymentSheet(
            activity = this,
            callback = viewModel::handlePaymentSheetResult,
        )

        setContent {
            MaterialTheme {
                val uiState by viewModel.state.collectAsState()

                uiState.paymentInfo?.let { paymentInfo ->
                    LaunchedEffect(paymentInfo) {
                        presentPaymentSheet(paymentInfo)
                    }
                }

                uiState.status?.let {
                    LaunchedEffect(it) {
                        snackbar.setText(it).show()
                        viewModel.statusDisplayed()
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

    private fun presentPaymentSheet(paymentInfo: CompleteFlowViewState.PaymentInfo) {
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
