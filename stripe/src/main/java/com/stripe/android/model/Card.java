package com.stripe.android.model;

import android.support.annotation.IntRange;
import android.support.annotation.Nullable;
import android.support.annotation.Size;
import android.support.annotation.StringDef;

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
    public @interface CardBrand { }
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
    @CardBrand private String brand;
    @FundingType private String funding;
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
        private @CardBrand String brand;
        private @FundingType String funding;
        private @Size(4) String last4;
        private String fingerprint;
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

        public Builder brand(@CardBrand String brand) {
            this.brand = brand;
            return this;
        }

        public Builder fingerprint(String fingerprint) {
            this.fingerprint = fingerprint;
            return this;
        }

        public Builder funding(@FundingType String funding) {
            this.funding = funding;
            return this;
        }

        public Builder country(String country) {
            this.country = country;
            return this;
        }

        public Builder currency(String currency) {
            this.currency = currency;
            return this;
        }

        public Builder last4(String last4) {
            this.last4 = last4;
            return this;
        }

        /**
         * Generate a new {@link Card} object based on the arguments held by this Builder.
         *
         * @return the newly created {@link Card} object
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
     * @param brand brand of this card
     * @param last4 last 4 digits of the card
     * @param fingerprint the card fingerprint
     * @param funding the funding type of the card
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
            String brand,
            @Size(4) String last4,
            String fingerprint,
            String funding,
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
        this.brand = StripeTextUtils.asCardBrand(brand) == null ? getBrand() : brand;
        this.last4 = StripeTextUtils.nullIfBlank(last4) == null ? getLast4() : last4;
        this.fingerprint = StripeTextUtils.nullIfBlank(fingerprint);
        this.funding = StripeTextUtils.asFundingType(funding);
        this.country = StripeTextUtils.nullIfBlank(country);
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

    /**
     * @return the {@link #number} of this card
     */
    public String getNumber() {
        return number;
    }

    /**
     * Setter for the card number. Note that mutating the number of this card object
     * invalidates the {@link #brand} and {@link #last4}.
     *
     * @param number the new {@link #number}
     */
    @Deprecated
    public void setNumber(String number) {
        this.number = number;
        this.brand = null;
        this.last4 = null;
    }

    /**
     * @return the {@link #cvc} for this card
     */
    public String getCVC() {
        return cvc;
    }

    /**
     * @param cvc the new {@link #cvc} code for this card
     */
    @Deprecated
    public void setCVC(String cvc) {
        this.cvc = cvc;
    }

    /**
     * @return the {@link #expMonth} for this card
     */
    @Nullable
    @IntRange(from = 1, to = 12)
    public Integer getExpMonth() {
        return expMonth;
    }

    /**
     * @param expMonth sets the {@link #expMonth} for this card
     */
    @Deprecated
    public void setExpMonth(@Nullable @IntRange(from = 1, to = 12) Integer expMonth) {
        this.expMonth = expMonth;
    }

    /**
     * @return the {@link #expYear} for this card
     */
    public Integer getExpYear() {
        return expYear;
    }

    /**
     * @param expYear sets the {@link #expYear} for this card
     */
    @Deprecated
    public void setExpYear(Integer expYear) {
        this.expYear = expYear;
    }

    /**
     * @return the cardholder {@link #name} for this card
     */
    public String getName() {
        return name;
    }

    /**
     * @param name sets the cardholder {@link #name} for this card
     */
    @Deprecated
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the {@link #addressLine1} of this card
     */
    public String getAddressLine1() {
        return addressLine1;
    }

    /**
     * @param addressLine1 sets the {@link #addressLine1} for this card
     */
    @Deprecated
    public void setAddressLine1(String addressLine1) {
        this.addressLine1 = addressLine1;
    }

    /**
     * @return the {@link #addressLine2} of this card
     */
    public String getAddressLine2() {
        return addressLine2;
    }

    /**
     * @param addressLine2 sets the {@link #addressLine2} for this card
     */
    @Deprecated
    public void setAddressLine2(String addressLine2) {
        this.addressLine2 = addressLine2;
    }

    /**
     * @return the {@link #addressCity} for this card
     */
    public String getAddressCity() {
        return addressCity;
    }

    /**
     * @param addressCity sets the {@link #addressCity} for this card
     */
    @Deprecated
    public void setAddressCity(String addressCity) {
        this.addressCity = addressCity;
    }

    /**
     * @return the {@link #addressZip} of this card
     */
    public String getAddressZip() {
        return addressZip;
    }

    /**
     * @param addressZip sets the {@link #addressZip} for this card
     */
    @Deprecated
    public void setAddressZip(String addressZip) {
        this.addressZip = addressZip;
    }

    /**
     * @return the {@link #addressState} of this card
     */
    public String getAddressState() {
        return addressState;
    }

    /**
     * @param addressState sets the {@link #addressState} for this card
     */
    @Deprecated
    public void setAddressState(String addressState) {
        this.addressState = addressState;
    }

    /**
     * @return the {@link #addressCountry} of this card
     */
    public String getAddressCountry() {
        return addressCountry;
    }

    /**
     * @param addressCountry sets the {@link #addressCountry} for this card
     */
    @Deprecated
    public void setAddressCountry(String addressCountry) {
        this.addressCountry = addressCountry;
    }

    /**
     * @return the {@link #currency} of this card. Only supported for Managed accounts.
     */
    public String getCurrency() {
        return currency;
    }

    /**
     * @param currency sets the {@link #currency} of this card. Only supported for Managed accounts.
     */
    @Deprecated
    public void setCurrency(String currency) {
        this.currency = currency;
    }

    /**
     * @return the {@link #last4} digits of this card. Sets the value based on the {@link #number}
     * if it has not already been set.
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
     * Gets the {@link #brand} of this card, changed from the "type" field. Use {@link #getBrand()}
     * instead.
     *
     * @return the {@link #brand} of this card
     */
    @Deprecated
    @CardBrand
    public String getType() {
        return getBrand();
    }

    /**
     * Gets the {@link #brand} of this card. Updates the value if none has yet been set, or
     * if the {@link #number} has been changed.
     *
     * @return the {@link #brand} of this card
     */
    @CardBrand
    public String getBrand() {
        if (StripeTextUtils.isBlank(brand) && !StripeTextUtils.isBlank(number)) {
            @CardBrand String evaluatedType;
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
     * @return the {@link #fingerprint} of this card
     */
    public String getFingerprint() {
        return fingerprint;
    }

    /**
     * @return the {@link #funding} type of this card
     */
    @Nullable
    @FundingType
    public String getFunding() {
        return funding;
    }

    /**
     * @return the {@link #country} of this card
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
        this.last4 = StripeTextUtils.nullIfBlank(builder.last4) == null
                ? getLast4()
                : builder.last4;
        this.brand = StripeTextUtils.asCardBrand(builder.brand) == null
                ? getBrand()
                : builder.brand;
        this.fingerprint = StripeTextUtils.nullIfBlank(builder.fingerprint);
        this.funding = StripeTextUtils.asFundingType(builder.funding);
        this.country = StripeTextUtils.nullIfBlank(builder.country);
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
