package com.stripe.android.ui.core.elements

import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.R
import com.stripe.android.uicore.elements.PostalCodeConfig
import com.stripe.android.uicore.elements.TextFieldState
import org.junit.Test

class PostalCodeConfigTest {
    @Test
    fun `verify US config uses proper keyboard capitalization & keyboard type`() {
        with(createConfigForCountry("US")) {
            assertThat(capitalization).isEqualTo(KeyboardCapitalization.None)
            assertThat(keyboard).isEqualTo(KeyboardType.NumberPassword)
        }
    }

    @Test
    fun `verify CA config uses proper keyboard capitalization & keyboard type`() {
        with(createConfigForCountry("CA")) {
            assertThat(capitalization).isEqualTo(KeyboardCapitalization.Characters)
            assertThat(keyboard).isEqualTo(KeyboardType.Text)
        }
    }

    @Test
    fun `verify Other config uses proper keyboard capitalization & keyboard type`() {
        with(createConfigForCountry("UK")) {
            assertThat(capitalization).isEqualTo(KeyboardCapitalization.Characters)
            assertThat(keyboard).isEqualTo(KeyboardType.Text)
        }
    }

    @Test
    fun `verify US postal codes`() {
        with(createConfigForCountry("US")) {
            assertThat(determineStateForInput("").isValid()).isFalse()
            assertThat(determineStateForInput("").isFull()).isFalse()
            assertThat(determineStateForInput("12345").isValid()).isTrue()
            assertThat(determineStateForInput("12345").isFull()).isTrue()
            assertThat(determineStateForInput("abcde").isValid()).isFalse()
            assertThat(determineStateForInput("abcde").isFull()).isFalse()
        }
    }

    @Test
    fun `verify CA postal codes`() {
        with(createConfigForCountry("CA")) {
            assertThat(determineStateForInput("").isValid()).isFalse()
            assertThat(determineStateForInput("").isFull()).isFalse()
            assertThat(determineStateForInput("AAA AAA").isValid()).isFalse()
            assertThat(determineStateForInput("AAAAAA").isValid()).isFalse()
            assertThat(determineStateForInput("A0A 0A0").isValid()).isTrue()
            assertThat(determineStateForInput("A0A 0A0").isFull()).isTrue()
            assertThat(determineStateForInput("A0A0A0").isValid()).isTrue()
            assertThat(determineStateForInput("A0A0A0").isFull()).isTrue()
        }
    }

    @Test
    fun `verify other postal codes`() {
        with(createConfigForCountry("UK")) {
            assertThat(determineStateForInput("").isValid()).isFalse()
            assertThat(determineStateForInput("").isFull()).isFalse()
            assertThat(determineStateForInput(" ").isValid()).isFalse()
            assertThat(determineStateForInput(" ").isFull()).isFalse()
            assertThat(determineStateForInput("a").isValid()).isTrue()
            assertThat(determineStateForInput("a").isFull()).isFalse()
            assertThat(determineStateForInput("1").isValid()).isTrue()
            assertThat(determineStateForInput("1").isFull()).isFalse()
            assertThat(determineStateForInput("aaaaaa").isValid()).isTrue()
            assertThat(determineStateForInput("111111").isValid()).isTrue()
        }
    }

    @Test
    fun `invalid US postal codes emit error`() {
        with(createConfigForCountry("US")) {
            assertThat(determineStateForInput("").getError()).isNull()
            assertThat(determineStateForInput("1234").getError()).isNotNull()
            assertThat(determineStateForInput("12345").getError()).isNull()
        }
    }

    @Test
    fun `invalid CA postal codes emit error`() {
        with(createConfigForCountry("CA")) {
            assertThat(determineStateForInput("").getError()).isNull()
            assertThat(determineStateForInput("1N8E8R").getError()).isNotNull()
            assertThat(determineStateForInput("141124").getError()).isNotNull()
        }
    }

    private fun createConfigForCountry(country: String): PostalCodeConfig {
        return PostalCodeConfig(
            label = R.string.stripe_address_label_postal_code,
            country = country
        )
    }

    private fun PostalCodeConfig.determineStateForInput(input: String): TextFieldState {
        return determineState(filter(input))
    }
}
