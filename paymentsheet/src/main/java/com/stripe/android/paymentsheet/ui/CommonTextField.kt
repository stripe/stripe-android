package com.stripe.android.paymentsheet.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.input.VisualTransformation
import com.stripe.android.uicore.elements.TextFieldColors

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
    TextField(
        modifier = modifier.fillMaxWidth(),
        value = value,
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
        onValueChange = onValueChange,
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
        Color(
            red = 116f / 255f,
            green = 116f / 255f,
            blue = 128f / 255f,
            alpha = 0.18f
        )
    } else {
        Color(
            red = 248f / 255f,
            green = 248f / 255f,
            blue = 248f / 255f,
            alpha = 1f
        )
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
