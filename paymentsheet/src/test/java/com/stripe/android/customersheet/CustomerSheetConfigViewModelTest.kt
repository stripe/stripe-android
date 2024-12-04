package com.stripe.android.customersheet

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import kotlin.test.Test

class CustomerSheetConfigViewModelTest {
    @Test
    fun `On set configure request, should be able to retrieve the same request`() {
        val viewModel = CustomerSheetConfigViewModel(
            savedStateHandle = SavedStateHandle(),
        )

        val request = CustomerSheetConfigureRequest(
            configuration = CustomerSheet.Configuration.builder(
                merchantDisplayName = "Merchant, Inc.",
            )
                .googlePayEnabled(googlePayEnabled = true)
                .build()
        )

        viewModel.configureRequest = request

        assertThat(viewModel.configureRequest).isEqualTo(request)
    }

    @Test
    fun `On init with 'SavedStateHandle', should be able to retrieve the saved request`() {
        val handle = SavedStateHandle()
        val viewModel = CustomerSheetConfigViewModel(
            savedStateHandle = handle,
        )

        val request = CustomerSheetConfigureRequest(
            configuration = CustomerSheet.Configuration.builder(
                merchantDisplayName = "Merchant, Inc.",
            )
                .googlePayEnabled(googlePayEnabled = true)
                .build()
        )

        viewModel.configureRequest = request

        val recreatedViewModel = CustomerSheetConfigViewModel(
            savedStateHandle = handle
        )

        assertThat(recreatedViewModel.configureRequest).isEqualTo(request)
    }
}
