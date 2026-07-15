package com.stripe.android.checkout

import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkout.CheckoutBillingAddressRequirements.Field
import com.stripe.android.paymentelement.CheckoutSessionPreview
import org.junit.Test

@OptIn(CheckoutSessionPreview::class)
class CheckoutBillingAddressRequirementsTest {

    @Test
    fun `US requires line1, city, state, and postal code`() {
        assertThat(CheckoutBillingAddressRequirements.requiredFields("US"))
            .containsExactly(Field.LINE1, Field.CITY, Field.STATE, Field.POSTAL_CODE)
            .inOrder()
    }

    @Test
    fun `PR requires line1, city, and postal code`() {
        assertThat(CheckoutBillingAddressRequirements.requiredFields("PR"))
            .containsExactly(Field.LINE1, Field.CITY, Field.POSTAL_CODE)
            .inOrder()
    }

    @Test
    fun `CA requires only postal code`() {
        assertThat(CheckoutBillingAddressRequirements.requiredFields("CA"))
            .containsExactly(Field.POSTAL_CODE)
    }

    @Test
    fun `GB requires only postal code`() {
        assertThat(CheckoutBillingAddressRequirements.requiredFields("GB"))
            .containsExactly(Field.POSTAL_CODE)
    }

    @Test
    fun `IN requires only postal code`() {
        assertThat(CheckoutBillingAddressRequirements.requiredFields("IN"))
            .containsExactly(Field.POSTAL_CODE)
    }

    @Test
    fun `country not in the map requires no additional fields`() {
        assertThat(CheckoutBillingAddressRequirements.requiredFields("FR")).isEmpty()
    }

    @Test
    fun `unknown or empty country code requires no additional fields`() {
        assertThat(CheckoutBillingAddressRequirements.requiredFields("")).isEmpty()
        assertThat(CheckoutBillingAddressRequirements.requiredFields("ZZ")).isEmpty()
    }

    @Test
    fun `lookup is case insensitive`() {
        assertThat(CheckoutBillingAddressRequirements.requiredFields("us"))
            .containsExactly(Field.LINE1, Field.CITY, Field.STATE, Field.POSTAL_CODE)
            .inOrder()
    }
}
