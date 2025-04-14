package com.stripe.android.paymentsheet.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import com.stripe.android.uicore.elements.SectionElementUI
import com.stripe.android.uicore.utils.collectAsState
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
internal fun BillingDetailsFormUI(
    form: BillingDetailsForm,
    onValuesChanged: (BillingDetailsFormState) -> Unit
) {
    val hiddenIdentifiers by form.hiddenElements.collectAsState()

    SectionElementUI(
        enabled = true,
        element = form.addressSectionElement,
        hiddenIdentifiers = hiddenIdentifiers,
        lastTextFieldIdentifier = null
    )

    LaunchedEffect(Unit) {
        form.formFieldsState.distinctUntilChanged().collectLatest {
            onValuesChanged(it)
        }
    }
}
