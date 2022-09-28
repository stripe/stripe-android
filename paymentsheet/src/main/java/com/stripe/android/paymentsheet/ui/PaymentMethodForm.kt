package com.stripe.android.paymentsheet.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSavedStateRegistryOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stripe.android.paymentsheet.forms.Form
import com.stripe.android.paymentsheet.forms.FormFieldValues
import com.stripe.android.paymentsheet.forms.FormViewModel
import com.stripe.android.paymentsheet.paymentdatacollection.FormFragmentArguments

@Composable
internal fun PaymentMethodForm(
    args: FormFragmentArguments,
    enabled: Boolean,
    onFormFieldValuesChanged: (FormFieldValues?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val formViewModel: FormViewModel = viewModel(
        key = args.paymentMethodCode,
        factory = FormViewModel.Factory(
            config = args,
            contextSupplier = { context },
            owner = LocalSavedStateRegistryOwner.current
        )
    )

    val formValues by formViewModel.completeFormValues.collectAsState(null)

    LaunchedEffect(formValues) {
        onFormFieldValuesChanged(formValues)
    }

    LaunchedEffect(enabled) {
        formViewModel.setEnabled(enabled)
    }

    Form(
        formViewModel = formViewModel,
        modifier = modifier
    )
}
