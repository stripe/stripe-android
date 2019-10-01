package com.stripe.android.testharness

import android.view.View
import android.view.ViewTreeObserver
import androidx.annotation.IdRes

/**
 * Test class that listens for global focus changes and stores the transition.
 */
class TestFocusChangeListener : ViewTreeObserver.OnGlobalFocusChangeListener {
    private var oldFocus: View? = null
    private var newFocus: View? = null

    val oldFocusId: Int
        @IdRes
        get() = oldFocus!!.id

    val newFocusId: Int
        @IdRes
        get() = newFocus!!.id

    override fun onGlobalFocusChanged(oldFocus: View, newFocus: View) {
        this.oldFocus = oldFocus
        this.newFocus = newFocus
    }
}
