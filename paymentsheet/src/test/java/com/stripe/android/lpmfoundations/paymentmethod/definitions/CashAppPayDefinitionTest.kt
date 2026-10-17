package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.stripe.android.ui.core.R
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CashAppPayDefinitionTest {
    @Test
    fun `createFormElements returns no elements if payment intent`() {
        CashAppPayDefinition.basicEmptyFormTest()
    }

    @Test
    fun `createFormElements returns mandate if setup intent`() {
        CashAppPayDefinition.mandateTest(R.string.stripe_cash_app_pay_mandate) {
            listOf(it.merchantName, it.merchantName)
        }
    }

    @Test
    fun `createFormElements returns no elements if terms display is set to never`() {
        CashAppPayDefinition.noMandateWithTermsDisplayNeverTest()
    }

    @Test
    fun `createFormElements returns mandate & requested contact information fields`() {
        CashAppPayDefinition.mandateWithContactFieldsTest(R.string.stripe_cash_app_pay_mandate) {
            listOf(it.merchantName, it.merchantName)
        }
    }

    @Test
    fun `createFormElements returns mandate & all billing details fields`() {
        CashAppPayDefinition.mandateWithBillingInformationTest(R.string.stripe_cash_app_pay_mandate) {
            listOf(it.merchantName, it.merchantName)
        }
    }
}
