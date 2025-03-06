package com.stripe.android.financialconnections.features.notice

import com.stripe.android.financialconnections.features.notice.NoticeSheetState.NoticeSheetContent
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.navigation.Destination
import com.stripe.android.financialconnections.repository.AccountUpdateRequiredContentRepository
import com.stripe.android.financialconnections.repository.NoticeSheetContentRepository
import com.stripe.android.uicore.navigation.NavigationManager
import javax.inject.Inject

internal interface PresentSheet {
    operator fun invoke(content: NoticeSheetContent, referrer: Pane)
}

internal class RealPresentSheet @Inject constructor(
    private val navigationManager: NavigationManager,
    private val noticeSheetContentRepository: NoticeSheetContentRepository,
    private val accountUpdateRequiredContentRepository: AccountUpdateRequiredContentRepository,
) : PresentSheet {

    override fun invoke(content: NoticeSheetContent, referrer: Pane) {
        when (content) {
            is NoticeSheetContent.UpdateRequired -> {
                accountUpdateRequiredContentRepository.set(content)
                val route = Destination.AccountUpdateRequired(referrer = referrer)
                navigationManager.tryNavigateTo(route)
            }
            else -> {
                noticeSheetContentRepository.set(content)
                val route = Destination.Notice(referrer = referrer)
                navigationManager.tryNavigateTo(route)
            }
        }
    }
}
