package com.stripe.android;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.stripe.android.model.Source;

import java.lang.ref.WeakReference;

/**
 * Abstract implementation of {@link ApiResultCallback<Source>} that holds a {@link WeakReference}
 * to an {@link Activity} object.
 */
public abstract class ActivitySourceCallback<A extends Activity>
        implements ApiResultCallback<Source> {
    @NonNull private final WeakReference<A> mActivityRef;

    public ActivitySourceCallback(@NonNull A activity) {
        mActivityRef = new WeakReference<>(activity);
    }

    @Nullable
    public A getActivity() {
        return mActivityRef.get();
    }
}
