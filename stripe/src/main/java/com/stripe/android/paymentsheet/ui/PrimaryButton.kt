package com.stripe.android.paymentsheet.ui

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout

/**
 * The primary call-to-action for a payment sheet screen.
 */
internal abstract class PrimaryButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr)
