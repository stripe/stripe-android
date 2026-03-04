@file:OptIn(CheckoutSessionPreview::class)

package com.stripe.android.paymentsheet.example.playground.checkout

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.stripe.android.checkout.Checkout
import com.stripe.android.checkout.CheckoutSession
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.example.playground.PlaygroundTheme
import com.stripe.android.paymentsheet.example.playground.checkout.CheckoutPlaygroundViewModel.Companion.CHECKOUT_STATE_KEY
import com.stripe.android.paymentsheet.example.samples.ui.PADDING
import com.stripe.android.uicore.format.CurrencyFormatter

class CheckoutPlaygroundActivity : AppCompatActivity() {
    companion object {
        fun create(
            context: Context,
            checkoutState: Checkout.State,
        ): Intent {
            return Intent(context, CheckoutPlaygroundActivity::class.java).apply {
                putExtra(CHECKOUT_STATE_KEY, checkoutState)
            }
        }
    }

    private val viewModel: CheckoutPlaygroundViewModel by viewModels {
        @Suppress("DEPRECATION")
        val checkoutState: Checkout.State = requireNotNull(intent.getParcelableExtra(CHECKOUT_STATE_KEY))
        CheckoutPlaygroundViewModel.factory(checkoutState)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            CheckoutScreen(viewModel.checkout, viewModel)
        }
    }

    override fun finish() {
        setResult(RESULT_OK, Intent().putExtra(CHECKOUT_STATE_KEY, viewModel.checkout.state))

        super.finish()
    }
}

@Composable
private fun CheckoutScreen(checkout: Checkout, viewModel: CheckoutPlaygroundViewModel) {
    val checkoutSession by checkout.checkoutSession.collectAsState()
    var promotionCode by rememberSaveable { mutableStateOf("") }

    PlaygroundTheme(
        content = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(PADDING),
            ) {
                OutlinedTextField(
                    value = promotionCode,
                    onValueChange = { promotionCode = it },
                    label = { Text("Promotion code") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                Button(
                    onClick = { viewModel.applyPromotionCode(promotionCode) },
                ) {
                    Text("Apply")
                }
            }
        },
        bottomBarContent = {
            TotalSummary(checkoutSession)
        },
    )
}

@Composable
private fun TotalSummary(session: CheckoutSession) {
    val summary = session.totalSummary
    val currency = session.currency

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Total Summary",
            style = MaterialTheme.typography.h6,
        )

        Spacer(modifier = Modifier.height(PADDING))

        if (summary == null) {
            Text(
                text = "No total summary available",
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
            )
            return@Column
        }

        val appliedBalance = summary.appliedBalance

        SummaryRow(
            label = "Subtotal",
            amount = formatAmount(summary.subtotal, currency),
            bold = true,
        )

        DiscountRows(summary.discountAmounts, currency)
        summary.shippingRate?.let { ShippingRow(it, currency) }
        TaxRows(summary.taxAmounts, currency)
        if (appliedBalance != null && appliedBalance != 0L) {
            AppliedBalanceRow(appliedBalance, currency)
        }

        Divider(modifier = Modifier.padding(vertical = PADDING))

        SummaryRow(
            label = "Total due",
            amount = formatAmount(summary.totalDueToday, currency),
            bold = true,
        )
    }
}

@Composable
private fun SummaryRow(
    label: String,
    amount: String,
    bold: Boolean = false,
    subtext: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = PADDING / 2),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        val textStyle = if (bold) MaterialTheme.typography.subtitle1 else MaterialTheme.typography.body2
        Column {
            Text(
                text = label,
                style = textStyle,
            )
            if (subtext != null) {
                Text(
                    text = subtext,
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                )
            }
        }
        Text(
            text = amount,
            style = textStyle,
        )
    }
}

@Composable
private fun DiscountRows(discounts: List<CheckoutSession.DiscountAmount>, currency: String) {
    for (discount in discounts) {
        SummaryRow(
            label = discount.displayName,
            amount = "-${formatAmount(discount.amount, currency)}",
        )
    }
}

@Composable
private fun ShippingRow(shipping: CheckoutSession.ShippingRate, currency: String) {
    val amountText = if (shipping.amount == 0L) "Free" else formatAmount(shipping.amount, currency)
    val subtext = buildString {
        append(shipping.displayName)
        shipping.deliveryEstimate?.let {
            append(" ($it)")
        }
    }
    SummaryRow(
        label = "Shipping",
        amount = amountText,
        subtext = subtext,
    )
}

@Composable
private fun TaxRows(taxAmounts: List<CheckoutSession.TaxAmount>, currency: String) {
    if (taxAmounts.isEmpty()) return
    val allSameRate = taxAmounts.map { it.percentage }.distinct().size == 1
    for (tax in taxAmounts) {
        val label = if (allSameRate && taxAmounts.size > 1) {
            "Tax ${formatPercentage(tax.percentage)}"
        } else {
            tax.displayName
        }
        val inclusiveAnnotation = if (tax.inclusive) " (included)" else ""
        SummaryRow(
            label = "$label$inclusiveAnnotation",
            amount = formatAmount(tax.amount, currency),
        )
    }
}

@Composable
private fun AppliedBalanceRow(appliedBalance: Long, currency: String) {
    SummaryRow(
        label = "Applied balance",
        amount = "-${formatAmount(-appliedBalance, currency)}",
    )
}

private fun formatAmount(amount: Long, currencyCode: String): String {
    return CurrencyFormatter.format(amount, currencyCode)
}

private fun formatPercentage(percentage: Double): String {
    return if (percentage == percentage.toLong().toDouble()) {
        "${percentage.toLong()}%"
    } else {
        "$percentage%"
    }
}
