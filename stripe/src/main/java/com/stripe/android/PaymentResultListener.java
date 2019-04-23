package com.stripe.android;

import android.support.annotation.NonNull;
import android.support.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Represents a listener for the result of payment calls.
 */
public interface PaymentResultListener {

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({
            SUCCESS,
            USER_CANCELLED,
            ERROR,
            INCOMPLETE
    })
    @interface PaymentResult { }
    String SUCCESS = "success";
    String USER_CANCELLED = "user_cancelled";
    String ERROR = "error";
    String INCOMPLETE = "incomplete";

    /**
     * Called to notify the listener of the result of the payment.
     * @param paymentResult success, user_cancelled, or error
     */
    void onPaymentResult(@NonNull @PaymentResult String paymentResult);
}
