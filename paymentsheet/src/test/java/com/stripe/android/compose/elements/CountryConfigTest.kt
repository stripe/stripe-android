package com.stripe.android.compose.elements

import com.google.common.truth.Truth.assertThat
import com.stripe.android.compose.elements.country.CountryConfig
import org.junit.Test

class CountryConfigTest {
    private val countryConfig = CountryConfig()

    @Test
    fun `verify country display name converted to country code`() {
        assertThat(countryConfig.convertToPaymentMethodParam("United States"))
            .isEqualTo("US")
    }

    @Test
    fun `verify country code converted to country display name`() {
        assertThat(countryConfig.convertToDisplay("US"))
            .isEqualTo("United States")
    }
}