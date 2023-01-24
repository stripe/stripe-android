package com.stripe.android.ui.core.elements

import com.google.common.truth.Truth.assertThat
import com.stripe.android.uicore.address.AutocompleteCapableAddressType
import com.stripe.android.uicore.elements.AddressType
import com.stripe.android.uicore.elements.PhoneNumberState
import org.junit.Test

internal class AutocompleteCapableAddressTypeTest {
    @Test
    fun `supportsAutoComplete returns true when available and supported`() {
        val subject = createSubject(googleApiKey = "example", autocompleteCountries = setOf("US"))
        assertThat(
            subject.supportsAutoComplete("US") {
                true
            }
        ).isTrue()
    }

    @Test
    fun `supportsAutoComplete returns false when not available but supported`() {
        val subject = createSubject(googleApiKey = "example", autocompleteCountries = setOf("CAN"))
        assertThat(
            subject.supportsAutoComplete("US") {
                true
            }
        ).isFalse()
    }

    @Test
    fun `supportsAutoComplete returns false when available but not supported`() {
        val subject = createSubject(googleApiKey = "example", autocompleteCountries = setOf("US"))
        assertThat(
            subject.supportsAutoComplete("US") {
                false
            }
        ).isFalse()
    }

    @Test
    fun `supportsAutoComplete returns false when available and supported but missing api key`() {
        val subject = createSubject(googleApiKey = null, autocompleteCountries = setOf("US"))
        assertThat(
            subject.supportsAutoComplete("US") {
                true
            }
        ).isFalse()
    }

    @Test
    fun `supportsAutoComplete returns true with lowercase autocomplete country`() {
        val subject = createSubject(googleApiKey = "example", autocompleteCountries = setOf("us"))
        assertThat(
            subject.supportsAutoComplete("US") {
                true
            }
        ).isTrue()
    }

    @Test
    fun `supportsAutoComplete returns true when available and supported with lowercase country`() {
        val subject = createSubject(googleApiKey = "example", autocompleteCountries = setOf("US"))
        assertThat(
            subject.supportsAutoComplete("us") {
                true
            }
        ).isTrue()
    }

    @Test
    fun `supportsAutoComplete returns true with lowercase countries`() {
        val subject = createSubject(googleApiKey = "example", autocompleteCountries = setOf("us"))
        assertThat(
            subject.supportsAutoComplete("us") {
                true
            }
        ).isTrue()
    }

    private fun createSubject(
        googleApiKey: String?,
        autocompleteCountries: Set<String>?,
    ): AutocompleteCapableAddressType {
        return AddressType.ShippingExpanded(
            googleApiKey = googleApiKey,
            autocompleteCountries = autocompleteCountries,
            phoneNumberState = PhoneNumberState.REQUIRED
        ) {}
    }
}
