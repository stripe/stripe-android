package com.stripe.android.crypto.onramp.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.stripe.android.paymentsheet.PaymentSheet

@Composable
@Suppress("LongMethod")
internal fun AddressForm(
    onSubmit: (PaymentSheet.Address) -> Unit,
    onDismiss: () -> Unit
) {
    var values by remember {
        mutableStateOf(ADDRESS_FIELDS.associate { it.key to "" })
    }
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .imePadding()
    ) {
        ADDRESS_FIELDS.forEach { field ->
            OutlinedTextField(
                value = values[field.key].orEmpty(),
                onValueChange = { updatedValue ->
                    values = values.toMutableMap().apply {
                        set(field.key, updatedValue)
                    }
                },
                label = { Text(field.label) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .onPreviewKeyEvent {
                        if (it.key != Key.Tab) {
                            return@onPreviewKeyEvent false
                        }

                        focusManager.moveFocus(FocusDirection.Next)
                        true
                    }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    onSubmit(
                        PaymentSheet.Address(
                            line1 = values["line1"],
                            line2 = values["line2"],
                            city = values["city"],
                            state = values["state"],
                            country = values["country"],
                            postalCode = values["postalCode"]
                        )
                    )
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Submit")
            }

            TextButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancel")
            }
        }
    }
}

private data class AddressField(
    val key: String,
    val label: String
)

private val ADDRESS_FIELDS = listOf(
    AddressField(key = "line1", label = "Address Line 1"),
    AddressField(key = "line2", label = "Address Line 2"),
    AddressField(key = "city", label = "City"),
    AddressField(key = "state", label = "State"),
    AddressField(key = "country", label = "Country"),
    AddressField(key = "postalCode", label = "Postal Code")
)
