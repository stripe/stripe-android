package com.stripe.android.lpmfoundations.paymentmethod.definitions

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AlipayDefinitionTest {
    @Test
    fun `createFormElements returns no elements`() {
        AlipayDefinition.basicEmptyFormTest()
    }

    @Test
    fun `createFormElements returns requested contact information fields`() {
        AlipayDefinition.basicFormWithContactFieldsTest()
    }

    @Test
    fun `createFormElements returns all billing details fields`() {
        AlipayDefinition.basicFormWithBillingInformationTest()
    }
}
