package com.stripe.android.model;

import com.stripe.android.util.TextUtils;
import com.stripe.android.validators.CardParamsValidator;

public class CardParams extends CardParamsValidator {

    // Required Parameters
    private String number;
    private Integer expMonth;
    private Integer expYear;

    // Optional Parameters
    private String cvc;
    private String currency;
    private String name;
    private String addressLine1;
    private String addressLine2;
    private String addressCity;
    private String addressState;
    private String addressZip;
    private String addressCountry;

    public static class Builder {
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

        public Builder(String number, Integer expMonth, Integer expYear) {
            this.number = number;
            this.expMonth = expMonth;
            this.expYear = expYear;
        }

        public Builder(String number, Integer expMonth, Integer expYear, String cvc) {
            this.number = number;
            this.expMonth = expMonth;
            this.expYear = expYear;
            this.cvc = cvc;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder addressLine1(String address) {
            this.addressLine1 = address;
            return this;
        }

        public Builder addressLine2(String address) {
            this.addressLine2 = address;
            return this;
        }

        public Builder addressCity(String city) {
            this.addressCity = city;
            return this;
        }

        public Builder addressState(String state) {
            this.addressState = state;
            return this;
        }

        public Builder addressZip(String zip) {
            this.addressZip = zip;
            return this;
        }

        public Builder addressCountry(String country) {
            this.addressCountry = country;
            return this;
        }

        public Builder currency(String currency)
        {
            this.currency = currency;
            return this;
        }

        public CardParams build() {
            return new CardParams(number, expMonth, expYear, cvc, currency, name, addressLine1, addressLine2, addressCity, addressState, addressZip, addressCountry);
        }
    }

    public CardParams(String number, Integer expMonth, Integer expYear) {
        this.number = TextUtils.nullIfBlank(normalizeCardNumber(number));
        this.expMonth = expMonth;
        this.expYear = expYear;
    }

    public CardParams(String number, Integer expMonth, Integer expYear, String cvc) {
        this(number, expMonth, expYear);
        this.cvc = TextUtils.nullIfBlank(cvc);
    }

    public CardParams(String number, Integer expMonth, Integer expYear, String cvc, String currency) {
        this(number, expMonth, expYear, cvc);
        this.currency = TextUtils.nullIfBlank(currency);
    }

    public CardParams(String number, Integer expMonth, Integer expYear, String cvc, String currency, String name, String addressLine1, String addressLine2, String addressCity, String addressState, String addressZip, String addressCountry) {
        this(number, expMonth, expYear, cvc, currency);
        this.name = TextUtils.nullIfBlank(name);
        this.addressLine1 = TextUtils.nullIfBlank(addressLine1);
        this.addressLine2 = TextUtils.nullIfBlank(addressLine2);
        this.addressCity = TextUtils.nullIfBlank(addressCity);
        this.addressState = TextUtils.nullIfBlank(addressState);
        this.addressZip = TextUtils.nullIfBlank(addressZip);
        this.addressCountry = TextUtils.nullIfBlank(addressCountry);
    }

    // Helpers

    public String getLast4() {
        if (number != null && number.length() > 4) {
            return number.substring(number.length() - 4, number.length());
        }
        return null;
    }

    // Getters and Setters
    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public Integer getExpMonth() {
        return expMonth;
    }

    public void setExpMonth(Integer expMonth) {
        this.expMonth = expMonth;
    }

    public Integer getExpYear() {
        return expYear;
    }

    public void setExpYear(Integer expYear) {
        this.expYear = expYear;
    }

    public String getCvc() {
        return cvc;
    }

    public void setCvc(String cvc) {
        this.cvc = cvc;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddressLine1() {
        return addressLine1;
    }

    public void setAddressLine1(String addressLine1) {
        this.addressLine1 = addressLine1;
    }

    public String getAddressLine2() {
        return addressLine2;
    }

    public void setAddressLine2(String addressLine2) {
        this.addressLine2 = addressLine2;
    }

    public String getAddressCity() {
        return addressCity;
    }

    public void setAddressCity(String addressCity) {
        this.addressCity = addressCity;
    }

    public String getAddressState() {
        return addressState;
    }

    public void setAddressState(String addressState) {
        this.addressState = addressState;
    }

    public String getAddressZip() {
        return addressZip;
    }

    public void setAddressZip(String addressZip) {
        this.addressZip = addressZip;
    }

    public String getAddressCountry() {
        return addressCountry;
    }

    public void setAddressCountry(String addressCountry) {
        this.addressCountry = addressCountry;
    }

}
