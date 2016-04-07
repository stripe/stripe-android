package com.stripe.android.model;

import java.util.Date;

// This is different from Token in com.stripe.model because it does not
public class Token extends com.stripe.model.StripeObject {
    private final String id;
    private final Date created;
    private final String currency;
    private final String email;
    private final String clientIp;
    private final String type;
    private final Integer amount;
    private final Boolean livemode;
    private final Boolean used;
    private final Card card;
    //BankAccount bankAccount;

    public Token(String id, boolean livemode, Date created, Boolean used, String currency, String email, String clientIp, String type, Integer amount, Card card) {
        this.id = id;
        this.livemode = livemode;
        this.card = card;
        this.created = created;
        this.used = used;
        this.currency = currency;
        this.email = email;
        this.clientIp = clientIp;
        this.type = type;
        this.amount = amount;
    }

    public String getId() {
        return id;
    }

    public Date getCreated() {
        return created;
    }

    public String getCurrency() {
        return currency;
    }

    public String getEmail() {
        return email;
    }

    public String getClientIp() {
        return clientIp;
    }

    public String getType() {
        return type;
    }

    public Integer getAmount() {
        return amount;
    }

    public Boolean getLivemode() {
        return livemode;
    }

    public Card getCard() {
        return card;
    }

    public Boolean getUsed() {
        return used;
    }

}