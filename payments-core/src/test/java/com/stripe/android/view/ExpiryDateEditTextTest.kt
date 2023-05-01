package com.stripe.android.view

import android.content.Context
import android.text.TextWatcher
import androidx.appcompat.view.ContextThemeWrapper
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.R
import com.stripe.android.model.ExpirationDate
import com.stripe.android.testharness.ViewTestUtils
import com.stripe.android.utils.TestUtils.idleLooper
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.robolectric.RobolectricTestRunner
import java.util.Calendar
import kotlin.test.Test
import com.stripe.android.uicore.R as UiCoreR

/**
 * Test class for [ExpiryDateEditText].
 */
@RunWith(RobolectricTestRunner::class)
class ExpiryDateEditTextTest {
    private val context: Context = ApplicationProvider.getApplicationContext<Context>()
    private val expiryDateEditText = ExpiryDateEditText(
        ContextThemeWrapper(
            ApplicationProvider.getApplicationContext(),
            R.style.StripeDefaultTheme
        )
    )

    @Test
    fun afterInputTwoDigits_textContainsSlash() {
        expiryDateEditText.append("1")
        expiryDateEditText.append("2")

        assertThat(expiryDateEditText.text.toString())
            .isEqualTo("12/")
    }

    @Test
    fun inputSingleDigit_whenDigitIsTooLargeForMonth_prependsZero() {
        expiryDateEditText.append("4")
        assertThat(expiryDateEditText.text.toString())
            .isEqualTo("04/")
        assertThat(expiryDateEditText.selectionStart)
            .isEqualTo(3)
    }

    @Test
    fun inputSingleDigit_whenDigitIsZeroOrOne_doesNotPrependZero() {
        expiryDateEditText.append("1")
        assertThat(expiryDateEditText.text.toString())
            .isEqualTo("1")
        assertThat(expiryDateEditText.selectionStart)
            .isEqualTo(1)
    }

    @Test
    fun inputSingleDigit_whenAtFirstCharacterButTextNotEmpty_doesNotPrependZero() {
        expiryDateEditText.append("1")
        // This case can occur when the user moves the cursor behind the already-entered text.
        expiryDateEditText.setSelection(0)
        expiryDateEditText.editableText.replace(0, 0, "3", 0, 1)

        assertThat(expiryDateEditText.text.toString())
            .isEqualTo("31")
        assertThat(expiryDateEditText.selectionStart)
            .isEqualTo(1)
    }

    @Test
    fun inputMultipleValidDigits_whenEmpty_doesNotPrependZeroOrShowErrorState() {
        expiryDateEditText.append("11")

        assertThat(expiryDateEditText.text.toString())
            .isEqualTo("11/")
        assertThat(expiryDateEditText.shouldShowError)
            .isFalse()
        assertThat(expiryDateEditText.selectionStart)
            .isEqualTo(3)
    }

    @Test
    fun afterInputThreeDigits_whenDeletingOne_textDoesNotContainSlash() {
        expiryDateEditText.append("1")
        expiryDateEditText.append("2")
        expiryDateEditText.append("3")

        ViewTestUtils.sendDeleteKeyEvent(expiryDateEditText)
        assertThat(expiryDateEditText.text.toString())
            .isEqualTo("12")
    }

    @Test
    fun afterAddingFinalDigit_whenGoingFromInvalidToValid_callsListener() {
        var invocations = 0
        expiryDateEditText.completionCallback = {
            invocations++
        }

        assertThat(Calendar.getInstance().get(Calendar.YEAR) <= 2059)
            .isTrue()

        expiryDateEditText.append("1")
        expiryDateEditText.append("2")
        expiryDateEditText.append("5")
        assertThat(invocations)
            .isEqualTo(0)

        expiryDateEditText.append("9")
        assertThat(expiryDateEditText.isDateValid)
            .isTrue()
        assertThat(invocations)
            .isEqualTo(1)
    }

    @Test
    fun afterAddingFinalDigit_whenDeletingItem_revertsToInvalidState() {
        var invocations = 0
        expiryDateEditText.completionCallback = {
            invocations++
        }

        assertThat(Calendar.getInstance().get(Calendar.YEAR) <= 2059)
            .isTrue()

        expiryDateEditText.append("12")
        expiryDateEditText.append("59")
        assertThat(expiryDateEditText.isDateValid)
            .isTrue()

        ViewTestUtils.sendDeleteKeyEvent(expiryDateEditText)
        assertThat(invocations)
            .isEqualTo(1)
        assertThat(expiryDateEditText.isDateValid)
            .isFalse()
    }

    @Test
    fun updateSelectionIndex_whenMovingAcrossTheGap_movesToEnd() {
        assertThat(
            expiryDateEditText.updateSelectionIndex(3, 1, 1, 5)
        ).isEqualTo(3)
    }

    @Test
    fun updateSelectionIndex_atStart_onlyMovesForwardByOne() {
        assertThat(
            expiryDateEditText.updateSelectionIndex(1, 0, 1, 5)
        ).isEqualTo(1)
    }

