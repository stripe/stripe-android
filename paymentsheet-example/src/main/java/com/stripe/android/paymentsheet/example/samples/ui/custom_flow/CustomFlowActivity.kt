package com.stripe.android.paymentsheet.example.samples.ui.custom_flow

import android.graphics.Color
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.google.android.material.snackbar.Snackbar
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.R
import com.stripe.android.paymentsheet.example.samples.ui.BuyButton
import com.stripe.android.paymentsheet.example.samples.ui.PaymentMethodSelector
import com.stripe.android.paymentsheet.example.samples.ui.Receipt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class CustomFlowActivity : AppCompatActivity() {

    private val snackbar by lazy {
        Snackbar.make(findViewById(android.R.id.content), "", Snackbar.LENGTH_SHORT)
            .setBackgroundTint(Color.BLACK)
            .setTextColor(Color.WHITE)
    }

    private val viewModel by viewModels<CustomFlowViewModel>()

    private lateinit var flowController: PaymentSheet.FlowController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        flowController = PaymentSheet.FlowController.create(
            activity = this,
            paymentOptionCallback = viewModel::handlePaymentOptionChanged,
            paymentResultCallback = viewModel::handlePaymentSheetResult,
        )

        setContent {
            MaterialTheme {
                val uiState by viewModel.state.collectAsState()

                val paymentMethodLabel = remember(uiState) {
                    if (uiState.paymentOptionLabel  != null) {
                        uiState.paymentOptionLabel!!
                    } else if (!uiState.isProcessing) {
                        getString(R.string.select)
                    } else {
                        getString(R.string.loading)
                    }
                }

                uiState.paymentInfo?.let { paymentInfo ->
                    LaunchedEffect(paymentInfo) {
                        initializePaymentConfiguration(paymentInfo)
                        configureFlowController(paymentInfo)
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
                    PaymentMethodSelector(
                        isEnabled = uiState.isPaymentMethodButtonEnabled,
                        paymentMethodLabel = paymentMethodLabel,
                        paymentMethodIcon = uiState.paymentOptionIcon,
                        onClick = {
                            flowController.presentPaymentOptions()
                        }
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

    private suspend fun initializePaymentConfiguration(
        paymentInfo: CustomFlowViewState.PaymentInfo,
    ) = withContext(Dispatchers.IO) {
        PaymentConfiguration.init(
            context = applicationContext,
            publishableKey = paymentInfo.publishableKey,
        )
    }

    private fun configureFlowController(paymentInfo: CustomFlowViewState.PaymentInfo) {
        flowController.configureWithPaymentIntent(
            paymentIntentClientSecret = paymentInfo.clientSecret,
            configuration = paymentInfo.paymentSheetConfig,
            callback = viewModel::handleFlowControllerConfigured,
        )
    }
}
