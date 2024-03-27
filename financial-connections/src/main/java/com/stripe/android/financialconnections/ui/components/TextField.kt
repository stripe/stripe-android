package com.stripe.android.financialconnections.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ContentAlpha
import androidx.compose.material.ExposedDropdownMenuDefaults.outlinedTextFieldColors
import androidx.compose.material.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stripe.android.financialconnections.ui.FinancialConnectionsPreview
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme

@Composable
internal fun FinancialConnectionsOutlinedTextField(
    value: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onValueChange: (String) -> Unit,
    readOnly: Boolean = false,
    isError: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    placeholder: @Composable (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    label: @Composable (() -> Unit)? = null
) {
    val contentAlpha = if (enabled) ContentAlpha.high else ContentAlpha.disabled
    val shape = RoundedCornerShape(12.dp)
    OutlinedTextField(
        enabled = enabled,
        shape = shape,
        modifier = modifier
            .fillMaxWidth()
            .alpha(contentAlpha)
            .shadow(1.dp, shape),
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        placeholder = placeholder,
        maxLines = 1,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        readOnly = readOnly,
        isError = isError,
        value = value,
        colors = outlinedTextFieldColors(
            backgroundColor = FinancialConnectionsTheme.colors.backgroundSurface,
            focusedBorderColor = FinancialConnectionsTheme.colors.borderBrand,
            unfocusedBorderColor = FinancialConnectionsTheme.colors.border,
            disabledBorderColor = FinancialConnectionsTheme.colors.textDisabled,
            unfocusedLabelColor = FinancialConnectionsTheme.colors.textSubdued,
            errorBorderColor = FinancialConnectionsTheme.colors.textCritical,
            focusedLabelColor = FinancialConnectionsTheme.colors.textSubdued,
            cursorColor = FinancialConnectionsTheme.colors.borderBrand,
            errorCursorColor = FinancialConnectionsTheme.colors.textCritical,
            errorLabelColor = FinancialConnectionsTheme.colors.textCritical,
            errorTrailingIconColor = FinancialConnectionsTheme.colors.textCritical,
            trailingIconColor = FinancialConnectionsTheme.colors.iconDefault,
            focusedTrailingIconColor = FinancialConnectionsTheme.colors.iconDefault
        ),
        onValueChange = onValueChange,
        label = label
    )
}

@Preview(group = "Components", name = "TextField - idle")
@Composable
internal fun FinancialConnectionsOutlinedTextFieldPreview() {
    FinancialConnectionsPreview {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FinancialConnectionsOutlinedTextField(
                value = "test",
                enabled = true,
                onValueChange = {}
            )
        }
    }
}