    @Test
    fun updateSelectionIndex_whenDeletingAcrossTheGap_staysAtEnd() {
        assertThat(
            expiryDateEditText.updateSelectionIndex(2, 4, 0, 5)
        ).isEqualTo(2)
    }

    @Test
    fun updateSelectionIndex_whenInputVeryLong_respectMaxInputLength() {
        assertThat(
            expiryDateEditText.updateSelectionIndex(6, 4, 2, 5)
        ).isEqualTo(5)
    }

    @Test
    fun inputZero_whenEmpty_doesNotShowErrorState() {
        expiryDateEditText.append("0")

        assertThat(expiryDateEditText.shouldShowError)
            .isFalse()
        assertThat(expiryDateEditText.text.toString())
            .isEqualTo("0")
    }

    @Test
    fun inputOne_whenEmpty_doesNotShowErrorState() {
        expiryDateEditText.append("1")

        assertThat(expiryDateEditText.shouldShowError)
            .isFalse()
        assertThat(expiryDateEditText.text.toString())
            .isEqualTo("1")
    }

    @Test
    fun inputTwoDigitMonth_whenInvalid_showsErrorAndDoesNotAddSlash() {
        expiryDateEditText.append("14")

        assertThat(expiryDateEditText.shouldShowError)
            .isTrue()
        assertThat(expiryDateEditText.errorMessage)
            .isEqualTo(context.getString(UiCoreR.string.stripe_incomplete_expiry_date))
        assertThat(expiryDateEditText.text.toString())
            .isEqualTo("14")
    }

    @Test
    fun inputThreeDigits_whenInvalid_showsErrorAndDoesAddSlash() {
        expiryDateEditText.append("143")

        assertThat(expiryDateEditText.shouldShowError)
            .isTrue()
        assertThat(expiryDateEditText.errorMessage)
            .isEqualTo(context.getString(UiCoreR.string.stripe_incomplete_expiry_date))
        assertThat(expiryDateEditText.text.toString())
            .isEqualTo("14/3")
    }

    @Test
    fun delete_whenAcrossSeparator_deletesSeparatorAndLastCharacterBeforeSeparator() {
        expiryDateEditText.append("12")
        assertThat(expiryDateEditText.text.toString())
            .isEqualTo("12/")

        ViewTestUtils.sendDeleteKeyEvent(expiryDateEditText)
        assertThat(expiryDateEditText.text.toString())
            .isEqualTo("1")
    }

    @Test
    fun inputCompleteDate_whenInPast_showsInvalid() {
        var invocations = 0
        expiryDateEditText.completionCallback = {
            invocations++
        }

        // This test will be invalid if run between 2080 and 2112. Please update the code.
        assertThat(Calendar.getInstance().get(Calendar.YEAR) < 2080)
            .isTrue()
        // Date translates to December, 2012 UNTIL the 2080, at which point it translates to
        // December, 2112. Also, this simulates a PASTE action.
        expiryDateEditText.append("1212")

        assertThat(expiryDateEditText.shouldShowError)
            .isTrue()
        assertThat(expiryDateEditText.errorMessage)
            .isEqualTo(context.getString(UiCoreR.string.stripe_invalid_expiry_year))
        assertThat(invocations)
            .isEqualTo(0)
    }

    @Test
    fun inputCompleteDateInPast_thenDelete_showsValid() {
        var invocations = 0
        expiryDateEditText.completionCallback = {
            invocations++
        }

        // This test will be invalid if run between 2080 and 2112. Please update the code.
        assertThat(Calendar.getInstance().get(Calendar.YEAR) < 2080)
            .isTrue()
        // Date translates to December, 2012 UNTIL the 2080, at which point it translates to
        // December, 2112.
        expiryDateEditText.append("12/12")
        assertThat(expiryDateEditText.shouldShowError)
            .isTrue()
        assertThat(expiryDateEditText.errorMessage)
            .isEqualTo(context.getString(UiCoreR.string.stripe_invalid_expiry_year))

        ViewTestUtils.sendDeleteKeyEvent(expiryDateEditText)
        assertThat(expiryDateEditText.text.toString())
            .isEqualTo("12/1")
        assertThat(expiryDateEditText.shouldShowError)
            .isFalse()

        // The date is no longer "in error", but it still shouldn't have triggered the listener.
        assertThat(invocations)
            .isEqualTo(0)
    }

    @Test
    fun clearingDate_doesNotShowError() {
        expiryDateEditText.append("15")
        assertThat(expiryDateEditText.shouldShowError).isTrue()
        expiryDateEditText.setText("")
        assertThat(expiryDateEditText.text.isNullOrBlank()).isTrue()
        assertThat(expiryDateEditText.shouldShowError).isFalse()
    }

    @Test
    fun inputCompleteDate_whenMonthInvalid_showsInvalidMonth() {
        expiryDateEditText.append("15")
        expiryDateEditText.append("50")

        assertThat(expiryDateEditText.shouldShowError)
            .isTrue()
        assertThat(expiryDateEditText.errorMessage)
            .isEqualTo(context.getString(UiCoreR.string.stripe_invalid_expiry_month))
    }

