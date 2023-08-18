package com.stripe.android.customersheet

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.paymentsheet.forms.FormViewModel
import com.stripe.android.paymentsheet.injection.FormViewModelSubcomponent
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import javax.inject.Provider

@OptIn(ExperimentalCustomerSheetApi::class)
internal fun ViewModel.buildFormObserver(
    paymentMethodCode: PaymentMethodCode,
    application: Application,
    configuration: CustomerSheet.Configuration,
    formViewModelSubcomponentBuilderProvider: Provider<FormViewModelSubcomponent.Builder>,
    onFormDataUpdated: (data: FormViewModel.ViewData) -> Unit
): () -> Unit {
    val formArguments = FormArguments(
        paymentMethodCode = paymentMethodCode,
        showCheckbox = false,
        showCheckboxControlledFields = false,
        merchantName = configuration.merchantDisplayName
            ?: application.applicationInfo.loadLabel(application.packageManager).toString(),
        billingDetails = configuration.defaultBillingDetails,
        billingDetailsCollectionConfiguration = configuration.billingDetailsCollectionConfiguration
    )

    val formViewModel = formViewModelSubcomponentBuilderProvider.get()
        .formArguments(formArguments)
        .showCheckboxFlow(flowOf(false))
        .build()
        .viewModel

    return {
        viewModelScope.launch {
            formViewModel.viewDataFlow.collect { data ->
                onFormDataUpdated(data)
            }
        }
    }
}
