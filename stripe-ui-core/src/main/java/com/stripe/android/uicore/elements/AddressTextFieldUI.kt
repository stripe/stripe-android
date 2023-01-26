package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction

@Composable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun AddressTextFieldUI(
    controller: AddressTextFieldController,
    onClick: () -> Unit = {
        controller.launchAutocompleteScreen()
    }
) {
    TextField(
        textFieldController = controller,
        imeAction = ImeAction.Next,
        enabled = false,
        modifier = Modifier.clickable {
            onClick()
        }
    )
}
