package com.stripe.example.activity

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.accompanist.themeadapter.material.MdcTheme
import com.stripe.android.model.PaymentMethodCreateParams

class SwishExampleActivity : StripeIntentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val isProcessing by viewModel.inProgress.observeAsState(initial = false)
            val status by viewModel.status.observeAsState(initial = "")

            SwishScreen(
                isProcessing = isProcessing,
                status = status,
                onButtonPressed = { payWithSwish() },
            )
        }
    }

    private fun payWithSwish() {
        val params = PaymentMethodCreateParams.createSwish()

        createAndConfirmPaymentIntent(
            country = "FR",
            currency = "SEK",
            paymentMethodCreateParams = params,
            supportedPaymentMethods = "swish",
        )
    }
}

@Composable
private fun SwishScreen(
    isProcessing: Boolean,
    status: String,
    onButtonPressed: () -> Unit,
) {
    MdcTheme {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = onButtonPressed,
                    enabled = !isProcessing,
                    modifier = Modifier.padding(16.dp),
                ) {
                    Text("Pay with Swish")
                }

                if (isProcessing) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            }

            if (status.isNotBlank()) {
                Divider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )

                Text(
                    text = status,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                )
            }
        }
    }
}
