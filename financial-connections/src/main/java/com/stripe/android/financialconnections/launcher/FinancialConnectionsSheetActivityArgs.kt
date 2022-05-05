package com.stripe.android.financialconnections.launcher

import android.content.Intent
import android.os.Parcelable
import com.stripe.android.financialconnections.FinancialConnectionsSheet
import kotlinx.parcelize.Parcelize
import java.security.InvalidParameterException

/**
 * Args used internally to communicate between
 * [com.stripe.android.financialconnections.FinancialConnectionsSheetActivity] and
 * instances of [com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetLauncher].
 */
internal sealed class FinancialConnectionsSheetActivityArgs constructor(
    open val configuration: FinancialConnectionsSheet.Configuration,
) : Parcelable {

    @Parcelize
    data class ForData(
        override val configuration: FinancialConnectionsSheet.Configuration
    ) : FinancialConnectionsSheetActivityArgs(configuration)

    @Parcelize
    data class ForToken(
        override val configuration: FinancialConnectionsSheet.Configuration
    ) : FinancialConnectionsSheetActivityArgs(configuration)

    fun validate() {
        if (configuration.financialConnectionsSessionClientSecret.isBlank()) {
            throw InvalidParameterException(
                "The session client secret cannot be an empty string."
            )
        }
        if (configuration.publishableKey.isBlank()) {
            throw InvalidParameterException(
                "The publishable key cannot be an empty string."
            )
        }
    }

    companion object {
        const val EXTRA_ARGS =
            "com.stripe.android.financialconnections.ConnectionsSheetContract.extra_args"

        internal fun fromIntent(intent: Intent): FinancialConnectionsSheetActivityArgs? {
            return intent.getParcelableExtra(EXTRA_ARGS)
        }
    }
}
