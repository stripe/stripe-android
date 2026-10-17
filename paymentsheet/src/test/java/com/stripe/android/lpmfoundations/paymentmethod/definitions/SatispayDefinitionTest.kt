package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.stripe.android.ui.core.R
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SatispayDefinitionTest {
    @Test
    fun `createFormElements returns no elements if payment intent`() {
        SatispayDefinition.basicEmptyFormTest()
    }

    @Test
    fun `createFormElements returns mandate if setup intent`() {
        SatispayDefinition.mandateTest(R.string.stripe_satispay_mandate) {
            listOf(it.merchantName)
        }
    }

    @Test
    fun `createFormElements returns no elements if terms display is set to never`() {
        SatispayDefinition.noMandateWithTermsDisplayNeverTest()
    }

    @Test
    fun `createFormElements returns mandate & requested contact information fields`() {
        SatispayDefinition.mandateWithContactFieldsTest(R.string.stripe_satispay_mandate) {
            listOf(it.merchantName)
        }
    }

    @Test
    fun `createFormElements returns mandate & all billing details fields`() {
        SatispayDefinition.mandateWithBillingInformationTest(R.string.stripe_satispay_mandate) {
            listOf(it.merchantName)
        }
    }
}
