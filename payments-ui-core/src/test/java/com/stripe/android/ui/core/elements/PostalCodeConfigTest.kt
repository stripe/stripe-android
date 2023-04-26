package com.stripe.android.ui.core.elements

import com.google.common.truth.Truth
import com.stripe.android.core.R
import com.stripe.android.uicore.elements.PostalCodeConfig
import com.stripe.android.uicore.elements.TextFieldState
import org.junit.Test

class PostalCodeConfigTest {

    @Test
    fun `verify US postal codes`() {
        with(createConfigForCountry("US")) {
            Truth.assertThat(determineStateForInput("").isValid()).isFalse()
            Truth.assertThat(determineStateForInput("").isFull()).isFalse()
            Truth.assertThat(determineStateForInput("12345").isValid()).isTrue()
            Truth.assertThat(determineStateForInput("12345").isFull()).isTrue()
            Truth.assertThat(determineStateForInput("abcde").isValid()).isFalse()
            Truth.assertThat(determineStateForInput("abcde").isFull()).isTrue()
        }
    }

    @Test
    fun `verify CA postal codes`() {
        with(createConfigForCountry("CA")) {
            Truth.assertThat(determineStateForInput("").isValid()).isFalse()
            Truth.assertThat(determineStateForInput("").isFull()).isFalse()
            Truth.assertThat(determineStateForInput("AAA AAA").isValid()).isFalse()
            Truth.assertThat(determineStateForInput("AAAAAA").isValid()).isFalse()
            Truth.assertThat(determineStateForInput("A0A 0A0").isValid()).isTrue()
            Truth.assertThat(determineStateForInput("A0A 0A0").isFull()).isTrue()
            Truth.assertThat(determineStateForInput("A0A0A0").isValid()).isTrue()
            Truth.assertThat(determineStateForInput("A0A0A0").isFull()).isTrue()
        }
    }

    @Test
    fun `verify other postal codes`() {
        with(createConfigForCountry("UK")) {
            Truth.assertThat(determineStateForInput("").isValid()).isFalse()
            Truth.assertThat(determineStateForInput("").isFull()).isFalse()
            Truth.assertThat(determineStateForInput(" ").isValid()).isFalse()
            Truth.assertThat(determineStateForInput(" ").isFull()).isFalse()
            Truth.assertThat(determineStateForInput("a").isValid()).isTrue()
            Truth.assertThat(determineStateForInput("a").isFull()).isFalse()
            Truth.assertThat(determineStateForInput("1").isValid()).isTrue()
            Truth.assertThat(determineStateForInput("1").isFull()).isFalse()
            Truth.assertThat(determineStateForInput("aaaaaa").isValid()).isTrue()
            Truth.assertThat(determineStateForInput("111111").isValid()).isTrue()
        }
    }

    @Test
    fun `invalid postal codes emit error`() {
        with(createConfigForCountry("US")) {
            Truth.assertThat(determineStateForInput("").getError()).isNull()
            Truth.assertThat(determineStateForInput("1234").getError()).isNotNull()
            Truth.assertThat(determineStateForInput("12345").getError()).isNull()
        }
    }

    private fun createConfigForCountry(country: String): PostalCodeConfig {
        return PostalCodeConfig(
            label = R.string.stripe_address_label_postal_code,
            country = country
        )
    }

    private fun PostalCodeConfig.determineStateForInput(input: String): TextFieldState {
        return determineState(filter(convertFromRaw(input)))
    }
}
