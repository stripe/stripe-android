package com.stripe.android.financialconnections.features.notice

import com.stripe.android.financialconnections.features.notice.NoticeSheetState.NoticeSheetContent
import com.stripe.android.financialconnections.features.notice.NoticeSheetState.NoticeSheetContent.Generic
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.model.NoticeContent
import com.stripe.android.financialconnections.navigation.Destination
import com.stripe.android.financialconnections.navigation.NavigationManager
import com.stripe.android.financialconnections.repository.NoticeSheetContentRepository
import javax.inject.Inject

internal interface PresentNoticeSheet {
    operator fun invoke(content: NoticeSheetContent, referrer: Pane)
    operator fun invoke(content: NoticeContent, referrer: Pane)
}

internal class RealPresentNoticeSheet @Inject constructor(
    private val navigationManager: NavigationManager,
    private val noticeSheetContentRepository: NoticeSheetContentRepository,
) : PresentNoticeSheet {

    override fun invoke(content: NoticeSheetContent, referrer: Pane) {
        noticeSheetContentRepository.set(content)
        val route = Destination.Notice(referrer = referrer)
        navigationManager.tryNavigateTo(route)
    }

    override fun invoke(content: NoticeContent, referrer: Pane) {
        noticeSheetContentRepository.set(Generic(content))
        val route = Destination.Notice(referrer = referrer)
        navigationManager.tryNavigateTo(route)
    }
}
