package com.stripe.android.paymentsheet

import androidx.lifecycle.SavedStateHandle
import com.stripe.android.link.LinkConfigurationCoordinator
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import org.mockito.kotlin.mock

internal object TestViewModelFactory {
    fun <T : BaseSheetViewModel> create(
        linkConfigurationCoordinator: LinkConfigurationCoordinator = mock(),
        viewModelFactory: (
            linkHandler: LinkHandler,
            linkConfigurationCoordinator: LinkConfigurationCoordinator,
            savedStateHandle: SavedStateHandle,
        ) -> T
    ): T {
        val savedStateHandle = SavedStateHandle()
        val linkHandler = LinkHandler(
            linkLauncher = mock(),
            linkConfigurationCoordinator = linkConfigurationCoordinator,
            savedStateHandle = savedStateHandle,
            linkAnalyticsComponentBuilder = mock(),
            linkStore = mock(),
        )
        return viewModelFactory(linkHandler, linkConfigurationCoordinator, savedStateHandle)
    }
}
