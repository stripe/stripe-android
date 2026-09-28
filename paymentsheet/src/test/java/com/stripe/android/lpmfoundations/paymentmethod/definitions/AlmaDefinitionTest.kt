package com.stripe.android.lpmfoundations.paymentmethod.definitions

import org.junit.Test

class AlmaDefinitionTest {
    @Test
    fun `createFormElements returns no elements`() {
        AlmaDefinition.basicEmptyFormTest()
    }

    @Test
    fun `createFormElements returns requested contact information fields`() {
        AlmaDefinition.basicFormWithContactFieldsTest()
    }

    @Test
    fun `createFormElements returns all billing details fields`() {
        AlmaDefinition.basicFormWithBillingInformationTest()
    }
}
