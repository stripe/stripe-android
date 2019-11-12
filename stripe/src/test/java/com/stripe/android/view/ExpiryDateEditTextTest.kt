package com.stripe.android.view

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.stripe.android.testharness.ViewTestUtils
import java.util.Calendar
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Test class for [ExpiryDateEditText].
 */
@RunWith(RobolectricTestRunner::class)
class ExpiryDateEditTextTest {
    private lateinit var expiryDateEditText: ExpiryDateEditText

    @BeforeTest
    fun setup() {
        expiryDateEditText = ExpiryDateEditText(ApplicationProvider.getApplicationContext<Context>())
        expiryDateEditText.setText("")
    }

    @Test
    fun afterInputTwoDigits_textContainsSlash() {
        expiryDateEditText.append("1")
        expiryDateEditText.append("2")

        val text = expiryDateEditText.text.toString()
        assertEquals("12/", text)
    }

    @Test
    fun inputSingleDigit_whenDigitIsTooLargeForMonth_prependsZero() {
        expiryDateEditText.append("4")
        assertEquals("04/", expiryDateEditText.text.toString())
        assertEquals(3, expiryDateEditText.selectionStart)
    }

    @Test
    fun inputSingleDigit_whenDigitIsZeroOrOne_doesNotPrependZero() {
        expiryDateEditText.append("1")
        assertEquals("1", expiryDateEditText.text.toString())
        assertEquals(1, expiryDateEditText.selectionStart)
    }

    @Test
    fun inputSingleDigit_whenAtFirstCharacterButTextNotEmpty_doesNotPrependZero() {
        expiryDateEditText.append("1")
        // This case can occur when the user moves the cursor behind the already-entered text.
        expiryDateEditText.setSelection(0)
        expiryDateEditText.editableText.replace(0, 0, "3", 0, 1)

        assertEquals("31", expiryDateEditText.text.toString())
        assertEquals(1, expiryDateEditText.selectionStart)
    }

    @Test
    fun inputMultipleValidDigits_whenEmpty_doesNotPrependZeroOrShowErrorState() {
        expiryDateEditText.append("11")

        val text = expiryDateEditText.text.toString()
        assertEquals("11/", text)
        assertFalse(expiryDateEditText.shouldShowError)
        assertEquals(3, expiryDateEditText.selectionStart)
    }

    @Test
    fun afterInputThreeDigits_whenDeletingOne_textDoesNotContainSlash() {
        expiryDateEditText.append("1")
        expiryDateEditText.append("2")
        expiryDateEditText.append("3")
        ViewTestUtils.sendDeleteKeyEvent(expiryDateEditText)
        val text = expiryDateEditText.text.toString()
        assertEquals("12", text)
    }

    @Test
    fun afterAddingFinalDigit_whenGoingFromInvalidToValid_callsListener() {
        var invocations = 0
        expiryDateEditText.completionCallback = {
            invocations++
        }

        assertTrue(Calendar.getInstance().get(Calendar.YEAR) <= 2059)

        expiryDateEditText.append("1")
        expiryDateEditText.append("2")
        expiryDateEditText.append("5")
        assertEquals(0, invocations)

        expiryDateEditText.append("9")
        assertTrue(expiryDateEditText.isDateValid)
        assertEquals(1, invocations)
    }

    @Test
    fun afterAddingFinalDigit_whenDeletingItem_revertsToInvalidState() {
        var invocations = 0
        expiryDateEditText.completionCallback = {
            invocations++
        }

        assertTrue(Calendar.getInstance().get(Calendar.YEAR) <= 2059)

        expiryDateEditText.append("12")
        expiryDateEditText.append("59")
        assertTrue(expiryDateEditText.isDateValid)

        ViewTestUtils.sendDeleteKeyEvent(expiryDateEditText)
        assertEquals(1, invocations)
        assertFalse(expiryDateEditText.isDateValid)
    }

    @Test
    fun updateSelectionIndex_whenMovingAcrossTheGap_movesToEnd() {
        assertEquals(3, expiryDateEditText.updateSelectionIndex(3, 1, 1, 5))
    }

    @Test
    fun updateSelectionIndex_atStart_onlyMovesForwardByOne() {
        assertEquals(1, expiryDateEditText.updateSelectionIndex(1, 0, 1, 5))
    }

    @Test
    fun updateSelectionIndex_whenDeletingAcrossTheGap_staysAtEnd() {
        assertEquals(2, expiryDateEditText.updateSelectionIndex(2, 4, 0, 5))
    }

    @Test
    fun updateSelectionIndex_whenInputVeryLong_respectMaxInputLength() {
        assertEquals(5, expiryDateEditText.updateSelectionIndex(6, 4, 2, 5))
    }

