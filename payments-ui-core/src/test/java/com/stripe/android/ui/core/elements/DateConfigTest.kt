package com.stripe.android.ui.core.elements

import androidx.compose.ui.unit.LayoutDirection
import com.google.common.truth.Truth
import com.stripe.android.uicore.elements.DateConfig
import com.stripe.android.uicore.elements.TextFieldStateConstants
import com.stripe.android.utils.isInstanceOf
import org.junit.Test
import java.util.Calendar
import com.stripe.android.uicore.R as UiCoreR

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
            .isInstanceOf<TextFieldStateConstants.Error.Incomplete>()
        Truth.assertThat(
            state.getError()?.errorMessage
        ).isEqualTo(UiCoreR.string.stripe_incomplete_expiry_date)
    }

    @Test
    fun `date is too long`() {
        val state = dateConfig.determineState("1234567890123456789")
        Truth.assertThat(state)
            .isInstanceOf<TextFieldStateConstants.Error.Invalid>()
        Truth.assertThat(
            state.getError()?.errorMessage
        ).isEqualTo(UiCoreR.string.stripe_incomplete_expiry_date)
    }

    @Test
    fun `date invalid month and 2 digit year`() {
        val state = dateConfig.determineState("1955")
        Truth.assertThat(state)
            .isInstanceOf<TextFieldStateConstants.Error.Invalid>()
        Truth.assertThat(
            state.getError()?.errorMessage
        ).isEqualTo(UiCoreR.string.stripe_incomplete_expiry_date)
    }

    @Test
    fun `date in the past`() {
        val state = dateConfig.determineState("1299")
        Truth.assertThat(state)
            .isInstanceOf<TextFieldStateConstants.Error.Invalid>()
        Truth.assertThat(
            state.getError()?.errorMessage
        ).isEqualTo(UiCoreR.string.stripe_invalid_expiry_year)
    }

    @Test
    fun `date in the near past`() {
        val state = dateConfig.determineState("1220")
        Truth.assertThat(state)
            .isInstanceOf<TextFieldStateConstants.Error.Invalid>()
        Truth.assertThat(
            state.getError()?.errorMessage
        ).isEqualTo(UiCoreR.string.stripe_invalid_expiry_year)
    }

    @Test
    fun `current month and year`() {
        val input = produceInput(
            month = get1BasedCurrentMonth(),
            year = Calendar.getInstance().get(Calendar.YEAR) % 100,
        )

        val state = dateConfig.determineState(input)
        Truth.assertThat(state)
            .isInstanceOf<TextFieldStateConstants.Valid.Full>()
    }

    @Test
    fun `next month`() {
        var month = get1BasedCurrentMonth()
        var year = Calendar.getInstance().get(Calendar.YEAR) % 100

        if (month == 12) {
            month = 1
            year += 1
        }

        val input = produceInput(month, year)
        val state = dateConfig.determineState(input)

        Truth.assertThat(state)
            .isInstanceOf<TextFieldStateConstants.Valid.Full>()
    }

    @Test
    fun `current month and year + 1`() {
        val input = produceInput(
            month = get1BasedCurrentMonth(),
            year = (Calendar.getInstance().get(Calendar.YEAR) + 1) % 100,
        )
        val state = dateConfig.determineState(input)

        Truth.assertThat(state)
            .isInstanceOf<TextFieldStateConstants.Valid.Full>()
    }

    @Test
    fun `current month - 1 and year`() {
        var previousMonth = get1BasedCurrentMonth() - 1
        var year = Calendar.getInstance().get(Calendar.YEAR) % 100
        var expectedErrorMessage = UiCoreR.string.stripe_invalid_expiry_month

        // On January, use December of previous year.
        if (previousMonth == 0) {
            previousMonth = 12
            year -= 1
            expectedErrorMessage = UiCoreR.string.stripe_invalid_expiry_year
        }

        val input = produceInput(
            month = previousMonth,
            year = year,
        )

        val state = dateConfig.determineState(input)

        Truth.assertThat(state)
            .isInstanceOf<TextFieldStateConstants.Error.Invalid>()
        Truth.assertThat(
            state.getError()?.errorMessage
        ).isEqualTo(expectedErrorMessage)
    }

    @Test
    fun `current month and year - 1`() {
        val input = produceInput(
            month = get1BasedCurrentMonth(),
            year = (Calendar.getInstance().get(Calendar.YEAR) - 1) % 100,
        )

        val state = dateConfig.determineState(input)

        Truth.assertThat(state)
            .isInstanceOf<TextFieldStateConstants.Error.Invalid>()
        Truth.assertThat(
            state.getError()?.errorMessage
        ).isEqualTo(UiCoreR.string.stripe_invalid_expiry_year)
    }

    @Test
    fun `card expire 51 years from now`() {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val state = DateConfig.determineTextFieldState(
            3,
            (currentYear + 51) % 100,
            2,
            currentYear
        )
        Truth.assertThat(state)
            .isInstanceOf<TextFieldStateConstants.Error.Invalid>()
        Truth.assertThat(
            state.getError()?.errorMessage
        ).isEqualTo(UiCoreR.string.stripe_invalid_expiry_year)
    }

    @Test
    fun `card expire 50 years from now`() {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val state = DateConfig.determineTextFieldState(
            3,
            (currentYear + 50) % 100,
            2,
            currentYear
        )
        Truth.assertThat(state)
            .isInstanceOf<TextFieldStateConstants.Valid.Full>()
    }

    private fun get1BasedCurrentMonth() = Calendar.getInstance().get(Calendar.MONTH) + 1

    @Test
    fun `date is valid 2 digit month and 2 digit year`() {
        val state = dateConfig.determineState("1255")
        Truth.assertThat(state)
            .isInstanceOf<TextFieldStateConstants.Valid.Full>()
    }

    @Test
    fun `date is invalid 2X month and 2 digit year`() {
        val state = dateConfig.determineState("2123")
        Truth.assertThat(state)
            .isInstanceOf<TextFieldStateConstants.Error.Invalid>()
        Truth.assertThat(
            state.getError()?.errorMessage
        ).isEqualTo(UiCoreR.string.stripe_incomplete_expiry_date)
    }

    @Test
    fun `date is valid one digit month two digit year`() {
        val state = dateConfig.determineState("130")
        Truth.assertThat(state)
            .isInstanceOf<TextFieldStateConstants.Valid.Full>()
    }

    @Test
    fun `date is valid 0X month two digit year`() {
        val state = dateConfig.determineState("0130")
        Truth.assertThat(state)
            .isInstanceOf<TextFieldStateConstants.Valid.Full>()
    }

    @Test
    fun `date is valid 2X month and 2 digit year`() {
        val state = dateConfig.determineState("230")
        Truth.assertThat(state)
            .isInstanceOf<TextFieldStateConstants.Valid.Full>()
    }

    @Test
    fun `Layout direction should be Ltr`() {
        Truth.assertThat(dateConfig.layoutDirection).isEqualTo(LayoutDirection.Ltr)
    }

    private fun produceInput(month: Int, year: Int): String {
        val formattedMonth = month.toString().padStart(length = 2, padChar = '0')
        val formattedYear = year.toString().padStart(length = 2, padChar = '0')
        return formattedMonth + formattedYear
    }
}
