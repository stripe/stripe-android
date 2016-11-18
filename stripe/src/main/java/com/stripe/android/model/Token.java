package com.stripe.android.model;

import java.util.Date;

/**
 * The model of a Stripe card token.
 */
public class Token extends com.stripe.model.StripeObject {
    private final String id;
    private final Date created;
    private final boolean livemode;
    private final boolean used;
    private final Card card;

    /**
     * This method should not be invoked in your code.  This is used by Stripe to
     * create tokens using a Stripe API response
     */
    public Token(String id, boolean livemode, Date created, Boolean used, Card card) {
        this.id = id;
        this.livemode = livemode;
        this.card = card;
        this.created = created;
        this.used = used;
    }

    /***
     * @return the {@link Date} this token was created
     */
    public Date getCreated() {
        return created;
    }

    /**
     * @return the {@link #id} of this token
     */
    public String getId() {
        return id;
    }

    /**
     * @return {@code true} if this token is valid for a real payment, {@code false} if
     * it is only usable for testing
     */
    public boolean getLivemode() {
        return livemode;
    }

    /**
     * @return {@code true} if this token has been used, {@code false} otherwise
     */
    public boolean getUsed() {
        return used;
    }

    /**
     * @return the {@link Card} for this token
     */
    public Card getCard() {
        return card;
    }
}
