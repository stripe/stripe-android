package com.stripe.android;

import android.support.annotation.NonNull;

interface Factory<ArgType, ReturnType> {
    @NonNull
    ReturnType create(@NonNull ArgType arg);
}
