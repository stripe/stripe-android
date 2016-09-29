package com.stripe.android.model;

public class CardParamsBuilder {
    private final String number;
    private final Integer expMonth;
    private final Integer expYear;
    private String cvc;
    private String currency;
    private String name;
    private String addressLine1;
    private String addressLine2;
    private String addressCity;
    private String addressState;
    private String addressZip;
    private String addressCountry;

    public CardParamsBuilder(String number, Integer expMonth, Integer expYear) {
        this.number = number;
        this.expMonth = expMonth;
        this.expYear = expYear;
    }

    public CardParamsBuilder(String number, Integer expMonth, Integer expYear, String cvc) {
        this.number = number;
        this.expMonth = expMonth;
        this.expYear = expYear;
        this.cvc = cvc;
    }

    public CardParamsBuilder name(String name) {
        this.name = name;
        return this;
    }

    public CardParamsBuilder addressLine1(String address) {
        this.addressLine1 = address;
        return this;
    }

    public CardParamsBuilder addressLine2(String address) {
        this.addressLine2 = address;
        return this;
    }

    public CardParamsBuilder addressCity(String city) {
        this.addressCity = city;
        return this;
    }

    public CardParamsBuilder addressState(String state) {
        this.addressState = state;
        return this;
    }

    public CardParamsBuilder addressZip(String zip) {
        this.addressZip = zip;
        return this;
    }

    public CardParamsBuilder addressCountry(String country) {
        this.addressCountry = country;
        return this;
    }

    public CardParamsBuilder currency(String currency)
    {
        this.currency = currency;
        return this;
    }

    public CardParams build() {
        return new CardParams(number, expMonth, expYear, cvc, currency, name, addressLine1, addressLine2, addressCity, addressState, addressZip, addressCountry);
    }
}
