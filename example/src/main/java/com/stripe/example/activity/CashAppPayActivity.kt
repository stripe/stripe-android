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
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.model.PaymentMethodCreateParams

class CashAppPayActivity : StripeIntentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var flow by remember { mutableStateOf(CashAppPayFlow.Payment) }
            val isProcessing by viewModel.inProgress.observeAsState()
            val status by viewModel.status.observeAsState()

            CashAppPayScreen(
                flow = flow,
                isProcessing = isProcessing ?: false,
                status = status.orEmpty(),
                onFlowTypeChanged = { flow = it },
                onButtonPressed = { payWithCashAppPay(flow) },
            )
        }
    }

    private fun payWithCashAppPay(flow: CashAppPayFlow) {
        when (flow) {
            CashAppPayFlow.Payment -> {
                val params = PaymentMethodCreateParams.createCashAppPay()

                createAndConfirmPaymentIntent(
                    country = "US",
                    paymentMethodCreateParams = params,
                    supportedPaymentMethods = "cashapp",
                )
            }
            CashAppPayFlow.Setup -> {
                val params = PaymentMethodCreateParams.createCashAppPay()

                createAndConfirmSetupIntent(
                    country = "US",
                    params = params,
                    supportedPaymentMethods = "cashapp",
                )
            }
        }
    }
}

private enum class CashAppPayFlow {
    Payment,
    Setup,
}

@Composable
private fun CashAppPayScreen(
    flow: CashAppPayFlow,
    isProcessing: Boolean,
    status: String,
    onFlowTypeChanged: (CashAppPayFlow) -> Unit,
    onButtonPressed: () -> Unit,
) {
    MaterialTheme {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                for (flowType in CashAppPayFlow.values()) {
                    RadioButton(
                        selected = flow == flowType,
                        onClick = { onFlowTypeChanged(flowType) },
                    )

                    Text(flowType.name)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = onButtonPressed,
                    enabled = !isProcessing,
                    modifier = Modifier.padding(16.dp),
                ) {
                    val text = when (flow) {
                        CashAppPayFlow.Payment -> "Pay with Cash App"
                        CashAppPayFlow.Setup -> "Set up with Cash App"
                    }
                    Text(text)
                }

                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                    )
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
