package com.stripe.android.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringDef;
import android.text.TextUtils;

import com.stripe.android.utils.ObjectUtils;

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
    @StringDef({TYPE_CARD, TYPE_BANK_ACCOUNT, TYPE_PII, TYPE_ACCOUNT, TYPE_CVC_UPDATE})
    public @interface TokenType {}
    public static final String TYPE_CARD = "card";
    public static final String TYPE_BANK_ACCOUNT = "bank_account";
    public static final String TYPE_PII = "pii";
    public static final String TYPE_ACCOUNT = "account";
    public static final String TYPE_CVC_UPDATE = "cvc_update";

    // The key for these object fields is identical to their retrieved values
    // from the Type field.
    private static final String FIELD_BANK_ACCOUNT = Token.TYPE_BANK_ACCOUNT;
    private static final String FIELD_CARD = Token.TYPE_CARD;
    private static final String FIELD_CREATED = "created";
    private static final String FIELD_ID = "id";
    private static final String FIELD_LIVEMODE = "livemode";

    private static final String FIELD_TYPE = "type";
    private static final String FIELD_USED = "used";

    @NonNull private final String mId;
    @NonNull private final String mType;
    @NonNull private final Date mCreated;
    private final boolean mLivemode;
    private final boolean mUsed;
    @Nullable private final BankAccount mBankAccount;
    @Nullable private final Card mCard;

    /**
     * Constructor that should not be invoked in your code.  This is used by Stripe to
     * create tokens using a Stripe API response.
     */
    public Token(
            @NonNull String id,
            boolean livemode,
            @NonNull Date created,
            @Nullable Boolean used,
            @Nullable Card card) {
        mId = id;
        mType = TYPE_CARD;
        mCreated = created;
        mLivemode = livemode;
        mCard = card;
        mUsed = Boolean.TRUE.equals(used);
        mBankAccount = null;
    }

    /**
     * Constructor that should not be invoked in your code.  This is used by Stripe to
     * create tokens using a Stripe API response.
     */
    public Token(
            @NonNull String id,
            boolean livemode,
            @NonNull Date created,
            @Nullable Boolean used,
            @NonNull BankAccount bankAccount) {
        mId = id;
        mType = TYPE_BANK_ACCOUNT;
        mCreated = created;
        mLivemode = livemode;
        mCard = null;
        mUsed = Boolean.TRUE.equals(used);
        mBankAccount = bankAccount;
    }

    /**
     * Constructor that should not be invoked in your code.  This is used by Stripe to
     * create tokens using a Stripe API response.
     */
    public Token(
            @NonNull String id,
            @NonNull String type,
            boolean livemode,
            @NonNull Date created,
            @Nullable Boolean used) {
        mId = id;
        mType = type;
        mCreated = created;
        mCard = null;
        mBankAccount = null;
        mUsed = Boolean.TRUE.equals(used);
        mLivemode = livemode;
    }

    /***
     * @return the {@link Date} this token was created
     */
    @NonNull
    public Date getCreated() {
        return mCreated;
    }

    /**
     * @return the {@link #mId} of this token
     */
    @Nullable
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
    @NonNull
    @TokenType
    public String getType() {
        return mType;
    }

    /**
     * @return the {@link Card} for this token
     */
    @Nullable
    public Card getCard() {
        return mCard;
    }

    /**
     * @return the {@link BankAccount} for this token
     */
    @Nullable
    public BankAccount getBankAccount() {
        return mBankAccount;
    }

    @Override
    public int hashCode() {
        return ObjectUtils.hash(mId, mType, mCreated, mLivemode, mUsed, mBankAccount, mCard);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return super.equals(obj) || (obj instanceof Token && typedEquals((Token) obj));
    }

    private boolean typedEquals(@NonNull Token token) {
        return ObjectUtils.equals(mId, token.mId)
                && ObjectUtils.equals(mType, token.mType)
                && ObjectUtils.equals(mCreated, token.mCreated)
                && mLivemode == token.mLivemode
                && mUsed == token.mUsed
                && ObjectUtils.equals(mBankAccount, token.mBankAccount)
                && ObjectUtils.equals(mCard, token.mCard);
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
        final String tokenId = StripeJsonUtils.optString(jsonObject, FIELD_ID);
        final Long createdTimeStamp = StripeJsonUtils.optLong(jsonObject, FIELD_CREATED);
        final Boolean liveModeOpt = StripeJsonUtils.optBoolean(jsonObject, FIELD_LIVEMODE);
        @TokenType final String tokenType =
                asTokenType(StripeJsonUtils.optString(jsonObject, FIELD_TYPE));
        final Boolean usedOpt = StripeJsonUtils.optBoolean(jsonObject, FIELD_USED);
        if (tokenId == null || createdTimeStamp == null || liveModeOpt == null) {
            return null;
        }

        final boolean used = Boolean.TRUE.equals(usedOpt);
        final boolean liveMode = Boolean.TRUE.equals(liveModeOpt);
        final Date date = new Date(createdTimeStamp * 1000);

        final Token token;
        if (Token.TYPE_BANK_ACCOUNT.equals(tokenType)) {
            final JSONObject bankAccountObject = jsonObject.optJSONObject(FIELD_BANK_ACCOUNT);
            if (bankAccountObject == null) {
                return null;
            }
            token = new Token(tokenId, liveMode, date, used,
                    BankAccount.fromJson(bankAccountObject));
        } else if (Token.TYPE_CARD.equals(tokenType)) {
            final JSONObject cardObject = jsonObject.optJSONObject(FIELD_CARD);
            if (cardObject == null) {
                return null;
            }
            token = new Token(tokenId, liveMode, date, used, Card.fromJson(cardObject));
        } else if (Token.TYPE_PII.equals(tokenType) ||
                Token.TYPE_ACCOUNT.equals(tokenType) ||
                Token.TYPE_CVC_UPDATE.equals(tokenType)) {
            token = new Token(tokenId, tokenType, liveMode, date, used);
        } else {
            token = null;
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
    private static String asTokenType(@Nullable String possibleTokenType) {
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
        } else if (Token.TYPE_CVC_UPDATE.equals(possibleTokenType)) {
            return Token.TYPE_CVC_UPDATE;
        }

        return null;
    }
}
