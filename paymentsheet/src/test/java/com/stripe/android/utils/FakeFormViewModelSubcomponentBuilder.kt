package com.stripe.android.utils

import android.content.Context
import com.stripe.android.paymentsheet.forms.FormViewModel
import com.stripe.android.paymentsheet.injection.FormViewModelSubcomponent
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import com.stripe.android.ui.core.forms.resources.LpmRepository
import com.stripe.android.uicore.address.AddressRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import javax.inject.Provider

internal fun formViewModelSubcomponentBuilder(
    context: Context,
    lpmRepository: LpmRepository,
): Provider<FormViewModelSubcomponent.Builder> {
    val formViewModelProvider: (FormArguments, Flow<Boolean>) -> FormViewModel = { args, showCheckbox ->
        FormViewModel(
            context = context,
            formArguments = args,
            lpmRepository = lpmRepository,
            addressRepository = AddressRepository(context.resources, Dispatchers.Unconfined),
            showCheckboxFlow = showCheckbox,
        )
    }

    val mockFormSubComponentBuilderProvider = mock<Provider<FormViewModelSubcomponent.Builder>>()

    val mockFormBuilder = object : FormViewModelSubcomponent.Builder {

        private lateinit var formArguments: FormArguments
        private lateinit var showCheckboxFlow: Flow<Boolean>

        override fun formArguments(args: FormArguments): FormViewModelSubcomponent.Builder {
            formArguments = args
            return this
        }

        override fun showCheckboxFlow(
            saveForFutureUseVisibleFlow: Flow<Boolean>,
        ): FormViewModelSubcomponent.Builder {
            showCheckboxFlow = saveForFutureUseVisibleFlow
            return this
        }

        override fun build(): FormViewModelSubcomponent {
            return mock {
                on { viewModel } doReturn formViewModelProvider(formArguments, showCheckboxFlow)
            }
        }
    }

    whenever(mockFormSubComponentBuilderProvider.get()).thenReturn(mockFormBuilder)
    return mockFormSubComponentBuilderProvider
}
