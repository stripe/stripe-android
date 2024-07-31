package com.stripe.android.financialconnections.domain

import com.stripe.android.financialconnections.presentation.FinancialConnectionsSheetNativeState
import javax.inject.Inject

internal fun interface IsLinkWithStripe {
    operator fun invoke(): Boolean
}

internal class RealIsLinkWithStripe @Inject constructor(
    private val initialState: FinancialConnectionsSheetNativeState,
) : IsLinkWithStripe {

    override operator fun invoke(): Boolean {
        return initialState.isLinkWithStripe
    }
}
