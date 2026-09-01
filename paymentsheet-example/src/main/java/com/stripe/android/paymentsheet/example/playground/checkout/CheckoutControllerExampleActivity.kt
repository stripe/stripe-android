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
import com.stripe.android.elements.ShippingAddressElement
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
        val shippingAddressElement = presenter.shippingAddressElement()

        observeSessionComplete()
        setCheckoutContent(presenter, paymentElement, shippingAddressElement)
    }

    private fun setCheckoutContent(
        presenter: CheckoutPresenter,
        paymentElement: PaymentElement,
        shippingAddressElement: ShippingAddressElement,
    ) {
        setContent {
            val status by viewModel.status.collectAsState()
            val confirmationResult by viewModel.confirmationResult.collectAsState()

            PlaygroundTheme(
                content = {
                    CheckoutContent(
                        status = status,
                        presenter = presenter,
                        paymentElement = paymentElement,
                        confirmationResult = confirmationResult,
                        onScenarioSelected = viewModel::start,
                    )
                },
                bottomBarContent = {
                    val configured = status as? CheckoutControllerExampleViewModel.Status.Configured
                    if (configured != null) {
                        val isUpdating by viewModel.controller.isUpdating.collectAsState()
                        val isShippingScenario =
                            configured.scenario == CheckoutControllerExampleScenario.ShippingTax
                        val isComplete =
                            confirmationResult is CheckoutControllerExampleViewModel.ConfirmationResult.Completed
                        val controlsEnabled = configured.session != null &&
                            !isUpdating &&
                            !(isShippingScenario && isComplete)
                        ConfirmationControls(
                            session = configured.session,
                            confirmationResult = confirmationResult,
                            onSelectPaymentMethod = paymentElement::present,
                            onEditShippingAddress = shippingAddressElement::present,
                            controlsEnabled = controlsEnabled,
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

    private fun observeSessionComplete() {
        lifecycleScope.launch {
            viewModel.sessionComplete.collect {
                Toast.makeText(this@CheckoutControllerExampleActivity, "Payment complete!", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }
}

@Composable
private fun ShippingAddressButton(
    hasShippingAddress: Boolean,
    onClick: () -> Unit,
    enabled: Boolean,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(if (hasShippingAddress) "Edit Shipping Address" else "Add Shipping Address")
    }
}

@Composable
private fun CheckoutContent(
    status: CheckoutControllerExampleViewModel.Status,
    presenter: CheckoutPresenter,
    paymentElement: PaymentElement,
    confirmationResult: CheckoutControllerExampleViewModel.ConfirmationResult?,
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
                if (status.scenario == CheckoutControllerExampleScenario.ShippingTax) {
                    ShippingTaxSummary(session, confirmationResult)
                } else {
                    ScenarioSummary(status.scenario, session.tax.status)
                }
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
private fun ShippingTaxSummary(
    session: Session,
    confirmationResult: CheckoutControllerExampleViewModel.ConfirmationResult?,
) {
    Text(
        text = "Scenario: ${CheckoutControllerExampleScenario.ShippingTax.displayName}",
        style = MaterialTheme.typography.h6,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text("Review the session state, then verify persistence in Admin after completion.")
    Spacer(modifier = Modifier.height(16.dp))
    ActionStatus(
        step = "Shipping address",
        detail = if (session.shippingAddress != null) "Ready" else "Required",
        isReady = session.shippingAddress != null,
    )
    ActionStatus(
        step = "Automatic tax",
        detail = session.tax.status.displayName(),
        isReady = session.tax.status == Session.Tax.Status.Ready,
    )
    ActionStatus(
        step = "Payment method",
        detail = session.paymentOptionDisplayData?.label ?: "Required",
        isReady = session.paymentOptionDisplayData != null,
    )
    ActionStatus(
        step = "Confirmation",
        detail = confirmationResult.confirmationDetail(),
        isReady = confirmationResult is CheckoutControllerExampleViewModel.ConfirmationResult.Completed,
    )
    if (confirmationResult is CheckoutControllerExampleViewModel.ConfirmationResult.Completed) {
        CompletionVerification(confirmationResult.session)
    }
}

@Composable
private fun ActionStatus(
    step: String,
    detail: String,
    isReady: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(step, style = MaterialTheme.typography.subtitle1)
        Text(
            text = detail,
            color = if (isReady) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface,
        )
    }
}

@Composable
private fun CompletionVerification(session: Session?) {
    val shippingAddress = session?.shippingAddress
    Spacer(modifier = Modifier.height(24.dp))
    Text("Completed Controller State", style = MaterialTheme.typography.h6)
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        "Controller state can carry shipping locally. Verify collected_information.shipping_details " +
            "on this Checkout Session in Admin to prove backend persistence."
    )
    Spacer(modifier = Modifier.height(8.dp))
    VerificationRow("Session ID", session?.id, !session?.id.isNullOrBlank())
    VerificationRow(
        label = "Status",
        value = session?.status.displayName(),
        passes = session?.status is Session.Status.Complete,
    )
    VerificationRow(
        label = "Automatic tax",
        value = session?.tax?.status.displayName(),
        passes = session?.tax?.status == Session.Tax.Status.Ready,
    )
    VerificationRow("Name", shippingAddress?.name, !shippingAddress?.name.isNullOrBlank())
    VerificationRow(
        label = "Line 1",
        value = shippingAddress?.address?.line1,
        passes = !shippingAddress?.address?.line1.isNullOrBlank(),
    )
    VerificationRow(
        label = "Line 2",
        value = shippingAddress?.address?.line2,
        passes = !shippingAddress?.address?.line2.isNullOrBlank(),
    )
    VerificationRow("City", shippingAddress?.address?.city, !shippingAddress?.address?.city.isNullOrBlank())
    VerificationRow("State", shippingAddress?.address?.state, !shippingAddress?.address?.state.isNullOrBlank())
    VerificationRow(
        label = "Postal code",
        value = shippingAddress?.address?.postalCode,
        passes = !shippingAddress?.address?.postalCode.isNullOrBlank(),
    )
    VerificationRow(
        label = "Country",
        value = shippingAddress?.address?.country,
        passes = !shippingAddress?.address?.country.isNullOrBlank(),
    )
}

@Composable
private fun VerificationRow(
    label: String,
    value: String?,
    passes: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.body2)
            Text(value ?: "Missing", style = MaterialTheme.typography.caption)
        }
        Text(
            text = if (passes) "PASS" else "FAIL",
            color = if (passes) MaterialTheme.colors.primary else MaterialTheme.colors.error,
            style = MaterialTheme.typography.subtitle2,
        )
    }
}

@Composable
private fun ConfirmationControls(
    session: Session?,
    confirmationResult: CheckoutControllerExampleViewModel.ConfirmationResult?,
    onSelectPaymentMethod: () -> Unit,
    onEditShippingAddress: () -> Unit,
    controlsEnabled: Boolean,
    onConfirm: () -> Unit,
) {
    PaymentOptionRow(session?.paymentOptionDisplayData)
    confirmationResult?.let { result ->
        val message = when (result) {
            is CheckoutControllerExampleViewModel.ConfirmationResult.Completed -> null
            CheckoutControllerExampleViewModel.ConfirmationResult.Canceled -> {
                "Confirmation canceled. You can retry."
            }
            is CheckoutControllerExampleViewModel.ConfirmationResult.Failed -> {
                "Confirmation failed: ${result.message}. You can retry."
            }
        }
        message?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = it, color = Color.Red)
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
    Button(
        onClick = onSelectPaymentMethod,
        enabled = controlsEnabled,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("Select Payment Method")
    }
    Spacer(modifier = Modifier.height(8.dp))
    ShippingAddressButton(
        hasShippingAddress = session?.shippingAddress != null,
        onClick = onEditShippingAddress,
        enabled = controlsEnabled,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Button(
        onClick = onConfirm,
        enabled = controlsEnabled,
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

private fun CheckoutControllerExampleViewModel.ConfirmationResult?.confirmationDetail(): String {
    return when (this) {
        null -> "Pending"
        CheckoutControllerExampleViewModel.ConfirmationResult.Canceled -> "Canceled"
        is CheckoutControllerExampleViewModel.ConfirmationResult.Failed -> "Failed"
        is CheckoutControllerExampleViewModel.ConfirmationResult.Completed -> "Completed"
    }
}

private fun Session.Status?.displayName(): String {
    return when (this) {
        is Session.Status.Open -> "Open"
        is Session.Status.Complete -> "Complete"
        is Session.Status.Expired -> "Expired"
        null -> "Missing"
    }
}

private fun Session.Tax.Status?.displayName(): String {
    return when (this) {
        Session.Tax.Status.Ready -> "Ready"
        Session.Tax.Status.RequiresShippingAddress -> "Requires shipping address"
        Session.Tax.Status.RequiresBillingAddress -> "Requires billing address"
        Session.Tax.Status.Unknown -> "Unknown"
        null -> "Missing"
    }
}
