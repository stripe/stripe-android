package com.stripe.android.testharness

import android.view.View
import android.view.ViewTreeObserver
import androidx.annotation.IdRes

/**
 * Test class that listens for global focus changes and stores the transition.
 */
class TestFocusChangeListener : ViewTreeObserver.OnGlobalFocusChangeListener {
    private var mOldFocus: View? = null
    private var mNewFocus: View? = null

    val oldFocusId: Int
        @IdRes
        get() = mOldFocus!!.id

    val newFocusId: Int
        @IdRes
        get() = mNewFocus!!.id

    override fun onGlobalFocusChanged(oldFocus: View, newFocus: View) {
        mOldFocus = oldFocus
        mNewFocus = newFocus
    }
}
