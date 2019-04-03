package com.stripe.android;

import android.support.annotation.NonNull;

import com.stripe.android.model.Source;

/**
 * An interface representing a callback to be notified about the results of
 * {@link Source} creation.
 */
public interface SourceCallback {

    /**
     * Error callback method.
     * @param error the error that occurred.
     */
    void onError(@NonNull Exception error);

    /**
     * Success callback method.
     * @param source the {@link Source} that was found or created.
     */
    void onSuccess(@NonNull Source source);
}
