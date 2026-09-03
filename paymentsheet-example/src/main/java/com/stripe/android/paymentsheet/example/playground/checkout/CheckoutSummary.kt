@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)

package com.stripe.android.paymentsheet.example.playground.checkout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.stripe.android.checkout.CheckoutController.Session
import com.stripe.android.uicore.format.CurrencyFormatter

internal const val PENDING_TAX_TEST_TAG = "pending_tax"

@Composable
internal fun LineItemsSection(session: Session) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = "Line Items", style = MaterialTheme.typography.h6)
        session.lineItems.forEach { item ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("${item.name} x${item.quantity}", style = MaterialTheme.typography.body2)
                Text(formatAmount(item.total, session.currency), style = MaterialTheme.typography.body2)
            }
        }
    }
}

@Composable
internal fun TotalSummarySection(session: Session) {
    val summary = session.totalSummary ?: return
    Column(modifier = Modifier.fillMaxWidth()) {
        Divider(modifier = Modifier.padding(vertical = 12.dp))
        SummaryRow("Subtotal", formatAmount(summary.subtotal, session.currency))
        summary.discountAmounts.forEach { discount ->
            SummaryRow(discount.displayName, "-${formatAmount(discount.amount, session.currency)}")
        }
        summary.shippingRate?.let { shipping ->
            SummaryRow(
                shipping.displayName,
                if (shipping.amount == 0L) "Free" else formatAmount(shipping.amount, session.currency),
            )
        }
        TaxSummaryRows(taxStatus = session.tax.status) {
            summary.taxAmounts.forEach { tax ->
                SummaryRow(
                    if (tax.inclusive) "${tax.displayName} (included)" else tax.displayName,
                    formatAmount(tax.amount, session.currency),
                )
            }
        }
        Divider(modifier = Modifier.padding(vertical = 8.dp))
        SummaryRow("Total", formatAmount(summary.totalDueToday, session.currency))
    }
}

@Composable
internal fun TaxSummaryRows(
    taxStatus: Session.Tax.Status,
    calculatedTaxRows: @Composable () -> Unit,
) {
    val pendingMessage = when (taxStatus) {
        Session.Tax.Status.RequiresShippingAddress -> "Enter shipping address to calculate"
        Session.Tax.Status.RequiresBillingAddress -> "Enter billing address to calculate"
        Session.Tax.Status.Ready,
        Session.Tax.Status.Unknown -> null
    }

    if (pendingMessage != null) {
        val color = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.medium)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {}
                .testTag(PENDING_TAX_TEST_TAG),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(text = "Tax", color = color)
            Text(text = pendingMessage, style = MaterialTheme.typography.caption, color = color)
        }
    } else {
        calculatedTaxRows()
    }
}

@Composable
private fun SummaryRow(label: String, amount: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label)
        Text(amount)
    }
}

private fun formatAmount(amount: Long, currency: String): String {
    return CurrencyFormatter.format(amount, currency)
}
