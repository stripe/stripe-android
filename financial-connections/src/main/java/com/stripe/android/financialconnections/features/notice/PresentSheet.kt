package com.stripe.android.financialconnections.features.notice

import com.stripe.android.financialconnections.features.notice.NoticeSheetState.NoticeSheetContent
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.navigation.Destination
import com.stripe.android.financialconnections.navigation.NavigationManager
import com.stripe.android.financialconnections.repository.NoticeSheetContentRepository
import javax.inject.Inject

internal interface PresentSheet {
    operator fun invoke(content: NoticeSheetContent, referrer: Pane)
}

internal class RealPresentSheet @Inject constructor(
    private val navigationManager: NavigationManager,
    private val noticeSheetContentRepository: NoticeSheetContentRepository,
) : PresentSheet {

    override fun invoke(content: NoticeSheetContent, referrer: Pane) {
        noticeSheetContentRepository.set(content)
        val route = Destination.Notice(referrer = referrer)
        navigationManager.tryNavigateTo(route)
    }
}
