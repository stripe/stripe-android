package com.stripe.android.paymentsheet.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import com.stripe.android.databinding.StripeGooglepayButtonBlackBinding

class GooglePayButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    init {
        StripeGooglepayButtonBlackBinding.inflate(
            LayoutInflater.from(context),
            this,
            true
        )
    }
}
