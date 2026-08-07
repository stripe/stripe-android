package com.stripe.android.ui.core.elements

import com.google.common.truth.Truth.assertThat
import com.stripe.android.uicore.elements.IdentifierSpec
import org.junit.Test

class AutomaticTaxBillingFieldsTest {
    @Test
    fun `US requires line 1 city state and postal code`() {
        assertThat(automaticTaxRequiredFields("US")).containsExactly(
            IdentifierSpec.Line1,
            IdentifierSpec.City,
            IdentifierSpec.State,
            IdentifierSpec.PostalCode,
        )
    }

    @Test
    fun `CA requires postal code`() {
        assertThat(automaticTaxRequiredFields("CA")).containsExactly(IdentifierSpec.PostalCode)
    }

    @Test
    fun `GB requires postal code`() {
        assertThat(automaticTaxRequiredFields("GB")).containsExactly(IdentifierSpec.PostalCode)
    }

    @Test
    fun `IN requires postal code`() {
        assertThat(automaticTaxRequiredFields("IN")).containsExactly(IdentifierSpec.PostalCode)
    }

    @Test
    fun `PR requires line 1 city and postal code`() {
        assertThat(automaticTaxRequiredFields("PR")).containsExactly(
            IdentifierSpec.Line1,
            IdentifierSpec.City,
            IdentifierSpec.PostalCode,
        )
    }

    @Test
    fun `country absent from policy requires only country`() {
        assertThat(automaticTaxRequiredFields("DE")).isEmpty()
    }
}
