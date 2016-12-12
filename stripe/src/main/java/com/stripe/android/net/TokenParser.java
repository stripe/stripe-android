package com.stripe.android.net;

import com.stripe.android.model.Card;
import com.stripe.android.model.Token;
import com.stripe.android.util.StripeJsonUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

/**
 * A class that handles parsing the JSON of a {@link com.stripe.android.model.Token} object.
 */
public class TokenParser {

    private static final String FIELD_CARD = "card";
    private static final String FIELD_ID = "id";
    private static final String FIELD_LIVEMODE = "livemode";
    private static final String FIELD_TYPE = "type";
    private static final String FIELD_USED = "used";
    private static final String FIELD_CREATED = "created";

    static Token parseToken(String jsonToken) throws JSONException {
        JSONObject jsonObject = new JSONObject(jsonToken);
        String tokenId = StripeJsonUtils.getString(jsonObject, FIELD_ID);
        Long createdTimeStamp = jsonObject.getLong(FIELD_CREATED);
        Boolean liveMode = jsonObject.getBoolean(FIELD_LIVEMODE);
        String tokenType = StripeJsonUtils.getString(jsonObject, FIELD_TYPE);
        Boolean used = jsonObject.getBoolean(FIELD_USED);

        JSONObject cardObject = jsonObject.getJSONObject(FIELD_CARD);
        Card card = CardParser.parseCard(cardObject);

        Date date = new Date(createdTimeStamp * 1000);
        return new Token(tokenId, liveMode, date, used, card, tokenType);
    }
}
