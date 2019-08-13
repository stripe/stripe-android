package com.stripe.android.view;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import com.stripe.android.model.PaymentMethodCreateParams;

abstract class AddPaymentMethodView extends FrameLayout {

    public AddPaymentMethodView(@NonNull Context context) {
        super(context);
    }

    public AddPaymentMethodView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public AddPaymentMethodView(@NonNull Context context, @Nullable AttributeSet attrs,
                                int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * @return a {@link PaymentMethodCreateParams} if the customer input for the given payment
     *         method type is valid; otherwise, {@code null}
     */
    @Nullable abstract PaymentMethodCreateParams getCreateParams();

    abstract void setCommunicatingProgress(boolean communicating);
}
