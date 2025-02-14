package com.stripe.android.uicore.utils

import androidx.compose.ui.text.intl.Locale
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.uicore.R
import org.junit.Test

class ExpiryDateContentDescriptionFormatterTest {

    @Test
    fun `formats correctly for empty input`() {
        val result = formatExpirationDateForAccessibility(Locale.current, "4")
        val expected = resolvableString(
            R.string.stripe_expiration_date_month_complete_content_description,
            "April"
        )

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `formats correctly for month only`() {
        val result = formatExpirationDateForAccessibility(Locale.current, "4")
        val expected = resolvableString(
            R.string.stripe_expiration_date_month_complete_content_description,
            "April"
        )

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `formats correctly for month and incomplete year`() {
        val result = formatExpirationDateForAccessibility(Locale.current, "55")
        val expected = resolvableString(
            R.string.stripe_expiration_date_year_incomplete_content_description,
            "May"
        )

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `formats correctly for month and year`() {
        val result = formatExpirationDateForAccessibility(Locale.current, "555")
        val expected = resolvableString(
            R.string.stripe_expiration_date_content_description,
            "May",
            2055
        )

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `formats correctly for double digit month`() {
        val result = formatExpirationDateForAccessibility(Locale.current, "1255")
        val expected = resolvableString(
            R.string.stripe_expiration_date_content_description,
            "December",
            2055
        )

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `formats correctly for single digit month with leading 0`() {
        val result = formatExpirationDateForAccessibility(Locale.current, "0155")
        val expected = resolvableString(
            R.string.stripe_expiration_date_content_description,
            "January",
            2055
        )

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `formats first two numbers as month if less than 13`() {
        val result = formatExpirationDateForAccessibility(Locale.current, "126")
        val expected = resolvableString(
            R.string.stripe_expiration_date_year_incomplete_content_description,
            "December"
        )

        assertThat(result).isEqualTo(expected)
    }
}
