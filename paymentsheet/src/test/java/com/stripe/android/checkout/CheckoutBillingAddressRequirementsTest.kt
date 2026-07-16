package com.stripe.android.checkout

import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.uicore.elements.IdentifierSpec
import org.junit.Test

@OptIn(CheckoutSessionPreview::class)
class CheckoutBillingAddressRequirementsTest {

    @Test
    fun `US requires line1, city, state, and postal code`() {
        assertThat(CheckoutBillingAddressRequirements.requiredFields("US"))
            .containsExactly(IdentifierSpec.Line1, IdentifierSpec.City, IdentifierSpec.State, IdentifierSpec.PostalCode)
            .inOrder()
    }

    @Test
    fun `PR requires line1, city, and postal code`() {
        assertThat(CheckoutBillingAddressRequirements.requiredFields("PR"))
            .containsExactly(IdentifierSpec.Line1, IdentifierSpec.City, IdentifierSpec.PostalCode)
            .inOrder()
    }

    @Test
    fun `CA requires only postal code`() {
        assertThat(CheckoutBillingAddressRequirements.requiredFields("CA"))
            .containsExactly(IdentifierSpec.PostalCode)
    }

    @Test
    fun `GB requires only postal code`() {
        assertThat(CheckoutBillingAddressRequirements.requiredFields("GB"))
            .containsExactly(IdentifierSpec.PostalCode)
    }

    @Test
    fun `IN requires only postal code`() {
        assertThat(CheckoutBillingAddressRequirements.requiredFields("IN"))
            .containsExactly(IdentifierSpec.PostalCode)
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
            .containsExactly(IdentifierSpec.Line1, IdentifierSpec.City, IdentifierSpec.State, IdentifierSpec.PostalCode)
            .inOrder()
    }

    @Test
    fun `only the five expected countries require additional fields`() {
        val countriesWithFields = listOf("CA", "GB", "IN", "PR", "US")
            .associateWith { CheckoutBillingAddressRequirements.requiredFields(it) }
            .filterValues { it.isNotEmpty() }

        assertThat(countriesWithFields.keys)
            .containsExactly("CA", "GB", "IN", "PR", "US")
    }
}
