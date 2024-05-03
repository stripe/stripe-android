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
    }

    @Test
    fun `Can add more than minimum length for phone numbers`() {
        val usFilteredNumber = PhoneNumberFormatter.forCountry("US")
            .userInputFilter("123456789012")

        assertThat(usFilteredNumber).isEqualTo("123456789012")
        assertThat(usFilteredNumber.length).isGreaterThan(
            PhoneNumberFormatter.lengthForCountry("US")
        )

        val gbFilteredNumber = PhoneNumberFormatter.forCountry("GB")
            .userInputFilter("01234567890123")

        assertThat(gbFilteredNumber).isEqualTo("01234567890123")
        assertThat(gbFilteredNumber.length).isGreaterThan(
            PhoneNumberFormatter.lengthForCountry("GB")
        )
    }

    @Test
    fun `Phone number is correctly formatted for FI locale`() {
        val formatter = PhoneNumberFormatter.forCountry("FI") // "## ### ## ##"

        assertThat(formatter.format("123")).isEqualTo("12 3")
        assertThat(formatter.format("1234")).isEqualTo("12 34")
        assertThat(formatter.format("123456")).isEqualTo("12 345 6")
        assertThat(formatter.format("1234567")).isEqualTo("12 345 67")
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
    }

    @Test
    fun `Should filter out digits past E164 defined maximum digits`() {
        val withRegionFormatter = PhoneNumberFormatter.WithRegion(
            PhoneNumberFormatter.Metadata(
                prefix = "prefix",
                regionCode = "regionCode",
                pattern = "(###)-###+#####!#"
            )
        )

        assertThat(
            withRegionFormatter.userInputFilter("1234567890123456789")
        ).isEqualTo("123456789012345")

        val unknownRegionFormatter = PhoneNumberFormatter.UnknownRegion(
            countryCode = "ZZ"
        )

        assertThat(
            unknownRegionFormatter.userInputFilter("1234567890123456789")
        ).isEqualTo("123456789012345")
    }

    @Test
    fun `Leading zeros should be stripped in E164 format`() {
        val formatter = PhoneNumberFormatter.forCountry("GB")

        assertThat(formatter.toE164Format("01371112")).isEqualTo("+441371112")
        assertThat(formatter.toE164Format("013711122")).isEqualTo("+4413711122")
        assertThat(formatter.toE164Format("0137111222")).isEqualTo("+44137111222")
    }

    @Test
    fun `When pattern is missing, there is no formatting`() {
        val formatter = PhoneNumberFormatter.WithRegion(
            PhoneNumberFormatter.Metadata(
                prefix = "prefix",
                regionCode = "regionCode",
                pattern = null
            )
        )

        assertThat(formatter.format("123")).isEqualTo("123")
        assertThat(formatter.format("1234567")).isEqualTo("1234567")
        assertThat(formatter.format("123456789012")).isEqualTo("123456789012")
        assertThat(formatter.format("123456789012456")).isEqualTo("123456789012456")
    }

    private fun PhoneNumberFormatter.format(input: String) =
        visualTransformation.filter(AnnotatedString(userInputFilter(input))).text.text
}
