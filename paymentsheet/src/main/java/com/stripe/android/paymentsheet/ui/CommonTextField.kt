package com.stripe.android.paymentsheet.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.input.VisualTransformation
import com.stripe.android.uicore.LocalTextFieldInsets
import com.stripe.android.uicore.elements.FieldDisplayState
import com.stripe.android.uicore.elements.TextFieldColors
import com.stripe.android.uicore.elements.compat.CompatTextField
import com.stripe.android.uicore.elements.compat.StripeTextFieldColors
import com.stripe.android.uicore.stripeColorScheme

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
    colors: StripeTextFieldColors = commonTextFieldColors(
        shouldShowError = shouldShowError,
        enabled = enabled
    ),
) {
    val textFieldInsets = LocalTextFieldInsets.current

    CompatTextField(
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
        errorMessage = null,
        contentPadding = textFieldInsets.asPaddingValues()
    )
}

@Composable
private fun Label(
    text: String,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium
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
): StripeTextFieldColors {
    return TextFieldColors(
        fieldDisplayState = when (shouldShowError) {
            true -> FieldDisplayState.ERROR
            false -> FieldDisplayState.NORMAL
        },
        containerColor = if (enabled) {
            MaterialTheme.stripeColorScheme.component
        } else {
            disabledBackgroundColor()
        }
    )
}
