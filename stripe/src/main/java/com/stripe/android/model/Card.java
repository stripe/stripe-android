package com.stripe.android.model;

import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.Size;
import android.support.annotation.StringDef;
import android.text.TextUtils;

import com.stripe.android.CardUtils;
import com.stripe.android.R;
import com.stripe.android.StripeNetworkUtils;
import com.stripe.android.StripeTextUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.stripe.android.model.StripeJsonUtils.optCountryCode;
import static com.stripe.android.model.StripeJsonUtils.optCurrency;
import static com.stripe.android.model.StripeJsonUtils.optInteger;
import static com.stripe.android.model.StripeJsonUtils.optString;
import static com.stripe.android.model.StripeJsonUtils.putIntegerIfNotNull;
import static com.stripe.android.model.StripeJsonUtils.putStringIfNotNull;
/**
 * A model object representing a Card in the Android SDK.
 */
public class Card extends StripeJsonModel implements StripePaymentSource {

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({
            AMERICAN_EXPRESS,
            DISCOVER,
            JCB,
            DINERS_CLUB,
            VISA,
            MASTERCARD,
            UNIONPAY,
            UNKNOWN
    })
    public @interface CardBrand { }
    public static final String AMERICAN_EXPRESS = "American Express";
    public static final String DISCOVER = "Discover";
    public static final String JCB = "JCB";
    public static final String DINERS_CLUB = "Diners Club";
    public static final String VISA = "Visa";
    public static final String MASTERCARD = "MasterCard";
    public static final String UNIONPAY = "UnionPay";
    public static final String UNKNOWN = "Unknown";

    public static final int CVC_LENGTH_AMERICAN_EXPRESS = 4;
    public static final int CVC_LENGTH_COMMON = 3;

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

    public static final Map<String , Integer> BRAND_RESOURCE_MAP =
            new HashMap<String , Integer>() {{
                put(Card.AMERICAN_EXPRESS, R.drawable.ic_amex);
                put(Card.DINERS_CLUB, R.drawable.ic_diners);
                put(Card.DISCOVER, R.drawable.ic_discover);
                put(Card.JCB, R.drawable.ic_jcb);
                put(Card.MASTERCARD, R.drawable.ic_mastercard);
                put(Card.VISA, R.drawable.ic_visa);
                put(Card.UNIONPAY, R.drawable.ic_unionpay);
                put(Card.UNKNOWN, R.drawable.ic_unknown);
            }};

    // Based on http://en.wikipedia.org/wiki/Bank_card_number#Issuer_identification_number_.28IIN.29
    public static final String[] PREFIXES_AMERICAN_EXPRESS = {"34", "37"};
    public static final String[] PREFIXES_DISCOVER = {"60", "64", "65"};
    public static final String[] PREFIXES_JCB = {"35"};
    public static final String[] PREFIXES_DINERS_CLUB = {"300", "301", "302", "303", "304",
            "305", "309", "36", "38", "39"};
    public static final String[] PREFIXES_VISA = {"4"};
    public static final String[] PREFIXES_MASTERCARD = {
        "2221", "2222", "2223", "2224", "2225", "2226", "2227", "2228", "2229",
        "223", "224", "225", "226", "227", "228", "229",
        "23", "24", "25", "26",
        "270", "271", "2720",
        "50", "51", "52", "53", "54", "55", "67"
    };
    public static final String[] PREFIXES_UNIONPAY = {"62"};

    public static final int MAX_LENGTH_STANDARD = 16;
    public static final int MAX_LENGTH_AMERICAN_EXPRESS = 15;
    public static final int MAX_LENGTH_DINERS_CLUB = 14;

    static final String VALUE_CARD = "card";

    private static final String FIELD_OBJECT = "object";
    private static final String FIELD_ADDRESS_CITY = "address_city";
    private static final String FIELD_ADDRESS_COUNTRY = "address_country";
    private static final String FIELD_ADDRESS_LINE1 = "address_line1";
    private static final String FIELD_ADDRESS_LINE1_CHECK = "address_line1_check";
    private static final String FIELD_ADDRESS_LINE2 = "address_line2";
    private static final String FIELD_ADDRESS_STATE = "address_state";
    private static final String FIELD_ADDRESS_ZIP = "address_zip";
    private static final String FIELD_ADDRESS_ZIP_CHECK = "address_zip_check";
    private static final String FIELD_BRAND = "brand";
    private static final String FIELD_COUNTRY = "country";
    private static final String FIELD_CURRENCY = "currency";
    private static final String FIELD_CUSTOMER = "customer";
    private static final String FIELD_CVC_CHECK = "cvc_check";
    private static final String FIELD_EXP_MONTH = "exp_month";
    private static final String FIELD_EXP_YEAR = "exp_year";
    private static final String FIELD_FINGERPRINT = "fingerprint";
    private static final String FIELD_FUNDING = "funding";
    private static final String FIELD_NAME = "name";
    private static final String FIELD_LAST4 = "last4";
    private static final String FIELD_ID = "id";
    private static final String FIELD_TOKENIZATION_METHOD = "tokenization_method";

