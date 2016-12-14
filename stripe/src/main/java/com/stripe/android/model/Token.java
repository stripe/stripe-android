package com.stripe.android.model;

import android.support.annotation.NonNull;
import android.support.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Date;

/**
 * The model of a Stripe card token.
 */
public class Token {

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({TYPE_CARD})
    public @interface TokenType {}
    public static final String TYPE_CARD = "card";

    private final String mId;
    private final String mType;
    private final Date mCreated;
    private final boolean mLivemode;
    private final boolean mUsed;
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
            Card card,
            @TokenType String type) {
        mId = id;
        mType = type;
        mCreated = created;
        mLivemode = livemode;
        mCard = card;
        mUsed = used;
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
}
