package com.stripe.android;

import android.support.annotation.NonNull;

interface Supplier<T> {
    @NonNull T get();
}
