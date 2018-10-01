package com.stripe.android.model;

import android.support.annotation.Nullable;
import android.support.annotation.StringDef;
import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Date;

/**
 * Tokenization is the process Stripe uses to collect sensitive card, bank account details, Stripe
 * account details or personally identifiable information (PII), directly from your customers in a
 * secure manner. A Token representing this information is returned to you to use.
 */
public class Token implements StripePaymentSource {

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({TYPE_CARD, TYPE_BANK_ACCOUNT, TYPE_PII, TYPE_ACCOUNT})
    public @interface TokenType {}
    public static final String TYPE_CARD = "card";
    public static final String TYPE_BANK_ACCOUNT = "bank_account";
    public static final String TYPE_PII = "pii";
    public static final String TYPE_ACCOUNT = "account";

    // The key for these object fields is identical to their retrieved values
    // from the Type field.
    private static final String FIELD_BANK_ACCOUNT = Token.TYPE_BANK_ACCOUNT;
    private static final String FIELD_CARD = Token.TYPE_CARD;
    private static final String FIELD_CREATED = "created";
    private static final String FIELD_ID = "id";
    private static final String FIELD_LIVEMODE = "livemode";

    private static final String FIELD_TYPE = "type";
    private static final String FIELD_USED = "used";
    private final String mId;
    private final String mType;
    private final Date mCreated;
    private final boolean mLivemode;
    private final boolean mUsed;
    private final BankAccount mBankAccount;
    private final Card mCard;

    /**
     * Constructor that should not be invoked in your code.  This is used by Stripe to
     * create tokens using a Stripe API response.
     */
    public Token(
            String id,
            boolean livemode,
            Date created,
            Boolean used,
            Card card) {
        mId = id;
        mType = TYPE_CARD;
        mCreated = created;
        mLivemode = livemode;
        mCard = card;
        mUsed = used;
        mBankAccount = null;
    }

    /**
     * Constructor that should not be invoked in your code.  This is used by Stripe to
     * create tokens using a Stripe API response.
     */
    public Token(
            String id,
            boolean livemode,
            Date created,
            Boolean used,
            BankAccount bankAccount) {
        mId = id;
        mType = TYPE_BANK_ACCOUNT;
        mCreated = created;
        mLivemode = livemode;
        mCard = null;
        mUsed = used;
        mBankAccount = bankAccount;
    }

    /**
     * Constructor that should not be invoked in your code.  This is used by Stripe to
     * create tokens using a Stripe API response.
     */
    public Token(
            String id,
            String type,
            boolean livemode,
            Date created,
            Boolean used
    ) {
        mId = id;
        mType = type;
        mCreated = created;
        mCard = null;
        mBankAccount = null;
        mUsed = used;
        mLivemode = livemode;
    }

    /***
     * @return the {@link Date} this token was created
     */
    public Date getCreated() {
        return mCreated;
    }

    /**
     * @return the {@link #mId} of this token
     */
    @Override
    public String getId() {
        return mId;
    }

    /**
     * @return {@code true} if this token is valid for a real payment, {@code false} if
     * it is only usable for testing
     */
    public boolean getLivemode() {
        return mLivemode;
    }

    /**
     * @return {@code true} if this token has been used, {@code false} otherwise
     */
    public boolean getUsed() {
        return mUsed;
    }

    /**
     * @return Get the {@link TokenType} of this token.
     */
    @TokenType
    public String getType() {
        return mType;
    }

    /**
     * @return the {@link Card} for this token
     */
    public Card getCard() {
        return mCard;
    }

    /**
     * @return the {@link BankAccount} for this token
     */
    public BankAccount getBankAccount() {
        return mBankAccount;
    }

    @Nullable
    public static Token fromString(@Nullable String jsonString) {
        if (jsonString == null) {
            return null;
        }
        try {
            JSONObject tokenObject = new JSONObject(jsonString);
            return fromJson(tokenObject);
        } catch (JSONException exception) {
            return null;
        }
    }

    @Nullable
    public static Token fromJson(@Nullable JSONObject jsonObject) {
        if (jsonObject == null) {
            return null;
        }
        String tokenId = StripeJsonUtils.optString(jsonObject, FIELD_ID);
        Long createdTimeStamp = StripeJsonUtils.optLong(jsonObject, FIELD_CREATED);
        Boolean liveMode = StripeJsonUtils.optBoolean(jsonObject, FIELD_LIVEMODE);
        @TokenType String tokenType =
                asTokenType(StripeJsonUtils.optString(jsonObject, FIELD_TYPE));
        Boolean used = StripeJsonUtils.optBoolean(jsonObject, FIELD_USED);

        if (tokenId == null || createdTimeStamp == null || liveMode == null) {
            return null;
        }
        Date date = new Date(createdTimeStamp * 1000);

        Token token = null;
        if (Token.TYPE_BANK_ACCOUNT.equals(tokenType)) {
            JSONObject bankAccountObject = jsonObject.optJSONObject(FIELD_BANK_ACCOUNT);
            if (bankAccountObject == null) {
                return null;
            }
            BankAccount bankAccount = BankAccount.fromJson(bankAccountObject);
            token = new Token(tokenId, liveMode, date, used, bankAccount);
        } else if (Token.TYPE_CARD.equals(tokenType)) {
            JSONObject cardObject = jsonObject.optJSONObject(FIELD_CARD);
            if (cardObject == null) {
                return null;
            }
            Card card = Card.fromJson(cardObject);
            token = new Token(tokenId, liveMode, date, used, card);
        } else if (Token.TYPE_PII.equals(tokenType) || Token.TYPE_ACCOUNT.equals(tokenType)) {
            token = new Token(tokenId, tokenType, liveMode, date, used);
        }
        return token;
    }

    /**
     * Converts an unchecked String value to a {@link TokenType} or {@code null}.
     *
     * @param possibleTokenType a String that might match a {@link TokenType} or be empty
     * @return {@code null} if the input is blank or otherwise does not match a {@link TokenType},
     * else the appropriate {@link TokenType}.
     */
    @Nullable
    @TokenType
    static String asTokenType(@Nullable String possibleTokenType) {
        if (possibleTokenType == null || TextUtils.isEmpty(possibleTokenType.trim())) {
            return null;
        }

        if (Token.TYPE_CARD.equals(possibleTokenType)) {
            return Token.TYPE_CARD;
        } else if (Token.TYPE_BANK_ACCOUNT.equals(possibleTokenType)) {
            return Token.TYPE_BANK_ACCOUNT;
        } else if (Token.TYPE_PII.equals(possibleTokenType)) {
            return Token.TYPE_PII;
        } else if (Token.TYPE_ACCOUNT.equals(possibleTokenType)) {
            return Token.TYPE_ACCOUNT;
        }

        return null;
    }
}
