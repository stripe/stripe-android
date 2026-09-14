package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.stripe.android.ui.core.R
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class KrCardDefinitionTest {
    @Test
    fun `createFormElements returns no elements if payment intent`() {
        KrCardDefinition.basicEmptyFormTest()
    }

    @Test
    fun `createFormElements returns mandate if setup intent`() {
        KrCardDefinition.mandateTest(R.string.stripe_kr_card_mandate) {
            listOf(it.merchantName)
        }
    }

    @Test
    fun `createFormElements returns no elements if terms display is set to never`() {
        KrCardDefinition.noMandateWithTermsDisplayNeverTest()
    }

    @Test
    fun `createFormElements returns mandate and requested contact information fields`() {
        KrCardDefinition.mandateWithContactFieldsTest(R.string.stripe_kr_card_mandate) {
            listOf(it.merchantName)
        }
    }

    @Test
    fun `createFormElements returns mandate and all billing details fields`() {
        KrCardDefinition.mandateWithBillingInformationTest(R.string.stripe_kr_card_mandate) {
            listOf(it.merchantName)
        }
    }
}
