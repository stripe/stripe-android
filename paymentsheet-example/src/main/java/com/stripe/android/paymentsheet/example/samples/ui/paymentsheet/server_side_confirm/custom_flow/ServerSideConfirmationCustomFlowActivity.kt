package com.stripe.android.paymentsheet.example.samples.ui.paymentsheet.server_side_confirm.custom_flow

import android.graphics.Color
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.google.android.material.snackbar.Snackbar
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.R
import com.stripe.android.paymentsheet.example.samples.model.toIntentConfiguration
import com.stripe.android.paymentsheet.example.samples.ui.paymentsheet.server_side_confirm.custom_flow.ServerSideConfirmationCustomFlowViewModel.ConfigureResult
import com.stripe.android.paymentsheet.example.samples.ui.shared.BuyButton
import com.stripe.android.paymentsheet.example.samples.ui.shared.CompletedPaymentAlertDialog
import com.stripe.android.paymentsheet.example.samples.ui.shared.ErrorScreen
import com.stripe.android.paymentsheet.example.samples.ui.shared.PaymentMethodSelector
import com.stripe.android.paymentsheet.example.samples.ui.shared.PaymentSheetExampleTheme
import com.stripe.android.paymentsheet.example.samples.ui.shared.Receipt
import com.stripe.android.paymentsheet.example.samples.ui.shared.SubscriptionToggle
import com.stripe.android.paymentsheet.rememberPaymentSheetFlowController
import kotlinx.coroutines.CompletableDeferred

internal class ServerSideConfirmationCustomFlowActivity : AppCompatActivity() {

    private val snackbar by lazy {
        Snackbar.make(findViewById(android.R.id.content), "", Snackbar.LENGTH_SHORT)
            .setBackgroundTint(Color.BLACK)
            .setTextColor(Color.WHITE)
    }

    private val viewModel by viewModels<ServerSideConfirmationCustomFlowViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            PaymentSheetExampleTheme {
                val flowController = rememberPaymentSheetFlowController(
                    createIntentCallback = viewModel::createAndConfirmIntent,
                    paymentOptionCallback = viewModel::handlePaymentOptionChanged,
                    paymentResultCallback = viewModel::handlePaymentSheetResult,
                )

                val uiState by viewModel.state.collectAsState()
                val paymentMethodLabel = determinePaymentMethodLabel(uiState)

                AttachFlowControllerToViewModel(flowController, uiState)

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
                        PaymentMethodSelector(
                            isEnabled = uiState.isPaymentMethodButtonEnabled,
                            paymentMethodLabel = paymentMethodLabel,
                            paymentMethodPainter = uiState.paymentOption?.iconPainter,
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
    fun AttachFlowControllerToViewModel(
        flowController: PaymentSheet.FlowController,
        uiState: ServerSideConfirmationCustomFlowViewState,
    ) {
        DisposableEffect(Unit) {
            viewModel.registerFlowControllerConfigureHandler { cartState ->
                val completable = CompletableDeferred<Throwable?>()
                flowController.configureWithIntentConfiguration(
                    intentConfiguration = cartState.toIntentConfiguration(),
                    configuration = uiState.paymentSheetConfig,
                    callback = { _, error -> completable.complete(error) },
                )
                val error = completable.await()
                val paymentOption = flowController.getPaymentOption()
                ConfigureResult(paymentOption, error)
            }

            onDispose {
                viewModel.unregisterFlowControllerConfigureHandler()
            }
        }
    }
}

@Composable
private fun determinePaymentMethodLabel(uiState: ServerSideConfirmationCustomFlowViewState): String {
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
