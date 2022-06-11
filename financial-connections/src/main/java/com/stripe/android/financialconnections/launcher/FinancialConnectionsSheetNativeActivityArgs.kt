package com.stripe.android.financialconnections.launcher

import android.os.Parcelable
import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class FinancialConnectionsSheetNativeActivityArgs constructor(
    val configuration: FinancialConnectionsSheet.Configuration,
    val manifest: FinancialConnectionsSessionManifest
) : Parcelable
