package com.stripe.android.view

import android.app.Activity
import android.support.annotation.IdRes
import android.support.annotation.LayoutRes
import android.view.View
import android.widget.FrameLayout

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
    }
}
