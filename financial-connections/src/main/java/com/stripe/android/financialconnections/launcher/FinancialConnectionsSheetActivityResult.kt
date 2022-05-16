package com.stripe.android.financialconnections.launcher

import android.os.Bundle
import android.os.Parcelable
import androidx.core.os.bundleOf
import com.stripe.android.financialconnections.model.FinancialConnectionsSession
import com.stripe.android.model.Token
import kotlinx.parcelize.Parcelize

/**
 * Result used internally to communicate between
 * [com.stripe.android.financialconnections.FinancialConnectionsSheetActivity] and
 * instances of [com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetLauncher].
 */
internal sealed class FinancialConnectionsSheetActivityResult : Parcelable {
    /**
     * The customer completed the connections session.
     * @param financialConnectionsSession The financial connections session connected
     */
    @Parcelize
    data class Completed(
        val financialConnectionsSession: FinancialConnectionsSession,
        val token: Token? = null
    ) : FinancialConnectionsSheetActivityResult()

    /**
     * The customer canceled the connections session attempt.
     */
    @Parcelize
    object Canceled : FinancialConnectionsSheetActivityResult()

    /**
     * The connections session attempt failed.
     * @param error The error encountered by the customer.
     */
    @Parcelize
    data class Failed(
        val error: Throwable
    ) : FinancialConnectionsSheetActivityResult()

    fun toBundle(): Bundle {
        return bundleOf(EXTRA_RESULT to this)
    }

    companion object {
        const val EXTRA_RESULT =
            "com.stripe.android.financialconnections.ConnectionsSheetContract.extra_result"
    }
}
