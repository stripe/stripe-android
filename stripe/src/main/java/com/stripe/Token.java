package com.stripe;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

public class Token {
    public final String tokenId;
    public final String object;
    public final boolean livemode;
    public final Card card;
    public final Date created;
    public final boolean used;

    protected static Token fromJSON(String jsonToken) throws JSONException {
        if (jsonToken == null) {
            return null;
        }

        return fromJSON(new JSONObject(jsonToken));
    }

    protected static Token fromJSON(JSONObject tokenMap) {
        if (tokenMap == null) {
            return null;
        }

        long timestamp = tokenMap.optLong("created", -1);

        return new Token(
                tokenMap.optString("id"),
                tokenMap.optString("object"),
                tokenMap.optBoolean("livemode"),
                timestamp != -1 ? new Date(timestamp) : null,
                tokenMap.optBoolean("used"),
                Card.fromJSON(tokenMap.optJSONObject("card"))
        );
    }

    protected Token(String tokenId, String object, boolean live, Date created, boolean used, Card card) {
        this.tokenId = tokenId;
        this.object = object;
        this.livemode = live;
        this.card = card;
        this.created = created;
        this.used = used;
    }
}
