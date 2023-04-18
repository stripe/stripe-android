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

class CashAppPayActivity : StripeIntentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var flow by rememberSaveable { mutableStateOf(CashAppPayFlow.Payment) }
            var customerId by rememberSaveable { mutableStateOf("") }

            val isProcessing by viewModel.inProgress.observeAsState(initial = false)
            val status by viewModel.status.observeAsState(initial = "")

            val canSubmit = remember(flow, customerId) {
                flow == CashAppPayFlow.Payment || customerId.isNotBlank()
            }

            CashAppPayScreen(
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
                onButtonPressed = { payWithCashAppPay(flow, customerId) },
            )
        }
    }

    private fun payWithCashAppPay(
        flow: CashAppPayFlow,
        customerId: String,
    ) {
        when (flow) {
            CashAppPayFlow.Payment -> {
                val params = PaymentMethodCreateParams.createCashAppPay()

                createAndConfirmPaymentIntent(
                    country = "US",
                    paymentMethodCreateParams = params,
                    supportedPaymentMethods = "cashapp",
                )
            }
            CashAppPayFlow.PaymentWithFutureUse -> {
                val params = PaymentMethodCreateParams.createCashAppPay()

                val mandateData = MandateDataParams(
                    type = MandateDataParams.Type.Online.DEFAULT,
                )

                createAndConfirmPaymentIntent(
                    country = "US",
                    paymentMethodCreateParams = params,
                    mandateDataParams = mandateData,
                    customerId = customerId,
                    setupFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.OffSession,
                    supportedPaymentMethods = "cashapp",
                )
            }
            CashAppPayFlow.Setup -> {
                val params = PaymentMethodCreateParams.createCashAppPay()

                val mandateData = MandateDataParams(
                    type = MandateDataParams.Type.Online.DEFAULT,
                )

                createAndConfirmSetupIntent(
                    country = "US",
                    params = params,
                    mandateData = mandateData,
                    customerId = customerId,
                    supportedPaymentMethods = "cashapp",
                )
            }
        }
    }
}

private enum class CashAppPayFlow {
    Payment,
    PaymentWithFutureUse,
    Setup,
}

@Composable
private fun CashAppPayScreen(
    flow: CashAppPayFlow,
    customerId: String,
    isProcessing: Boolean,
    canSubmit: Boolean,
    status: String,
    onFlowTypeChanged: (CashAppPayFlow) -> Unit,
    onCustomerIdChanged: (String) -> Unit,
    onButtonPressed: () -> Unit,
) {
    MdcTheme {
        Column(modifier = Modifier.fillMaxSize()) {
            for (flowType in CashAppPayFlow.values()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = flow == flowType,
                        onClick = { onFlowTypeChanged(flowType) },
                    )

                    Text(flowType.name)
                }
            }

            if (flow == CashAppPayFlow.PaymentWithFutureUse || flow == CashAppPayFlow.Setup) {
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
                        CashAppPayFlow.Payment -> "Pay with Cash App"
                        CashAppPayFlow.PaymentWithFutureUse -> "Pay & setup with Cash App"
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
