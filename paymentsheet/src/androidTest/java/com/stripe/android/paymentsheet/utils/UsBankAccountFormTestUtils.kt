package com.stripe.android.paymentsheet.utils

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import com.stripe.android.financialconnections.model.BankAccount
import com.stripe.android.financialconnections.model.FinancialConnectionsSession
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountContract
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountResponseInternal
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountResultInternal
import com.stripe.android.payments.bankaccount.ui.CollectBankAccountActivity

object UsBankAccountFormTestUtils {
    private val bankAccountCompletedResult: CollectBankAccountResultInternal.Completed
        get() {
            val usBankAccountData = CollectBankAccountResponseInternal.USBankAccountData(
                financialConnectionsSession = FinancialConnectionsSession(
                    clientSecret = "cs_123",
                    id = "unique_id",
                    livemode = false,
                    paymentAccount = BankAccount(
                        id = "id_1234",
                        last4 = "6789",
                    )
                )
            )

            return CollectBankAccountResultInternal.Completed(
                CollectBankAccountResponseInternal(
                    intent = null,
                    usBankAccountData = usBankAccountData,
                    instantDebitsData = null,
                )
            )
        }

    fun setupSuccessfulCompletionOfUsBankAccountForm() {
        intending(
            hasComponent(
                CollectBankAccountActivity::class.java.name
            )
        ).respondWith(
            Instrumentation.ActivityResult(
                Activity.RESULT_OK,
                Intent().putExtras(
                    CollectBankAccountContract.Result(
                        bankAccountCompletedResult
                    ).toBundle()
                )
            )
        )
    }
}
