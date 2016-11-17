package com.stripe.android.model;

import android.support.annotation.IntRange;
import android.support.annotation.Size;
import android.support.annotation.StringDef;
import android.text.TextUtils;

import com.stripe.android.util.DateUtils;
import com.stripe.android.util.StripeTextUtils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * A model object representing a Card in the Android SDK. Note that this is slightly different
 * from the Card model in stripe-java.
 */
public class Card extends com.stripe.model.StripeObject {

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({
            AMERICAN_EXPRESS,
            DISCOVER,
            JCB,
            DINERS_CLUB,
            VISA,
            MASTERCARD,
            UNKNOWN
    })
    public @interface CardType { }
    public static final String AMERICAN_EXPRESS = "American Express";
    public static final String DISCOVER = "Discover";
    public static final String JCB = "JCB";
    public static final String DINERS_CLUB = "Diners Club";
    public static final String VISA = "Visa";
    public static final String MASTERCARD = "MasterCard";
    public static final String UNKNOWN = "Unknown";

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({
            FUNDING_CREDIT,
            FUNDING_DEBIT,
            FUNDING_PREPAID,
            FUNDING_UNKNOWN
    })
    public @interface FundingType { }
    public static final String FUNDING_CREDIT = "credit";
    public static final String FUNDING_DEBIT = "debit";
    public static final String FUNDING_PREPAID = "prepaid";
    public static final String FUNDING_UNKNOWN = "unknown";

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
    @Size(4) private String last4;
    @CardType private String brand;
    @FundingType private String fundingType;
    private String fingerprint;
    private String country;
    private String currency;

    /**
     * Builder class for a {@link Card} model.
     */
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
        private @Size(4) String last4;
        private String fingerprint;
        private @FundingType String fundingType;
        private String country;
        private String currency;

        /**
         * Constructor with most common {@link Card} fields.
         *
         * @param number the credit card number
         * @param expMonth the expiry month, as an integer value between 1 and 12
         * @param expYear the expiry year
         * @param cvc the card CVC number
         */
        public Builder(
                String number,
                @IntRange(from = 1, to = 12) Integer expMonth,
                @IntRange(from = 0) Integer expYear,
                String cvc) {
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

        public Builder fingerprint(String fingerprint) {
            this.fingerprint = fingerprint;
            return this;
        }

        public Builder fundingType(@FundingType String fundingType) {
            this.fundingType = fundingType;
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

        /**
         * Generate a new {@link Card} object based on the arguments held by this Builder.
         * @return
         */
        public Card build() {
            return new Card(this);
        }
    }

    /**
     * Card constructor with all available fields.
     *
     * @param number the credit card number
     * @param expMonth the expiry month
     * @param expYear the expiry year
     * @param cvc the CVC number
     * @param name the card name
     * @param addressLine1 first line of the billing address
     * @param addressLine2 second line of the billing address
     * @param addressCity city of the billing address
     * @param addressState state of the billing address
     * @param addressZip zip code of the billing address
     * @param addressCountry country for the billing address
     * @param last4 last 4 digits of the card
     * @param fingerprint the card fingerprint
     * @param country ISO country code of the card itself
     * @param currency currency used by the card
     */
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
            @Size(4) String last4,
            String fingerprint,
            String country,
            String currency) {
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
        this.brand = StripeTextUtils.isBlank(brand) ? null : brand;
        this.fingerprint = StripeTextUtils.nullIfBlank(fingerprint);
        this.country = StripeTextUtils.nullIfBlank(country);
        this.brand = getBrand();
        this.last4 = getLast4();
        this.currency = StripeTextUtils.nullIfBlank(currency);
    }

