package com.stripe.example.activity

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.stripe.android.confirmPaymentIntent
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.payments.paymentlauncher.PaymentResult
import com.stripe.android.payments.paymentlauncher.rememberPaymentLauncher
import com.stripe.example.StripeFactory
import com.stripe.example.module.StripeIntentViewModel
import com.stripe.example.theme.DefaultExampleTheme
import kotlinx.coroutines.launch

class PaymentLauncherPlaygroundActivity : AppCompatActivity() {
    private val viewModel: StripeIntentViewModel by viewModels()
    private val stripe by lazy { StripeFactory(this).create() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DefaultExampleTheme {
                PaymentLauncherPlaygroundScreen()
            }
        }
    }

    @Composable
    private fun PaymentLauncherPlaygroundScreen() {
        var inProgress by rememberSaveable { mutableStateOf(false) }
        var resultText by rememberSaveable { mutableStateOf("No result") }

        val paymentLauncher = rememberPaymentLauncher { result ->
            resultText = result.displayText()
            inProgress = false
        }

        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Text(
                    "This creates a PaymentIntent, confirms it with test card " +
                        "$TEST_CARD_NUMBER, and opens its 3DS challenge. Cancel or back out " +
                        "of the challenge to inspect the PaymentLauncher callback."
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        inProgress = true
                        resultText = "Creating PaymentIntent"
                        createPaymentIntentRequiringAction(
                            onReady = { clientSecret ->
                                resultText = "Waiting for PaymentLauncher result"
                                paymentLauncher.handleNextActionForPaymentIntent(
                                    clientSecret = clientSecret,
                                )
                            },
                            onFailure = { error ->
                                resultText = "Failed to prepare PaymentIntent: ${error.message}"
                                inProgress = false
                            }
                        )
                    },
                    enabled = !inProgress,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Create PaymentIntent and launch 3DS")
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("Callback result: $resultText")
            }
        }
    }

    private fun createPaymentIntentRequiringAction(
        onReady: (String) -> Unit,
        onFailure: (Throwable) -> Unit,
    ) {
        viewModel.createPaymentIntent(country = "us").observe(this) { createResult ->
            createResult.fold(
                onSuccess = { response ->
                    val clientSecret = response.getString("secret")
                    lifecycleScope.launch {
                        runCatching {
                            stripe.confirmPaymentIntent(
                                ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(
                                    paymentMethodCreateParams = PAYMENT_METHOD_CREATE_PARAMS,
                                    clientSecret = clientSecret,
                                )
                            )
                        }.fold(
                            onSuccess = { onReady(clientSecret) },
                            onFailure = onFailure,
                        )
                    }
                },
                onFailure = onFailure,
            )
        }
    }

    private fun PaymentResult.displayText(): String {
        return when (this) {
            PaymentResult.Completed -> "PaymentResult.Completed"
            PaymentResult.Canceled -> "PaymentResult.Canceled"
            is PaymentResult.Failed -> "PaymentResult.Failed: ${throwable.message}"
        }
    }

    private companion object {
        private const val TEST_CARD_NUMBER = "4000582600000094"

        private val PAYMENT_METHOD_CREATE_PARAMS = PaymentMethodCreateParams.create(
            PaymentMethodCreateParams.Card.Builder()
                .setNumber(TEST_CARD_NUMBER)
                .setExpiryMonth(1)
                .setExpiryYear(2045)
                .setCvc("123")
                .build()
        )
    }
}
