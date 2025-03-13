package com.stripe.android.financialconnections.launcher

import android.os.Parcelable
import androidx.annotation.RestrictTo
import com.stripe.android.financialconnections.ElementsSessionContext
import com.stripe.android.financialconnections.FinancialConnectionsSheetConfiguration
import kotlinx.parcelize.Parcelize
import java.security.InvalidParameterException

/**
 * Args used internally to communicate between
 * [com.stripe.android.financialconnections.FinancialConnectionsSheetActivity] and
 * instances of [FinancialConnectionsSheetLauncher].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed class FinancialConnectionsSheetActivityArgs(
    open val configuration: FinancialConnectionsSheetConfiguration,
    open val elementsSessionContext: ElementsSessionContext?,
) : Parcelable {

    @Parcelize
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class ForData(
        override val configuration: FinancialConnectionsSheetConfiguration,
        override val elementsSessionContext: ElementsSessionContext? = null,
    ) : FinancialConnectionsSheetActivityArgs(configuration, elementsSessionContext)

    @Parcelize
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class ForToken(
        override val configuration: FinancialConnectionsSheetConfiguration,
        override val elementsSessionContext: ElementsSessionContext? = null,
    ) : FinancialConnectionsSheetActivityArgs(configuration, elementsSessionContext)

    @Parcelize
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class ForInstantDebits(
        override val configuration: FinancialConnectionsSheetConfiguration,
        override val elementsSessionContext: ElementsSessionContext? = null,
    ) : FinancialConnectionsSheetActivityArgs(configuration, elementsSessionContext)

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
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

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun isValid(): Boolean = runCatching { validate() }.isSuccess
}
