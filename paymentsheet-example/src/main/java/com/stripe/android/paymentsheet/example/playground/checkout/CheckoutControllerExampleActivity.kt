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
import androidx.compose.material.OutlinedTextField
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
import com.stripe.android.paymentsheet.example.playground.settings.RadioButtonSetting
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

        observeSessionCompletion()
        setCheckoutContent(presenter, paymentElement)
    }

    private fun observeSessionCompletion() {
        lifecycleScope.launch {
            viewModel.sessionComplete.collect {
                Toast.makeText(this@CheckoutControllerExampleActivity, "Payment complete!", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    private fun setCheckoutContent(
        presenter: CheckoutPresenter,
        paymentElement: PaymentElement,
    ) {
        setContent {
            val status by viewModel.status.collectAsState()
            val settings by viewModel.settings.collectAsState()
            val confirmationResult by viewModel.confirmationResult.collectAsState()

            PlaygroundTheme(
                content = {
                    CheckoutContent(
                        status = status,
                        presenter = presenter,
                        paymentElement = paymentElement,
                        settings = settings,
                        onSettingChanged = viewModel::updateSetting,
                    )
                },
                bottomBarContent = {
                    when (status) {
                        is CheckoutControllerExampleViewModel.Status.ChooseSettings -> {
                            Button(
                                onClick = viewModel::start,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Create session")
                            }
                        }
                        is CheckoutControllerExampleViewModel.Status.Configured -> {
                            val configured = status as CheckoutControllerExampleViewModel.Status.Configured
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
                        else -> Unit
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
    settings: CheckoutControllerExampleSettings,
    onSettingChanged: (CheckoutControllerExampleSettingDefinition<Any>, Any) -> Unit,
) {
    when (status) {
        is CheckoutControllerExampleViewModel.Status.ChooseSettings -> {
            SettingsChooser(settings, onSettingChanged)
        }
        is CheckoutControllerExampleViewModel.Status.Loading -> {
            LoadingContent(status.settings)
        }
        is CheckoutControllerExampleViewModel.Status.Error -> {
            ErrorContent(status.settings, status.message)
        }
        is CheckoutControllerExampleViewModel.Status.Configured -> {
            status.session?.let { session ->
                SettingsSummary(status.settings, session.tax.status)
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
private fun SettingsChooser(
    settings: CheckoutControllerExampleSettings,
    onSettingChanged: (CheckoutControllerExampleSettingDefinition<Any>, Any) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        settings.activeSettings().forEach { setting ->
            CheckoutControllerSetting(
                setting = setting,
                settings = settings,
                onSettingChanged = onSettingChanged,
            )
        }
    }
}

@Composable
private fun CheckoutControllerSetting(
    setting: CheckoutControllerExampleSettings.ActiveSetting,
    settings: CheckoutControllerExampleSettings,
    onSettingChanged: (CheckoutControllerExampleSettingDefinition<Any>, Any) -> Unit,
) {
    @Suppress("UNCHECKED_CAST")
    val definition = setting.definition as CheckoutControllerExampleSettingDefinition<Any>
    val value = setting.value as Any

    Column(modifier = Modifier.padding(start = (setting.indentation * 16).dp)) {
        RadioButtonSetting(
            name = definition.displayName,
            options = definition.options(settings),
            value = value,
            onOptionChanged = { updatedValue ->
                onSettingChanged(definition, updatedValue)
            },
        )
        setting.displayDetails.forEach { detail ->
            OutlinedTextField(
                value = detail.value,
                onValueChange = {},
                label = { Text(detail.name) },
                readOnly = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun LoadingContent(settings: CheckoutControllerExampleSettings.Snapshot) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            SettingsSummary(settings)
            Spacer(modifier = Modifier.height(12.dp))
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun ErrorContent(
    settings: CheckoutControllerExampleSettings.Snapshot,
    message: String,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SettingsSummary(settings)
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
private fun SettingsSummary(
    settings: CheckoutControllerExampleSettings.Snapshot,
    taxStatus: Session.Tax.Status? = null,
) {
    settings.summaryLines().forEach { summaryLine ->
        Text(text = summaryLine, style = MaterialTheme.typography.h6)
    }
    taxStatus?.let { status ->
        Text(text = "Tax status: $status", style = MaterialTheme.typography.body1)
    }
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
