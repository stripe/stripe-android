package com.stripe.android.paymentsheet.example.samples.ui.server_side_confirm.complete_flow

import android.graphics.Color
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.google.android.material.snackbar.Snackbar
import com.stripe.android.ExperimentalPaymentSheetDecouplingApi
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.samples.model.toIntentConfiguration
import com.stripe.android.paymentsheet.example.samples.ui.shared.BuyButton
import com.stripe.android.paymentsheet.example.samples.ui.shared.CompletedPaymentAlertDialog
import com.stripe.android.paymentsheet.example.samples.ui.shared.ErrorScreen
import com.stripe.android.paymentsheet.example.samples.ui.shared.PaymentSheetExampleTheme
import com.stripe.android.paymentsheet.example.samples.ui.shared.Receipt
import com.stripe.android.paymentsheet.example.samples.ui.shared.SubscriptionToggle

internal class ServerSideConfirmationCompleteFlowActivity : AppCompatActivity() {

    private val snackbar by lazy {
        Snackbar.make(findViewById(android.R.id.content), "", Snackbar.LENGTH_SHORT)
            .setBackgroundTint(Color.BLACK)
            .setTextColor(Color.WHITE)
    }

    private val viewModel by viewModels<ServerSideConfirmationCompleteFlowViewModel>()

    private lateinit var paymentSheet: PaymentSheet

    @OptIn(ExperimentalPaymentSheetDecouplingApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        paymentSheet = PaymentSheet(
            activity = this,
            createIntentCallback = viewModel::createAndConfirmIntent,
            paymentResultCallback = viewModel::handlePaymentSheetResult,
        )

        setContent {
            PaymentSheetExampleTheme {
                val uiState by viewModel.state.collectAsState()

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

                if (uiState.isError) {
                    ErrorScreen(onRetry = viewModel::retry)
                } else {
                    Receipt(
                        isLoading = uiState.isProcessing,
                        cartState = uiState.cartState,
                        isEditable = true,
                        onQuantityChanged = viewModel::updateQuantity,
                    ) {
                        SubscriptionToggle(
                            checked = uiState.cartState.isSubscription,
                            onCheckedChange = viewModel::updateSubscription,
                        )

                        BuyButton(
                            buyButtonEnabled = uiState.isBuyButtonEnabled,
                            onClick = {
                                paymentSheet.presentWithIntentConfiguration(
                                    intentConfiguration = uiState.cartState.toIntentConfiguration(),
                                    configuration = uiState.paymentSheetConfig,
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}
