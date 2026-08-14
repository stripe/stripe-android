package com.stripe.android.financialconnections.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.material.ContentAlpha
import androidx.compose.material.ExposedDropdownMenuDefaults.outlinedTextFieldColors
import androidx.compose.material.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stripe.android.financialconnections.ui.FinancialConnectionsPreview
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme
import com.stripe.android.financialconnections.ui.theme.TextSelectionColors
import com.stripe.android.financialconnections.ui.theme.Theme
import com.stripe.android.financialconnections.ui.theme.isLinkDs3

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
    val isLinkDs3 = FinancialConnectionsTheme.theme.isLinkDs3

    // `TextFieldColors.backgroundColor` isn't focus-aware, so DS 3.0's focus-dependent fill has to
    // be resolved here rather than inside `outlinedTextFieldColors`.
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()

    CompositionLocalProvider(
        LocalTextSelectionColors provides TextSelectionColors,
    ) {
        OutlinedTextField(
            enabled = enabled,
            shape = shape,
            interactionSource = interactionSource,
            modifier = modifier
                .fillMaxWidth()
                .alpha(contentAlpha)
                // DS 3.0 has no shadow in either state.
                .then(if (isLinkDs3) Modifier else Modifier.shadow(1.dp, shape)),
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
                backgroundColor = textFieldBackgroundColor(isLinkDs3 = isLinkDs3, focused = focused),
                focusedBorderColor = textFieldFocusedBorderColor(isLinkDs3 = isLinkDs3),
                unfocusedBorderColor = textFieldRestingBorderColor(isLinkDs3 = isLinkDs3),
                disabledBorderColor = textFieldRestingBorderColor(isLinkDs3 = isLinkDs3),
                unfocusedLabelColor = FinancialConnectionsTheme.colors.textSubdued,
                errorBorderColor = FinancialConnectionsTheme.colors.textCritical,
                focusedLabelColor = FinancialConnectionsTheme.colors.textSubdued,
                cursorColor = FinancialConnectionsTheme.colors.textDefault,
                errorCursorColor = FinancialConnectionsTheme.colors.textCritical,
                errorLabelColor = FinancialConnectionsTheme.colors.textCritical,
                errorTrailingIconColor = FinancialConnectionsTheme.colors.textCritical,
                trailingIconColor = FinancialConnectionsTheme.colors.icon,
                focusedTrailingIconColor = FinancialConnectionsTheme.colors.icon,
            ),
            onValueChange = onValueChange,
            label = label
        )
    }
}

/** DS 3.0 fills the resting field and clears it on focus; other themes stay on the background. */
@Composable
private fun textFieldBackgroundColor(isLinkDs3: Boolean, focused: Boolean): Color = when {
    isLinkDs3.not() || focused -> FinancialConnectionsTheme.colors.background
    else -> FinancialConnectionsTheme.colors.iconBackground
}

/**
 * DS 3.0 inverts the Stripe theme: no border at rest, a thick one on focus. Material's
 * OutlinedTextField already animates 1.dp -> 2.dp internally, so a transparent resting color is all
 * that's needed to hide the border.
 */
@Composable
private fun textFieldRestingBorderColor(isLinkDs3: Boolean): Color = if (isLinkDs3) {
    Color.Transparent
} else {
    FinancialConnectionsTheme.colors.borderNeutral
}

@Composable
private fun textFieldFocusedBorderColor(isLinkDs3: Boolean): Color = if (isLinkDs3) {
    FinancialConnectionsTheme.colors.textFieldFocused
} else {
    FinancialConnectionsTheme.colors.border
}

@Preview(group = "Components", name = "TextField - Link DS 3.0 resting")
@Composable
internal fun FinancialConnectionsOutlinedTextFieldLinkDs3Preview() {
    // Resting state only: a preview can't take focus, so the focused 2.dp border and white fill
    // have to be checked on device.
    FinancialConnectionsPreview(theme = Theme.LinkDs3) {
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
