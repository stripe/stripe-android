package com.stripe.android.testharness

import android.os.Build
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
     * looping through this manually each time is way too much code. Note that this will throw if
     * you don't run your tests in an API set to Lollipop (21) or above. You can control the SDK
     * of the tests by adding an @Config(sdk=someInt) at the top of your test class.
     *
     * @param editText the [EditText] you'd like to test
     * @param max the max value that you're looking for
     * @return `true` if there is an [android.text.InputFilter.LengthFilter] on this
     * [EditText] has a "max" value equal to the input value.
     * @throws UnsupportedOperationException if this method is run for SDK < 21
     */
    fun hasMaxLength(editText: EditText, @IntRange(from = 0) max: Int): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            throw UnsupportedOperationException(
                "Must be testing on SDK >= 21 to call this method.")
        }

        val filters = editText.filters ?: return false

        return filters
            .mapNotNull { it as? InputFilter.LengthFilter }
            .any { it.max == max }
    }
}
