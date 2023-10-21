package com.stripe.android.customersheet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stripe.android.paymentsheet.forms.FormViewModel
import com.stripe.android.paymentsheet.injection.FormViewModelSubcomponent
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import javax.inject.Provider

internal fun ViewModel.buildFormObserver(
    formArguments: FormArguments,
    formViewModelSubcomponentBuilderProvider: Provider<FormViewModelSubcomponent.Builder>,
    onFormDataUpdated: (data: FormViewModel.ViewData) -> Unit
): () -> Unit {
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
