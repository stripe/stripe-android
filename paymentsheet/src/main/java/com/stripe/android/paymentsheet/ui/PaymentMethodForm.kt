package com.stripe.android.paymentsheet.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.paymentsheet.forms.FormFieldValues
import com.stripe.android.paymentsheet.forms.FormViewModel
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import com.stripe.android.ui.core.FormUI
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.utils.collectAsState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
internal fun PaymentMethodForm(
    uuid: String,
    args: FormArguments,
    enabled: Boolean,
    onFormFieldValuesChanged: (FormFieldValues?) -> Unit,
    formElements: List<FormElement>,
    modifier: Modifier = Modifier,
) {
    val formViewModel: FormViewModel = viewModel(
        key = args.paymentMethodCode + "_" + uuid,
        factory = FormViewModel.Factory(
            formElements = formElements,
            formArguments = args,
        )
    )

    val elements = formViewModel.elements
    val hiddenIdentifiers by formViewModel.hiddenIdentifiers.collectAsState()
    val lastTextFieldIdentifier by formViewModel.lastTextFieldIdentifier.collectAsState()

    PaymentMethodForm(
        paymentMethodCode = args.paymentMethodCode,
        enabled = enabled,
        onFormFieldValuesChanged = onFormFieldValuesChanged,
        completeFormValues = formViewModel.completeFormValues,
        hiddenIdentifiers = hiddenIdentifiers,
        elements = elements,
        lastTextFieldIdentifier = lastTextFieldIdentifier,
        modifier = modifier,
    )
}

@Composable
internal fun PaymentMethodForm(
    paymentMethodCode: PaymentMethodCode,
    enabled: Boolean,
    onFormFieldValuesChanged: (FormFieldValues?) -> Unit,
    completeFormValues: Flow<FormFieldValues?>,
    hiddenIdentifiers: Set<IdentifierSpec>,
    elements: List<FormElement>,
    lastTextFieldIdentifier: IdentifierSpec?,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(paymentMethodCode) {
        completeFormValues.distinctUntilChanged().collect {
            onFormFieldValuesChanged(it)
        }
    }

    FormUI(
        hiddenIdentifiers = hiddenIdentifiers,
        enabled = enabled,
        elements = elements,
        lastTextFieldIdentifier = lastTextFieldIdentifier,
        modifier = modifier
    )
}
