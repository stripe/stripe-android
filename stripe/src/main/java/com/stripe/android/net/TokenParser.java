package com.stripe.android.net;

import com.stripe.android.model.BankAccount;
import com.stripe.android.model.Card;
import com.stripe.android.model.Token;
import com.stripe.android.util.StripeJsonUtils;
import com.stripe.android.util.StripeTextUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

/**
 * A class that handles parsing the JSON of a {@link com.stripe.android.model.Token} object.
 */
public class TokenParser {

    private static final String FIELD_BANK_ACCOUNT = "bank_account";
    private static final String FIELD_CARD = "card";
    private static final String FIELD_CREATED = "created";
    private static final String FIELD_ID = "id";
    private static final String FIELD_LIVEMODE = "livemode";
    private static final String FIELD_TYPE = "type";
    private static final String FIELD_USED = "used";

    public static Token parseToken(String jsonToken) throws JSONException {
        JSONObject jsonObject = new JSONObject(jsonToken);

        String tokenId = StripeJsonUtils.getString(jsonObject, FIELD_ID);
        Long createdTimeStamp = jsonObject.getLong(FIELD_CREATED);
        Boolean liveMode = jsonObject.getBoolean(FIELD_LIVEMODE);
        @Token.TokenType String tokenType =
                StripeTextUtils.asTokenType(StripeJsonUtils.getString(jsonObject, FIELD_TYPE));
        Boolean used = jsonObject.getBoolean(FIELD_USED);

        Date date = new Date(createdTimeStamp * 1000);

        Token token = null;
        if (Token.TYPE_BANK_ACCOUNT.equals(tokenType)) {
            JSONObject bankAccountObject = jsonObject.getJSONObject(FIELD_BANK_ACCOUNT);
            BankAccount bankAccount = BankAccountParser.parseBankAccount(bankAccountObject);
            token = new Token(tokenId, liveMode, date, used, bankAccount);
        } else if (Token.TYPE_CARD.equals(tokenType)) {
            JSONObject cardObject = jsonObject.getJSONObject(FIELD_CARD);
            Card card = CardParser.parseCard(cardObject);
            token = new Token(tokenId, liveMode, date, used, card);
        }

        return token;
    }
}
