@file:OptIn(CheckoutSessionPreview::class)

package com.stripe.android.paymentsheet.example.playground.checkout

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.stripe.android.PaymentConfiguration
import com.stripe.android.checkout.Checkout
import com.stripe.android.checkout.CheckoutSession
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.addresselement.AddressLauncher
import com.stripe.android.paymentsheet.addresselement.AddressLauncherResult
import com.stripe.android.paymentsheet.addresselement.rememberAddressLauncher
import com.stripe.android.paymentsheet.example.playground.PlaygroundTheme
import com.stripe.android.paymentsheet.example.playground.checkout.CheckoutPlaygroundViewModel.Companion.CHECKOUT_STATE_KEY
import com.stripe.android.paymentsheet.example.samples.ui.PADDING
import com.stripe.android.uicore.format.CurrencyFormatter
import kotlinx.coroutines.flow.StateFlow

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
            val shippingAddressLauncher = rememberAddressLauncher { result ->
                if (result is AddressLauncherResult.Succeeded) {
                    viewModel.updateShippingAddress(result.address)
                }
            }

            val billingAddressLauncher = rememberAddressLauncher { result ->
                if (result is AddressLauncherResult.Succeeded) {
                    viewModel.updateBillingAddress(result.address)
                }
            }

            CheckoutScreen(
                checkout = viewModel.checkout,
                isLoading = viewModel.isLoading,
                errorMessage = viewModel.errorMessage,
                clearErrorMessage = viewModel::clearErrorMessage,
                applyPromotionCode = viewModel::applyPromotionCode,
                removePromotionCode = viewModel::removePromotionCode,
                updateLineItemQuantity = viewModel::updateLineItemQuantity,
                selectShippingRate = viewModel::selectShippingRate,
                lastAddressDetails = viewModel.lastAddressDetails,
                clearShippingAddress = viewModel::clearShippingAddress,
                updatePostalCode = viewModel::updatePostalCode,
                updateShippingAddress = {
                    val publishableKey = PaymentConfiguration.getInstance(applicationContext).publishableKey
                    val configuration = AddressLauncher.Configuration(
                        address = viewModel.lastAddressDetails.value,
                    )
                    shippingAddressLauncher.present(publishableKey, configuration)
                },
                lastBillingAddressDetails = viewModel.lastBillingAddressDetails,
                updateTaxId = viewModel::updateTaxId,
                clearBillingAddress = viewModel::clearBillingAddress,
                updateBillingAddress = {
                    val publishableKey = PaymentConfiguration.getInstance(applicationContext).publishableKey
                    val configuration = AddressLauncher.Configuration(
                        address = viewModel.lastBillingAddressDetails.value,
                    )
                    billingAddressLauncher.present(publishableKey, configuration)
                },
                refresh = viewModel::refresh,
            )
        }
    }

    override fun finish() {
        setResult(RESULT_OK, Intent().putExtra(CHECKOUT_STATE_KEY, viewModel.checkout.state))

        super.finish()
    }
}

