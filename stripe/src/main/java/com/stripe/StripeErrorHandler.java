package com.stripe;

public abstract class StripeErrorHandler {
    public abstract void onError(StripeError error);
}
