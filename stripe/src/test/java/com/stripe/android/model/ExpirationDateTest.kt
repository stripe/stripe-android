package com.stripe.android.model

import com.google.common.truth.Truth.assertThat
import kotlin.test.Test

class ExpirationDateTest {

    @Test
    fun unvalidatedCreate_withSeparator_createsExpectedValue() {
        assertThat(
            ExpirationDate.Unvalidated.create("10/20")
        ).isEqualTo(
            ExpirationDate.Unvalidated(
                month = "10",
                year = "20"
            )
        )
    }

    @Test
    fun unvalidatedCreate_withValidDate_createsExpectedValue() {
        assertThat(
            ExpirationDate.Unvalidated.create("1234")
        ).isEqualTo(
            ExpirationDate.Unvalidated(
                month = "12",
                year = "34"
            )
        )
    }

    @Test
    fun unvalidatedCreate_withPartialDate_createsExpectedValue() {
        assertThat(
            ExpirationDate.Unvalidated.create("123")
        ).isEqualTo(
            ExpirationDate.Unvalidated(
                month = "12",
                year = "3"
            )
        )
    }

    @Test
    fun unvalidatedCreate_withLessThanHalfOfDate_createsExpectedValue() {
        assertThat(
            ExpirationDate.Unvalidated.create("1")
        ).isEqualTo(
            ExpirationDate.Unvalidated(
                month = "1",
                year = ""
            )
        )
    }

    @Test
    fun unvalidatedCreate_withEmptyInput_returnsNonNullEmptyOutput() {
        assertThat(
            ExpirationDate.Unvalidated.create("")
        ).isEqualTo(
            ExpirationDate.Unvalidated(
                month = "",
                year = ""
            )
        )
    }

    @Test
    fun isMonthValid_forProperMonths_returnsTrue() {
        val validMonths = listOf(
            "01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12"
        )

        assertThat(
            validMonths.all {
                ExpirationDate.Unvalidated.create(it).isMonthValid
            }
        ).isTrue()
    }

    @Test
    fun isMonthValid_forInvalidNumericInput_returnsFalse() {
        assertThat(ExpirationDate.Unvalidated.create("15").isMonthValid)
            .isFalse()
        assertThat(ExpirationDate.Unvalidated.create("0").isMonthValid)
            .isFalse()
        assertThat(ExpirationDate.Unvalidated.create("-08").isMonthValid)
            .isFalse()
    }

    @Test
    fun isMonthValid_forEmptyString_returnsFalse() {
        assertThat(ExpirationDate.Unvalidated.create("").isMonthValid)
            .isFalse()
    }

    @Test
    fun isMonthValid_forNonNumericInput_returnsFalse() {
        assertThat(ExpirationDate.Unvalidated.create("     ").isMonthValid)
            .isFalse()
        assertThat(ExpirationDate.Unvalidated.create("abc").isMonthValid)
            .isFalse()

        // This is looking for a valid numeric month, not month names.
        assertThat(ExpirationDate.Unvalidated.create("January").isMonthValid)
            .isFalse()
        assertThat(ExpirationDate.Unvalidated.create("\n").isMonthValid)
            .isFalse()
    }

    @Test
    fun getDisplayString_whenDateHasOneDigitMonthAndYear_addsZero() {
        assertThat(ExpirationDate.Unvalidated(1, 2).getDisplayString())
            .isEqualTo("0102")
    }

    @Test
    fun getDisplayString_whenDateHasTwoDigitValues_returnsExpectedValue() {
        assertThat(ExpirationDate.Unvalidated(11, 32).getDisplayString())
            .isEqualTo("1132")
    }

    @Test
    fun getDisplayString_whenDateHasFullYear_truncatesYear() {
        assertThat(ExpirationDate.Unvalidated(1, 2032).getDisplayString())
            .isEqualTo("0132")
    }

    @Test
    fun getDisplayString_whenDateHasThreeDigitYear_returnsEmpty() {
        assertThat(ExpirationDate.Unvalidated(12, 101).getDisplayString())
            .isEmpty()
    }
}
