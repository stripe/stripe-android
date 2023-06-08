package com.stripe.android.paymentsheet

import androidx.lifecycle.SavedStateHandle
import com.stripe.android.link.LinkConfigurationInteractor
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import org.mockito.kotlin.mock

internal object TestViewModelFactory {
    fun <T : BaseSheetViewModel> create(
        linkConfigurationInteractor: LinkConfigurationInteractor = mock(),
        viewModelFactory: (
            linkHandler: LinkHandler,
            linkConfigurationInteractor: LinkConfigurationInteractor,
            savedStateHandle: SavedStateHandle,
        ) -> T
    ): T {
        val savedStateHandle = SavedStateHandle()
        val linkHandler = LinkHandler(
            linkLauncher = mock(),
            linkConfigurationInteractor = linkConfigurationInteractor,
            savedStateHandle = savedStateHandle,
        )
        return viewModelFactory(linkHandler, linkConfigurationInteractor, savedStateHandle)
    }
}
