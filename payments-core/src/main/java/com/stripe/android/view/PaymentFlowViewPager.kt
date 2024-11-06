package com.stripe.android.view

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.viewpager.widget.ViewPager

class PaymentFlowViewPager @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    private val isSwipingAllowed: Boolean = false
) : ViewPager(context, attrs) {
    override fun onInterceptTouchEvent(event: MotionEvent?): Boolean {
        return if (isSwipingAllowed) {
            super.onInterceptTouchEvent(event)
        } else {
            false
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return if (isSwipingAllowed) {
            super.onTouchEvent(event)
        } else {
            false
        }
    }
}
