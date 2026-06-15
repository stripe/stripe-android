package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.stripe.android.uicore.LocalTextFieldInsets
import com.stripe.android.uicore.elements.compat.CompatTextField
import com.stripe.android.uicore.strings.resolve
import com.stripe.android.uicore.utils.collectAsState

@Composable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun AddressTextFieldUI(
    controller: AddressTextFieldController,
    modifier: Modifier = Modifier,
    enabled: Boolean,
    onClick: () -> Unit = {
        controller.launchAutocompleteScreen()
    }
) {
    val label by controller.label.collectAsState()
    val textFieldState by controller.textFieldState.collectAsState()
    val textFieldInsets = LocalTextFieldInsets.current

    val fieldModifier = remember(textFieldState.isEditable, enabled) {
        modifier.fillMaxWidth().then(
            if (textFieldState.isEditable) Modifier else Modifier.clickable(enabled = enabled) { onClick() }
        )
    }

    CompatTextField(
        value = textFieldState.value,
        enabled = textFieldState.isEditable && enabled,
        onValueChange = { v -> controller.onInlineQueryChanged(v) },
        errorMessage = null,
        isError = textFieldState.fieldDisplayState == FieldDisplayState.ERROR,
        label = {
            FormLabel(label.resolve())
        },
        placeholder = null,
        trailingIcon = null,
        singleLine = true,
        contentPadding = textFieldInsets.asPaddingValues(),
        colors = TextFieldColors(
            fieldDisplayState = textFieldState.fieldDisplayState,
            disabledIndicatorColor = if (textFieldState.showDisabledErrorIndicator) {
                MaterialTheme.colors.error
            } else {
                Color.Transparent
            },
        ),
        modifier = fieldModifier,
    )
}