@Composable
private fun CheckoutScreen(
    checkout: Checkout,
    isLoading: StateFlow<Boolean>,
    errorMessage: StateFlow<String?>,
    clearErrorMessage: () -> Unit,
    applyPromotionCode: (String) -> Unit,
    removePromotionCode: () -> Unit,
    updateLineItemQuantity: (String, Int) -> Unit,
    selectShippingRate: (String) -> Unit,
    lastAddressDetails: StateFlow<AddressDetails?>,
    clearShippingAddress: () -> Unit,
    updatePostalCode: (String) -> Unit,
    updateShippingAddress: () -> Unit,
    updateTaxId: (String, String) -> Unit,
    lastBillingAddressDetails: StateFlow<AddressDetails?>,
    clearBillingAddress: () -> Unit,
    updateBillingAddress: () -> Unit,
    refresh: () -> Unit,
) {
    val checkoutSession by checkout.checkoutSession.collectAsState()
    val loading by isLoading.collectAsState()
    val error by errorMessage.collectAsState()
    var promotionCode by rememberSaveable { mutableStateOf("") }

    val context = LocalContext.current
    LaunchedEffect(error) {
        error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            clearErrorMessage()
        }
    }

    BackHandler(enabled = loading) { }

    Box {
        PlaygroundTheme(
            content = {
                checkout.AdaptivePricingContent()
                LineItemsSection(checkoutSession, updateLineItemQuantity)
                ShippingAddressSection(
                    lastAddressDetails = lastAddressDetails,
                    clearShippingAddress = clearShippingAddress,
                    updatePostalCode = updatePostalCode,
                    updateShippingAddress = updateShippingAddress,
                )
                BillingAddressSection(
                    lastBillingAddressDetails = lastBillingAddressDetails,
                    clearBillingAddress = clearBillingAddress,
                    updateBillingAddress = updateBillingAddress,
                )
                TaxIdSection(updateTaxId = updateTaxId)
                ShippingOptionsSection(checkoutSession, selectShippingRate)
                PromotionCodeInput(promotionCode, { promotionCode = it }, applyPromotionCode)
                Button(
                    onClick = refresh,
                ) {
                    Text("Refresh")
                }
            },
            bottomBarContent = {
                TotalSummary(checkoutSession, removePromotionCode)
            },
        )
        if (loading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
                    .clickable(enabled = false, onClick = {}),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
private fun PromotionCodeInput(
    promotionCode: String,
    onPromotionCodeChange: (String) -> Unit,
    applyPromotionCode: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PADDING),
    ) {
        OutlinedTextField(
            value = promotionCode,
            onValueChange = onPromotionCodeChange,
            label = { Text("Promotion code") },
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        Button(
            onClick = { applyPromotionCode(promotionCode) },
        ) {
            Text("Apply")
        }
    }
}

@Composable
private fun LineItemsSection(
    session: CheckoutSession,
    updateLineItemQuantity: (String, Int) -> Unit,
) {
    val lineItems = session.lineItems
    val currency = session.currency

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Items",
            style = MaterialTheme.typography.h6,
        )

        Spacer(modifier = Modifier.height(PADDING))

        for (item in lineItems) {
            val unitPrice = item.unitAmount?.let { formatAmount(it, currency) }
            val lineTotal = formatAmount(item.total, currency)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = PADDING / 2),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.body2,
                    )
                    if (unitPrice != null) {
                        Text(
                            text = "$unitPrice each",
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { updateLineItemQuantity(item.id, item.quantity - 1) },
                        enabled = item.quantity > 1,
                    ) {
                        Text("-")
                    }
                    Text(
                        text = "${item.quantity}",
                        style = MaterialTheme.typography.body2,
                    )
                    IconButton(
                        onClick = { updateLineItemQuantity(item.id, item.quantity + 1) },
                    ) {
                        Text("+")
                    }
                }
                Text(
                    text = lineTotal,
                    style = MaterialTheme.typography.body2,
                )
            }
        }

        Divider(modifier = Modifier.padding(vertical = PADDING))
    }
}

@Composable
private fun ShippingAddressSection(
    lastAddressDetails: StateFlow<AddressDetails?>,
    clearShippingAddress: () -> Unit,
    updatePostalCode: (String) -> Unit,
    updateShippingAddress: () -> Unit,
) {
    val addressDetails by lastAddressDetails.collectAsState()

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Shipping Address",
            style = MaterialTheme.typography.h6,
        )

        Spacer(modifier = Modifier.height(PADDING))

        AddressSummary(addressDetails?.address)

        Spacer(modifier = Modifier.height(PADDING))

        var useFullAddress by rememberSaveable { mutableStateOf(false) }
        var postalCode by rememberSaveable { mutableStateOf("") }
        AddressModePicker(
            useFullAddress = useFullAddress,
            onPostalCodeMode = {
                useFullAddress = false
                postalCode = ""
                clearShippingAddress()
            },
            onFullAddressMode = {
                useFullAddress = true
                postalCode = ""
                clearShippingAddress()
            },
        )

        Spacer(modifier = Modifier.height(PADDING))

        if (useFullAddress) {
            Button(onClick = updateShippingAddress) {
                Text(if (addressDetails != null) "Edit Shipping Address" else "Add Shipping Address")
            }
        } else {
            PostalCodeInput(
                postalCode = postalCode,
                onPostalCodeChange = { postalCode = it },
                onApply = { updatePostalCode(postalCode) },
            )
        }

        Divider(modifier = Modifier.padding(vertical = PADDING))
    }
}

@Composable
private fun BillingAddressSection(
    lastBillingAddressDetails: StateFlow<AddressDetails?>,
    clearBillingAddress: () -> Unit,
    updateBillingAddress: () -> Unit,
) {
    val addressDetails by lastBillingAddressDetails.collectAsState()

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Billing Address",
            style = MaterialTheme.typography.h6,
        )

        Spacer(modifier = Modifier.height(PADDING))

        AddressSummary(addressDetails?.address)

        if (addressDetails?.name != null) {
            Text(
                text = "Name: ${addressDetails?.name}",
                style = MaterialTheme.typography.body2,
            )
        }

        Spacer(modifier = Modifier.height(PADDING))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(PADDING),
        ) {
            Button(onClick = updateBillingAddress) {
                Text(if (addressDetails != null) "Edit Billing Address" else "Add Billing Address")
            }
            if (addressDetails != null) {
                Button(onClick = clearBillingAddress) {
                    Text("Clear")
                }
            }
        }

        Divider(modifier = Modifier.padding(vertical = PADDING))
    }
}

