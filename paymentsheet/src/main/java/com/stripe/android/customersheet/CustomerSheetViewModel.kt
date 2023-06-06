package com.stripe.android.customersheet

import android.content.res.Resources
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.stripe.android.customersheet.injection.CustomerSessionScope
import com.stripe.android.ui.core.forms.resources.LpmRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCustomerSheetApi::class)
@CustomerSessionScope
@Suppress("unused")
internal class CustomerSheetViewModel @Inject constructor(
    private val resources: Resources,
    private val configuration: CustomerSheet.Configuration,
    private val customerAdapter: CustomerAdapter,
    private val lpmRepository: LpmRepository,
) : ViewModel() {

    private val _viewState = MutableStateFlow<CustomerSheetViewState>(CustomerSheetViewState.Loading)
    val viewState: StateFlow<CustomerSheetViewState> = _viewState

    init {
        viewModelScope.launch {
            delay(DELAY)
            _viewState.update {
                CustomerSheetViewState.SelectPaymentMethod(
                    configuration.headerTextForSelectionScreen
                )
            }
        }
    }

    object Factory : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            @Suppress("UNCHECKED_CAST")
            return CustomerSessionViewModel.component.customerSheetViewModel as T
        }
    }

    private companion object {
        const val DELAY = 2000L
    }
}
