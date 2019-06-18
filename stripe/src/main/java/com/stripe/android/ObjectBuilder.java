package com.stripe.android;

import android.support.annotation.NonNull;

public interface ObjectBuilder<T> {
    @NonNull T build();
}
