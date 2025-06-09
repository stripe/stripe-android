package com.stripe.android.paymentsheet.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.elements.TextFieldColors
import com.stripe.android.uicore.elements.compat.CompatTextField
import com.stripe.android.uicore.stripeColors

@Composable
internal fun CommonTextField(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    onValueChange: (String) -> Unit = {},
    trailingIcon: @Composable (() -> Unit)? = null,
    shouldShowError: Boolean = false,
    enabled: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    shape: Shape =
        MaterialTheme.shapes.small.copy(bottomEnd = ZeroCornerSize, bottomStart = ZeroCornerSize),
    colors: TextFieldColors = commonTextFieldColors(
        shouldShowError = shouldShowError,
        enabled = enabled
    ),
) {
    val textFieldInsets = StripeTheme.textFieldInsets

    CompatTextField(
        modifier = modifier
            .fillMaxWidth(),
        value = TextFieldValue(value),
        enabled = enabled,
        label = {
            Label(
                text = label,
            )
        },
        trailingIcon = trailingIcon,
        shape = shape,
        colors = colors,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        visualTransformation = visualTransformation,
        onValueChange = { textFieldValue ->
            onValueChange(textFieldValue.text)
        },
        errorMessage = null,
        contentPadding = remember(textFieldInsets) {
            PaddingValues(
                start = textFieldInsets.start.dp,
                end = textFieldInsets.end.dp,
                top = textFieldInsets.top.dp,
                bottom = textFieldInsets.bottom.dp,
            )
        }
    )
}

@Composable
private fun Label(
    text: String,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.subtitle1
    )
}

@Composable
private fun disabledBackgroundColor(): Color {
    return if (isSystemInDarkTheme()) {
        Color.White.copy(alpha = 0.04f)
    } else {
        Color.Black.copy(alpha = 0.04f)
    }
}

@Composable
internal fun commonTextFieldColors(
    shouldShowError: Boolean,
    enabled: Boolean
): TextFieldColors {
    return TextFieldColors(
        shouldShowError = shouldShowError,
        backgroundColor = if (enabled) {
            MaterialTheme.stripeColors.component
        } else {
            disabledBackgroundColor()
        }
    )
}
