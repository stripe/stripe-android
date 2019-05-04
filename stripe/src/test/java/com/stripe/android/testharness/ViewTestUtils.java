package com.stripe.android.testharness;


import android.os.Build;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.text.InputFilter;
import android.view.KeyEvent;
import android.widget.EditText;

/**
 * Utility class for common actions to perform on Views under test.
 */
public class ViewTestUtils {

    /**
     * Send an action down call on the delete key.
     *
     * @param editText the {@link EditText} to which to dispatch the key press.
     */
    public static void sendDeleteKeyEvent(@NonNull EditText editText) {
        editText.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL));
        editText.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL));
    }

    /**
     * Utility function to check and see whether or not an EditText has a max length set, because
     * looping through this manually each time is way too much code. Note that this will throw if
     * you don't run your tests in an API set to Lollipop (21) or above. You can control the SDK
     * of the tests by adding an @Config(sdk=someInt) at the top of your test class.
     *
     * @param editText the {@link EditText} you'd like to test
     * @param max the max value that you're looking for
     * @return {@code true} if there is an {@link android.text.InputFilter.LengthFilter} on this
     * {@link EditText} has a "max" value equal to the input value.
     * @throws UnsupportedOperationException if this method is run for SDK < 21
     */
    @SuppressWarnings("NewApi")
    public static boolean hasMaxLength(@NonNull EditText editText, @IntRange(from = 0) int max) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            throw new UnsupportedOperationException(
                    "Must be testing on SDK >= 21 to call this method.");
        }

        boolean foundLengthFilterOfCorrectSize = false;
        InputFilter[] filters = editText.getFilters();
        if (filters == null) {
            return false;
        }

        for (int i = 0; i < filters.length; i++) {
            InputFilter filter = filters[i];
            if (filter instanceof InputFilter.LengthFilter) {
                foundLengthFilterOfCorrectSize =
                        ((InputFilter.LengthFilter) filter).getMax() == max;
                if (foundLengthFilterOfCorrectSize) {
                    break;
                }
            }
        }
        return foundLengthFilterOfCorrectSize;
    }
}
