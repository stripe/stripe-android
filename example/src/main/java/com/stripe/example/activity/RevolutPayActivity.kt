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
import androidx.compose.material.OutlinedTextField
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
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.MandateDataParams
import com.stripe.android.model.PaymentMethodCreateParams

class RevolutPayActivity : StripeIntentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var flow by rememberSaveable { mutableStateOf(RevolutPayFlow.Payment) }
            var customerId by rememberSaveable { mutableStateOf("") }

            val isProcessing by viewModel.inProgress.observeAsState(initial = false)
            val status by viewModel.status.observeAsState(initial = "")

            val canSubmit = remember(flow, customerId) {
                flow == RevolutPayFlow.Payment || customerId.isNotBlank()
            }

            RevolutPayScreen(
                flow = flow,
                customerId = customerId,
                isProcessing = isProcessing,
                canSubmit = canSubmit,
                status = status,
                onFlowTypeChanged = {
                    viewModel.status.postValue("")
                    flow = it
                },
                onCustomerIdChanged = { customerId = it },
                onButtonPressed = { payWithRevolutPay(flow, customerId) },
            )
        }
    }

    private fun payWithRevolutPay(
        flow: RevolutPayFlow,
        customerId: String,
    ) {
        when (flow) {
            RevolutPayFlow.Payment -> {
                val params = PaymentMethodCreateParams.createRevolutPay()

                createAndConfirmPaymentIntent(
                    country = "GB",
                    paymentMethodCreateParams = params,
                    supportedPaymentMethods = "revolut_pay"
                )
            }
            RevolutPayFlow.PaymentWithFutureUse -> {
                val params = PaymentMethodCreateParams.createRevolutPay()

                val mandateData = MandateDataParams(
                    type = MandateDataParams.Type.Online.DEFAULT,
                )

                createAndConfirmPaymentIntent(
                    country = "GB",
                    paymentMethodCreateParams = params,
                    mandateDataParams = mandateData,
                    customerId = customerId,
                    setupFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.OffSession,
                    supportedPaymentMethods = "revolut_pay"
                )
            }
            RevolutPayFlow.Setup -> {
                val params = PaymentMethodCreateParams.createRevolutPay()

                val mandateData = MandateDataParams(
                    type = MandateDataParams.Type.Online.DEFAULT,
                )

                createAndConfirmSetupIntent(
                    country = "GB",
                    params = params,
                    mandateData = mandateData,
                    customerId = customerId,
                    supportedPaymentMethods = "revolut_pay"
                )
            }
        }
    }
}

private enum class RevolutPayFlow {
    Payment,
    PaymentWithFutureUse,
    Setup
}

@Composable
private fun RevolutPayScreen(
    flow: RevolutPayFlow,
    customerId: String,
    isProcessing: Boolean,
    canSubmit: Boolean,
    status: String,
    onFlowTypeChanged: (RevolutPayFlow) -> Unit,
    onCustomerIdChanged: (String) -> Unit,
    onButtonPressed: () -> Unit,
) {
    MdcTheme {
        Column(modifier = Modifier.fillMaxSize()) {
            for (flowType in RevolutPayFlow.values()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = flow == flowType,
                        onClick = { onFlowTypeChanged(flowType) },
                    )

                    Text(flowType.name)
                }
            }

            if (flow == RevolutPayFlow.PaymentWithFutureUse || flow == RevolutPayFlow.Setup) {
                OutlinedTextField(
                    value = customerId,
                    onValueChange = onCustomerIdChanged,
                    placeholder = { Text("Customer ID (required)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = onButtonPressed,
                    enabled = !isProcessing && canSubmit,
                    modifier = Modifier.padding(16.dp),
                ) {
                    val text = when (flow) {
                        RevolutPayFlow.Payment -> "Pay with Revolut Pay"
                        RevolutPayFlow.PaymentWithFutureUse -> "Pay & setup with Revolut Pay"
                        RevolutPayFlow.Setup -> "Set up with Revolut Pay"
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
