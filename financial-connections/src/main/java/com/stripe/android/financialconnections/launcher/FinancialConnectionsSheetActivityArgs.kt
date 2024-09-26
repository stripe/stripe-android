package com.stripe.android.financialconnections.launcher

import android.os.Parcelable
import androidx.annotation.RestrictTo
import com.stripe.android.financialconnections.FinancialConnectionsSheet
import kotlinx.parcelize.Parcelize
import java.security.InvalidParameterException

/**
 * Args used internally to communicate between
 * [com.stripe.android.financialconnections.FinancialConnectionsSheetActivity] and
 * instances of [com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetLauncher].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed class FinancialConnectionsSheetActivityArgs(
    open val configuration: FinancialConnectionsSheet.Configuration,
    open val elementsSessionContext: FinancialConnectionsSheet.ElementsSessionContext?,
) : Parcelable {

    @Parcelize
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class ForData(
        override val configuration: FinancialConnectionsSheet.Configuration
    ) : FinancialConnectionsSheetActivityArgs(configuration, elementsSessionContext = null)

    @Parcelize
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class ForToken(
        override val configuration: FinancialConnectionsSheet.Configuration
    ) : FinancialConnectionsSheetActivityArgs(configuration, elementsSessionContext = null)

    @Parcelize
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class ForInstantDebits(
        override val configuration: FinancialConnectionsSheet.Configuration,
        override val elementsSessionContext: FinancialConnectionsSheet.ElementsSessionContext? = null,
    ) : FinancialConnectionsSheetActivityArgs(configuration, elementsSessionContext)

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
}
