@file:OptIn(CheckoutSessionPreview::class)

package com.stripe.android.paymentsheet.example.playground.checkout

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
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
import androidx.lifecycle.lifecycleScope
import com.stripe.android.checkout.CheckoutController.Session
import com.stripe.android.checkout.CheckoutController.Session.PaymentOptionDisplayData
import com.stripe.android.checkout.CheckoutPresenter
import com.stripe.android.elements.PaymentElement
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
            val confirmationResult by viewModel.confirmationResult.collectAsState()

            PlaygroundTheme(
                content = {
                    CheckoutContent(
                        status = status,
                        presenter = presenter,
                        paymentElement = paymentElement,
                        onScenarioSelected = viewModel::start,
                    )
                },
                bottomBarContent = {
                    val configured = status as? CheckoutControllerExampleViewModel.Status.Configured
                    if (configured != null) {
                        ConfirmationControls(
                            paymentOption = configured.session?.paymentOptionDisplayData,
                            confirmationResult = confirmationResult,
                            onSelectPaymentMethod = paymentElement::present,
                            onConfirm = {
                                viewModel.clearConfirmationResult()
                                presenter.confirm()
                            },
                        )
                    }
                },
            )
        }
    }
}

@Composable
private fun CheckoutContent(
    status: CheckoutControllerExampleViewModel.Status,
    presenter: CheckoutPresenter,
    paymentElement: PaymentElement,
    onScenarioSelected: (CheckoutControllerExampleScenario) -> Unit,
) {
    when (status) {
        is CheckoutControllerExampleViewModel.Status.ChooseScenario -> {
            ScenarioChooser(onScenarioSelected)
        }
        is CheckoutControllerExampleViewModel.Status.Loading -> {
            LoadingContent(status.scenario)
        }
        is CheckoutControllerExampleViewModel.Status.Error -> {
            ErrorContent(status.scenario, status.message)
        }
        is CheckoutControllerExampleViewModel.Status.Configured -> {
            status.session?.let { session ->
                ScenarioSummary(status.scenario, session.tax.status)
                LineItemsSection(session)
                TotalSummarySection(session)
                if (session.availableExpressCheckoutPaymentMethods.isNotEmpty()) {
                    presenter.expressCheckoutElement().Content()
                }
                paymentElement.Content()
            }
        }
    }
}

@Composable
private fun ScenarioChooser(
    onScenarioSelected: (CheckoutControllerExampleScenario) -> Unit,
) {
    Text(text = "Choose a session scenario", style = MaterialTheme.typography.h6)
    Spacer(modifier = Modifier.height(16.dp))
    CheckoutControllerExampleScenario.entries.forEach { scenario ->
        Button(
            onClick = { onScenarioSelected(scenario) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(scenario.displayName)
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
    Text(
        text = "Relaunch this activity to choose another scenario.",
        style = MaterialTheme.typography.body2,
        color = Color.Gray,
    )
}

@Composable
private fun LoadingContent(scenario: CheckoutControllerExampleScenario) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "Scenario: ${scenario.displayName}")
            Spacer(modifier = Modifier.height(12.dp))
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun ErrorContent(
    scenario: CheckoutControllerExampleScenario,
    message: String,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "Scenario: ${scenario.displayName}")
        Spacer(modifier = Modifier.height(8.dp))
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
private fun ScenarioSummary(
    scenario: CheckoutControllerExampleScenario,
    taxStatus: Session.Tax.Status,
) {
    Text(text = "Scenario: ${scenario.displayName}", style = MaterialTheme.typography.h6)
    Text(text = "Tax status: $taxStatus", style = MaterialTheme.typography.body1)
    Spacer(modifier = Modifier.height(16.dp))
}

@Composable
private fun ConfirmationControls(
    paymentOption: PaymentOptionDisplayData?,
    confirmationResult: CheckoutControllerExampleViewModel.ConfirmationResult?,
    onSelectPaymentMethod: () -> Unit,
    onConfirm: () -> Unit,
) {
    PaymentOptionRow(paymentOption)
    confirmationResult?.let { result ->
        Spacer(modifier = Modifier.height(8.dp))
        val message = when (result) {
            CheckoutControllerExampleViewModel.ConfirmationResult.Canceled -> "Confirmation canceled"
            is CheckoutControllerExampleViewModel.ConfirmationResult.Failed -> {
                "Confirmation failed: ${result.message}"
            }
        }
        Text(text = message, color = Color.Red)
    }
    Spacer(modifier = Modifier.height(8.dp))
    Button(
        onClick = onSelectPaymentMethod,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("Select Payment Method")
    }
    Spacer(modifier = Modifier.height(8.dp))
    Button(
        onClick = onConfirm,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("Confirm")
    }
}

@Composable
private fun PaymentOptionRow(paymentOption: PaymentOptionDisplayData?) {
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
private fun LineItemsSection(session: Session) {
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
private fun TotalSummarySection(session: Session) {
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