    @Test
    fun validatedDate_whenDataIsValid_returnsExpectedValues() {
        // This test will be invalid if run after the year 2050. Please update the code.
        assertThat(Calendar.getInstance().get(Calendar.YEAR) < 2050)
            .isTrue()

        expiryDateEditText.append("12")
        expiryDateEditText.append("50")

        assertThat(
            expiryDateEditText.validatedDate
        ).isEqualTo(
            ExpirationDate.Validated(
                month = 12,
                year = 2050
            )
        )
    }

    @Test
    fun validatedDate_whenDateIsValidFormatButExpired_returnsNull() {
        // This test will be invalid if run after the year 2080. Please update the code.
        assertThat(Calendar.getInstance().get(Calendar.YEAR) < 2080)
            .isTrue()

        expiryDateEditText.append("12")
        expiryDateEditText.append("12")
        // 12/12 is an invalid date until 2080, at which point it will be interpreted as 12/2112

        assertThat(expiryDateEditText.validatedDate)
            .isNull()
    }

    @Test
    fun validatedDate_whenDateIsIncomplete_returnsNull() {
        expiryDateEditText.append("4")
        assertThat(expiryDateEditText.validatedDate)
            .isNull()
    }

    @Test
    fun validatedDate_whenDateIsValidAndThenChangedToInvalid_returnsNull() {
        // This test will be invalid if run after the year 2050. Please update the code.
        assertThat(Calendar.getInstance().get(Calendar.YEAR) < 2050)
            .isTrue()

        expiryDateEditText.append("12")
        expiryDateEditText.append("50")
        ViewTestUtils.sendDeleteKeyEvent(expiryDateEditText)

        assertThat(expiryDateEditText.validatedDate)
            .isNull()
    }

    @Test
    fun `input with includeSeparatorGaps=true should have expected fieldText and validatedDate values`() {
        expiryDateEditText.includeSeparatorGaps = true
        expiryDateEditText.append("12")
        expiryDateEditText.append("50")

        assertThat(expiryDateEditText.fieldText)
            .isEqualTo("12 / 50")

        assertThat(expiryDateEditText.validatedDate)
            .isEqualTo(ExpirationDate.Validated(12, 2050))
    }

    @Test
    fun `input with includeSeparatorGaps=true should correctly handle delete key`() {
        expiryDateEditText.includeSeparatorGaps = true
        expiryDateEditText.append("12")
        expiryDateEditText.append("50")

        assertThat(expiryDateEditText.fieldText)
            .isEqualTo("12 / 50")

        ViewTestUtils.sendDeleteKeyEvent(expiryDateEditText)
        assertThat(expiryDateEditText.fieldText)
            .isEqualTo("12 / 5")

        ViewTestUtils.sendDeleteKeyEvent(expiryDateEditText)
        assertThat(expiryDateEditText.fieldText)
            .isEqualTo("12")
    }

    @Test
    fun `test setText with null month`() {
        expiryDateEditText.setText(null as Int?, null as Int?)
        assertThat(expiryDateEditText.fieldText)
            .isEqualTo("")
    }

    @Test
    fun `test setText with month and year`() {
        expiryDateEditText.setText(3, 4)
        assertThat(expiryDateEditText.fieldText)
            .isEqualTo("03/04")
    }

    @Test
    fun verifyAdditionalTextChangeListenerGetTriggeredOnlyOnce() {
        val textChangeListener = mock<TextWatcher>()
        expiryDateEditText.addTextChangedListener(textChangeListener)
        expiryDateEditText.setText("1")

        idleLooper()

        verify(textChangeListener, times(1)).afterTextChanged(any())
    }

    @Test
    fun `when losing focus and has invalid date then error message listener should trigger`() {
        val errorMessageListener = mock<StripeEditText.ErrorMessageListener>()
        expiryDateEditText.requestFocus()
        expiryDateEditText.append("1")
        expiryDateEditText.setErrorMessageListener(errorMessageListener)
        expiryDateEditText.clearFocus()

        idleLooper()

        verify(errorMessageListener).displayErrorMessage(context.getString(UiCoreR.string.stripe_incomplete_expiry_date))
    }

    @Test
    fun `when losing focus and has valid date then error message listener should not trigger`() {
        val errorMessageListener = mock<StripeEditText.ErrorMessageListener>()
        expiryDateEditText.requestFocus()
        expiryDateEditText.append("12/50")
        expiryDateEditText.setErrorMessageListener(errorMessageListener)
        expiryDateEditText.clearFocus()

        idleLooper()

        verifyNoMoreInteractions(errorMessageListener)
    }

    @Test
    fun `when losing focus and has empty text then error message listener should not trigger`() {
        val errorMessageListener = mock<StripeEditText.ErrorMessageListener>()
        expiryDateEditText.requestFocus()
        expiryDateEditText.append("1")
        expiryDateEditText.setText("")
        expiryDateEditText.setErrorMessageListener(errorMessageListener)
        expiryDateEditText.clearFocus()

        idleLooper()

        verifyNoMoreInteractions(errorMessageListener)
    }
}
