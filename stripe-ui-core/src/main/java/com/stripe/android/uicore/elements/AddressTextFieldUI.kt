package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import com.stripe.android.uicore.LocalTextFieldInsets
import com.stripe.android.uicore.elements.compat.CompatTextField
import com.stripe.android.uicore.strings.resolve
import com.stripe.android.uicore.utils.collectAsState

@Composable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun AddressTextFieldUI(
    controller: AddressTextFieldController,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {
        controller.launchAutocompleteScreen()
    }
) {
    val label by controller.label.collectAsState()
    val error by controller.error.collectAsState()

    val textFieldInsets = LocalTextFieldInsets.current

    val isError = error != null

    CompatTextField(
        value = TextFieldValue(""),
        enabled = false,
        onValueChange = {},
        errorMessage = null,
        isError = isError,
        label = {
            FormLabel(label.resolve())
        },
        contentPadding = textFieldInsets.asPaddingValues(),
        colors = TextFieldColors(
            shouldShowError = isError,
            disabledIndicatorColor = if (isError) {
                MaterialTheme.colors.error
            } else {
                Color.Transparent
            },
        ),
        modifier = modifier
            .clickable { onClick() }
            .fillMaxWidth(),
    )
}
