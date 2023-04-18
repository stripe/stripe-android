@file:OptIn(ExperimentalMaterialApi::class)
@file:Suppress("ktlint:filename")

package com.stripe.android.financialconnections.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ExposedDropdownMenuDefaults.outlinedTextFieldColors
import androidx.compose.material.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stripe.android.financialconnections.ui.FinancialConnectionsPreview
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme

@Composable
internal fun FinancialConnectionsOutlinedTextField(
    value: TextFieldValue,
    modifier: Modifier = Modifier,
    onValueChange: (TextFieldValue) -> Unit,
    readOnly: Boolean = false,
    isError: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
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
        keyboardOptions = keyboardOptions,
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

internal fun TextFieldValue.filtered(predicate: (Char) -> Boolean): TextFieldValue = copy(
    text = text.filter(predicate),
    selection = selection.adjustForFilter(text, predicate),
    composition = composition?.adjustForFilter(text, predicate),
)

private fun TextRange.adjustForFilter(
    text: String,
    predicate: (Char) -> Boolean
): TextRange = TextRange(
    start = text.subSequence(0, start).count(predicate),
    end = text.subSequence(0, end).count(predicate),
)

@Preview(group = "Components", name = "TextField - idle")
@Composable
internal fun FinancialConnectionsOutlinedTextFieldPreview() {
    FinancialConnectionsPreview {
        Column {
            FinancialConnectionsOutlinedTextField(value = TextFieldValue("test"), onValueChange = {})
        }
    }
}
