package com.stripe.android.paymentsheet.example.samples.ui.server_side_confirm

import android.graphics.Color
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.google.android.material.snackbar.Snackbar
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.R
import com.stripe.android.paymentsheet.example.samples.model.toIntentConfiguration
import com.stripe.android.paymentsheet.example.samples.ui.BuyButton
import com.stripe.android.paymentsheet.example.samples.ui.PaymentMethodSelector
import com.stripe.android.paymentsheet.example.samples.ui.Receipt
import com.stripe.android.paymentsheet.example.samples.ui.SubscriptionToggle

internal class ServerSideConfirmationActivity : AppCompatActivity() {

    private val snackbar by lazy {
        Snackbar.make(findViewById(android.R.id.content), "", Snackbar.LENGTH_SHORT)
            .setBackgroundTint(Color.BLACK)
            .setTextColor(Color.WHITE)
    }

    private val viewModel by viewModels<ServerSideConfirmationViewModel>()

    private lateinit var flowController: PaymentSheet.FlowController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        flowController = PaymentSheet.FlowController.create(
            activity = this,
            paymentOptionCallback = viewModel::handlePaymentOptionChanged,
            createIntentCallbackForServerSideConfirmation = viewModel::createAndConfirmIntent,
            paymentResultCallback = viewModel::handlePaymentSheetResult,
        )

        setContent {
            MaterialTheme {
                val uiState by viewModel.state.collectAsState()
                val paymentMethodLabel = determinePaymentMethodLabel(uiState)

                uiState.status?.let {
                    LaunchedEffect(it) {
                        snackbar.setText(it).show()
                        viewModel.statusDisplayed()
                    }
                }

                LaunchedEffect(uiState.requiresFlowControllerConfigure) {
                    if (uiState.requiresFlowControllerConfigure) {
                        flowController.configureWithIntentConfiguration(
                            intentConfiguration = uiState.cartState.toIntentConfiguration(),
                            configuration = uiState.paymentSheetConfig,
                            callback = viewModel::handleFlowControllerConfigured,
                        )
                    }
                }

                Receipt(
                    isLoading = uiState.isProcessing,
                    cartState = uiState.cartState,
                    isEditable = true,
                    onQuantityChanged = viewModel::updateQuantity,
                ) {
                    PaymentMethodSelector(
                        isEnabled = uiState.isPaymentMethodButtonEnabled,
                        paymentMethodLabel = paymentMethodLabel,
                        paymentMethodIcon = uiState.paymentOption?.icon(),
                        onClick = flowController::presentPaymentOptions,
                    )

                    SubscriptionToggle(
                        checked = uiState.cartState.isSubscription,
                        onCheckedChange = viewModel::updateSubscription,
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

@Composable
private fun determinePaymentMethodLabel(uiState: ServerSideConfirmationViewState): String {
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
