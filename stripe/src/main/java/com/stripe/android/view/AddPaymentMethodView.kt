package com.stripe.android.view

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import com.stripe.android.model.PaymentMethodCreateParams

internal abstract class AddPaymentMethodView : FrameLayout {

    /**
     * @return a [PaymentMethodCreateParams] if the customer input for the given payment
     * method type is valid; otherwise, `null`
     */
    abstract val createParams: PaymentMethodCreateParams?

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) :
        super(context, attrs, defStyleAttr)

    open fun setCommunicatingProgress(communicating: Boolean) {}
}
