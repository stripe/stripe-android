package com.stripe.android.paymentsheet

import androidx.lifecycle.SavedStateHandle
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel

internal object TestViewModelFactory {
    fun <T : BaseSheetViewModel> create(
        viewModelFactory: (savedStateHandle: SavedStateHandle) -> T
    ): T {
        lateinit var viewModel: T
        val savedStateHandle = SavedStateHandle()
        viewModel = viewModelFactory(savedStateHandle)
        return viewModel
    }
}
