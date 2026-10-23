package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.stripe.android.ui.core.R
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TwintDefinitionTest {
    @Test
    fun `createFormElements returns no elements`() {
        TwintDefinition.basicEmptyFormTest()
    }

    @Test
    fun `createFormElements returns mandate if setup intent`() {
        TwintDefinition.mandateTest(R.string.stripe_twint_mandate) {
            listOf(it.merchantName)
        }
    }

    @Test
    fun `createFormElements returns no elements if terms display is set to never`() {
        TwintDefinition.noMandateWithTermsDisplayNeverTest()
    }

    @Test
    fun `createFormElements returns requested contact information fields`() {
        TwintDefinition.basicFormWithContactFieldsTest()
    }

    @Test
    fun `createFormElements returns all billing details fields`() {
        TwintDefinition.basicFormWithBillingInformationTest()
    }
}
