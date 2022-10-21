package com.stripe.android.paymentsheet.example.samples.activity

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.stripe.android.elements.PaymentElement
import com.stripe.android.elements.PaymentElementController
import com.stripe.android.payments.paymentlauncher.PaymentResult
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.PaymentSelection
import kotlinx.coroutines.flow.MutableStateFlow

internal class PaymentElementActivity : BasePaymentSheetActivity() {
    private val paymentCompleted = MutableStateFlow(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val paymentController = PaymentElementController.create(
            activity = this,
            paymentResultCallback = ::onPaymentResult
        )

        val paymentElementController = MutableStateFlow<PaymentElementController?>(null)

        prepareCheckout { customerConfig, clientSecret ->
            paymentController.configureWithPaymentIntent(
                paymentIntentClientSecret = clientSecret,
                paymentSheetConfig = PaymentSheet.Configuration(
                    merchantDisplayName = merchantName,
                    customer = customerConfig,
                    googlePay = googlePayConfig,
                    allowsDelayedPaymentMethods = true
                )
            )
            paymentElementController.value = paymentController
        }

        setContent {
            MaterialTheme {
                val inProgress by viewModel.inProgress.observeAsState(false)
                val paymentCompletedState by paymentCompleted.collectAsState()
                val status by viewModel.status.observeAsState("")
                val controller by paymentElementController.collectAsState()
                var paymentSelection by remember {
                    mutableStateOf<PaymentSelection?>(null)
                }

                if (status.isNotBlank()) {
                    snackbar.setText(status).show()
                    viewModel.statusDisplayed()
                }

                Surface(
                    modifier = Modifier.fillMaxHeight(),
                    color = BACKGROUND_COLOR
                ) {
                    ScrollableTopLevelColumn {
                        Receipt(inProgress)

                        controller?.let { controller ->
                            PaymentElement(
                                controller = controller,
                                enabled = !inProgress && !paymentCompletedState,
                                onPaymentMethodSelected = {
                                    paymentSelection = it
                                }
                            )

                            BuyButton(
                                enabled = !inProgress && paymentSelection != null && !paymentCompletedState,
                                onClick = {
                                    paymentSelection?.let {
                                        viewModel.inProgress.value = true
                                        controller.completePayment(it)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp)
                            )
                        } ?: run {

                            Text(
                                text = "Creating Payment Intent...",
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }

    private fun onPaymentResult(paymentResult: PaymentResult) {
        viewModel.status.value = when(paymentResult) {
            is PaymentResult.Canceled -> "Payment canceled"
            is PaymentResult.Completed -> "Payment completed"
            is PaymentResult.Failed -> "Payment failed: ${paymentResult.throwable.localizedMessage}"
        }
        viewModel.inProgress.value = false
        if (paymentResult !is PaymentResult.Canceled) {
            paymentCompleted.value = true
        }
    }
}

@Composable
internal fun ScrollableTopLevelColumn(
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = Modifier.verticalScroll(rememberScrollState())
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            content()
        }
    }
}