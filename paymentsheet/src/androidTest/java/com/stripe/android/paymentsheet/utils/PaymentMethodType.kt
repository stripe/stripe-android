package com.stripe.android.paymentsheet.utils

import androidx.compose.ui.test.junit4.ComposeTestRule
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.PaymentSheetPage
import com.stripe.paymentelementtestpages.FormPage

internal enum class PaymentMethodType(
    val type: PaymentMethod.Type
) {
    Card(
        type = PaymentMethod.Type.Card
    ) {
        override fun paymentMethodSetup() {}

        override fun fillOutFormDetails(composeTestRule: ComposeTestRule) {
            val paymentSheetPage = PaymentSheetPage(composeTestRule)
            paymentSheetPage.fillOutCardDetails()
        }
    },

    UsBankAccount(
        type = PaymentMethod.Type.USBankAccount
    ) {
        override fun paymentMethodSetup() {
           UsBankAccountFormTestUtils.setupSuccessfulCompletionOfUsBankAccountForm()
        }

        override fun fillOutFormDetails(composeTestRule: ComposeTestRule) {
            val paymentSheetPage = PaymentSheetPage(composeTestRule)
            val formPage = FormPage(composeTestRule)

            formPage.fillOutName()
            formPage.fillOutEmail()

            paymentSheetPage.clickPrimaryButton()
        }
    };

    abstract fun paymentMethodSetup()

    abstract fun fillOutFormDetails(composeTestRule: ComposeTestRule)
}
