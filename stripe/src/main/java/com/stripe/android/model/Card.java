package com.stripe.android.model;

import com.stripe.android.util.DateUtils;
import com.stripe.android.util.TextUtils;

public class Card extends com.stripe.model.StripeObject {
    public static final String AMERICAN_EXPRESS = "American Express";
    public static final String DISCOVER = "Discover";
    public static final String JCB = "JCB";
    public static final String DINERS_CLUB = "Diners Club";
    public static final String VISA = "Visa";
    public static final String MASTERCARD = "MasterCard";
    public static final String UNKNOWN = "Unknown";

    // Based on http://en.wikipedia.org/wiki/Bank_card_number#Issuer_identification_number_.28IIN.29
    public static final String[] PREFIXES_AMERICAN_EXPRESS = {"34", "37"};
    public static final String[] PREFIXES_DISCOVER = {"60", "62", "64", "65"};
    public static final String[] PREFIXES_JCB = {"35"};
    public static final String[] PREFIXES_DINERS_CLUB = {"300", "301", "302", "303", "304", "305", "309", "36", "38", "39"};
    public static final String[] PREFIXES_VISA = {"4"};
    public static final String[] PREFIXES_MASTERCARD = {"50", "51", "52", "53", "54", "55"};

    public static final int MAX_LENGTH_STANDARD = 16;
    public static final int MAX_LENGTH_AMERICAN_EXPRESS = 15;
    public static final int MAX_LENGTH_DINERS_CLUB = 14;


    private String number;
    private String cvc;
    private Integer expMonth;
    private Integer expYear;
    private String name;
    private String addressLine1;
    private String addressLine2;
    private String addressCity;
    private String addressState;
    private String addressZip;
    private String addressCountry;
    private String last4;
    private String type;
    private String fingerprint;
    private String country;
    private String currency;

    public static class Builder {
        private final String number;
        private final String cvc;
        private final Integer expMonth;
        private final Integer expYear;
        private String name;
        private String addressLine1;
        private String addressLine2;
        private String addressCity;
        private String addressState;
        private String addressZip;
        private String addressCountry;
        private String last4;
        private String type;
        private String fingerprint;
        private String country;
        private String currency;

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

        public Builder last4(String last4) {
            this.last4 = last4;
            return this;
        }

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder fingerprint(String fingerprint) {
            this.fingerprint = fingerprint;
            return this;
        }

        public Builder country(String country) {
            this.country = country;
            return this;
        }

        public Builder currency(String currency)
        {
            this.currency = currency;
            return this;
        }

        public Card build() {
            return new Card(this);
        }
    }

    private Card(Builder builder) {
        this.number = TextUtils.nullIfBlank(normalizeCardNumber(builder.number));
        this.expMonth = builder.expMonth;
        this.expYear = builder.expYear;
        this.cvc = TextUtils.nullIfBlank(builder.cvc);
        this.name = TextUtils.nullIfBlank(builder.name);
        this.addressLine1 = TextUtils.nullIfBlank(builder.addressLine1);
        this.addressLine2 = TextUtils.nullIfBlank(builder.addressLine2);
        this.addressCity = TextUtils.nullIfBlank(builder.addressCity);
        this.addressState = TextUtils.nullIfBlank(builder.addressState);
        this.addressZip = TextUtils.nullIfBlank(builder.addressZip);
        this.addressCountry = TextUtils.nullIfBlank(builder.addressCountry);
        this.last4 = TextUtils.nullIfBlank(builder.last4);
        this.type = TextUtils.nullIfBlank(builder.type);
        this.fingerprint = TextUtils.nullIfBlank(builder.fingerprint);
        this.country = TextUtils.nullIfBlank(builder.country);
        this.type = getType();
        this.last4 = getLast4();
        this.currency = TextUtils.nullIfBlank(builder.currency);
    }

    public Card(String number, Integer expMonth, Integer expYear, String cvc, String name, String addressLine1, String addressLine2, String addressCity,
                String addressState, String addressZip, String addressCountry, String last4, String type, String fingerprint, String country) {
        this.number = TextUtils.nullIfBlank(normalizeCardNumber(number));
        this.expMonth = expMonth;
        this.expYear = expYear;
        this.cvc = TextUtils.nullIfBlank(cvc);
        this.name = TextUtils.nullIfBlank(name);
        this.addressLine1 = TextUtils.nullIfBlank(addressLine1);
        this.addressLine2 = TextUtils.nullIfBlank(addressLine2);
        this.addressCity = TextUtils.nullIfBlank(addressCity);
        this.addressState = TextUtils.nullIfBlank(addressState);
        this.addressZip = TextUtils.nullIfBlank(addressZip);
        this.addressCountry = TextUtils.nullIfBlank(addressCountry);
        this.last4 = TextUtils.nullIfBlank(last4);
        this.type = TextUtils.nullIfBlank(type);
        this.fingerprint = TextUtils.nullIfBlank(fingerprint);
        this.country = TextUtils.nullIfBlank(country);
        this.type = getType();
        this.last4 = getLast4();
        this.currency = TextUtils.nullIfBlank(currency);
    }

