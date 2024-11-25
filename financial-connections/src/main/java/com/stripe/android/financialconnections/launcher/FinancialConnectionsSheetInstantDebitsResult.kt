package com.stripe.android.financialconnections.launcher

import android.os.Parcelable
import androidx.annotation.RestrictTo
import kotlinx.parcelize.Parcelize

/**
 * The result of an attempt to complete a connections session
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed class FinancialConnectionsSheetInstantDebitsResult : Parcelable {
    /**
     * The customer completed the connections session.
     * @param paymentMethodId The payment method id, that can be used to confirm the payment.
     * @param last4 The last 4 digits of the bank account.
     * @param bankName The name of the bank.
     */
    @Parcelize
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class Completed(
        val encodedPaymentMethod: String,
        val last4: String?,
        val bankName: String?
    ) : FinancialConnectionsSheetInstantDebitsResult()

    /**
     * The customer canceled the connections session attempt.
     */
    @Parcelize
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data object Canceled : FinancialConnectionsSheetInstantDebitsResult()

    /**
     * The connections session attempt failed.
     * @param error The error encountered by the customer.
     */
    @Parcelize
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class Failed(
        val error: Throwable
    ) : FinancialConnectionsSheetInstantDebitsResult()
}
