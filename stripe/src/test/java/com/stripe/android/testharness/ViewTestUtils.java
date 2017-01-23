package com.stripe.android.testharness;


import android.support.annotation.NonNull;
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
}
