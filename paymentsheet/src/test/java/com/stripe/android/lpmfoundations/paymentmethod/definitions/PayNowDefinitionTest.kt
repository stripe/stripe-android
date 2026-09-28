package com.stripe.android.lpmfoundations.paymentmethod.definitions

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PayNowDefinitionTest {
    @Test
    fun `createFormElements returns no elements`() {
        PayNowDefinition.basicEmptyFormTest()
    }

    @Test
    fun `createFormElements returns requested contact information fields`() {
        PayNowDefinition.basicFormWithContactFieldsTest()
    }

    @Test
    fun `createFormElements returns all billing details fields`() {
        PayNowDefinition.basicFormWithBillingInformationTest()
    }
}
