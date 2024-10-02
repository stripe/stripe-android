package com.stripe.android.payments.bankaccount.navigation

import android.os.Parcelable
import androidx.annotation.RestrictTo
import com.stripe.android.financialconnections.model.FinancialConnectionsSession
import com.stripe.android.model.StripeIntent
import dev.drewhamilton.poko.Poko
import kotlinx.parcelize.Parcelize

/**
 * The result of an attempt to collect a bank account for ACH payments
 */
sealed class CollectBankAccountResult : Parcelable {

    @Parcelize
    @Poko
    class Completed(
        val response: CollectBankAccountResponse
    ) : CollectBankAccountResult()

    @Parcelize
    @Poko
    class Failed(
        val error: Throwable
    ) : CollectBankAccountResult()

    @Parcelize
    data object Cancelled : CollectBankAccountResult()
}

@Parcelize
@Poko
class CollectBankAccountResponse(
    val intent: StripeIntent,
    val financialConnectionsSession: FinancialConnectionsSession
) : Parcelable

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun CollectBankAccountResultInternal.toUSBankAccountResult(): CollectBankAccountResult {
    return when (this) {
        is CollectBankAccountResultInternal.Cancelled -> CollectBankAccountResult.Cancelled

        is CollectBankAccountResultInternal.Completed -> when {
            response.usBankAccountData == null -> {
                CollectBankAccountResult.Failed(
                    IllegalArgumentException("ACH payload cannot be null")
                )
            }

            response.intent == null -> {
                CollectBankAccountResult.Failed(
                    IllegalArgumentException("StripeIntent cannot be null")
                )
            }

            else -> {
                CollectBankAccountResult.Completed(
                    response = CollectBankAccountResponse(
                        intent = response.intent,
                        financialConnectionsSession = response.usBankAccountData.financialConnectionsSession
                    )
                )
            }
        }

        is CollectBankAccountResultInternal.Failed -> {
            CollectBankAccountResult.Failed(error)
        }
    }
}
