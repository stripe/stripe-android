package com.stripe.android;


import android.support.annotation.NonNull;

import static com.stripe.android.PaymentResultListener.ERROR;
import static com.stripe.android.PaymentResultListener.INCOMPLETE;
import static com.stripe.android.PaymentResultListener.SUCCESS;
import static com.stripe.android.PaymentResultListener.USER_CANCELLED;

class PaymentSessionUtils {

    @NonNull
    @PaymentResultListener.PaymentResult
    public static String paymentResultFromString(String paymentResultRaw) {
        switch (paymentResultRaw) {
            case SUCCESS:
                return SUCCESS;
            case USER_CANCELLED:
                return USER_CANCELLED;
            case ERROR:
                return ERROR;
            case INCOMPLETE:
                return INCOMPLETE;
            default:
                return ERROR;
        }
    }

}
