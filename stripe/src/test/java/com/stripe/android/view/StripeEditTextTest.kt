package com.stripe.android.view

import android.content.Context
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.stripe.android.R
import com.stripe.android.testharness.ViewTestUtils
import com.stripe.android.utils.TestUtils.idleLooper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.junit.runner.RunWith
import org.mockito.Mockito.reset
import org.mockito.Mockito.verify
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
@ExperimentalCoroutinesApi
internal class StripeEditTextTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val afterTextChangedListener: StripeEditText.AfterTextChangedListener = mock()
    private val deleteEmptyListener: StripeEditText.DeleteEmptyListener = mock()
    private val testDispatcher = TestCoroutineDispatcher()

    private val editText = StripeEditText(
        context,
        workDispatcher = testDispatcher
    ).also {
        it.setDeleteEmptyListener(deleteEmptyListener)
        it.setAfterTextChangedListener(afterTextChangedListener)
    }

    @Test
    fun deleteText_whenZeroLength_callsDeleteListener() {
        ViewTestUtils.sendDeleteKeyEvent(editText)
        verify(deleteEmptyListener).onDeleteEmpty()
        verifyNoMoreInteractions(afterTextChangedListener)
    }

    @Test
    fun addText_callsAppropriateListeners() {
        editText.append("1")
        verifyNoMoreInteractions(deleteEmptyListener)
        verify(afterTextChangedListener)
            .onTextChanged("1")
    }

    @Test
    fun deleteText_whenNonZeroLength_callsAppropriateListeners() {
        editText.append("1")
        reset(afterTextChangedListener)

        ViewTestUtils.sendDeleteKeyEvent(editText)
        verifyNoMoreInteractions(deleteEmptyListener)
        verify(afterTextChangedListener)
            .onTextChanged("")
    }

    @Test
    fun deleteText_whenSelectionAtBeginningButLengthNonZero_doesNotCallListener() {
        editText.append("12")
        verify(afterTextChangedListener)
            .onTextChanged("12")
        editText.setSelection(0)
        ViewTestUtils.sendDeleteKeyEvent(editText)
        verifyNoMoreInteractions(deleteEmptyListener)
        verifyNoMoreInteractions(afterTextChangedListener)
    }

    @Test
    fun deleteText_whenDeletingMultipleItems_onlyCallsListenerOneTime() {
        editText.append("123")
        // Doing this four times because we need to delete all three items, then jump back.

        repeat(4) {
            ViewTestUtils.sendDeleteKeyEvent(editText)
        }

        verify(deleteEmptyListener).onDeleteEmpty()
    }

    @Test
    fun getDefaultErrorColorInt_onDarkTheme_returnsDarkError() {
        editText.setTextColor(ContextCompat.getColor(context, android.R.color.primary_text_dark))
        assertThat(editText.defaultErrorColorInt)
            .isEqualTo(ContextCompat.getColor(context, R.color.stripe_error_text_dark_theme))
    }

    @Test
    fun getDefaultErrorColorInt_onLightTheme_returnsLightError() {
        editText.setTextColor(ContextCompat.getColor(context, android.R.color.primary_text_light))
        assertThat(editText.defaultErrorColorInt)
            .isEqualTo(ContextCompat.getColor(context, R.color.stripe_error_text_light_theme))
    }

    @Test
    fun setErrorColor_whenInError_overridesDefault() {
        // By default, the text color in this test activity is a light theme
        @ColorInt val blueError = 0x0000ff
        editText.setErrorColor(blueError)
        editText.shouldShowError = true
        val currentColorInt = editText.textColors.defaultColor
        assertThat(currentColorInt)
            .isEqualTo(blueError)
    }

    @Test
    fun getCachedColorStateList_afterInit_returnsNotNull() {
        assertThat(editText.cachedColorStateList)
            .isNotNull()
    }

    @Test
    fun setShouldShowError_whenErrorColorNotSet_shouldUseDefaultErrorColor() {
        editText.shouldShowError = true
        assertThat(editText.textColors.defaultColor)
            .isEqualTo(ContextCompat.getColor(context, R.color.stripe_error_text_light_theme))
    }

    @Test
    fun shouldShowError_whenChanged_changesTextColor() {
        editText.errorMessage = "There was an error!"

        editText.shouldShowError = true
        assertThat(editText.currentTextColor)
            .isEqualTo(-1369050)

        editText.shouldShowError = false
        assertThat(editText.currentTextColor)
            .isEqualTo(-570425344)
    }

    @Test
    fun `setHintDelayed should set hint after delay`() = testDispatcher.runBlockingTest {
        assertThat(editText.hint)
            .isNull()

        editText.setHintDelayed("Here's a hint", DELAY)
        testDispatcher.advanceTimeBy(DELAY + 10)
        idleLooper()

        assertThat(editText.hint)
            .isEqualTo("Here's a hint")
    }

    @Test
    fun `setHintDelayed when Job is canceled before delay should not set hint`() {
        assertThat(editText.hint)
            .isNull()
        editText.setHintDelayed("Here's a hint", DELAY)
        testDispatcher.advanceTimeBy(DELAY - 10)

        editText.job.cancel()

        assertThat(editText.hint)
            .isNull()
    }

    private companion object {
        private const val DELAY = 100L
    }
}
