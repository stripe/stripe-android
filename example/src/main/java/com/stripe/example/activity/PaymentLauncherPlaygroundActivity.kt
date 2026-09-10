package com.stripe.example.activity

import android.os.Bundle
import androidx.activity.compose.setContent
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
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.payments.paymentlauncher.PaymentResult
import com.stripe.android.payments.paymentlauncher.rememberPaymentLauncher
import com.stripe.example.theme.DefaultExampleTheme

class PaymentLauncherPlaygroundActivity : AppCompatActivity() {
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
        var clientSecret by rememberSaveable { mutableStateOf("") }
        var resultText by rememberSaveable { mutableStateOf("No result") }

        val paymentLauncher = rememberPaymentLauncher { result ->
            resultText = result.displayText()
        }

        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Text(
                    "Paste the client secret for a PaymentIntent that requires a next action, " +
                        "then cancel or back out of the 3DS challenge."
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = clientSecret,
                    onValueChange = { clientSecret = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("PaymentIntent client secret") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        resultText = "Waiting for result"
                        paymentLauncher.handleNextActionForPaymentIntent(
                            clientSecret = clientSecret,
                        )
                    },
                    enabled = clientSecret.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Handle next action")
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("Callback result: $resultText")
            }
        }
    }

    private fun PaymentResult.displayText(): String {
        return when (this) {
            PaymentResult.Completed -> "PaymentResult.Completed"
            PaymentResult.Canceled -> "PaymentResult.Canceled"
            is PaymentResult.Failed -> "PaymentResult.Failed: ${throwable.message}"
        }
    }
}
