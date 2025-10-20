package com.stripe.android.paymentsheet.utils

import androidx.compose.ui.test.junit4.ComposeTestRule
import com.google.testing.junit.testparameterinjector.TestParameterValuesProvider
import com.stripe.android.model.PaymentMethod
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
