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

internal const val PENDING_TAX_TEST_TAG = "pending_tax"

@Composable
internal fun LineItemsSection(session: Session) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = "Line Items", style = MaterialTheme.typography.h6)
        session.orderSummaryItems.forEach { summaryItem ->
            if (summaryItem is Session.OrderSummaryItem.OneTimePrice) {
                summaryItem.items.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("${item.displayName} x${item.quantity}", style = MaterialTheme.typography.body2)
                        Text(item.amountDetails.total.amount, style = MaterialTheme.typography.body2)
                    }
                }
            }
        }
    }
}

@Composable
internal fun TotalSummarySection(session: Session) {
    val summary = session.totals
    Column(modifier = Modifier.fillMaxWidth()) {
        Divider(modifier = Modifier.padding(vertical = 12.dp))
        SummaryRow("Subtotal", summary.subtotal.amount)
        session.discountAmounts.forEach { discount ->
            SummaryRow(discount.displayName, "-${discount.amount}")
        }
        TaxSummaryRows(taxStatus = session.tax?.status) {
            session.taxAmounts.orEmpty().forEach { tax ->
                SummaryRow(
                    if (tax.inclusive) "${tax.displayName} (included)" else tax.displayName,
                    tax.amount,
                )
            }
        }
        Divider(modifier = Modifier.padding(vertical = 8.dp))
        SummaryRow("Total", summary.total.amount)
    }
}

@Composable
internal fun TaxSummaryRows(
    taxStatus: Session.Tax.Status?,
    calculatedTaxRows: @Composable () -> Unit,
) {
    val pendingMessage = when (taxStatus) {
        Session.Tax.Status.RequiresShippingAddress -> "Enter shipping address to calculate"
        Session.Tax.Status.RequiresBillingAddress -> "Enter billing address to calculate"
        Session.Tax.Status.Ready,
        null -> null
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
