@file:OptIn(ExperimentalMaterialApi::class)
@file:Suppress("ktlint:filename")

package com.stripe.android.financialconnections.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ExposedDropdownMenuDefaults.outlinedTextFieldColors
import androidx.compose.material.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stripe.android.financialconnections.ui.FinancialConnectionsPreview
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme

@Composable
internal fun FinancialConnectionsOutlinedTextField(
    value: String,
    modifier: Modifier = Modifier,
    onValueChange: (String) -> Unit,
    readOnly: Boolean = false,
    isError: Boolean = false,
    placeholder: @Composable (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    label: @Composable (() -> Unit)? = null
) {
    OutlinedTextField(
        shape = RoundedCornerShape(8.dp),
        modifier = modifier.fillMaxWidth(),
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        placeholder = placeholder,
        visualTransformation = visualTransformation,
        readOnly = readOnly,
        isError = isError,
        value = value,
        colors = outlinedTextFieldColors(
            focusedBorderColor = FinancialConnectionsTheme.colors.textBrand,
            unfocusedBorderColor = FinancialConnectionsTheme.colors.borderDefault,
            disabledBorderColor = FinancialConnectionsTheme.colors.textDisabled,
            unfocusedLabelColor = FinancialConnectionsTheme.colors.textSecondary,
            errorBorderColor = FinancialConnectionsTheme.colors.textCritical,
            focusedLabelColor = FinancialConnectionsTheme.colors.textBrand,
            cursorColor = FinancialConnectionsTheme.colors.textBrand,
            errorCursorColor = FinancialConnectionsTheme.colors.textCritical,
            errorLabelColor = FinancialConnectionsTheme.colors.textCritical,
            errorTrailingIconColor = FinancialConnectionsTheme.colors.textCritical,
            trailingIconColor = FinancialConnectionsTheme.colors.borderDefault,
            focusedTrailingIconColor = FinancialConnectionsTheme.colors.borderDefault
        ),
        onValueChange = onValueChange,
        label = label
    )
}

@Composable
@Preview(group = "Components", name = "TextField - idle")
internal fun FinancialConnectionsOutlinedTextFieldPreview() {
    FinancialConnectionsPreview {
        Column {
            FinancialConnectionsOutlinedTextField(value = "test", onValueChange = {})
        }
    }
}
