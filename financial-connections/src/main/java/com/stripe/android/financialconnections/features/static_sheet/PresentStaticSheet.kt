package com.stripe.android.financialconnections.features.static_sheet

import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.navigation.Destination
import com.stripe.android.financialconnections.navigation.NavigationManager
import com.stripe.android.financialconnections.repository.StaticSheetContent
import com.stripe.android.financialconnections.repository.StaticSheetContentRepository
import javax.inject.Inject

internal interface PresentStaticSheet {
    operator fun invoke(content: StaticSheetContent, referrer: Pane)
}

internal class RealPresentStaticSheet @Inject constructor(
    private val navigationManager: NavigationManager,
    private val staticSheetContentRepository: StaticSheetContentRepository,
) : PresentStaticSheet {

    override fun invoke(content: StaticSheetContent, referrer: Pane) {
        staticSheetContentRepository.update { copy(content = content) }
        val route = Destination.StaticSheet(referrer = referrer)
        navigationManager.tryNavigateTo(route)
    }
}
