package com.stripe.android.testharness

import android.text.InputFilter
import android.view.KeyEvent
import android.widget.EditText
import androidx.annotation.IntRange

/**
 * Utility class for common actions to perform on Views under test.
 */
internal object ViewTestUtils {

    /**
     * Send an action down call on the delete key.
     *
     * @param editText the [EditText] to which to dispatch the key press.
     */
    fun sendDeleteKeyEvent(editText: EditText) {
        editText.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
        editText.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL))
    }

    /**
     * Utility function to check and see whether or not an EditText has a max length set, because
     * looping through this manually each time is way too much code.
     *
     * @param editText the [EditText] you'd like to test
     * @param max the max value that you're looking for
     * @return `true` if there is an [android.text.InputFilter.LengthFilter] on this
     * [EditText] has a "max" value equal to the input value.
     * @throws UnsupportedOperationException if this method is run for SDK < 21
     */
    fun hasMaxLength(editText: EditText, @IntRange(from = 0) max: Int): Boolean {
        return editText.filters.orEmpty()
            .mapNotNull { it as? InputFilter.LengthFilter }
            .any { it.max == max }
    }
}
