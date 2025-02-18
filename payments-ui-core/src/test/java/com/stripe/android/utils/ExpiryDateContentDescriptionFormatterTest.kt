package com.stripe.android.utils

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.ui.core.elements.formatExpirationDateForAccessibility
import com.stripe.android.uicore.R
import org.junit.Test

class ExpiryDateContentDescriptionFormatterTest {

    @Test
    fun `formats correctly for empty input`() {
        val result = formatExpirationDateForAccessibility("4")
        val expected = resolvableString(
            R.string.stripe_expiration_date_month_complete_content_description,
            "April"
        )

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `formats correctly for month only`() {
        val result = formatExpirationDateForAccessibility("4")
        val expected = resolvableString(
            R.string.stripe_expiration_date_month_complete_content_description,
            "April"
        )

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `formats correctly for month and incomplete year`() {
        val result = formatExpirationDateForAccessibility("55")
        val expected = resolvableString(
            R.string.stripe_expiration_date_year_incomplete_content_description,
            "May"
        )

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `formats correctly for month and year`() {
        val result = formatExpirationDateForAccessibility("555")
        val expected = resolvableString(
            R.string.stripe_expiration_date_content_description,
            "May",
            2055
        )

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `formats correctly for double digit month`() {
        val result = formatExpirationDateForAccessibility("1255")
        val expected = resolvableString(
            R.string.stripe_expiration_date_content_description,
            "December",
            2055
        )

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `formats correctly for single digit month with leading 0`() {
        val result = formatExpirationDateForAccessibility("0155")
        val expected = resolvableString(
            R.string.stripe_expiration_date_content_description,
            "January",
            2055
        )

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `formats first two numbers as month if less than 13`() {
        val result = formatExpirationDateForAccessibility("126")
        val expected = resolvableString(
            R.string.stripe_expiration_date_year_incomplete_content_description,
            "December"
        )

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `formats month correctly based on locale`() {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("FR"))
        val result = formatExpirationDateForAccessibility("126")
        val expected = resolvableString(
            R.string.stripe_expiration_date_year_incomplete_content_description,
            "décembre"
        )

        assertThat(result).isEqualTo(expected)
    }
}
