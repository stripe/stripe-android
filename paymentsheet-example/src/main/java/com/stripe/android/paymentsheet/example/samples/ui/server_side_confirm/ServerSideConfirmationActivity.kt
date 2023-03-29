package com.stripe.android.paymentsheet.example.samples.ui.server_side_confirm

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
import com.stripe.android.ExperimentalPaymentSheetDecouplingApi
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.R
import com.stripe.android.paymentsheet.example.samples.ui.BuyButton
import com.stripe.android.paymentsheet.example.samples.ui.PaymentMethodSelector
import com.stripe.android.paymentsheet.example.samples.ui.Receipt
import com.stripe.android.paymentsheet.example.samples.ui.SubscriptionToggle
import com.stripe.android.paymentsheet.example.samples.ui.shared.ErrorScreen
import com.stripe.android.paymentsheet.example.samples.ui.shared.PaymentSheetExampleTheme
import kotlinx.coroutines.CompletableDeferred

internal class ServerSideConfirmationActivity : AppCompatActivity() {

    private val snackbar by lazy {
        Snackbar.make(findViewById(android.R.id.content), "", Snackbar.LENGTH_SHORT)
            .setBackgroundTint(Color.BLACK)
            .setTextColor(Color.WHITE)
    }

    private val viewModel by viewModels<ServerSideConfirmationViewModel>()

    private lateinit var flowController: PaymentSheet.FlowController
    private lateinit var paymentSheet: PaymentSheet

    @OptIn(ExperimentalPaymentSheetDecouplingApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Hints
        val activity = this
        val paymentOptionCallback = viewModel::handlePaymentOptionChanged
        val paymentResultCallback = viewModel::handlePaymentSheetResult
        val createAndConfirmIntent: suspend (String, Boolean) -> Unit = { paymentMethodId, shouldSavePM ->
            viewModel.createAndConfirmIntent(paymentMethodId, shouldSavePM)
        }

        // TODO: create FlowController
        // flowController = FlowController.create(...)

        // or TODO: create PaymentSheet
        // paymentSheet = PaymentSheet(...)

        setContent {
            PaymentSheetExampleTheme {
                val uiState by viewModel.state.collectAsState()
                val paymentMethodLabel = determinePaymentMethodLabel(uiState)

                AttachFlowControllerToViewModel(uiState)

                uiState.status?.let {
                    LaunchedEffect(it) {
                        snackbar.setText(it).show()
                        viewModel.statusDisplayed()
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
                            paymentMethodIcon = uiState.paymentOption?.icon(),
                            onClick = {
                                // TODO present payment options
                                flowController::presentPaymentOptions

                                // or TODO present PaymentSheet
                                // paymentSheet.presentWithIntentConfiguration(...)
                            },
                        )

                        SubscriptionToggle(
                            checked = uiState.cartState.isSubscription,
                            onCheckedChange = viewModel::updateSubscription,
                        )

                        BuyButton(
                            buyButtonEnabled = uiState.isBuyButtonEnabled,
                            onClick = {
                                viewModel.handleBuyButtonPressed()

                                // TODO no code, if using PaymentSheet, don't press buy button
                                flowController.confirm()
                            }
                        )
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalPaymentSheetDecouplingApi::class)
    @Composable
    fun AttachFlowControllerToViewModel(
        uiState: ServerSideConfirmationViewState,
    ) {
        DisposableEffect(Unit) {
            viewModel.registerFlowControllerConfigureHandler { cartState ->
                val completable = CompletableDeferred<Throwable?>()

                // Hints
                val amount = cartState.total ?: 0L
                val currency = "usd"
                val shouldSetupFutureUse = cartState.isSubscription
                val configuration = uiState.paymentSheetConfig
                val callback: (Boolean, Throwable) -> Unit = { _, error -> completable.complete(error) }

                // TODO configure FlowController
                // flowController.configureWithIntentConfiguration(...)

                completable.await()
            }

            onDispose {
                viewModel.unregisterFlowControllerConfigureHandler()
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
