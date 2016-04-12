package com.stripe.android.model;

import java.util.Date;

public class TokenBuilder {
    private String id;
    private Date created;
    private Boolean livemode;
    private Boolean used;
    private String currency;
    private String email;
    private String clientIp;
    private String type;
    private Integer amount;
    private Card card;
    //private BankAccount bankAccount;

    public TokenBuilder(String id, Date created, Boolean livemode) {
        this.id = id;
        this.created = created;
        this.livemode = livemode;
    }

    public TokenBuilder setUsed(Boolean used) {
        this.used = used;
        return this;
    }

    public TokenBuilder setCurrency(String currency) {
        this.currency = currency;
        return this;
    }

    public TokenBuilder setEmail(String email) {
        this.email = email;
        return this;
    }

    public TokenBuilder setClientIp(String clientIp) {
        this.clientIp = clientIp;
        return this;
    }

    public TokenBuilder setType(String type) {
        this.type = type;
        return this;
    }

    public TokenBuilder setAmount(Integer amount) {
        this.amount = amount;
        return this;
    }

    public TokenBuilder setCard(Card card) {
        this.card = card;
        return this;
    }

//    public TokenBuilder setBankAccount(BankAccount bankAccount) {
//        this.bankAccount = bankAccount;
//        return this;
//    }

    public Token createToken() {
        //return new Token(id, livemode, created, used, currency, email, clientIp, type, amount, card, bankAccount);
        return new Token(id, livemode, created, used, currency, email, clientIp, type, amount, card);
    }
}