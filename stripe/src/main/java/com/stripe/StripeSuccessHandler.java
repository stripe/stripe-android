package com.stripe;

public abstract class StripeSuccessHandler {
    public abstract void onSuccess(Token token);
}
