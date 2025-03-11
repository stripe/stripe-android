package com.stripe.android.financialconnections.launcher

import android.os.Parcelable
import com.stripe.android.financialconnections.ElementsSessionContext
import com.stripe.android.financialconnections.FinancialConnectionsSheetConfiguration
import com.stripe.android.financialconnections.model.SynchronizeSessionResponse
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class FinancialConnectionsSheetNativeActivityArgs(
    val configuration: FinancialConnectionsSheetConfiguration,
    val initialSyncResponse: SynchronizeSessionResponse,
    val elementsSessionContext: ElementsSessionContext? = null,
) : Parcelable