    @Test
    fun inputZero_whenEmpty_doesNotShowErrorState() {
        expiryDateEditText.append("0")

        assertFalse(expiryDateEditText.shouldShowError)
        assertEquals("0", expiryDateEditText.text.toString())
    }

    @Test
    fun inputOne_whenEmpty_doesNotShowErrorState() {
        expiryDateEditText.append("1")

        assertFalse(expiryDateEditText.shouldShowError)
        assertEquals("1", expiryDateEditText.text.toString())
    }

    @Test
    fun inputTwoDigitMonth_whenInvalid_showsErrorAndDoesNotAddSlash() {
        expiryDateEditText.append("14")

        assertTrue(expiryDateEditText.shouldShowError)
        assertEquals("14", expiryDateEditText.text.toString())
    }

    @Test
    fun inputThreeDigits_whenInvalid_showsErrorAndDoesAddSlash() {
        expiryDateEditText.append("143")

        assertTrue(expiryDateEditText.shouldShowError)
        assertEquals("14/3", expiryDateEditText.text.toString())
    }

    @Test
    fun delete_whenAcrossSeparator_alwaysDeletesNumber() {
        expiryDateEditText.append("12")
        assertEquals("12/", expiryDateEditText.text.toString())

        ViewTestUtils.sendDeleteKeyEvent(expiryDateEditText)
        assertEquals("1", expiryDateEditText.text.toString())
    }

    @Test
    fun inputCompleteDate_whenInPast_showsInvalid() {
        var invocations = 0
        expiryDateEditText.completionCallback = {
            invocations++
        }

        // This test will be invalid if run between 2080 and 2112. Please update the code.
        assertTrue(Calendar.getInstance().get(Calendar.YEAR) < 2080)
        // Date translates to December, 2012 UNTIL the 2080, at which point it translates to
        // December, 2112. Also, this simulates a PASTE action.
        expiryDateEditText.append("1212")

        assertTrue(expiryDateEditText.shouldShowError)
        assertEquals(0, invocations)
    }

    @Test
    fun inputCompleteDateInPast_thenDelete_showsValid() {
        var invocations = 0
        expiryDateEditText.completionCallback = {
            invocations++
        }

        // This test will be invalid if run between 2080 and 2112. Please update the code.
        assertTrue(Calendar.getInstance().get(Calendar.YEAR) < 2080)
        // Date translates to December, 2012 UNTIL the 2080, at which point it translates to
        // December, 2112.
        expiryDateEditText.append("12/12")
        assertTrue(expiryDateEditText.shouldShowError)

        ViewTestUtils.sendDeleteKeyEvent(expiryDateEditText)
        assertEquals("12/1", expiryDateEditText.text.toString())
        assertFalse(expiryDateEditText.shouldShowError)

        // The date is no longer "in error", but it still shouldn't have triggered the listener.
        assertEquals(0, invocations)
    }

    @Test
    fun getValidDateFields_whenDataIsValid_returnsExpectedValues() {
        // This test will be invalid if run after the year 2050. Please update the code.
        assertTrue(Calendar.getInstance().get(Calendar.YEAR) < 2050)

        expiryDateEditText.append("12")
        expiryDateEditText.append("50")

        val retrievedDate = expiryDateEditText.validDateFields
        assertNotNull(retrievedDate)
        assertEquals(12, retrievedDate.first)
        assertEquals(2050, retrievedDate.second)
    }

    @Test
    fun getValidDateFields_whenDateIsValidFormatButExpired_returnsNull() {
        // This test will be invalid if run after the year 2050. Please update the code.
        assertTrue(Calendar.getInstance().get(Calendar.YEAR) < 2080)

        expiryDateEditText.append("12")
        expiryDateEditText.append("12")
        // 12/12 is an invalid date until 2080, at which point it will be interpreted as 12/2112

        assertNull(expiryDateEditText.validDateFields)
    }

    @Test
    fun getValidDateFields_whenDateIsIncomplete_returnsNull() {
        expiryDateEditText.append("4")
        assertNull(expiryDateEditText.validDateFields)
    }

    @Test
    fun getValidDateFields_whenDateIsValidAndThenChangedToInvalid_returnsNull() {
        // This test will be invalid if run after the year 2050. Please update the code.
        assertTrue(Calendar.getInstance().get(Calendar.YEAR) < 2050)

        expiryDateEditText.append("12")
        expiryDateEditText.append("50")
        ViewTestUtils.sendDeleteKeyEvent(expiryDateEditText)

        assertNull(expiryDateEditText.validDateFields)
    }
}
