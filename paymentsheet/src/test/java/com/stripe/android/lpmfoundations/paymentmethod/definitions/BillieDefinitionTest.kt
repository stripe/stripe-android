package com.stripe.android.lpmfoundations.paymentmethod.definitions

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BillieDefinitionTest {
    @Test
    fun `createFormElements returns no elements`() {
        BillieDefinition.basicEmptyFormTest()
    }

    @Test
    fun `createFormElements returns requested contact information fields`() {
        BillieDefinition.basicFormWithContactFieldsTest()
    }

    @Test
    fun `createFormElements returns all billing details fields`() {
        BillieDefinition.basicFormWithBillingInformationTest()
    }
}
