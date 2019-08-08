package com.stripe.android.model;

import android.support.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;

public final class CvcTokenParams implements StripeParamsModel {
    private final String mCvc;

    public CvcTokenParams(String cvc) {
        this.mCvc = cvc;
    }

    @NonNull
    @Override
    public Map<String, Object> toParamMap() {
        final Map<String, Object> tokenParams = new HashMap<>();
        tokenParams.put("cvc", mCvc);
        final Map<String, Object> cvcParams = new HashMap<>();
        cvcParams.put(Token.TokenType.CVC_UPDATE, tokenParams);
        return cvcParams;
    }
}
