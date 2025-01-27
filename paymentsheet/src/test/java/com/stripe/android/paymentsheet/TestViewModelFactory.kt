package com.stripe.android.paymentsheet

import androidx.lifecycle.SavedStateHandle
import com.stripe.android.link.LinkConfigurationCoordinator
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.utils.FakeLinkConfigurationCoordinator

internal object TestViewModelFactory {
    fun <T : BaseSheetViewModel> create(
        linkConfigurationCoordinator: LinkConfigurationCoordinator = FakeLinkConfigurationCoordinator(),
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
        viewModelFactory: (
            linkHandler: LinkHandler,
            savedStateHandle: SavedStateHandle,
        ) -> T
    ): T {
        val linkHandler = LinkHandler(
            linkConfigurationCoordinator = linkConfigurationCoordinator,
        )
        return viewModelFactory(linkHandler, savedStateHandle)
    }
}
