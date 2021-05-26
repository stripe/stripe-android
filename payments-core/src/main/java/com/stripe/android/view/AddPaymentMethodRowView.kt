package com.stripe.android.view

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import com.stripe.android.databinding.AddPaymentMethodRowBinding

internal class AddPaymentMethodRowView private constructor(
    context: Context
) : FrameLayout(context) {

    private val viewBinding = AddPaymentMethodRowBinding.inflate(
        LayoutInflater.from(context),
        this,
        true
    )

    internal val label = viewBinding.label

    init {
        layoutParams = ViewGroup.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        isFocusable = true
        isClickable = true
    }
}
