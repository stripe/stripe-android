package com.stripe.android.paymentsheet.elements

import com.google.common.truth.Truth
import com.stripe.android.paymentsheet.R
import org.junit.Test

class DateConfigTest {
    private val dateConfig = DateConfig()

    @Test
    fun `only numbers are allowed in the field`() {
        Truth.assertThat(dateConfig.filter("123^@Numbe/-r[\uD83E\uDD57."))
            .isEqualTo("123")
    }

    @Test
    fun `blank number returns blank state`() {
        Truth.assertThat(dateConfig.determineState(""))
            .isEqualTo(TextFieldStateConstants.Error.Blank)
    }

    @Test
    fun `incomplete number is in incomplete state`() {
        val state = dateConfig.determineState("12")
        Truth.assertThat(state)
            .isInstanceOf(TextFieldStateConstants.Error.Incomplete::class.java)
        Truth.assertThat(
            state.getError()?.errorMessage
        ).isEqualTo(R.string.incomplete_expiry_date)
    }

    @Test
    fun `date is too long`() {
        val state = dateConfig.determineState("1234567890123456789")
        Truth.assertThat(state)
            .isInstanceOf(TextFieldStateConstants.Error.Invalid::class.java)
        Truth.assertThat(
            state.getError()?.errorMessage
        ).isEqualTo(R.string.invalid_expiry_date)
    }

    @Test
    fun `date invalid month and 2 digit year`() {
        val state = dateConfig.determineState("1955")
        Truth.assertThat(state)
            .isInstanceOf(TextFieldStateConstants.Error.Invalid::class.java)
        Truth.assertThat(
            state.getError()?.errorMessage
        ).isEqualTo(R.string.invalid_expiry_date)
    }

    @Test
    fun `date in the past`() {
        val state = dateConfig.determineState("1299")
        Truth.assertThat(state)
            .isInstanceOf(TextFieldStateConstants.Error.Invalid::class.java)
        Truth.assertThat(
            state.getError()?.errorMessage
        ).isEqualTo(R.string.invalid_expiry_year)
    }

    @Test
    fun `date in the near past`() {
        val state = dateConfig.determineState("1220")
        Truth.assertThat(state)
            .isInstanceOf(TextFieldStateConstants.Error.Invalid::class.java)
        Truth.assertThat(
            state.getError()?.errorMessage
        ).isEqualTo(R.string.invalid_expiry_year_past)
    }

    @Test
    fun `date is valid 2 digit month and 2 digit year`() {
        val state = dateConfig.determineState("1255")
        Truth.assertThat(state)
            .isInstanceOf(TextFieldStateConstants.Valid.Full::class.java)
    }

    @Test
    fun `date is invalid 2X month and 2 digit year`() {
        val state = dateConfig.determineState("2123")
        Truth.assertThat(state)
            .isInstanceOf(TextFieldStateConstants.Error.Invalid::class.java)
        Truth.assertThat(
            state.getError()?.errorMessage
        ).isEqualTo(R.string.invalid_expiry_date)
    }

    @Test
    fun `date is valid one digit month two digit year`() {
        val state = dateConfig.determineState("130")
        Truth.assertThat(state)
            .isInstanceOf(TextFieldStateConstants.Valid.Full::class.java)
    }

    @Test
    fun `date is valid 0X month two digit year`() {
        val state = dateConfig.determineState("0130")
        Truth.assertThat(state)
            .isInstanceOf(TextFieldStateConstants.Valid.Full::class.java)
    }

    @Test
    fun `date is valid 2X month and 2 digit year`() {
        val state = dateConfig.determineState("222")
        Truth.assertThat(state)
            .isInstanceOf(TextFieldStateConstants.Valid.Full::class.java)
    }
}
