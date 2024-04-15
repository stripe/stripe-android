package com.stripe.android.payments.bankaccount.navigation

import android.os.Parcelable
import androidx.annotation.RestrictTo
import com.stripe.android.model.StripeIntent
import kotlinx.parcelize.Parcelize

/**
 * The result of an attempt to collect a bank account
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed interface CollectBankAccountForInstantDebitsResult : Parcelable {

    @Parcelize
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class Completed(
        val intent: StripeIntent,
        val paymentMethodId: String,
        val last4: String?,
        val bankName: String?
    ) : CollectBankAccountForInstantDebitsResult

    @Parcelize
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class Failed(
        val error: Throwable
    ) : CollectBankAccountForInstantDebitsResult

    @Parcelize
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data object Cancelled : CollectBankAccountForInstantDebitsResult
}

internal fun CollectBankAccountResultInternal.toInstantDebitsResult(): CollectBankAccountForInstantDebitsResult {
    return when (this) {
        is CollectBankAccountResultInternal.Cancelled -> {
            CollectBankAccountForInstantDebitsResult.Cancelled
        }

        is CollectBankAccountResultInternal.Completed -> {
            when {
                response.intent == null -> {
                    CollectBankAccountForInstantDebitsResult.Failed(
                        IllegalArgumentException("StripeIntent not set for this session")
                    )
                }

                response.instantDebitsData == null -> {
                    CollectBankAccountForInstantDebitsResult.Failed(
                        IllegalArgumentException("instant debits data cannot be null")
                    )
                }

                else -> {
                    CollectBankAccountForInstantDebitsResult.Completed(
                        intent = response.intent,
                        paymentMethodId = response.instantDebitsData.paymentMethodId,
                        last4 = response.instantDebitsData.last4,
                        bankName = response.instantDebitsData.bankName
                    )
                }
            }
        }

        is CollectBankAccountResultInternal.Failed -> {
            CollectBankAccountForInstantDebitsResult.Failed(error)
        }
    }
}
