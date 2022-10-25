package com.stripe.android.paymentsheet.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stripe.android.core.injection.NonFallbackInjector
import com.stripe.android.paymentsheet.forms.FormFieldValues
import com.stripe.android.paymentsheet.forms.FormViewModel
import com.stripe.android.paymentsheet.paymentdatacollection.FormFragmentArguments
import com.stripe.android.ui.core.FormUI
import com.stripe.android.ui.core.injection.NonFallbackInjector
import kotlinx.coroutines.FlowPreview

@FlowPreview
@Composable
internal fun PaymentMethodForm(
    args: FormFragmentArguments,
    enabled: Boolean,
    showCheckbox: Boolean,
    onFormFieldValuesChanged: (FormFieldValues?) -> Unit,
    injector: NonFallbackInjector,
    modifier: Modifier = Modifier
) {
    val formViewModel: FormViewModel = viewModel(
        key = args.paymentMethodCode,
        factory = FormViewModel.Factory(
            config = args,
            injector = injector
        )
    )

    formViewModel.setShowCheckbox(showCheckbox)

    val formValues by formViewModel.completeFormValues.collectAsState(null)

    LaunchedEffect(formValues) {
        onFormFieldValuesChanged(formValues)
    }

    val hiddenIdentifiers by formViewModel.hiddenIdentifiers.collectAsState(emptySet())
    val elements by formViewModel.elementsFlow.collectAsState(null)
    val lastTextFieldIdentifier by formViewModel.lastTextFieldIdentifier.collectAsState(null)

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
