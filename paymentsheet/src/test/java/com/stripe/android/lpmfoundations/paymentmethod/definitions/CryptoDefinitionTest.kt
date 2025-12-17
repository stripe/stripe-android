package com.stripe.android.lpmfoundations.paymentmethod.definitions

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CryptoDefinitionTest {
    @Test
    fun `createFormElements returns no elements`() {
        CryptoDefinition.basicEmptyFormTest()
    }

    @Test
    fun `createFormElements returns requested contact information fields`() {
        CryptoDefinition.basicFormWithContactFieldsTest()
    }

    @Test
    fun `createFormElements returns all billing details fields`() {
        CryptoDefinition.basicFormWithBillingInformationTest()
    }
}
