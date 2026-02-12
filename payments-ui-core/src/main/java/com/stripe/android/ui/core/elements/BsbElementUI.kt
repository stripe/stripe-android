package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.Section
import com.stripe.android.uicore.elements.TextField
import com.stripe.android.uicore.stripeColorScheme
import com.stripe.android.uicore.utils.collectAsState

@Composable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun BsbElementUI(
    enabled: Boolean,
    element: BsbElement,
    lastTextFieldIdentifier: IdentifierSpec?,
    modifier: Modifier = Modifier,
) {
    val validationMessage by element.textElement.controller.validationMessage.collectAsState()
    val bankName by element.bankName.collectAsState()
    Column {
        Section(
            null,
            validationMessage,
            modifier = modifier,
        ) {
            TextField(
                element.textElement.controller,
                enabled = enabled,
                imeAction = if (lastTextFieldIdentifier == element.identifier) {
                    ImeAction.Done
                } else {
                    ImeAction.Next
                }
            )
        }

        bankName?.let {
            Text(
                it,
                color = MaterialTheme.stripeColorScheme.subtitle
            )
        }
    }
}
