package com.stripe.android.payments.bankaccount.navigation

import android.os.Parcelable
import com.stripe.android.model.StripeIntent
import kotlinx.parcelize.Parcelize

/**
 * The result of an attempt to collect a bank account
 */
sealed class CollectBankAccountForInstantDebitsResult : Parcelable {

    @Parcelize
    data class Completed(
        val intent: StripeIntent,
        val paymentMethodId: String,
        val last4: String?,
        val bankName: String?
    ) : CollectBankAccountForInstantDebitsResult()

    @Parcelize
    data class Failed(
        val error: Throwable
    ) : CollectBankAccountForInstantDebitsResult()

    @Parcelize
    data object Cancelled : CollectBankAccountForInstantDebitsResult()
}

@Suppress("unused")
internal fun CollectBankAccountResultInternal.toInstantDebitsResult(): CollectBankAccountForInstantDebitsResult {
    return when (this) {
        is CollectBankAccountResultInternal.Cancelled -> {
            CollectBankAccountForInstantDebitsResult.Cancelled
        }

        is CollectBankAccountResultInternal.Completed -> {
            when {
                response.intent !is StripeIntent -> {
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
