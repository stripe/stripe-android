package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.stripe.android.ui.core.R
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RevolutPayDefinitionTest {
    @Test
    fun `createFormElements returns no elements if payment intent`() {
        RevolutPayDefinition.basicEmptyFormTest()
    }

    @Test
    fun `createFormElements returns mandate if setup intent`() {
        RevolutPayDefinition.mandateTest(R.string.stripe_revolut_mandate) {
            listOf(it.merchantName)
        }
    }

    @Test
    fun `createFormElements returns no elements if terms display is set to never`() {
        RevolutPayDefinition.noMandateWithTermsDisplayNeverTest()
    }

    @Test
    fun `createFormElements returns mandate & requested contact information fields`() {
        RevolutPayDefinition.mandateWithContactFieldsTest(R.string.stripe_revolut_mandate) {
            listOf(it.merchantName)
        }
    }

    @Test
    fun `createFormElements returns mandate & all billing details fields`() {
        RevolutPayDefinition.mandateWithBillingInformationTest(R.string.stripe_revolut_mandate) {
            listOf(it.merchantName)
        }
    }
}
