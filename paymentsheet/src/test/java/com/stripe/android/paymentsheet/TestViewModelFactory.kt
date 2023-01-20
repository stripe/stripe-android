package com.stripe.android.paymentsheet

import androidx.lifecycle.SavedStateHandle
import com.stripe.android.link.LinkPaymentLauncher
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import org.mockito.kotlin.mock

internal object TestViewModelFactory {
    fun <T : BaseSheetViewModel> create(
        linkLauncher: LinkPaymentLauncher = mock(),
        viewModelFactory: (linkHandler: LinkHandler, savedStateHandle: SavedStateHandle) -> T
    ): T {
        lateinit var viewModel: T
        val savedStateHandle = SavedStateHandle()
        val linkHandler = LinkHandler(
            linkLauncher = linkLauncher,
            savedStateHandle = savedStateHandle,
        )
        viewModel = viewModelFactory(linkHandler, savedStateHandle)
        return viewModel
    }
}
