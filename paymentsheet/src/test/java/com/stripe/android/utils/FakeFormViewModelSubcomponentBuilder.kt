package com.stripe.android.utils

import android.content.Context
import com.stripe.android.paymentsheet.forms.FormViewModel
import com.stripe.android.paymentsheet.injection.FormViewModelSubcomponent
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import com.stripe.android.ui.core.forms.resources.LpmRepository
import com.stripe.android.uicore.address.AddressRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import javax.inject.Provider

internal fun formViewModelSubcomponentBuilder(
    context: Context,
    lpmRepository: LpmRepository,
): Provider<FormViewModelSubcomponent.Builder> {
    val formViewModelProvider: (FormArguments, StateFlow<Boolean>, StateFlow<Boolean>) -> FormViewModel = { args, showCheckbox, processingWithLinkFlow ->
        FormViewModel(
            context = context,
            formArguments = args,
            lpmRepository = lpmRepository,
            addressRepository = AddressRepository(context.resources, Dispatchers.Unconfined),
            showCheckboxFlow = showCheckbox,
            processingWithLinkFlow = processingWithLinkFlow,
        )
    }

    val mockFormSubComponentBuilderProvider = mock<Provider<FormViewModelSubcomponent.Builder>>()

    val mockFormBuilder = object : FormViewModelSubcomponent.Builder {

        private lateinit var formArguments: FormArguments
        private lateinit var showCheckboxFlow: StateFlow<Boolean>
        private lateinit var processingWithLinkFlow: StateFlow<Boolean>

        override fun formArguments(args: FormArguments): FormViewModelSubcomponent.Builder {
            formArguments = args
            return this
        }

        override fun showCheckboxFlow(
            saveForFutureUseVisibleFlow: StateFlow<Boolean>,
        ): FormViewModelSubcomponent.Builder {
            showCheckboxFlow = saveForFutureUseVisibleFlow
            return this
        }

        override fun processingWithLinkFlow(
            processingWithLinkFlow: StateFlow<Boolean>,
        ): FormViewModelSubcomponent.Builder {
            this.processingWithLinkFlow = processingWithLinkFlow
            return this
        }

        override fun build(): FormViewModelSubcomponent {
            return mock {
                on { viewModel } doReturn formViewModelProvider(formArguments, showCheckboxFlow, processingWithLinkFlow)
            }
        }
    }

    whenever(mockFormSubComponentBuilderProvider.get()).thenReturn(mockFormBuilder)
    return mockFormSubComponentBuilderProvider
}
