package com.stripe.android.model;

import android.support.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Date;

/**
 * The model of a Stripe card token.
 */
public class Token implements StripePaymentSource {

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({TYPE_CARD, TYPE_BANK_ACCOUNT})
    public @interface TokenType {}
    public static final String TYPE_CARD = "card";
    public static final String TYPE_BANK_ACCOUNT = "bank_account";

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
}
