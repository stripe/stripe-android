package com.stripe.android.view

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import com.stripe.android.model.PaymentMethodCreateParams

internal abstract class AddPaymentMethodView constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    /**
     * A [PaymentMethodCreateParams] if the customer's input for the given payment
     * method type is valid; otherwise, `null`.
     */
    abstract val createParams: PaymentMethodCreateParams?

    open fun setCommunicatingProgress(communicating: Boolean) {}
}
