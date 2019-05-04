package com.stripe.android;

import android.support.annotation.NonNull;

import java.util.UUID;

class OperationIdFactory {
    @NonNull
    String create() {
        return UUID.randomUUID().toString();
    }
}
