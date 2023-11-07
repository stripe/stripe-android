package com.stripe.elements_ui

import androidx.annotation.RestrictTo
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.TextField as MaterialTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.VisualTransformation

@Composable
fun TextField(
    value: String,
    enabled: Boolean,
    label: String?,
    placeholder: String?,
    trailingIcon: (@Composable () -> Unit)?,
    isError: Boolean,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions(),
    onValueChange: (value: String) -> Unit = {},
) {
    val colors = TextFieldColors(isError)

    MaterialTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        label = label?.let {
            {
                Label(
                    value = it,
                    enabled = enabled
                )
            }
        },
        placeholder = placeholder?.let {
            {
                Label(
                    value = it,
                    enabled = true
                )
            }
        },
        trailingIcon = trailingIcon,
        isError = isError,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = true,
        colors = colors
    )
}

@Composable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
private fun TextFieldColors(
    isError: Boolean = false
) = TextFieldDefaults.textFieldColors(
    textColor = if (isError) {
        ElementsTheme.colors.error
    } else {
        ElementsTheme.colors.onComponent
    },
    unfocusedLabelColor = ElementsTheme.colors.placeholder,
    focusedLabelColor = ElementsTheme.colors.placeholder,
    placeholderColor = ElementsTheme.colors.placeholder,
    backgroundColor = ElementsTheme.colors.component,
    focusedIndicatorColor = Color.Transparent,
    disabledIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    cursorColor = ElementsTheme.colors.textCursor
)

@Composable
internal fun Label(
    value: String,
    enabled: Boolean
) {
    val color = ElementsTheme.colors.placeholder

    Text(
        text = value,
        color = if (enabled) {
            color
        } else {
            color.copy(alpha = ContentAlpha.disabled)
        },
        style = MaterialTheme.typography.subtitle1
    )
}
