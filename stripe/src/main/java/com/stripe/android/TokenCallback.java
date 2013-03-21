package com.stripe.android;

import com.stripe.android.model.Token;

public abstract class TokenCallback {
    public abstract void onError(Exception error);
    public abstract void onSuccess(Token token);
}
