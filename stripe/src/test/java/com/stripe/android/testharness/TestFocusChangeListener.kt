package com.stripe.android.testharness

import android.view.View
import android.view.ViewTreeObserver

/**
 * Test class that listens for global focus changes and stores the transition.
 */
internal class TestFocusChangeListener : ViewTreeObserver.OnGlobalFocusChangeListener {
    var oldFocusId = 0
    var newFocusId = 0

    override fun onGlobalFocusChanged(oldFocus: View?, newFocus: View?) {
        oldFocusId = oldFocus?.id ?: 0
        newFocusId = newFocus?.id ?: 0
    }
}
