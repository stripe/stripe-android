package com.stripe.android.paymentsheet.utils

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import com.google.testing.junit.testparameterinjector.TestParameterValuesProvider
import com.stripe.android.financialconnections.model.BankAccount
import com.stripe.android.financialconnections.model.FinancialConnectionsSession
import com.stripe.android.model.PaymentMethod
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountContract
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountResponseInternal
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountResultInternal
import com.stripe.android.payments.bankaccount.ui.CollectBankAccountActivity
import com.stripe.android.paymentsheet.PaymentSheetPage
import com.stripe.paymentelementtestpages.FormPage

internal sealed class PaymentMethodType(
    val type: PaymentMethod.Type
) {
    abstract fun paymentMethodSetup()

    abstract fun fillOutFormDetails(composeTestRule: ComposeTestRule)

    data object Card : PaymentMethodType(
        type = PaymentMethod.Type.Card
    ) {
        override fun paymentMethodSetup() {}

        override fun fillOutFormDetails(composeTestRule: ComposeTestRule) {
            val paymentSheetPage = PaymentSheetPage(composeTestRule)
            paymentSheetPage.fillOutCardDetails()
        }
    }

    data object UsBankAccount : PaymentMethodType(
        type = PaymentMethod.Type.USBankAccount
    ) {
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

        override fun paymentMethodSetup() {
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

        override fun fillOutFormDetails(composeTestRule: ComposeTestRule) {
            val paymentSheetPage = PaymentSheetPage(composeTestRule)
            val formPage = FormPage(composeTestRule)

            formPage.fillOutName()
            formPage.fillOutEmail()

            paymentSheetPage.clickPrimaryButton()
        }
    }
}

internal object PaymentMethodTypeProvider : TestParameterValuesProvider() {
    override fun provideValues(context: Context?): List<PaymentMethodType> {
        return listOf(
            PaymentMethodType.Card,
            PaymentMethodType.UsBankAccount,
        )
    }
}
