package com.stripe.android;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.stripe.android.model.Source;

import java.lang.ref.WeakReference;

/**
 * Abstract implementation of {@link ApiResultCallback} that holds a {@link WeakReference}
 * to an <code>Activity</code> object.
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
