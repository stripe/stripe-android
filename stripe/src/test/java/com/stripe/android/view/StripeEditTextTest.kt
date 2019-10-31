package com.stripe.android.view

import android.content.Context
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.stripe.android.R
import com.stripe.android.testharness.ViewTestUtils
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.reset
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner

/**
 * Test class for [StripeEditText].
 */
@RunWith(RobolectricTestRunner::class)
class StripeEditTextTest {

    @Mock
    private lateinit var afterTextChangedListener: StripeEditText.AfterTextChangedListener
    @Mock
    private lateinit var deleteEmptyListener: StripeEditText.DeleteEmptyListener
    private lateinit var editText: StripeEditText

    private val context: Context = ApplicationProvider.getApplicationContext()

    @BeforeTest
    fun setup() {
        MockitoAnnotations.initMocks(this)

        // Note that the CVC EditText is a StripeEditText
        editText = StripeEditText(context)
        editText.setText("")
        editText.setDeleteEmptyListener(deleteEmptyListener)
        editText.setAfterTextChangedListener(afterTextChangedListener)
    }

    @Test
    fun deleteText_whenZeroLength_callsDeleteListener() {
        ViewTestUtils.sendDeleteKeyEvent(editText)
        verify<StripeEditText.DeleteEmptyListener>(deleteEmptyListener).onDeleteEmpty()
        verifyNoMoreInteractions(afterTextChangedListener)
    }

    @Test
    fun addText_callsAppropriateListeners() {
        editText.append("1")
        verifyNoMoreInteractions(deleteEmptyListener)
        verify<StripeEditText.AfterTextChangedListener>(afterTextChangedListener).onTextChanged("1")
    }

    @Test
    fun deleteText_whenNonZeroLength_callsAppropriateListeners() {
        editText.append("1")
        reset<StripeEditText.AfterTextChangedListener>(afterTextChangedListener)

        ViewTestUtils.sendDeleteKeyEvent(editText)
        verifyNoMoreInteractions(deleteEmptyListener)
        verify<StripeEditText.AfterTextChangedListener>(afterTextChangedListener).onTextChanged("")
    }

    @Test
    fun deleteText_whenSelectionAtBeginningButLengthNonZero_doesNotCallListener() {
        editText.append("12")
        verify<StripeEditText.AfterTextChangedListener>(afterTextChangedListener).onTextChanged("12")
        editText.setSelection(0)
        ViewTestUtils.sendDeleteKeyEvent(editText)
        verifyNoMoreInteractions(deleteEmptyListener)
        verifyNoMoreInteractions(afterTextChangedListener)
    }

    @Test
    fun deleteText_whenDeletingMultipleItems_onlyCallsListenerOneTime() {
        editText.append("123")
        // Doing this four times because we need to delete all three items, then jump back.
        for (i in 0..3) {
            ViewTestUtils.sendDeleteKeyEvent(editText)
        }

        verify<StripeEditText.DeleteEmptyListener>(deleteEmptyListener).onDeleteEmpty()
    }

    @Test
    fun getDefaultErrorColorInt_onDarkTheme_returnsDarkError() {
        editText.setTextColor(context.resources
            .getColor(android.R.color.primary_text_dark))
        @ColorInt val colorInt = editText.defaultErrorColorInt
        @ColorInt val expectedErrorInt = ContextCompat.getColor(context,
            R.color.stripe_error_text_dark_theme)
        assertEquals(expectedErrorInt, colorInt)
    }

    @Test
    fun getDefaultErrorColorInt_onLightTheme_returnsLightError() {
        editText.setTextColor(context.resources
            .getColor(android.R.color.primary_text_light))
        @ColorInt val colorInt = editText.defaultErrorColorInt
        @ColorInt val expectedErrorInt = ContextCompat.getColor(context,
            R.color.stripe_error_text_light_theme)
        assertEquals(expectedErrorInt, colorInt)
    }

    @Test
    fun setErrorColor_whenInError_overridesDefault() {
        // By default, the text color in this test activity is a light theme
        @ColorInt val blueError = 0x0000ff
        editText.setErrorColor(blueError)
        editText.shouldShowError = true
        val currentColorInt = editText.textColors.defaultColor
        assertEquals(blueError, currentColorInt)
    }

    @Test
    fun getCachedColorStateList_afterInit_returnsNotNull() {
        assertNotNull(editText.cachedColorStateList)
    }

    @Test
    fun setShouldShowError_whenErrorColorNotSet_shouldUseDefaultErrorColor() {
        editText.shouldShowError = true
        assertEquals(ContextCompat.getColor(context, R.color.stripe_error_text_light_theme),
            editText.textColors.defaultColor)
    }
}
