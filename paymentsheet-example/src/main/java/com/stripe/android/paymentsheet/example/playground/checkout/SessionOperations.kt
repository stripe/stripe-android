package com.stripe.android.paymentsheet.example.playground.checkout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.unit.dp

@Composable
internal fun SessionOperations(
    initialEmail: String,
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
    message?.let {
        Text(text = it, color = MaterialTheme.colors.secondary)
    }
}
