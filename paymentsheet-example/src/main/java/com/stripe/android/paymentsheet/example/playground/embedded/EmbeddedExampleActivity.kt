@file:OptIn(ExperimentalEmbeddedPaymentElementApi::class)

package com.stripe.android.paymentsheet.example.playground.embedded

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentelement.rememberEmbeddedPaymentElement
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.R

internal class EmbeddedExampleActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.title = getString(R.string.embedded_example_title)
        PaymentConfiguration.init(this, publishableKey = "pk_test_TYooMQauvdEDq54NiTphI7jx")

        setContent {
            CheckoutScreen()
        }
    }
}

@Composable
fun CheckoutScreen() {
    val embeddedBuilder = remember {
        EmbeddedPaymentElement.Builder(
            createIntentCallback = { _, _ ->
                TODO("Make a request to your own server and receive a client secret or an error.")
            },
            resultCallback = { result ->
                when (result) {
                    is EmbeddedPaymentElement.Result.Completed -> {
                        // Payment completed - show a confirmation screen.
                    }
                    is EmbeddedPaymentElement.Result.Failed -> {
                        // Encountered an unrecoverable error. You can display the error to the user, log it, etc.
                    }
                    is EmbeddedPaymentElement.Result.Canceled -> {
                        // Customer canceled - you should probably do nothing.
                    }
                }
            },
        )
    }

    val embeddedPaymentElement = rememberEmbeddedPaymentElement(embeddedBuilder)

    LaunchedEffect(embeddedPaymentElement) {
        embeddedPaymentElement.configure(
            intentConfiguration = PaymentSheet.IntentConfiguration(
                mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                    amount = 1099,
                    currency = "USD",
                ),
                // Optional intent configuration options...
            ),
            configuration = EmbeddedPaymentElement.Configuration.Builder("Powdur").build()
        )
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        embeddedPaymentElement.Content()
        Button(
            onClick = {
                embeddedPaymentElement.confirm()
            }
        ) {
            Text("Confirm payment")
        }
    }
}
