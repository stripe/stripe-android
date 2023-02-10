package com.stripe.android.paymentsheet.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stripe.android.core.injection.NonFallbackInjector
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.paymentsheet.forms.FormFieldValues
import com.stripe.android.paymentsheet.forms.FormViewModel
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import com.stripe.android.ui.core.FormUI
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.IdentifierSpec
import kotlinx.coroutines.flow.Flow

@Composable
internal fun PaymentMethodForm(
    args: FormArguments,
    enabled: Boolean,
    onFormFieldValuesChanged: (FormFieldValues?) -> Unit,
    showCheckboxFlow: Flow<Boolean>,
    injector: NonFallbackInjector,
    modifier: Modifier = Modifier,
) {
    val formViewModel: FormViewModel = viewModel(
        key = args.paymentMethodCode,
        factory = FormViewModel.Factory(
            config = args,
            showCheckboxFlow = showCheckboxFlow,
            injector = injector,
        )
    )

    val hiddenIdentifiers by formViewModel.hiddenIdentifiers.collectAsState(emptySet())
    val elements by formViewModel.elementsFlow.collectAsState(null)
    val lastTextFieldIdentifier by formViewModel.lastTextFieldIdentifier.collectAsState(null)

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
    elements: List<FormElement>?,
    lastTextFieldIdentifier: IdentifierSpec?,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(paymentMethodCode) {
        completeFormValues.collect {
            onFormFieldValuesChanged(it)
        }
    }

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
