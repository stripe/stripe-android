package com.stripe.android;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Locale;

class PaymentSessionPrefs {
    private static final String PREF_FILE = "PaymentSessionPrefs";

    @NonNull private final SharedPreferences mPrefs;

    PaymentSessionPrefs(@NonNull Context context) {
        this(context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE));
    }

    private PaymentSessionPrefs(@NonNull SharedPreferences prefs) {
        mPrefs = prefs;
    }

    @Nullable
    String getSelectedPaymentMethodId(@NonNull String customerId) {
        return mPrefs.getString(getPaymentMethodKey(customerId), null);
    }

    void saveSelectedPaymentMethodId(@NonNull String customerId,
                                     @NonNull String paymentMethodId) {
        mPrefs.edit()
                .putString(getPaymentMethodKey(customerId), paymentMethodId)
                .apply();
    }

    @NonNull
    private static String getPaymentMethodKey(@NonNull String customerId) {
        return String.format(Locale.US, "customer[%s].payment_method", customerId);
    }
}
