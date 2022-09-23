package com.stripe.android.view

import android.content.Context
import android.content.res.ColorStateList
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.widget.EditText
import androidx.annotation.ColorInt
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.R
import com.stripe.android.testharness.ViewTestUtils
import com.stripe.android.utils.TestUtils.idleLooper
import org.junit.runner.RunWith
import org.mockito.Mockito.reset
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verifyNoMoreInteractions
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
internal class StripeEditTextTest {
    private val context: Context = ContextThemeWrapper(
        ApplicationProvider.getApplicationContext(),
        R.style.StripeDefaultTheme
    )
    private val afterTextChangedListener: StripeEditText.AfterTextChangedListener = mock()
    private val deleteEmptyListener: StripeEditText.DeleteEmptyListener = mock()

    private val editText = StripeEditText(
        context
    ).also {
        it.setDeleteEmptyListener(deleteEmptyListener)
        it.setAfterTextChangedListener(afterTextChangedListener)
    }

    @Test
    fun onFocusListenerSetupCalledOnInit() {
        val onFocusListener = mock<View.OnFocusChangeListener>()
        editText.internalFocusChangeListeners.add(onFocusListener)
        editText.getParentOnFocusChangeListener()!!.onFocusChange(editText, false)
        verify(onFocusListener).onFocusChange(editText, false)
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
        editText.defaultColorStateList = ColorStateList.valueOf(ContextCompat.getColor(context, android.R.color.primary_text_dark))
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
    fun setTextColor() {
        editText.setTextColor(
            ColorStateList.valueOf(
                ContextCompat.getColor(
                    context,
                    android.R.color.holo_red_dark
                )
            )
        )

        // The field state must be toggled to show an error
        editText.shouldShowError = true
        editText.shouldShowError = false

        assertThat(editText.textColors)
            .isEqualTo(
                ColorStateList.valueOf(
                    ContextCompat.getColor(
                        context,
                        android.R.color.holo_red_dark
                    )
                )
            )
    }

    @Test
    fun getCachedColorStateList_afterInit_returnsNotNull() {
        assertThat(editText.defaultColorStateList)
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
    fun setTextSilent_shouldNotNotifyTextWatchers() {
        val textWatcher = mock<TextWatcher>()
        editText.addTextChangedListener(textWatcher)
        editText.setText("1")

        verifyTextWatcherIsTriggered(textWatcher, 1)

        editText.setTextSilent("1")

        verifyNoMoreInteractions(textWatcher)
    }

    @Test
    fun setTextSilent_shouldNotChangeSuperClassTextWatchers() {
        val textWatcher1 = mock<TextWatcher>()
        val textWatcher2 = mock<TextWatcher>()
        editText.addTextChangedListener(textWatcher1)
        editText.addTextChangedListener(textWatcher2)
        editText.setText("1")
        idleLooper()

        verifyTextWatcherIsTriggered(textWatcher1, 1)
        verifyTextWatcherIsTriggered(textWatcher2, 1)

        reset(textWatcher1)
        reset(textWatcher2)

        editText.removeTextChangedListener(textWatcher2)
        editText.setText("1")

        verifyTextWatcherIsTriggered(textWatcher1, 1)
        verifyTextWatcherIsTriggered(textWatcher2, 0)

        editText.removeTextChangedListener(textWatcher1)
        editText.setText("1")
        idleLooper()

        verifyNoMoreInteractions(textWatcher1)
        verifyNoMoreInteractions(textWatcher2)
    }

    @Test
    fun invokesDeleteListenerIfFieldIsEmpty() {
        val deleteListener = mock<StripeEditText.DeleteEmptyListener>()
        editText.setDeleteEmptyListener(deleteListener)

        editText.setText("1")
        editText.enterBackspace()
        verify(deleteListener, never()).onDeleteEmpty()

        editText.enterBackspace()
        verify(deleteListener).onDeleteEmpty()
    }

    @Test
    fun setsIsLastKeyDeleteCorrectlyOnSoftKeyboardInput() {
        // We use `setText()` to simulate the behavior on software keyboards, which result in
        // OnKeyListener being called on some devices, but not others.
        editText.setText("1")
        assertThat(editText.isLastKeyDelete).isFalse()

        editText.enterBackspace()
        assertThat(editText.isLastKeyDelete).isTrue()

        editText.setText("2")
        assertThat(editText.isLastKeyDelete).isFalse()
    }

    private fun verifyTextWatcherIsTriggered(watcher: TextWatcher, count: Int) {
        verify(watcher, times(count)).beforeTextChanged(any(), any(), any(), any())
        verify(watcher, times(count)).onTextChanged(any(), any(), any(), any())
        verify(watcher, times(count)).afterTextChanged(any())
    }
}

private fun EditText.enterBackspace() {
    dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
    setText(text.toString().dropLast(1))
    dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL))
}
