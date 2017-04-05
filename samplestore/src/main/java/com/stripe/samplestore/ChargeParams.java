package com.stripe.samplestore;

import com.google.gson.annotations.SerializedName;

import java.util.Currency;

/**
 * Created by mrmcduff on 4/5/17.
 */

public class ChargeParams {

    @SerializedName("amount")
    public long amount;

    @SerializedName("source")
    public String source;

    public ChargeParams(
            long amount,
            String sourceId) {
        this.amount = amount;
        this.source = sourceId;
    }
}
