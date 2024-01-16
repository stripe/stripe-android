@file:OptIn(ExperimentalMaterialApi::class)
@file:Suppress("ktlint:filename")

package com.stripe.android.financialconnections.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ExposedDropdownMenuDefaults.outlinedTextFieldColors
import androidx.compose.material.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
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
    val shape = RoundedCornerShape(12.dp)
    OutlinedTextField(
        shape = shape,
        modifier = modifier
            .fillMaxWidth()
            .shadow(1.dp, shape),
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        placeholder = placeholder,
        maxLines = 1,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        readOnly = readOnly,
        isError = isError,
        value = value,
        colors = outlinedTextFieldColors(
            backgroundColor = FinancialConnectionsTheme.v3Colors.backgroundSurface,
            focusedBorderColor = FinancialConnectionsTheme.v3Colors.borderBrand,
            unfocusedBorderColor = FinancialConnectionsTheme.v3Colors.border,
            disabledBorderColor = FinancialConnectionsTheme.v3Colors.textDisabled,
            unfocusedLabelColor = FinancialConnectionsTheme.v3Colors.textSubdued,
            errorBorderColor = FinancialConnectionsTheme.v3Colors.textCritical,
            focusedLabelColor = FinancialConnectionsTheme.v3Colors.textSubdued,
            cursorColor = FinancialConnectionsTheme.v3Colors.borderBrand,
            errorCursorColor = FinancialConnectionsTheme.v3Colors.textCritical,
            errorLabelColor = FinancialConnectionsTheme.v3Colors.textCritical,
            errorTrailingIconColor = FinancialConnectionsTheme.v3Colors.textCritical,
            trailingIconColor = FinancialConnectionsTheme.v3Colors.iconDefault,
            focusedTrailingIconColor = FinancialConnectionsTheme.v3Colors.iconDefault
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
        FinancialConnectionsScaffold(
            topBar = { FinancialConnectionsTopAppBar { } },
            content = {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FinancialConnectionsOutlinedTextField(
                        value = TextFieldValue("test"),
                        onValueChange = {}
                    )
                }
            }
        )
    }
}
