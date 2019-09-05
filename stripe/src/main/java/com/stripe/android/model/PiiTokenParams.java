package com.stripe.android.model;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;

public final class PiiTokenParams implements StripeParamsModel {
    @NonNull private final String mPersonalId;

    public PiiTokenParams(@NonNull String personalId) {
        this.mPersonalId = personalId;
    }

    @NonNull
    @Override
    public Map<String, Object> toParamMap() {
        final Map<String, Object> tokenParams = new HashMap<>();
        tokenParams.put("personal_id_number", mPersonalId);
        final Map<String, Object> piiParams = new HashMap<>();
        piiParams.put(Token.TokenType.PII, tokenParams);
        return piiParams;
    }
}
