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
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.samples.ui.BuyButton
import com.stripe.android.paymentsheet.example.samples.ui.Receipt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

                uiState.status?.let {
                    LaunchedEffect(it) {
                        snackbar.setText(it).show()
                        viewModel.statusDisplayed()
                    }
                }

                Receipt(
                    isLoading = uiState.isProcessing,
                    cartState = uiState.cartState,
                    isEditable = false,
                ) {
                    BuyButton(
                        buyButtonEnabled = !uiState.isProcessing,
                        onClick = ::handleBuyButtonClick,
                    )
                }
            }
        }
    }

    private fun handleBuyButtonClick() {
        lifecycleScope.launch {
            val result = viewModel.prepareCheckout()

            if (result != null) {
                initializePaymentConfig(result.publishableKey)
                presentPaymentSheet(result.customerConfig, result.clientSecret)
            }
        }
    }

    private suspend fun initializePaymentConfig(
        publishableKey: String,
    ) = withContext(Dispatchers.IO) {
        PaymentConfiguration.init(
            context = this@CompleteFlowActivity,
            publishableKey = publishableKey,
        )
    }

    private fun presentPaymentSheet(
        customerConfig: PaymentSheet.CustomerConfiguration?,
        clientSecret: String,
    ) {
        paymentSheet.presentWithPaymentIntent(
            clientSecret,
            PaymentSheet.Configuration(
                merchantDisplayName = "Example, Inc.",
                customer = customerConfig,
                googlePay = PaymentSheet.GooglePayConfiguration(
                    environment = PaymentSheet.GooglePayConfiguration.Environment.Test,
                    countryCode = "US"
                ),
                // Set `allowsDelayedPaymentMethods` to true if your
                // business can handle payment methods that complete payment
                // after a delay, like SEPA Debit and Sofort.
                allowsDelayedPaymentMethods = true,
            )
        )
    }
}
