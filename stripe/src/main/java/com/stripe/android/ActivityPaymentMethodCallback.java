package com.stripe.android;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.lang.ref.WeakReference;

/**
 * Abstract implementation of {@link PaymentMethodCallback} that holds a {@link WeakReference} to
 * an {@link Activity} object.
 */
public abstract class ActivityPaymentMethodCallback<A extends Activity>
        implements PaymentMethodCallback {
    @NonNull private final WeakReference<A> mActivityRef;

    public ActivityPaymentMethodCallback(@NonNull A activity) {
        mActivityRef = new WeakReference<>(activity);
    }

    @Nullable
    public A getActivity() {
        return mActivityRef.get();
    }
}
