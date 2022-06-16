@file:OptIn(ExperimentalMaterialApi::class)

package com.stripe.android.financialconnections.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ExposedDropdownMenuDefaults.outlinedTextFieldColors
import androidx.compose.material.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme

@Composable
internal fun FinancialConnectionsOutlinedTextField(
    value: String,
    modifier: Modifier = Modifier,
    onValueChange: (String) -> Unit,
    label: @Composable (() -> Unit)? = null,
) {
    OutlinedTextField(
        modifier = modifier.fillMaxWidth(),
        value = value,
        colors = outlinedTextFieldColors(
            focusedBorderColor = FinancialConnectionsTheme.colors.textBrand,
            unfocusedBorderColor = FinancialConnectionsTheme.colors.textSecondary,
            disabledBorderColor = FinancialConnectionsTheme.colors.textDisabled,
            unfocusedLabelColor = FinancialConnectionsTheme.colors.textSecondary,
            focusedLabelColor = FinancialConnectionsTheme.colors.textBrand,
            cursorColor = FinancialConnectionsTheme.colors.textBrand,
        ),
        onValueChange = onValueChange,
        label = label
    )
}


@Composable
@Preview(showBackground = true)
private fun FinancialConnectionsOutlinedTextFieldPreview() {
    FinancialConnectionsTheme {
        Column {
            FinancialConnectionsOutlinedTextField(value = "test", onValueChange = {})
        }
    }
}


