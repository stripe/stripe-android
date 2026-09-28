package com.stripe.android.lpmfoundations.paymentmethod.definitions

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GrabPayDefinitionTest {
    @Test
    fun `createFormElements returns no elements`() {
        GrabPayDefinition.basicEmptyFormTest()
    }

    @Test
    fun `createFormElements returns requested contact information fields`() {
        GrabPayDefinition.basicFormWithContactFieldsTest()
    }

    @Test
    fun `createFormElements returns all billing details fields`() {
        GrabPayDefinition.basicFormWithBillingInformationTest()
    }
}
