package com.stripe.android.uicore.elements

import androidx.compose.ui.text.AnnotatedString
import com.google.common.truth.Truth.assertThat
import org.junit.Test

internal class PhoneNumberFormatterTest {

    @Test
    fun `Phone number is correctly formatted for US locale`() {
        val formatter = PhoneNumberFormatter.forCountry("US") // "(###) ###-####"

        assertThat(formatter.format("123")).isEqualTo("(123")
        assertThat(formatter.format("1234")).isEqualTo("(123) 4")
        assertThat(formatter.format("123456")).isEqualTo("(123) 456")
        assertThat(formatter.format("1234567")).isEqualTo("(123) 456-7")
        assertThat(formatter.format("1234567890")).isEqualTo("(123) 456-7890")
        // prefix has 1 digit so full number must be at most 14 digits
        assertThat(formatter.format("12345asdfg678901234567890"))
            .isEqualTo("(123) 456-7890")
    }

    @Test
    fun `Phone number is correctly formatted for FI locale`() {
        val formatter = PhoneNumberFormatter.forCountry("FI") // "## ### ## ##"

        assertThat(formatter.format("123")).isEqualTo("12 3")
        assertThat(formatter.format("1234")).isEqualTo("12 34")
        assertThat(formatter.format("123456")).isEqualTo("12 345 6")
        assertThat(formatter.format("1234567")).isEqualTo("12 345 67")
        assertThat(formatter.format("1234567890")).isEqualTo("12 345 67 89")
        // prefix has 3 digits so full number must be at most 12 digits
        assertThat(formatter.format("12345asdfg678901234567890"))
            .isEqualTo("12 345 67 89")
    }

    @Test
    fun `Phone number is correctly formatted when locale does not exist`() {
        val formatter = PhoneNumberFormatter.forCountry("ZZ")

        assertThat(formatter.format("123asdf+")).isEqualTo("+123")
        assertThat(formatter.format("+abc12+34")).isEqualTo("+1234")
        assertThat(formatter.format("+12345asdfg6+++789012345ð6789"))
            .isEqualTo("+123456789012345")
    }

    @Test
    fun `WithRegion correctly formats with pattern`() {
        val pattern = "(###)-###+#####!#"
        val formatter = PhoneNumberFormatter.WithRegion(
            PhoneNumberFormatter.Metadata(
                "prefix",
                "regionCode",
                pattern
            )
        )

        assertThat(formatter.format("123")).isEqualTo("(123")
        assertThat(formatter.format("1234567")).isEqualTo("(123)-456+7")
        assertThat(formatter.format("123456789012")).isEqualTo("(123)-456+78901!2")
        assertThat(formatter.format("123456789012456")).isEqualTo("(123)-456+78901!2")
    }

    private fun PhoneNumberFormatter.format(input: String) =
        visualTransformation.filter(AnnotatedString(userInputFilter(input))).text.text
}
