package com.stripe.android.financialconnections.launcher

import android.content.Intent
import android.os.Parcelable
import androidx.annotation.RestrictTo
import com.airbnb.mvrx.Mavericks
import com.stripe.android.financialconnections.FinancialConnectionsSheet
import kotlinx.parcelize.Parcelize
import java.security.InvalidParameterException

/**
 * Args used internally to communicate between
 * [com.stripe.android.financialconnections.FinancialConnectionsSheetActivity] and
 * instances of [com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetLauncher].
 */

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed class FinancialConnectionsSheetActivityArgs constructor(
    open val configuration: FinancialConnectionsSheet.Configuration
) : Parcelable {

    @Parcelize
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class ForData(
        override val configuration: FinancialConnectionsSheet.Configuration
    ) : FinancialConnectionsSheetActivityArgs(configuration)

    @Parcelize
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class ForToken(
        override val configuration: FinancialConnectionsSheet.Configuration
    ) : FinancialConnectionsSheetActivityArgs(configuration)

    @Parcelize
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class ForLink(
        override val configuration: FinancialConnectionsSheet.Configuration
    ) : FinancialConnectionsSheetActivityArgs(configuration)

    internal fun validate() {
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

    internal fun isValid(): Boolean = kotlin.runCatching { validate() }.isSuccess

    companion object {
        internal fun fromIntent(intent: Intent): FinancialConnectionsSheetActivityArgs? {
            return intent.getParcelableExtra(Mavericks.KEY_ARG)
        }
    }
}
