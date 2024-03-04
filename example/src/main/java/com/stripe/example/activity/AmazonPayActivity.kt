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
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.accompanist.themeadapter.material.MdcTheme
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams

class AmazonPayActivity : StripeIntentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var flow by rememberSaveable { mutableStateOf(AmazonPayFlow.Payment) }

            val isProcessing by viewModel.inProgress.observeAsState(initial = false)
            val status by viewModel.status.observeAsState(initial = "")

            val canSubmit = remember(flow) {
                flow == AmazonPayFlow.Payment
            }

            AmazonPayScreen(
                flow = flow,
                isProcessing = isProcessing,
                canSubmit = canSubmit,
                status = status,
                onFlowTypeChanged = {
                    viewModel.status.postValue("")
                    flow = it
                },
                onButtonPressed = { payWithAmazonPay(flow) },
            )
        }
    }

    private fun payWithAmazonPay(
        flow: AmazonPayFlow,
    ) {
        when (flow) {
            AmazonPayFlow.Payment -> {
                val params = PaymentMethodCreateParams.createAmazonPay()

                createAndConfirmPaymentIntent(
                    country = "US",
                    paymentMethodCreateParams = params,
                    supportedPaymentMethods = PaymentMethod.Type.AmazonPay.code
                )
            }
        }
    }
}

private enum class AmazonPayFlow {
    Payment,
}

@Composable
private fun AmazonPayScreen(
    flow: AmazonPayFlow,
    isProcessing: Boolean,
    canSubmit: Boolean,
    status: String,
    onFlowTypeChanged: (AmazonPayFlow) -> Unit,
    onButtonPressed: () -> Unit,
) {
    MdcTheme {
        Column(modifier = Modifier.fillMaxSize()) {
            for (flowType in AmazonPayFlow.entries) {
                Row(verticalAlignment = Alignment.CenterVertically) {
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
                    enabled = !isProcessing && canSubmit,
                    modifier = Modifier.padding(16.dp),
                ) {
                    val text = when (flow) {
                        AmazonPayFlow.Payment -> "Pay with Amazon Pay"
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
