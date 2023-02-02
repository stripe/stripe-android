package com.stripe.android.financialconnections.features.manualentry

import com.stripe.android.financialconnections.model.FinancialConnectionsSession
import com.stripe.android.financialconnections.model.FinancialConnectionsSession.StatusDetails.Cancelled.Reason

internal fun FinancialConnectionsSession.isCustomManualEntryError(): Boolean {
    return statusDetails?.cancelled?.reason == Reason.CUSTOM_MANUAL_ENTRY
}
