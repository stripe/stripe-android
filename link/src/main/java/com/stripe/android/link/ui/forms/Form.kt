package com.stripe.android.link.ui.forms

import androidx.compose.runtime.Composable
import com.stripe.android.ui.core.FormController
import com.stripe.android.ui.core.FormUI
import kotlinx.coroutines.flow.Flow

@Composable
internal fun Form(
    formController: FormController,
    enabledFlow: Flow<Boolean>
) {
    FormUI(
        hiddenIdentifiersFlow = formController.hiddenIdentifiers,
        enabledFlow = enabledFlow,
        elementsFlow = formController.elements,
        lastTextFieldIdentifierFlow = formController.lastTextFieldIdentifier,
    )
}