    private String number;
    private String cvc;
    private Integer expMonth;
    private Integer expYear;
    private String name;
    private String addressLine1;
    private String addressLine1Check;
    private String addressLine2;
    private String addressCity;
    private String addressState;
    private String addressZip;
    private String addressZipCheck;
    private String addressCountry;
    @Size(4) private String last4;
    @CardBrand private String brand;
    @FundingType private String funding;
    private String fingerprint;
    private String country;
    private String currency;
    private String customerId;
    private String cvcCheck;
    private String id;
    @NonNull private List<String> loggingTokens = new ArrayList<>();
    @Nullable private String tokenizationMethod;

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
        private String addressLine1Check;
        private String addressLine2;
        private String addressCity;
        private String addressState;
        private String addressZip;
        private String addressZipCheck;
        private String addressCountry;
        private @CardBrand String brand;
        private @FundingType String funding;
        private @Size(4) String last4;
        private String fingerprint;
        private String country;
        private String currency;
        private String customer;
        private String cvcCheck;
        private String id;
        private String tokenizationMethod;

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

        @NonNull
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        @NonNull
        public Builder addressLine1(String address) {
            this.addressLine1 = address;
            return this;
        }

        @NonNull
        public Builder addressLine1Check(String addressLine1Check) {
            this.addressLine1Check = addressLine1Check;
            return this;
        }

        @NonNull
        public Builder addressLine2(String address) {
            this.addressLine2 = address;
            return this;
        }

        @NonNull
        public Builder addressCity(String city) {
            this.addressCity = city;
            return this;
        }

        @NonNull
        public Builder addressState(String state) {
            this.addressState = state;
            return this;
        }

        @NonNull
        public Builder addressZip(String zip) {
            this.addressZip = zip;
            return this;
        }

        @NonNull
        public Builder addressZipCheck(String zipCheck) {
            this.addressZipCheck = zipCheck;
            return this;
        }

        @NonNull
        public Builder addressCountry(String country) {
            this.addressCountry = country;
            return this;
        }

        @NonNull
        public Builder brand(@CardBrand String brand) {
            this.brand = brand;
            return this;
        }

        @NonNull
        public Builder fingerprint(String fingerprint) {
            this.fingerprint = fingerprint;
            return this;
        }

        @NonNull
        public Builder funding(@FundingType String funding) {
            this.funding = funding;
            return this;
        }

        @NonNull
        public Builder country(String country) {
            this.country = country;
            return this;
        }

        @NonNull
        public Builder currency(String currency) {
            this.currency = currency;
            return this;
        }

        @NonNull
        public Builder customer(String customer) {
            this.customer = customer;
            return this;
        }

        @NonNull
        public Builder cvcCheck(String cvcCheck) {
            this.cvcCheck = cvcCheck;
            return this;
        }

        @NonNull
        public Builder last4(String last4) {
            this.last4 = last4;
            return this;
        }

        @NonNull
        public Builder id(String id) {
            this.id = id;
            return this;
        }

