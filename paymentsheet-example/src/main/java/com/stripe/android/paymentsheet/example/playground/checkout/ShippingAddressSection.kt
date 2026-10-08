@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)

package com.stripe.android.paymentsheet.example.playground.checkout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.stripe.android.checkout.CheckoutController.Session

@Composable
internal fun ShippingAddressSection(shippingAddress: Session.ShippingAddress?) {
    val addressFields = shippingAddress?.let { shipping ->
        val address = shipping.address
        listOf(
            "Name" to shipping.name,
            "Line 1" to address.line1,
            "Line 2" to address.line2,
            "City" to address.city,
            "State" to address.state,
            "Postal code" to address.postalCode,
            "Country" to address.country,
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
    ) {
        Text("Shipping address", style = MaterialTheme.typography.h6)
        if (addressFields == null) {
            Text("No shipping address")
        } else {
            addressFields.forEach { (label, value) ->
                if (!value.isNullOrBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = label,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.body2,
                        )
                        Text(
                            text = value,
                            modifier = Modifier.weight(2f),
                            style = MaterialTheme.typography.body2,
                            textAlign = TextAlign.End,
                        )
                    }
                }
            }
        }
    }
}
