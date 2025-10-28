package com.stripe.android.crypto.onramp.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.stripe.android.link.LinkAppearance
import com.stripe.android.paymentsheet.PaymentSheet
import androidx.compose.material.Card
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.Icon
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.foundation.isSystemInDarkTheme

@Composable
internal fun KYCRefreshScreen(
    appearance: LinkAppearance,
    updatedAddress: PaymentSheet.Address? = null
) {
    var name by remember { mutableStateOf("Satoshi Nakamoto") }
    var dob by remember { mutableStateOf("10/06/1995") }
    var ssnLast4 by remember { mutableStateOf("5678") }
    var address by remember { mutableStateOf("2930 E 2nd Ave, Denver, CO, United States of America, 80206") }

    OnrampExampleTheme {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = "Confirm your information",
                style = MaterialTheme.typography.h5.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colors.onSurface,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            Card(
                shape = RoundedCornerShape(16.dp),
                elevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    InfoRow(title = "Name", value = name)
                    Divider()
                    InfoRow(title = "Date of Birth", value = dob)
                    Divider()
                    InfoRow(title = "Last 4 digits of SSN", value = ssnLast4)
                    Divider()
                    AddressRow(title = "Address", value = address, onEdit = {  })
                }
            }
        }
    }
}

@Composable
private fun InfoRow(title: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.subtitle2,
            color = MaterialTheme.colors.onBackground
        )
        Text(
            text = value,
            style = MaterialTheme.typography.body1,
            color = MaterialTheme.colors.onSurface
        )
    }
}

@Composable
private fun AddressRow(title: String, value: String, onEdit: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.subtitle2,
                color = MaterialTheme.colors.onBackground
            )
            Text(
                text = value,
                style = MaterialTheme.typography.body1,
                color = MaterialTheme.colors.onSurface
            )
        }
        IconButton(onClick = onEdit) {
            Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Address")
        }
    }
}

@Composable
fun OnrampExampleTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colors = if (isSystemInDarkTheme()) darkColors() else lightColors(),
        content = content,
    )
}
