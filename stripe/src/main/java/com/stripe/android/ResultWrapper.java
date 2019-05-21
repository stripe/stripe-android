package com.stripe.android;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

final class ResultWrapper<T> {
    @Nullable final T result;
    @Nullable final Exception error;

    ResultWrapper(@Nullable T result) {
        this.result = result;
        this.error = null;
    }

    ResultWrapper(@NonNull Exception error) {
        this.error = error;
        this.result = null;
    }
}
