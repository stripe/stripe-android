package com.stripe.android.paymentsheet.example.samples.ui.paymentsheet.flow_controller_with_intent_configuration

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
import androidx.compose.ui.platform.LocalResources
import com.google.android.material.snackbar.Snackbar
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.R
import com.stripe.android.paymentsheet.example.samples.model.toIntentConfiguration
import com.stripe.android.paymentsheet.example.samples.ui.shared.BuyButton
import com.stripe.android.paymentsheet.example.samples.ui.shared.CompletedPaymentAlertDialog
import com.stripe.android.paymentsheet.example.samples.ui.shared.ErrorScreen
import com.stripe.android.paymentsheet.example.samples.ui.shared.PaymentMethodSelector
import com.stripe.android.paymentsheet.example.samples.ui.shared.PaymentSheetExampleTheme
import com.stripe.android.paymentsheet.example.samples.ui.shared.Receipt

internal class FlowControllerIntentConfigActivity : AppCompatActivity() {

    private val snackbar by lazy {
        Snackbar.make(findViewById(android.R.id.content), "", Snackbar.LENGTH_SHORT)
            .setBackgroundTint(Color.BLACK)
            .setTextColor(Color.WHITE)
    }

    private val viewModel by viewModels<FlowControllerIntentConfigViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            PaymentSheetExampleTheme {
                FlowControllerIntentConfigScreen(
                    viewModel = viewModel,
                    snackbar = snackbar,
                    onFinish = ::finish,
                )
            }
        }
    }
}

@Composable
private fun FlowControllerIntentConfigScreen(
    viewModel: FlowControllerIntentConfigViewModel,
    snackbar: Snackbar,
    onFinish: () -> Unit,
) {
    val flowController = remember {
        PaymentSheet.FlowController.Builder(
            resultCallback = viewModel::handlePaymentSheetResult,
            paymentOptionCallback = viewModel::handlePaymentOptionChanged,
        ).createIntentCallback(viewModel::createAndConfirmIntent)
    }.build()

    val uiState by viewModel.state.collectAsState()
    val paymentMethodLabel = determinePaymentMethodLabel(uiState)

    if (uiState.cartState.total != null) {
        LaunchedEffect(uiState.cartState) {
            flowController.configureWithIntentConfiguration(
                intentConfiguration = uiState.cartState.toIntentConfiguration(),
                configuration = uiState.paymentSheetConfig,
                callback = viewModel::handleFlowControllerConfigured,
            )
        }
    }

    uiState.status?.let { status ->
        if (uiState.didComplete) {
            CompletedPaymentAlertDialog(onDismiss = onFinish)
        } else {
            LaunchedEffect(status) {
                snackbar.setText(status).show()
                viewModel.statusDisplayed()
            }
        }
    }

    FlowControllerIntentConfigContent(
        uiState = uiState,
        paymentMethodLabel = paymentMethodLabel,
        flowController = flowController,
        viewModel = viewModel,
    )
}

@Composable
private fun FlowControllerIntentConfigContent(
    uiState: FlowControllerIntentConfigViewState,
    paymentMethodLabel: String,
    flowController: PaymentSheet.FlowController,
    viewModel: FlowControllerIntentConfigViewModel,
) {
    Box(
        modifier = Modifier.padding(
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

@Composable
private fun determinePaymentMethodLabel(uiState: FlowControllerIntentConfigViewState): String {
    val resources = LocalResources.current
    return remember(uiState) {
        if (uiState.paymentOption?.label != null) {
            uiState.paymentOption.label
        } else if (!uiState.isProcessing) {
            resources.getString(R.string.select)
        } else {
            resources.getString(R.string.loading)
        }
    }
}
