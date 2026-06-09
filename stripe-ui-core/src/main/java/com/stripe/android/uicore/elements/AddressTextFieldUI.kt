package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
    val error by controller.validationMessage.collectAsState()

    val textFieldInsets = LocalTextFieldInsets.current

    val isError = error != null
    val isInline = controller.inlinePredictionsState != null
    val query by controller.inlineQuery.collectAsState()

    val fieldModifier = if (isInline) {
        modifier.fillMaxWidth()
    } else {
        modifier.fillMaxWidth().clickable(enabled = enabled) { onClick() }
    }

    CompatTextField(
        value = if (isInline) query else "",
        enabled = if (isInline) enabled else false,
        onValueChange = { v -> if (isInline) controller.onInlineQueryChanged(v) },
        errorMessage = null,
        isError = isError,
        label = {
            FormLabel(label.resolve())
        },
        placeholder = null,
        trailingIcon = null,
        singleLine = true,
        contentPadding = textFieldInsets.asPaddingValues(),
        colors = TextFieldColors(
            fieldDisplayState = if (isError) FieldDisplayState.ERROR else FieldDisplayState.NORMAL,
            disabledIndicatorColor = if (!isInline && isError) MaterialTheme.colors.error else Color.Transparent,
        ),
        modifier = fieldModifier,
    )
}
