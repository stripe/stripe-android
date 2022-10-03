package com.stripe.android.paymentsheet.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.stripe.android.paymentsheet.forms.FormController
import com.stripe.android.ui.core.FormUI
import kotlinx.coroutines.FlowPreview

@FlowPreview
@Composable
internal fun PaymentMethodForm(
    formController: FormController,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val hiddenIdentifiers by formController.hiddenIdentifiers.collectAsState(emptySet())
    val elements by formController.elementsFlow.collectAsState(null)
    val lastTextFieldIdentifier by formController.lastTextFieldIdentifier.collectAsState(null)

    FormUI(
        hiddenIdentifiers = hiddenIdentifiers,
        enabled = enabled,
        elements = elements,
        lastTextFieldIdentifier = lastTextFieldIdentifier,
        loadingComposable = {
            Loading()
        },
        modifier = modifier
    )
}
