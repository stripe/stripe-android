package com.stripe.elements_ui

import androidx.annotation.RestrictTo
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
import com.stripe.android.uicore.elements.AnimatedIcons
import com.stripe.android.uicore.elements.TextFieldColors
import com.stripe.android.uicore.elements.TextFieldIcon
import com.stripe.android.uicore.stripeColors

@Composable
fun TextField(
    value: String,
    enabled: Boolean,
    loading: Boolean,
    label: String?,
    placeholder: String?,
    trailingIcon: TextFieldIcon?,
    showOptionalLabel: Boolean,
    shouldShowError: Boolean,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions(),
    onValueChange: (value: String) -> Unit = {},
    onDropdownItemClicked: (item: TextFieldIcon.Dropdown.Item) -> Unit = {}
) {
    val colors = TextFieldColors(shouldShowError)

    MaterialTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        label = label?.let { label ->
            {
                Label(
                    value = label,
                    enabled = enabled
                )
            }
        },
        placeholder = placeholder?.let { placeholder ->
            {
                Label(
                    value = placeholder,
                    enabled = true
                )
            }
        },
        trailingIcon = trailingIcon?.let {
            {
                Row {
                    when (it) {
                        is TextFieldIcon.Trailing -> {
                            TrailingIcon(it, loading)
                        }

                        is TextFieldIcon.MultiTrailing -> {
                            Row(modifier = Modifier.padding(10.dp)) {
                                it.staticIcons.forEach {
                                    TrailingIcon(it, loading)
                                }
                                AnimatedIcons(icons = it.animatedIcons, loading = loading)
                            }
                        }

                        is TextFieldIcon.Dropdown -> {
                            TrailingDropdown(
                                icon = it,
                                loading = loading,
                                onDropdownItemClicked = onDropdownItemClicked
                            )
                        }
                    }
                }
            }
        },
        isError = shouldShowError,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = true,
        colors = colors
    )
}

@Composable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun TextFieldColors(
    shouldShowError: Boolean = false
) = TextFieldDefaults.textFieldColors(
    textColor = if (shouldShowError) {
        MaterialTheme.colors.error
    } else {
        MaterialTheme.stripeColors.onComponent
    },
    unfocusedLabelColor = MaterialTheme.stripeColors.placeholderText,
    focusedLabelColor = MaterialTheme.stripeColors.placeholderText,
    placeholderColor = MaterialTheme.stripeColors.placeholderText,
    backgroundColor = MaterialTheme.stripeColors.component,
    focusedIndicatorColor = Color.Transparent,
    disabledIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    cursorColor = MaterialTheme.stripeColors.textCursor
)

@Composable
private fun Label(
    value: String,
    enabled: Boolean
) {
    val color = MaterialTheme.stripeColors.placeholderText

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