        @NonNull
        public Builder tokenizationMethod(@Nullable String tokenizationMethod) {
            this.tokenizationMethod = tokenizationMethod;
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
     * Converts an unchecked String value to a {@link CardBrand} or {@code null}.
     *
     * @param possibleCardType a String that might match a {@link CardBrand} or be empty.
     * @return {@code null} if the input is blank, else the appropriate {@link CardBrand}.
     */
    @Nullable
    @CardBrand
    public static String asCardBrand(@Nullable String possibleCardType) {
        if (possibleCardType == null || TextUtils.isEmpty(possibleCardType.trim())) {
            return null;
        }

        if (Card.AMERICAN_EXPRESS.equalsIgnoreCase(possibleCardType)) {
            return Card.AMERICAN_EXPRESS;
        } else if (Card.MASTERCARD.equalsIgnoreCase(possibleCardType)) {
            return Card.MASTERCARD;
        } else if (Card.DINERS_CLUB.equalsIgnoreCase(possibleCardType)) {
            return Card.DINERS_CLUB;
        } else if (Card.DISCOVER.equalsIgnoreCase(possibleCardType)) {
            return Card.DISCOVER;
        } else if (Card.JCB.equalsIgnoreCase(possibleCardType)) {
            return Card.JCB;
        } else if (Card.VISA.equalsIgnoreCase(possibleCardType)) {
            return Card.VISA;
        } else if (Card.UNIONPAY.equalsIgnoreCase(possibleCardType)) {
            return Card.UNIONPAY;
        } else {
            return Card.UNKNOWN;
        }
    }

    /**
     * Converts an unchecked String value to a {@link FundingType} or {@code null}.
     *
     * @param possibleFundingType a String that might match a {@link FundingType} or be empty
     * @return {@code null} if the input is blank, else the appropriate {@link FundingType}
     */
    @Nullable
    @FundingType
    public static String asFundingType(@Nullable String possibleFundingType) {
        if (possibleFundingType == null || TextUtils.isEmpty(possibleFundingType.trim())) {
            return null;
        }

        if (Card.FUNDING_CREDIT.equalsIgnoreCase(possibleFundingType)) {
            return Card.FUNDING_CREDIT;
        } else if (Card.FUNDING_DEBIT.equalsIgnoreCase(possibleFundingType)) {
            return Card.FUNDING_DEBIT;
        } else if (Card.FUNDING_PREPAID.equalsIgnoreCase(possibleFundingType)) {
            return Card.FUNDING_PREPAID;
        } else {
            return Card.FUNDING_UNKNOWN;
        }
    }

    /**
     * Create a Card object from a raw JSON string.
     *
     * @param jsonString the JSON string representing the potential Card
     * @return A Card if one can be made from the JSON, or {@code null} if one cannot be made
     * or the JSON is invalid.
     */
    @Nullable
    public static Card fromString(String jsonString) {
        try {
            JSONObject jsonObject = new JSONObject(jsonString);
            return fromJson(jsonObject);
        } catch (JSONException ignored) {
            return null;
        }
    }

    @Nullable
    public static Card fromJson(JSONObject jsonObject) {
        if (jsonObject == null || !VALUE_CARD.equals(jsonObject.optString(FIELD_OBJECT))) {
            return null;
        }

        Integer expMonth = optInteger(jsonObject, FIELD_EXP_MONTH);
        Integer expYear = optInteger(jsonObject, FIELD_EXP_YEAR);

        // It's okay for the month to be missing, but not for it to be outside 1-12.
        // We treat an invalid month the same way we would an invalid brand, by reading it as
        // null.
        if (expMonth != null && (expMonth < 1 || expMonth > 12)) {
            expMonth = null;
        }

        if (expYear != null && expYear < 0) {
            expYear = null;
        }

        // Note that we'll never get the CVC or card number in JSON, so those values are null
        Builder builder = new Builder(null, expMonth, expYear, null);
        builder.addressCity(optString(jsonObject, FIELD_ADDRESS_CITY));
        builder.addressLine1(optString(jsonObject, FIELD_ADDRESS_LINE1));
        builder.addressLine1Check(optString(jsonObject, FIELD_ADDRESS_LINE1_CHECK));
        builder.addressLine2(optString(jsonObject, FIELD_ADDRESS_LINE2));
        builder.addressCountry(optString(jsonObject, FIELD_ADDRESS_COUNTRY));
        builder.addressState(optString(jsonObject, FIELD_ADDRESS_STATE));
        builder.addressZip(optString(jsonObject, FIELD_ADDRESS_ZIP));
        builder.addressZipCheck(optString(jsonObject, FIELD_ADDRESS_ZIP_CHECK));
        builder.brand(asCardBrand(optString(jsonObject, FIELD_BRAND)));
        builder.country(optCountryCode(jsonObject, FIELD_COUNTRY));
        builder.customer(optString(jsonObject, FIELD_CUSTOMER));
        builder.currency(optCurrency(jsonObject, FIELD_CURRENCY));
        builder.cvcCheck(optString(jsonObject, FIELD_CVC_CHECK));
        builder.funding(asFundingType(optString(jsonObject, FIELD_FUNDING)));
        builder.fingerprint(optString(jsonObject, FIELD_FINGERPRINT));
        builder.id(optString(jsonObject, FIELD_ID));
        builder.last4(optString(jsonObject, FIELD_LAST4));
        builder.name(optString(jsonObject, FIELD_NAME));
        builder.tokenizationMethod(optString(jsonObject, FIELD_TOKENIZATION_METHOD));

        return builder.build();
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
     * @param id the cardId
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
            String currency,
            String id) {
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
        this.brand = asCardBrand(brand) == null ? getBrand() : brand;
        this.last4 = StripeTextUtils.nullIfBlank(last4) == null ? getLast4() : last4;
        this.fingerprint = StripeTextUtils.nullIfBlank(fingerprint);
        this.funding = asFundingType(funding);
        this.country = StripeTextUtils.nullIfBlank(country);
        this.currency = StripeTextUtils.nullIfBlank(currency);
        this.id = StripeTextUtils.nullIfBlank(id);
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
                currency,
                null);
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
                null,
                null);
    }

    /**
     * Checks whether {@code this} represents a valid card.
     *
     * @return {@code true} if valid, {@code false} otherwise.
     */
    public boolean validateCard() {
        return validateCard(Calendar.getInstance());
    }

    /**
     * Checks whether or not the {@link #number} field is valid.
     *
     * @return {@code true} if valid, {@code false} otherwise.
     */
    public boolean validateNumber() {
        return CardUtils.isValidCardNumber(number);
    }

    /**
     * Checks whether or not the {@link #expMonth} and {@link #expYear} fields represent a valid
     * expiry date.
     *
     * @return {@code true} if valid, {@code false} otherwise
     */
    public boolean validateExpiryDate() {
        return validateExpiryDate(Calendar.getInstance());
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

        return ModelUtils.isWholePositiveNumber(cvcValue) && validLength;
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
    boolean validateExpYear(Calendar now) {
        return expYear != null && !ModelUtils.hasYearPassed(expYear, now);
    }

    /**
     * @return the {@link #number} of this card
     */
    public String getNumber() {
        return number;
    }

    /**
     * @return the {@link List} of logging tokens associated with this {@link Card} object
     */
    @NonNull
    public List<String> getLoggingTokens() {
        return loggingTokens;
    }

    /**
     * Add a logging token to this {@link Card} object.
     *
     * @param loggingToken a token to be logged with this card
     * @return {@code this}, for chaining purposes
     */
    @NonNull
    public Card addLoggingToken(@NonNull String loggingToken) {
        loggingTokens.add(loggingToken);
        return this;
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
            brand = CardUtils.getPossibleCardType(number);
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

    /**
     * @return the {@link #id} of this card
     */
    @Override
    public String getId() {
        return id;
    }

    /**
     * @return If address_line1 was provided, results of the check:
     * pass, fail, unavailable, or unchecked.
     */
    @Nullable
    public String getAddressLine1Check() {
        return addressLine1Check;
    }

    /**
     * @return If address_zip was provided, results of the check:
     * pass, fail, unavailable, or unchecked.
     */
    @Nullable
    public String getAddressZipCheck() {
        return addressZipCheck;
    }

    /**
     * @return The ID of the customer that this card belongs to.
     */
    @Nullable
    public String getCustomerId() {
        return customerId;
    }

    /**
     * @return If a CVC was provided, results of the check:
     * pass, fail, unavailable, or unchecked.
     */
    @Nullable
    public String getCvcCheck() {
        return cvcCheck;
    }

    @NonNull
    @Override
    public JSONObject toJson() {
        JSONObject object = new JSONObject();
        putStringIfNotNull(object, FIELD_NAME, name);
        putStringIfNotNull(object, FIELD_ADDRESS_CITY, addressCity);
        putStringIfNotNull(object, FIELD_ADDRESS_COUNTRY, addressCountry);
        putStringIfNotNull(object, FIELD_ADDRESS_LINE1, addressLine1);
        putStringIfNotNull(object, FIELD_ADDRESS_LINE1_CHECK, addressLine1Check);
        putStringIfNotNull(object, FIELD_ADDRESS_LINE2, addressLine2);
        putStringIfNotNull(object, FIELD_ADDRESS_STATE, addressState);
        putStringIfNotNull(object, FIELD_ADDRESS_ZIP, addressZip);
        putStringIfNotNull(object, FIELD_ADDRESS_ZIP_CHECK, addressZipCheck);
        putStringIfNotNull(object, FIELD_BRAND, brand);
        putStringIfNotNull(object, FIELD_CURRENCY, currency);
        putStringIfNotNull(object, FIELD_COUNTRY, country);
        putStringIfNotNull(object, FIELD_CUSTOMER, customerId);
        putIntegerIfNotNull(object, FIELD_EXP_MONTH, expMonth);
        putIntegerIfNotNull(object, FIELD_EXP_YEAR, expYear);
        putStringIfNotNull(object, FIELD_FINGERPRINT, fingerprint);
        putStringIfNotNull(object, FIELD_FUNDING, funding);
        putStringIfNotNull(object, FIELD_CVC_CHECK, cvcCheck);
        putStringIfNotNull(object, FIELD_LAST4, last4);
        putStringIfNotNull(object, FIELD_ID, id);
        putStringIfNotNull(object, FIELD_TOKENIZATION_METHOD, tokenizationMethod);
        putStringIfNotNull(object, FIELD_OBJECT, VALUE_CARD);
        return object;
    }

    @NonNull
    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put(FIELD_NAME, name);
        map.put(FIELD_ADDRESS_CITY, addressCity);
        map.put(FIELD_ADDRESS_COUNTRY, addressCountry);
        map.put(FIELD_ADDRESS_LINE1, addressLine1);
        map.put(FIELD_ADDRESS_LINE1_CHECK, addressLine1Check);
        map.put(FIELD_ADDRESS_LINE2, addressLine2);
        map.put(FIELD_ADDRESS_STATE, addressState);
        map.put(FIELD_ADDRESS_ZIP, addressZip);
        map.put(FIELD_ADDRESS_ZIP_CHECK, addressZipCheck);
        map.put(FIELD_BRAND, brand);
        map.put(FIELD_CURRENCY, currency);
        map.put(FIELD_COUNTRY, country);
        map.put(FIELD_CUSTOMER, customerId);
        map.put(FIELD_CVC_CHECK, cvcCheck);
        map.put(FIELD_EXP_MONTH, expMonth);
        map.put(FIELD_EXP_YEAR, expYear);
        map.put(FIELD_FINGERPRINT, fingerprint);
        map.put(FIELD_FUNDING, funding);
        map.put(FIELD_ID, id);
        map.put(FIELD_LAST4, last4);
        map.put(FIELD_TOKENIZATION_METHOD, tokenizationMethod);
        map.put(FIELD_OBJECT, VALUE_CARD);
        StripeNetworkUtils.removeNullAndEmptyParams(map);
        return map;
    }

    @Nullable
    String getTokenizationMethod() {
        return this.tokenizationMethod;
    }

    boolean validateCard(Calendar now) {
        if (cvc == null) {
            return validateNumber() && validateExpiryDate(now);
        } else {
            return validateNumber() && validateExpiryDate(now) && validateCVC();
        }
    }

    boolean validateExpiryDate(Calendar now) {
        if (!validateExpMonth()) {
            return false;
        }
        if (!validateExpYear(now)) {
            return false;
        }
        return !ModelUtils.hasMonthPassed(expYear, expMonth, now);
    }

    private Card(Builder builder) {
        this.number = StripeTextUtils.nullIfBlank(normalizeCardNumber(builder.number));
        this.expMonth = builder.expMonth;
        this.expYear = builder.expYear;
        this.cvc = StripeTextUtils.nullIfBlank(builder.cvc);
        this.name = StripeTextUtils.nullIfBlank(builder.name);
        this.addressLine1 = StripeTextUtils.nullIfBlank(builder.addressLine1);
        this.addressLine1Check = StripeTextUtils.nullIfBlank(builder.addressLine1Check);
        this.addressLine2 = StripeTextUtils.nullIfBlank(builder.addressLine2);
        this.addressCity = StripeTextUtils.nullIfBlank(builder.addressCity);
        this.addressState = StripeTextUtils.nullIfBlank(builder.addressState);
        this.addressZip = StripeTextUtils.nullIfBlank(builder.addressZip);
        this.addressZipCheck = StripeTextUtils.nullIfBlank(builder.addressZipCheck);
        this.addressCountry = StripeTextUtils.nullIfBlank(builder.addressCountry);
        this.last4 = StripeTextUtils.nullIfBlank(builder.last4) == null
                ? getLast4()
                : builder.last4;
        this.brand = asCardBrand(builder.brand) == null
                ? getBrand()
                : builder.brand;
        this.fingerprint = StripeTextUtils.nullIfBlank(builder.fingerprint);
        this.funding = asFundingType(builder.funding);
        this.country = StripeTextUtils.nullIfBlank(builder.country);
        this.currency = StripeTextUtils.nullIfBlank(builder.currency);
        this.customerId = StripeTextUtils.nullIfBlank(builder.customer);
        this.cvcCheck = StripeTextUtils.nullIfBlank(builder.cvcCheck);
        this.id = StripeTextUtils.nullIfBlank(builder.id);
        this.tokenizationMethod = StripeTextUtils.nullIfBlank(builder.tokenizationMethod);
    }

    private String normalizeCardNumber(String number) {
        if (number == null) {
            return null;
        }
        return number.trim().replaceAll("\\s+|-", "");
    }
}
