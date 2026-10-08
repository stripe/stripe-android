@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)

package com.stripe.android.paymentsheet.example.playground.checkout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.stripe.android.checkout.CheckoutController.Session

internal const val SHIPPING_ADDRESS_TEST_TAG = "shipping_address"

@Composable
internal fun SessionOperations(
    initialEmail: String,
    shippingAddress: Session.ShippingAddress?,
    isUpdating: Boolean,
    message: String?,
    onApplyPromotionCode: (String) -> Unit,
    onRemovePromotionCode: () -> Unit,
    onUpdateEmail: (String) -> Unit,
) {
    var promotionCode by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable(initialEmail) { mutableStateOf(initialEmail) }
    Text("Session controls", style = MaterialTheme.typography.h6)
    OutlinedTextField(
        value = promotionCode,
        onValueChange = { promotionCode = it },
        label = { Text("Promotion code") },
        modifier = Modifier.fillMaxWidth(),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = { onApplyPromotionCode(promotionCode) },
            enabled = !isUpdating && promotionCode.isNotBlank(),
        ) { Text("Apply") }
        OutlinedButton(onClick = onRemovePromotionCode, enabled = !isUpdating) { Text("Remove") }
    }
    OutlinedTextField(
        value = email,
        onValueChange = { email = it },
        label = { Text("Email") },
        modifier = Modifier.fillMaxWidth(),
    )
    Button(
        onClick = { onUpdateEmail(email) },
        enabled = !isUpdating,
        modifier = Modifier.fillMaxWidth(),
    ) { Text("Update email") }
    ShippingAddressSection(shippingAddress)
    message?.let {
        Text(text = it, color = MaterialTheme.colors.secondary)
    }
}

@Composable
private fun ShippingAddressSection(shippingAddress: Session.ShippingAddress?) {
    val addressLines = shippingAddress?.let { shipping ->
        val address = shipping.address
        listOf(
            shipping.name,
            address.line1,
            address.line2,
            listOf(address.city, address.state, address.postalCode)
                .filterNot { it.isNullOrBlank() }
                .joinToString(" "),
            address.country,
        ).filterNot { it.isNullOrBlank() }.joinToString("\n")
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .semantics(mergeDescendants = true) {}
            .testTag(SHIPPING_ADDRESS_TEST_TAG),
    ) {
        Text("Shipping address", style = MaterialTheme.typography.h6)
        Text(addressLines ?: "No shipping address")
    }
}
