package com.stripe.android.view

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes

internal abstract class AddPaymentMethodRowView(
    activity: Activity,
    @LayoutRes layoutId: Int,
    @IdRes idRes: Int,
    args: AddPaymentMethodActivityStarter.Args
) : FrameLayout(activity) {
    init {
        View.inflate(activity, layoutId, this)
        id = idRes
        setOnClickListener { AddPaymentMethodActivityStarter(activity).startForResult(args) }
        layoutParams = ViewGroup.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)

        isFocusable = true
        isClickable = true
        isFocusableInTouchMode = true
    }
}
