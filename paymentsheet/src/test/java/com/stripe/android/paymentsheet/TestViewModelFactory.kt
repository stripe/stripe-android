package com.stripe.android.paymentsheet

import androidx.lifecycle.SavedStateHandle
import com.stripe.android.link.LinkInteractor
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import org.mockito.kotlin.mock

internal object TestViewModelFactory {
    fun <T : BaseSheetViewModel> create(
        linkInteractor: LinkInteractor = mock(),
        viewModelFactory: (
            linkHandler: LinkHandler,
            linkInteractor: LinkInteractor,
            savedStateHandle: SavedStateHandle,
        ) -> T
    ): T {
        val savedStateHandle = SavedStateHandle()
        val linkHandler = LinkHandler(
            linkLauncher = mock(),
            linkInteractor = linkInteractor,
            savedStateHandle = savedStateHandle,
        )
        return viewModelFactory(linkHandler, linkInteractor, savedStateHandle)
    }
}
