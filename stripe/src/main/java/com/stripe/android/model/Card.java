package com.stripe.android.model;

public class Card extends com.stripe.model.StripeObject {

    private final String Id;
    private final String last4;
    private final String dynamicLast4;
    private final String country;
    private final String type;
    private final String brand;
    private final String currency;
    private final String addressLine1Check;
    private final String cvcCheck;
    private final String fingerprint;
    private final String funding;
    private final String tokenizationMethod;

    // Update-friendly Parameters
    private Integer expMonth;
    private Integer expYear;
    private String name;
    private String addressLine1;
    private String addressLine2;
    private String addressZip;
    private String addressCity;
    private String addressState;
    private String addressCountry;
    private String addressZipCheck;
    private Boolean default_for_currency;

    // TODO: Do we want to support the 'status' parameter here?
    // TODO: Do we want to support the 'recipient' parameter here?
    // TODO: Metadata

    public Card(String id, String status, Integer expMonth, Integer expYear, String last4, String dynamicLast4, String country, String type, String name, String addressLine1, String addressLine2, String addressZip, String addressCity, String addressState, String addressCountry, String addressZipCheck, String addressLine1Check, String cvcCheck, String fingerprint, String brand, String funding, String currency, String tokenizationMethod) {
        this.Id = id;
        this.setExpMonth(expMonth);
        this.setExpYear(expYear);
        this.last4 = last4;
        this.dynamicLast4 = dynamicLast4;
        this.country = country;
        this.type = type;
        this.setName(name);
        this.addressLine1 = addressLine1;
        this.addressLine2 = addressLine2;
        this.addressZip = addressZip;
        this.addressCity = addressCity;
        this.addressState = addressState;
        this.addressCountry = addressCountry;
        this.addressZipCheck = addressZipCheck;
        this.addressLine1Check = addressLine1Check;
        this.cvcCheck = cvcCheck;
        this.fingerprint = fingerprint;
        this.brand = brand;
        this.funding = funding;
        this.currency = currency;
        this.tokenizationMethod = tokenizationMethod;
    }

    public String getId() {
        return Id;
    }

    public String getLast4() {
        return last4;
    }

    public String getDynamicLast4() {
        return dynamicLast4;
    }

    public String getCountry() {
        return country;
    }

    public String getType() {
        return type;
    }

    public String getBrand() {
        return brand;
    }

    public String getCurrency() {
        return currency;
    }

    public String getAddressLine1Check() {
        return addressLine1Check;
    }

    public String getCvcCheck() {
        return cvcCheck;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public String getFunding() {
        return funding;
    }

    public String getTokenizationMethod() {
        return tokenizationMethod;
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

    public String getAddressZip() {
        return addressZip;
    }

    public void setAddressZip(String addressZip) {
        this.addressZip = addressZip;
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

    public String getAddressCountry() {
        return addressCountry;
    }

    public void setAddressCountry(String addressCountry) {
        this.addressCountry = addressCountry;
    }

    public String getAddressZipCheck() {
        return addressZipCheck;
    }

    public void setAddressZipCheck(String addressZipCheck) {
        this.addressZipCheck = addressZipCheck;
    }
}