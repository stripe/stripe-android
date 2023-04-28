package com.stripe.android.paymentsheet.example.samples.ui.shared

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.example.R
import com.stripe.android.paymentsheet.example.samples.ui.MAIN_FONT_SIZE

@Composable
fun ShippingAddressButton(
    addressButtonEnabled: Boolean,
    onClick: () -> Unit
) {
    TextButton(
        enabled = addressButtonEnabled,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        onClick = onClick,
        colors = ButtonDefaults.textButtonColors(
            backgroundColor = MaterialTheme.colors.primary,
            contentColor = MaterialTheme.colors.onPrimary,
        )
    ) {
        Text(
            stringResource(R.string.add_shipping_address),
            modifier = Modifier.padding(vertical = 2.dp),
            fontSize = MAIN_FONT_SIZE
        )
    }
}

@Composable
fun ShippingAddressLabel(address: AddressDetails?) {
    Text(
        text = buildString {
            val shippingAddress = address?.address
            append("${stringResource(id = R.string.ship_to)}\n")
            address?.name?.takeIf { it.isNotEmpty() }?.let { append("$it, ") }
            shippingAddress?.line1?.takeIf { it.isNotEmpty() }?.let { append("$it, ") }
            shippingAddress?.line2?.takeIf { it.isNotEmpty() }?.let { append("$it, ") }
            shippingAddress?.city?.takeIf { it.isNotEmpty() }?.let { append("$it, ") }
            shippingAddress?.state?.takeIf { it.isNotEmpty() }?.let { append("$it, ") }
            shippingAddress?.country?.takeIf { it.isNotEmpty() }?.let { append("$it, ") }
            shippingAddress?.postalCode?.takeIf { it.isNotEmpty() }?.let { append(it) }
        }
    )
}
