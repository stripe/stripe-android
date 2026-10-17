package com.stripe.android.lpmfoundations.paymentmethod.definitions

import org.junit.Test

class PayPayDefinitionTest {
    @Test
    fun `createFormElements returns no elements`() {
        PayPayDefinition.basicEmptyFormTest()
    }

    @Test
    fun `createFormElements returns requested contact information fields`() {
        PayPayDefinition.basicFormWithContactFieldsTest()
    }

    @Test
    fun `createFormElements returns all billing details fields`() {
        PayPayDefinition.basicFormWithBillingInformationTest()
    }
}
