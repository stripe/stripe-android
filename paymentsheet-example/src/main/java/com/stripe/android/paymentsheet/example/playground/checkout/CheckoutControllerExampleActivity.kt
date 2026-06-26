@file:OptIn(CheckoutSessionPreview::class)

package com.stripe.android.paymentsheet.example.playground.checkout

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.stripe.android.checkout.CheckoutSession
import com.stripe.android.checkout.PaymentElement
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.example.playground.PlaygroundTheme
import com.stripe.android.uicore.format.CurrencyFormatter
import kotlinx.coroutines.launch

internal class CheckoutControllerExampleActivity : AppCompatActivity() {

    private val viewModel: CheckoutControllerExampleViewModel by viewModels {
        CheckoutControllerExampleViewModel.factory
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val presenter = viewModel.controller.createPresenter(this)
        val paymentElement = presenter.paymentElement()

        lifecycleScope.launch {
            viewModel.sessionComplete.collect {
                Toast.makeText(this@CheckoutControllerExampleActivity, "Payment complete!", Toast.LENGTH_LONG).show()
                finish()
            }
        }

        setContent {
            val status by viewModel.status.collectAsState()

            PlaygroundTheme(
                content = {
                    when (val currentStatus = status) {
                        is CheckoutControllerExampleViewModel.Status.Loading -> {
                            LoadingContent()
                        }
                        is CheckoutControllerExampleViewModel.Status.Error -> {
                            ErrorContent(currentStatus.message)
                        }
                        is CheckoutControllerExampleViewModel.Status.Configured -> {
                            val session = currentStatus.checkoutSession
                            if (session != null) {
                                LineItemsSection(session)
                                TotalSummarySection(session)
                                paymentElement.PaymentOptionsContent()
                            }
                        }
                    }
                },
                bottomBarContent = {
                    val configured = status as? CheckoutControllerExampleViewModel.Status.Configured
                    PaymentOptionRow(configured?.paymentOption)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { paymentElement.presentPaymentOptions() },
                        enabled = configured != null && !configured.isLoading,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Select Payment Method")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { presenter.confirm() },
                        enabled = configured?.paymentOption != null && !configured.isLoading,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Confirm")
                    }
                },
            )
        }
    }
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorContent(message: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Error",
            style = MaterialTheme.typography.h6,
            color = Color.Red,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = message)
    }
}

@Composable
private fun PaymentOptionRow(paymentOption: PaymentElement.PaymentOptionDisplayData?) {
    if (paymentOption != null) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Image(
                painter = paymentOption.iconPainter,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
            )
            Text(
                text = paymentOption.label,
                style = MaterialTheme.typography.body1,
            )
        }
    } else {
        Text(
            text = "No payment method selected",
            style = MaterialTheme.typography.body2,
            color = Color.Gray,
        )
    }
}

@Composable
private fun LineItemsSection(session: CheckoutSession) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = "Line Items", style = MaterialTheme.typography.h6)
        Spacer(modifier = Modifier.height(8.dp))

        for (item in session.lineItems) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "${item.name} x${item.quantity}",
                    style = MaterialTheme.typography.body2,
                )
                Text(
                    text = formatAmount(item.total, session.currency),
                    style = MaterialTheme.typography.body2,
                )
            }
        }
    }
}

@Composable
private fun TotalSummarySection(session: CheckoutSession) {
    val summary = session.totalSummary ?: return

    Column(modifier = Modifier.fillMaxWidth()) {
        Divider(modifier = Modifier.padding(vertical = 12.dp))

        SummaryRow(label = "Subtotal", amount = formatAmount(summary.subtotal, session.currency))

        for (discount in summary.discountAmounts) {
            SummaryRow(
                label = discount.displayName,
                amount = "-${formatAmount(discount.amount, session.currency)}",
            )
        }

        summary.shippingRate?.let { shipping ->
            val amountText = if (shipping.amount == 0L) "Free" else formatAmount(shipping.amount, session.currency)
            SummaryRow(label = "Shipping", amount = amountText)
        }

        for (tax in summary.taxAmounts) {
            val label = if (tax.inclusive) "${tax.displayName} (included)" else tax.displayName
            SummaryRow(label = label, amount = formatAmount(tax.amount, session.currency))
        }

        Divider(modifier = Modifier.padding(vertical = 8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = "Total", style = MaterialTheme.typography.subtitle1)
            Text(
                text = formatAmount(summary.totalDueToday, session.currency),
                style = MaterialTheme.typography.subtitle1,
            )
        }
    }
}

@Composable
private fun SummaryRow(label: String, amount: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = MaterialTheme.typography.body2)
        Text(text = amount, style = MaterialTheme.typography.body2)
    }
}

private fun formatAmount(amount: Long, currency: String): String {
    return CurrencyFormatter.format(amount, currency)
}