    public Card(String number, Integer expMonth, Integer expYear, String cvc, String name, String addressLine1, String addressLine2, String addressCity, String addressState, String addressZip, String addressCountry, String currency) {
        this(number, expMonth, expYear, cvc, name, addressLine1, addressLine2, addressCity, addressState, addressZip, addressCountry, null, null, null, null);
    }

    public Card(String number, Integer expMonth, Integer expYear, String cvc) {
        this(number, expMonth, expYear, cvc, null, null, null, null, null, null, null, null, null, null, null);
    }

    public boolean validateCard() {
        if (cvc == null) {
            return validateNumber() && validateExpiryDate();
        } else {
            return validateNumber() && validateExpiryDate() && validateCVC();
        }
    }

    public boolean validateNumber() {
        if (TextUtils.isBlank(number)) {
            return false;
        }

        String rawNumber = number.trim().replaceAll("\\s+|-", "");
        if (TextUtils.isBlank(rawNumber)
                || !TextUtils.isWholePositiveNumber(rawNumber)
                || !isValidLuhnNumber(rawNumber)) {
            return false;
        }

        if (AMERICAN_EXPRESS.equals(type)) {
            return rawNumber.length() == MAX_LENGTH_AMERICAN_EXPRESS;
        } else if (DINERS_CLUB.equals(type)) {
            return rawNumber.length() == MAX_LENGTH_DINERS_CLUB;
        } else {
            return rawNumber.length() == MAX_LENGTH_STANDARD;
        }
    }

    public boolean validateExpiryDate() {
    	if (!validateExpMonth()) {
    		return false;
    	}
    	if (!validateExpYear()) {
    		return false;
    	}
    	return !DateUtils.hasMonthPassed(expYear, expMonth);
    }

    public boolean validateExpMonth() {
    	if (expMonth == null) {
    		return false;
    	}
    	return (expMonth >= 1 && expMonth <= 12);
    }

    public boolean validateExpYear() {
    	if (expYear == null) {
    		return false;
    	}
    	return !DateUtils.hasYearPassed(expYear);
    }

    public boolean validateCVC() {
        if (TextUtils.isBlank(cvc)) {
            return false;
        }
        String cvcValue = cvc.trim();

        boolean validLength = ((type == null && cvcValue.length() >= 3 && cvcValue.length() <= 4) ||
                (AMERICAN_EXPRESS.equals(type) && cvcValue.length() == 4) ||
                (!AMERICAN_EXPRESS.equals(type) && cvcValue.length() == 3));

        if (!TextUtils.isWholePositiveNumber(cvcValue) || !validLength) {
            return false;
        }
        return true;
    }

    private boolean isValidLuhnNumber(String number) {
        boolean isOdd = true;
        int sum = 0;

        for (int index = number.length() - 1; index >= 0; index--) {
            char c = number.charAt(index);
            if (!Character.isDigit(c)) {
                return false;
            }
            int digitInteger = Integer.parseInt("" + c);
            isOdd = !isOdd;

            if (isOdd) {
                digitInteger *= 2;
            }

            if (digitInteger > 9) {
                digitInteger -= 9;
            }

            sum += digitInteger;
        }

        return sum % 10 == 0;
    }

    private String normalizeCardNumber(String number) {
        if (number == null) {
            return null;
        }
        return number.trim().replaceAll("\\s+|-", "");
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getCVC() {
        return cvc;
    }

    public void setCVC(String cvc) {
        this.cvc = cvc;
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

    public String getAddressCity() {
        return addressCity;
    }

    public void setAddressCity(String addressCity) {
        this.addressCity = addressCity;
    }

    public String getAddressZip() {
        return addressZip;
    }

    public void setAddressZip(String addressZip) {
        this.addressZip = addressZip;
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

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getLast4() {
        if (!TextUtils.isBlank(last4)) {
            return last4;
        }
        if (number != null && number.length() > 4) {
            return number.substring(number.length() - 4, number.length());
        }
        return null;
    }

    public String getType() {
        if (TextUtils.isBlank(type) && !TextUtils.isBlank(number)) {
            if (TextUtils.hasAnyPrefix(number, PREFIXES_AMERICAN_EXPRESS)) {
                return AMERICAN_EXPRESS;
            } else if (TextUtils.hasAnyPrefix(number, PREFIXES_DISCOVER)) {
                return DISCOVER;
            } else if (TextUtils.hasAnyPrefix(number, PREFIXES_JCB)) {
                return JCB;
            } else if (TextUtils.hasAnyPrefix(number, PREFIXES_DINERS_CLUB)) {
                return DINERS_CLUB;
            } else if (TextUtils.hasAnyPrefix(number, PREFIXES_VISA)) {
                return VISA;
            } else if (TextUtils.hasAnyPrefix(number, PREFIXES_MASTERCARD)) {
                return MASTERCARD;
            } else {
                return UNKNOWN;
            }
        }

        return type;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public String getCountry() {
        return country;
    }
}