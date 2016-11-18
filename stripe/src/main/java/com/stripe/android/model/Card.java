package com.stripe.android.model;

import com.stripe.android.util.DateUtils;
import com.stripe.android.util.StripeTextUtils;

/**
 * A model object representing a Card in the Android SDK. Note that this is slightly different
 * from the Card model in stripe-java.
 */
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
    public static final String[] PREFIXES_MASTERCARD = {
        "2221", "2222", "2223", "2224", "2225", "2226", "2227", "2228", "2229",
        "223", "224", "225", "226", "227", "228", "229",
        "23", "24", "25", "26",
        "270", "271", "2720",
        "50", "51", "52", "53", "54", "55"
    };

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
        this.number = StripeTextUtils.nullIfBlank(normalizeCardNumber(builder.number));
        this.expMonth = builder.expMonth;
        this.expYear = builder.expYear;
        this.cvc = StripeTextUtils.nullIfBlank(builder.cvc);
        this.name = StripeTextUtils.nullIfBlank(builder.name);
        this.addressLine1 = StripeTextUtils.nullIfBlank(builder.addressLine1);
        this.addressLine2 = StripeTextUtils.nullIfBlank(builder.addressLine2);
        this.addressCity = StripeTextUtils.nullIfBlank(builder.addressCity);
        this.addressState = StripeTextUtils.nullIfBlank(builder.addressState);
        this.addressZip = StripeTextUtils.nullIfBlank(builder.addressZip);
        this.addressCountry = StripeTextUtils.nullIfBlank(builder.addressCountry);
        this.last4 = StripeTextUtils.nullIfBlank(builder.last4);
        this.type = StripeTextUtils.nullIfBlank(builder.type);
        this.fingerprint = StripeTextUtils.nullIfBlank(builder.fingerprint);
        this.country = StripeTextUtils.nullIfBlank(builder.country);
        this.type = getType();
        this.last4 = getLast4();
        this.currency = StripeTextUtils.nullIfBlank(builder.currency);
    }

    public Card(String number, Integer expMonth, Integer expYear, String cvc, String name, String addressLine1, String addressLine2, String addressCity,
                String addressState, String addressZip, String addressCountry, String last4, String type, String fingerprint, String country) {
        this.number = StripeTextUtils.nullIfBlank(normalizeCardNumber(number));
        this.expMonth = expMonth;
        this.expYear = expYear;
        this.cvc = StripeTextUtils.nullIfBlank(cvc);
        this.name = StripeTextUtils.nullIfBlank(name);
        this.addressLine1 = StripeTextUtils.nullIfBlank(addressLine1);
        this.addressLine2 = StripeTextUtils.nullIfBlank(addressLine2);
        this.addressCity = StripeTextUtils.nullIfBlank(addressCity);
        this.addressState = StripeTextUtils.nullIfBlank(addressState);
        this.addressZip = StripeTextUtils.nullIfBlank(addressZip);
        this.addressCountry = StripeTextUtils.nullIfBlank(addressCountry);
        this.last4 = StripeTextUtils.nullIfBlank(last4);
        this.type = StripeTextUtils.nullIfBlank(type);
        this.fingerprint = StripeTextUtils.nullIfBlank(fingerprint);
        this.country = StripeTextUtils.nullIfBlank(country);
        this.type = getType();
        this.last4 = getLast4();
        this.currency = StripeTextUtils.nullIfBlank(currency);
    }

    public Card(
            String number,
            Integer expMonth,
            Integer expYear,
            String cvc,
            String name,
            String addressLine1,
            String addressLine2,
            String addressCity,
            String addressState,
            String addressZip,
            String addressCountry,
            String currency) {
        this(
                number,
                expMonth,
                expYear,
                cvc,
                name,
                addressLine1,
                addressLine2,
                addressCity,
                addressState,
                addressZip,
                addressCountry,
                null,
                null,
                null,
                null);
    }

    public Card(
            String number,
            Integer expMonth,
            Integer expYear,
            String cvc) {
        this(
                number,
                expMonth,
                expYear,
                cvc,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
    }

    /**
     * Checks whether {@code this} represents a valid card.
     *
     * @return {@code true} if valid, {@code false} otherwise.
     */
    public boolean validateCard() {
        if (cvc == null) {
            return validateNumber() && validateExpiryDate();
        } else {
            return validateNumber() && validateExpiryDate() && validateCVC();
        }
    }

    /**
     * Checks whether or not the {@link #number} field is valid.
     *
     * @return {@code true} if valid, {@code false} otherwise.
     */
    public boolean validateNumber() {
        if (StripeTextUtils.isBlank(number)) {
            return false;
        }

        String rawNumber = number.trim().replaceAll("\\s+|-", "");
        if (StripeTextUtils.isBlank(rawNumber)
                || !StripeTextUtils.isWholePositiveNumber(rawNumber)
                || !isValidLuhnNumber(rawNumber)) {
            return false;
        }

        String updatedType = getType();
        if (AMERICAN_EXPRESS.equals(updatedType)) {
            return rawNumber.length() == MAX_LENGTH_AMERICAN_EXPRESS;
        } else if (DINERS_CLUB.equals(updatedType)) {
            return rawNumber.length() == MAX_LENGTH_DINERS_CLUB;
        } else {
            return rawNumber.length() == MAX_LENGTH_STANDARD;
        }
    }

    /**
     * Checks whether or not the {@link #expMonth} and {@link #expYear} fields represent a valid
     * expiry date.
     *
     * @return {@code true} if valid, {@code false} otherwise
     */
    public boolean validateExpiryDate() {
        if (!validateExpMonth()) {
            return false;
        }
        if (!validateExpYear()) {
            return false;
        }
        return !DateUtils.hasMonthPassed(expYear, expMonth);
    }

    /**
     * Checks whether or not the {@link #cvc} field is valid.
     *
     * @return {@code true} if valid, {@code false} otherwise
     */
    public boolean validateCVC() {
        if (StripeTextUtils.isBlank(cvc)) {
            return false;
        }
        String cvcValue = cvc.trim();

        String updatedType = getType();
        boolean validLength =
                (updatedType == null && cvcValue.length() >= 3 && cvcValue.length() <= 4)
                || (AMERICAN_EXPRESS.equals(updatedType) && cvcValue.length() == 4)
                || cvcValue.length() == 3;

        return StripeTextUtils.isWholePositiveNumber(cvcValue) && validLength;
    }

    /**
     * Checks whether or not the {@link #expMonth} field is valid.
     *
     * @return {@code true} if valid, {@code false} otherwise.
     */
    public boolean validateExpMonth() {
        return expMonth != null && expMonth >= 1 && expMonth <= 12;
    }

    /**
     * Checks whether or not the {@link #expYear} field is valid.
     *
     * @return {@code true} if valid, {@code false} otherwise.
     */
    public boolean validateExpYear() {
        return expYear != null && !DateUtils.hasYearPassed(expYear);
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
        if (!StripeTextUtils.isBlank(last4)) {
            return last4;
        }
        if (number != null && number.length() > 4) {
            return number.substring(number.length() - 4, number.length());
        }
        return null;
    }

    public String getType() {
        if (StripeTextUtils.isBlank(type) && !StripeTextUtils.isBlank(number)) {
            if (StripeTextUtils.hasAnyPrefix(number, PREFIXES_AMERICAN_EXPRESS)) {
                return AMERICAN_EXPRESS;
            } else if (StripeTextUtils.hasAnyPrefix(number, PREFIXES_DISCOVER)) {
                return DISCOVER;
            } else if (StripeTextUtils.hasAnyPrefix(number, PREFIXES_JCB)) {
                return JCB;
            } else if (StripeTextUtils.hasAnyPrefix(number, PREFIXES_DINERS_CLUB)) {
                return DINERS_CLUB;
            } else if (StripeTextUtils.hasAnyPrefix(number, PREFIXES_VISA)) {
                return VISA;
            } else if (StripeTextUtils.hasAnyPrefix(number, PREFIXES_MASTERCARD)) {
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
