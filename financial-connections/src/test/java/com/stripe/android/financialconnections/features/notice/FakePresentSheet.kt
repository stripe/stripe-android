package com.stripe.android.financialconnections.features.notice

import app.cash.turbine.Turbine
import com.stripe.android.financialconnections.features.notice.NoticeSheetState.NoticeSheetContent
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane

internal class FakePresentSheet : PresentSheet {

    val presentCalls = Turbine<PresentCall>()

    override fun invoke(content: NoticeSheetContent, referrer: Pane) {
        presentCalls.add(PresentCall(content, referrer))
    }

    fun ensureAllEventsConsumed() {
        presentCalls.ensureAllEventsConsumed()
    }

    data class PresentCall(
        val content: NoticeSheetContent,
        val referrer: Pane,
    )
}