@Composable
private fun AddressSummary(address: PaymentSheet.Address?) {
    if (address != null) {
        address.line1?.takeIf { it.isNotEmpty() }?.let { Text(text = it, style = MaterialTheme.typography.body2) }
        address.line2?.takeIf { it.isNotEmpty() }?.let { Text(text = it, style = MaterialTheme.typography.body2) }
        val cityStateZip = listOfNotNull(address.city, address.state, address.postalCode)
            .filter { it.isNotEmpty() }
            .joinToString(", ")
        if (cityStateZip.isNotEmpty()) {
            Text(text = cityStateZip, style = MaterialTheme.typography.body2)
        }
        address.country?.let { Text(text = it, style = MaterialTheme.typography.body2) }
    } else {
        Text(
            text = "No address set",
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
        )
    }
}

@Composable
private fun AddressModePicker(
    useFullAddress: Boolean,
    onPostalCodeMode: () -> Unit,
    onFullAddressMode: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(PADDING),
    ) {
        Button(
            onClick = onPostalCodeMode,
            enabled = useFullAddress,
            modifier = Modifier.weight(1f),
        ) {
            Text("Postal Code")
        }
        Button(
            onClick = onFullAddressMode,
            enabled = !useFullAddress,
            modifier = Modifier.weight(1f),
        ) {
            Text("Full Address")
        }
    }
}

@Composable
private fun PostalCodeInput(
    postalCode: String,
    onPostalCodeChange: (String) -> Unit,
    onApply: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PADDING),
    ) {
        OutlinedTextField(
            value = postalCode,
            onValueChange = onPostalCodeChange,
            label = { Text("Postal code") },
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        Button(
            onClick = onApply,
        ) {
            Text("Apply")
        }
    }
}

@Composable
private fun TaxIdSection(updateTaxId: (String, String) -> Unit) {
    var type by rememberSaveable { mutableStateOf("") }
    var value by rememberSaveable { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Tax ID",
            style = MaterialTheme.typography.h6,
        )

        Spacer(modifier = Modifier.height(PADDING))

        OutlinedTextField(
            value = type,
            onValueChange = { type = it },
            label = { Text("Type") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(PADDING))

        OutlinedTextField(
            value = value,
            onValueChange = { value = it },
            label = { Text("Value") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(PADDING))

        Button(
            onClick = { updateTaxId(type, value) },
        ) {
            Text("Apply")
        }

        Divider(modifier = Modifier.padding(vertical = PADDING))
    }
}

@Composable
private fun ShippingOptionsSection(session: CheckoutSession, selectShippingRate: (String) -> Unit) {
    val shippingOptions = session.shippingOptions
    if (shippingOptions.isEmpty()) return

    val selectedId = session.totalSummary?.shippingRate?.id
    val currency = session.currency

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Shipping",
            style = MaterialTheme.typography.h6,
        )

        Spacer(modifier = Modifier.height(PADDING))

        for (option in shippingOptions) {
            val isSelected = option.id == selectedId
            val amountText = if (option.amount == 0L) "Free" else formatAmount(option.amount, currency)
            val subtext = option.deliveryEstimate

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { selectShippingRate(option.id) }
                    .background(
                        if (isSelected) {
                            MaterialTheme.colors.primary.copy(alpha = 0.1f)
                        } else {
                            Color.Transparent
                        }
                    )
                    .padding(vertical = PADDING / 2),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = option.displayName,
                        style = if (isSelected) {
                            MaterialTheme.typography.subtitle1
                        } else {
                            MaterialTheme.typography.body2
                        },
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
                    text = amountText,
                    style = if (isSelected) {
                        MaterialTheme.typography.subtitle1
                    } else {
                        MaterialTheme.typography.body2
                    },
                )
            }
        }

        Divider(modifier = Modifier.padding(vertical = PADDING))
    }
}

@Composable
private fun TotalSummary(session: CheckoutSession, removePromotionCode: () -> Unit) {
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

        DiscountRows(summary.discountAmounts, currency, removePromotionCode)
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
private fun DiscountRows(
    discounts: List<CheckoutSession.DiscountAmount>,
    currency: String,
    removePromotionCode: () -> Unit,
) {
    for (discount in discounts) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
            ) {
                SummaryRow(
                    label = discount.displayName,
                    amount = "-${formatAmount(discount.amount, currency)}",
                )
            }
            IconButton(onClick = removePromotionCode) {
                Text("✕")
            }
        }
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
