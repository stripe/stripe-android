package com.stripe.android.paymentsheet.example.samples.ui.paymentsheet.server_side_confirm.complete_flow

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.google.android.material.snackbar.Snackbar
import com.stripe.android.paymentsheet.example.samples.model.toIntentConfiguration
import com.stripe.android.paymentsheet.example.samples.ui.shared.BuyButton
import com.stripe.android.paymentsheet.example.samples.ui.shared.CompletedPaymentAlertDialog
import com.stripe.android.paymentsheet.example.samples.ui.shared.ErrorScreen
import com.stripe.android.paymentsheet.example.samples.ui.shared.PaymentSheetExampleTheme
import com.stripe.android.paymentsheet.example.samples.ui.shared.Receipt
import com.stripe.android.paymentsheet.example.samples.ui.shared.SubscriptionToggle
import com.stripe.android.paymentsheet.rememberPaymentSheet

internal class ServerSideConfirmationCompleteFlowActivity : AppCompatActivity() {

    private val snackbar by lazy {
        Snackbar.make(findViewById(android.R.id.content), "", Snackbar.LENGTH_SHORT)
            .setBackgroundTint(Color.BLACK)
            .setTextColor(Color.WHITE)
    }

    private val viewModel by viewModels<ServerSideConfirmationCompleteFlowViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            PaymentSheetExampleTheme {
                val paymentSheet = rememberPaymentSheet(
                    createIntentCallback = viewModel::createAndConfirmIntent,
                    paymentResultCallback = viewModel::handlePaymentSheetResult,
                )

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
}
