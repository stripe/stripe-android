package com.stripe.android.model;

import java.util.Date;

// This is different from Token in com.stripe.model because it does not
public class Token extends com.stripe.model.StripeObject {
    private String mId;
    private Date mCreated;
    private boolean mLivemode;
    private boolean mUsed;
    private Card mCard;

    public Date getCreated() {
        return mCreated;
    }

    public String getId() {
        return mId;
    }

    public boolean getLivemode() {
        return mLivemode;
    }

    public boolean getUsed() {
        return mUsed;
    }

    public Card getCard() {
        return mCard;
    }

    /*
     * This method should not be invoked in your code. This is used by Stripe to
     * create tokens using a Stripe API response
     */
    public Token(String id, boolean livemode, Date created, Boolean used,
            Card card) {
        mId = id;
        mLivemode = livemode;
        mCard = card;
        mCreated = created;
        mUsed = used;
    }
}