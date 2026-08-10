package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.stripe.android.uicore.LocalTextFieldInsets
import com.stripe.android.uicore.R
import com.stripe.android.uicore.elements.compat.CompatTextField
import com.stripe.android.uicore.strings.resolve
import com.stripe.android.uicore.utils.collectAsState

@Composable
private fun SearchIconButton(onClick: () -> Unit, enabled: Boolean) {
    IconButton(onClick = onClick, enabled = enabled) {
        Icon(
            painter = painterResource(R.drawable.stripe_ic_search),
            contentDescription = stringResource(R.string.stripe_address_search_content_description),
        )
    }
}

@Composable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun AddressTextFieldUI(
    controller: AddressTextFieldController,
    modifier: Modifier = Modifier,
    enabled: Boolean,
    onSearchActivated: (() -> Unit)?,
    onClick: () -> Unit = {
        controller.launchAutocompleteScreen()
    }
) {
    val label by controller.label.collectAsState()
    val inlineQuery by controller.inlineQuery.collectAsState()
    val error by controller.validationMessage.collectAsState()
    val textFieldInsets = LocalTextFieldInsets.current

    val isEditable = controller.isEditable
    val isError = error != null

    val fieldModifier = modifier.fillMaxWidth().then(
        if (isEditable) Modifier else Modifier.clickable(enabled = enabled) { onClick() }
    )

    CompatTextField(
        value = if (isEditable) inlineQuery else "",
        enabled = isEditable && enabled,
        onValueChange = { v -> controller.onInlineQueryChanged(v) },
        errorMessage = null,
        isError = isError,
        label = {
            FormLabel(label.resolve())
        },
        placeholder = null,
        trailingIcon = if (onSearchActivated != null) {
            {
                SearchIconButton(
                    onClick = onSearchActivated,
                    enabled = enabled,
                )
            }
        } else {
            null
        },
        singleLine = true,
        contentPadding = textFieldInsets.asPaddingValues(),
        colors = TextFieldColors(
            fieldDisplayState = if (isError) FieldDisplayState.ERROR else FieldDisplayState.NORMAL,
            disabledIndicatorColor = if (!isEditable && isError) {
                MaterialTheme.colors.error
            } else {
                Color.Transparent
            },
        ),
        modifier = fieldModifier,
    )
}