    /**
     * Convenience constructor with address and currency.
     *
     * @param number the card number
     * @param expMonth the expiry month
     * @param expYear the expiry year
     * @param cvc the CVC code
     * @param name the cardholder name
     * @param addressLine1 the first line of the billing address
     * @param addressLine2 the second line of the billing address
     * @param addressCity the city of the billing address
     * @param addressState the state of the billing address
     * @param addressZip the zip code of the billing address
     * @param addressCountry the country of the billing address
     * @param currency the currency of the card
     */
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
                currency);
    }

    /**
     * Convenience constructor for a Card object with a minimum number of inputs.
     *
     * @param number the card number
     * @param expMonth the expiry month
     * @param expYear the expiry year
     * @param cvc the CVC code
     */
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
        if (TextUtils.isEmpty(rawNumber)
                || !StripeTextUtils.isWholePositiveNumber(rawNumber)
                || !isValidLuhnNumber(rawNumber)) {
            return false;
        }

        String updatedType = getBrand();
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

        String updatedType = getBrand();
        boolean validLength = ((updatedType == null && cvcValue.length() >= 3 && cvcValue.length() <= 4) ||
                (AMERICAN_EXPRESS.equals(updatedType) && cvcValue.length() == 4) ||
                (!AMERICAN_EXPRESS.equals(updatedType) && cvcValue.length() == 3));

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

    public String getNumber() {
        return number;
    }

    /**
     * Setter for the card number. Note that mutating the number of this card object
     * invalidates the {@link #brand}.
     *
     * @param number the new {@link #number}
     */
    public void setNumber(String number) {
        this.number = number;
        this.brand = null;
    }

    /**
     * @return
     */
    public String getCVC() {
        return cvc;
    }

    public void setCVC(String cvc) {
        this.cvc = cvc;
    }

    /**
     * @return
     */
    public Integer getExpMonth() {
        return expMonth;
    }

    public void setExpMonth(Integer expMonth) {
        this.expMonth = expMonth;
    }

    /**
     * @return
     */
    public Integer getExpYear() {
        return expYear;
    }

    public void setExpYear(Integer expYear) {
        this.expYear = expYear;
    }

    /**
     * @return
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return
     */
    public String getAddressLine1() {
        return addressLine1;
    }

    public void setAddressLine1(String addressLine1) {
        this.addressLine1 = addressLine1;
    }

    /**
     * @return
     */
    public String getAddressLine2() {
        return addressLine2;
    }

    public void setAddressLine2(String addressLine2) {
        this.addressLine2 = addressLine2;
    }

    /**
     * @return
     */
    public String getAddressCity() {
        return addressCity;
    }

    public void setAddressCity(String addressCity) {
        this.addressCity = addressCity;
    }

    /**
     * @return
     */
    public String getAddressZip() {
        return addressZip;
    }

    public void setAddressZip(String addressZip) {
        this.addressZip = addressZip;
    }

    /**
     * @return
     */
    public String getAddressState() {
        return addressState;
    }

    public void setAddressState(String addressState) {
        this.addressState = addressState;
    }

    /**
     * @return
     */
    public String getAddressCountry() {
        return addressCountry;
    }

    public void setAddressCountry(String addressCountry) {
        this.addressCountry = addressCountry;
    }

    /**
     * @return
     */
    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    /**
     * @return
     */
    public String getLast4() {
        if (!StripeTextUtils.isBlank(last4)) {
            return last4;
        }

        if (number != null && number.length() > 4) {
            last4 = number.substring(number.length() - 4, number.length());
            return last4;
        }

        return null;
    }

    /**
     * @return
     */
    public @CardType String getBrand() {
        if (StripeTextUtils.isBlank(brand) && !StripeTextUtils.isBlank(number)) {
            @CardType String evaluatedType = null;
            if (StripeTextUtils.hasAnyPrefix(number, PREFIXES_AMERICAN_EXPRESS)) {
                evaluatedType = AMERICAN_EXPRESS;
            } else if (StripeTextUtils.hasAnyPrefix(number, PREFIXES_DISCOVER)) {
                evaluatedType = DISCOVER;
            } else if (StripeTextUtils.hasAnyPrefix(number, PREFIXES_JCB)) {
                evaluatedType = JCB;
            } else if (StripeTextUtils.hasAnyPrefix(number, PREFIXES_DINERS_CLUB)) {
                evaluatedType = DINERS_CLUB;
            } else if (StripeTextUtils.hasAnyPrefix(number, PREFIXES_VISA)) {
                evaluatedType = VISA;
            } else if (StripeTextUtils.hasAnyPrefix(number, PREFIXES_MASTERCARD)) {
                evaluatedType = MASTERCARD;
            } else {
                evaluatedType = UNKNOWN;
            }
            brand = evaluatedType;
        }

        return brand;
    }

    /**
     * @return
     */
    public String getFingerprint() {
        return fingerprint;
    }

    /**
     * @return
     */
    public String getCountry() {
        return country;
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
        this.fingerprint = StripeTextUtils.nullIfBlank(builder.fingerprint);
        this.fundingType = StripeTextUtils.asFundingType(builder.fundingType);
        this.country = StripeTextUtils.nullIfBlank(builder.country);
        this.brand = getBrand();
        this.last4 = getLast4();
        this.currency = StripeTextUtils.nullIfBlank(builder.currency);
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

}
