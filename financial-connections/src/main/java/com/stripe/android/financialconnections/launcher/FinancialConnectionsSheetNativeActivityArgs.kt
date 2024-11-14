package com.stripe.android.financialconnections.launcher

import android.os.Parcelable
import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.model.SynchronizeSessionResponse
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class FinancialConnectionsSheetNativeActivityArgs(
    val configuration: FinancialConnectionsSheet.Configuration,
    val initialSyncResponse: SynchronizeSessionResponse,
    val elementsSessionContext: FinancialConnectionsSheet.ElementsSessionContext? = null,
) : Parcelable
