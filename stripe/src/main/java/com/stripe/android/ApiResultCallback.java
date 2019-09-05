package com.stripe.android;

import androidx.annotation.NonNull;

/**
 * Generic interface for an API operation callback that either returns a
 * result, {@link ResultType}, or an {@link Exception}
 */
public interface ApiResultCallback<ResultType> {
    void onSuccess(@NonNull ResultType result);

    void onError(@NonNull Exception e);
}
