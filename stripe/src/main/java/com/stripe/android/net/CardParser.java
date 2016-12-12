package com.stripe.android.net;

import android.support.annotation.NonNull;

import com.stripe.android.model.Card;
import com.stripe.android.util.StripeJsonUtils;
import com.stripe.android.util.StripeTextUtils;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * A helper class for parsing {@link Card} objects returned from the server.
 */
public class CardParser {

    static final String FIELD_ADDRESS_CITY = "address_city";
    static final String FIELD_ADDRESS_COUNTRY = "address_country";
    static final String FIELD_ADDRESS_LINE1 = "address_line1";
    static final String FIELD_ADDRESS_LINE2 = "address_line2";
    static final String FIELD_ADDRESS_STATE = "address_state";
    static final String FIELD_ADDRESS_ZIP = "address_zip";
    static final String FIELD_BRAND = "brand";
    static final String FIELD_COUNTRY = "country";
    static final String FIELD_CURRENCY = "currency";
    static final String FIELD_EXP_MONTH = "exp_month";
    static final String FIELD_EXP_YEAR = "exp_year";
    static final String FIELD_FINGERPRINT = "fingerprint";
    static final String FIELD_FUNDING = "funding";
    static final String FIELD_LAST4 = "last4";
    static final String FIELD_NAME = "name";

    /**
     * Parse the card directly from a JSON-formatted {@link String} value.
     *
     * @param jsonCard the raw JSON
     * @return a {@link Card} object represented by the JSON
     * @throws JSONException if the String is improperly formatted or is missing required values
     */
    @NonNull
    static Card parseCard(String jsonCard) throws JSONException {
        JSONObject cardObject = new JSONObject(jsonCard);
        return parseCard(cardObject);
    }

    /**
     * Convert a {@link JSONObject} into a {@link Card} object.
     *
     * @param objectCard a {@link JSONObject} that represents a {@link Card}
     * @return a {@link Card} with fields determined by the input
     * @throws JSONException if the input is missing a required field
     */
    @NonNull
    static Card parseCard(@NonNull JSONObject objectCard) throws JSONException {
        return new Card(
                null,
                objectCard.getInt(FIELD_EXP_MONTH),
                objectCard.getInt(FIELD_EXP_YEAR),
                null,
                StripeJsonUtils.optString(objectCard, FIELD_NAME),
                StripeJsonUtils.optString(objectCard, FIELD_ADDRESS_LINE1),
                StripeJsonUtils.optString(objectCard, FIELD_ADDRESS_LINE2),
                StripeJsonUtils.optString(objectCard, FIELD_ADDRESS_CITY),
                StripeJsonUtils.optString(objectCard, FIELD_ADDRESS_STATE),
                StripeJsonUtils.optString(objectCard, FIELD_ADDRESS_ZIP),
                StripeJsonUtils.optString(objectCard, FIELD_ADDRESS_COUNTRY),
                StripeTextUtils.asCardBrand(StripeJsonUtils.optString(objectCard, FIELD_BRAND)),
                StripeJsonUtils.optString(objectCard, FIELD_LAST4),
                StripeJsonUtils.optString(objectCard, FIELD_FINGERPRINT),
                StripeTextUtils.asFundingType(StripeJsonUtils.optString(objectCard, FIELD_FUNDING)),
                StripeJsonUtils.optString(objectCard, FIELD_COUNTRY),
                StripeJsonUtils.optString(objectCard, FIELD_CURRENCY));
    }
}
