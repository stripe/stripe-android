package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.stripe.android.ui.core.R
import org.junit.Test

class PayPalDefinitionTest {
    @Test
    fun `createFormElements returns no elements if payment intent`() {
        PayPalDefinition.basicEmptyFormTest()
    }

    @Test
    fun `createFormElements returns mandate if setup intent`() {
        PayPalDefinition.mandateTest(R.string.stripe_paypal_mandate) {
            listOf(it.merchantName)
        }
    }

    @Test
    fun `createFormElements returns no elements if terms display is set to never`() {
        PayPalDefinition.noMandateWithTermsDisplayNeverTest()
    }

    @Test
    fun `createFormElements returns mandate & requested contact information fields`() {
        PayPalDefinition.mandateWithContactFieldsTest(R.string.stripe_paypal_mandate) {
            listOf(it.merchantName)
        }
    }

    @Test
    fun `createFormElements returns mandate & all billing details fields`() {
        PayPalDefinition.mandateWithBillingInformationTest(R.string.stripe_paypal_mandate) {
            listOf(it.merchantName)
        }
    }
}
