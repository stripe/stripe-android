package com.stripe.android;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

final class ResultWrapper<ResultType> {
    @Nullable final ResultType result;
    @Nullable final Exception error;

    ResultWrapper(@Nullable ResultType result) {
        this.result = result;
        this.error = null;
    }

    ResultWrapper(@NonNull Exception error) {
        this.error = error;
        this.result = null;
    }
}
