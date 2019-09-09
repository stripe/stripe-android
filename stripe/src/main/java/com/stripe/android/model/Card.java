package com.stripe.android.model;

import androidx.annotation.DrawableRes;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Size;
import androidx.annotation.StringDef;

import com.stripe.android.CardUtils;
import com.stripe.android.ObjectBuilder;
import com.stripe.android.R;
import com.stripe.android.StripeTextUtils;
import com.stripe.android.utils.ObjectUtils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import static com.stripe.android.model.StripeJsonUtils.optCountryCode;
import static com.stripe.android.model.StripeJsonUtils.optCurrency;
import static com.stripe.android.model.StripeJsonUtils.optHash;
import static com.stripe.android.model.StripeJsonUtils.optInteger;
import static com.stripe.android.model.StripeJsonUtils.optString;

/**
 * A model object representing a Card in the Android SDK.
 */
public final class Card extends StripeModel implements StripePaymentSource {

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({
            CardBrand.AMERICAN_EXPRESS,
            CardBrand.DISCOVER,
            CardBrand.JCB,
            CardBrand.DINERS_CLUB,
            CardBrand.VISA,
            CardBrand.MASTERCARD,
            CardBrand.UNIONPAY,
            CardBrand.UNKNOWN
    })
    public @interface CardBrand {
        String AMERICAN_EXPRESS = "American Express";
        String DISCOVER = "Discover";
        String JCB = "JCB";
        String DINERS_CLUB = "Diners Club";
        String VISA = "Visa";
        String MASTERCARD = "MasterCard";
        String UNIONPAY = "UnionPay";
        String UNKNOWN = "Unknown";
    }

    public static final int CVC_LENGTH_AMERICAN_EXPRESS = 4;
    public static final int CVC_LENGTH_COMMON = 3;

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({
            FundingType.CREDIT,
            FundingType.DEBIT,
            FundingType.PREPAID,
            FundingType.UNKNOWN
    })
    public @interface FundingType {
        String CREDIT = "credit";
        String DEBIT = "debit";
        String PREPAID = "prepaid";
        String UNKNOWN = "unknown";
    }

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
    private static final String FIELD_METADATA = "metadata";
    private static final String FIELD_NAME = "name";
    private static final String FIELD_LAST4 = "last4";
    private static final String FIELD_ID = "id";
    private static final String FIELD_TOKENIZATION_METHOD = "tokenization_method";

    private static final Map<String , Integer> BRAND_RESOURCE_MAP =
            new HashMap<String , Integer>() {{
                put(CardBrand.AMERICAN_EXPRESS, R.drawable.ic_amex);
                put(CardBrand.DINERS_CLUB, R.drawable.ic_diners);
                put(CardBrand.DISCOVER, R.drawable.ic_discover);
                put(CardBrand.JCB, R.drawable.ic_jcb);
                put(CardBrand.MASTERCARD, R.drawable.ic_mastercard);
                put(CardBrand.VISA, R.drawable.ic_visa);
                put(CardBrand.UNIONPAY, R.drawable.ic_unionpay);
                put(CardBrand.UNKNOWN, R.drawable.ic_unknown);
            }};

    @Nullable private final String number;
    @Nullable private final String cvc;
    @Nullable private final Integer expMonth;
    @Nullable private final Integer expYear;
    @Nullable private final String name;
    @Nullable private final String addressLine1;
    @Nullable private final String addressLine1Check;
    @Nullable private final String addressLine2;
    @Nullable private final String addressCity;
    @Nullable private final String addressState;
    @Nullable private final String addressZip;
    @Nullable private final String addressZipCheck;
    @Nullable private final String addressCountry;
    @Nullable @Size(4) private final String last4;
    @Nullable @CardBrand private final String brand;
    @Nullable @FundingType private final String funding;
    @Nullable private final String fingerprint;
    @Nullable private final String country;
    @Nullable private final String currency;
    @Nullable private final String customerId;
    @Nullable private final String cvcCheck;
    @Nullable private final String id;
    @NonNull private final List<String> loggingTokens = new ArrayList<>();
    @Nullable private final String tokenizationMethod;
    @Nullable private final Map<String, String> metadata;

    /**
     * Converts an unchecked String value to a {@link CardBrand} or {@code null}.
     *
     * @param possibleCardType a String that might match a {@link CardBrand} or be empty.
     * @return {@code null} if the input is blank, else the appropriate {@link CardBrand}.
     */
    @Nullable
    @CardBrand
    public static String asCardBrand(@Nullable String possibleCardType) {
        if (possibleCardType == null || StripeTextUtils.isEmpty(possibleCardType.trim())) {
            return null;
        }

        if (CardBrand.AMERICAN_EXPRESS.equalsIgnoreCase(possibleCardType)) {
            return CardBrand.AMERICAN_EXPRESS;
        } else if (CardBrand.MASTERCARD.equalsIgnoreCase(possibleCardType)) {
            return CardBrand.MASTERCARD;
        } else if (CardBrand.DINERS_CLUB.equalsIgnoreCase(possibleCardType)) {
            return CardBrand.DINERS_CLUB;
        } else if (CardBrand.DISCOVER.equalsIgnoreCase(possibleCardType)) {
            return CardBrand.DISCOVER;
        } else if (CardBrand.JCB.equalsIgnoreCase(possibleCardType)) {
            return CardBrand.JCB;
        } else if (CardBrand.VISA.equalsIgnoreCase(possibleCardType)) {
            return CardBrand.VISA;
        } else if (CardBrand.UNIONPAY.equalsIgnoreCase(possibleCardType)) {
            return CardBrand.UNIONPAY;
        } else {
            return CardBrand.UNKNOWN;
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
        if (possibleFundingType == null || StripeTextUtils.isEmpty(possibleFundingType.trim())) {
            return null;
        }

        if (FundingType.CREDIT.equalsIgnoreCase(possibleFundingType)) {
            return FundingType.CREDIT;
        } else if (FundingType.DEBIT.equalsIgnoreCase(possibleFundingType)) {
            return FundingType.DEBIT;
        } else if (FundingType.PREPAID.equalsIgnoreCase(possibleFundingType)) {
            return FundingType.PREPAID;
        } else {
            return FundingType.UNKNOWN;
        }
    }

    @DrawableRes
    public static int getBrandIcon(@Nullable String brand) {
        final Integer brandIcon = BRAND_RESOURCE_MAP.get(brand);
        return brandIcon != null ? brandIcon : R.drawable.ic_unknown;
    }

    /**
     * Create a Card object from a raw JSON string.
     *
     * @param jsonString the JSON string representing the potential Card
     * @return A Card if one can be made from the JSON, or {@code null} if one cannot be made
     * or the JSON is invalid.
     */
    @Nullable
    public static Card fromString(@NonNull String jsonString) {
        try {
            JSONObject jsonObject = new JSONObject(jsonString);
            return fromJson(jsonObject);
        } catch (JSONException ignored) {
            return null;
        }
    }

    @Nullable
    public static Card fromJson(@Nullable JSONObject jsonObject) {
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
        return new Builder(null, expMonth, expYear, null)
                .addressCity(optString(jsonObject, FIELD_ADDRESS_CITY))
                .addressLine1(optString(jsonObject, FIELD_ADDRESS_LINE1))
                .addressLine1Check(optString(jsonObject, FIELD_ADDRESS_LINE1_CHECK))
                .addressLine2(optString(jsonObject, FIELD_ADDRESS_LINE2))
                .addressCountry(optString(jsonObject, FIELD_ADDRESS_COUNTRY))
                .addressState(optString(jsonObject, FIELD_ADDRESS_STATE))
                .addressZip(optString(jsonObject, FIELD_ADDRESS_ZIP))
                .addressZipCheck(optString(jsonObject, FIELD_ADDRESS_ZIP_CHECK))
                .brand(asCardBrand(optString(jsonObject, FIELD_BRAND)))
                .country(optCountryCode(jsonObject, FIELD_COUNTRY))
                .customer(optString(jsonObject, FIELD_CUSTOMER))
                .currency(optCurrency(jsonObject, FIELD_CURRENCY))
                .cvcCheck(optString(jsonObject, FIELD_CVC_CHECK))
                .funding(asFundingType(optString(jsonObject, FIELD_FUNDING)))
                .fingerprint(optString(jsonObject, FIELD_FINGERPRINT))
                .id(optString(jsonObject, FIELD_ID))
                .last4(optString(jsonObject, FIELD_LAST4))
                .name(optString(jsonObject, FIELD_NAME))
                .tokenizationMethod(optString(jsonObject, FIELD_TOKENIZATION_METHOD))
                .metadata(optHash(jsonObject, FIELD_METADATA))
                .build();
    }

    /**
     * Convenience constructor for a Card object with a minimum number of inputs.
     *
     * @param number the card number
     * @param expMonth the expiry month
     * @param expYear the expiry year
     * @param cvc the CVC code
     */
    @NonNull
    public static Card create(
            String number,
            Integer expMonth,
            Integer expYear,
            String cvc) {
        return new Builder(number, expMonth, expYear, cvc)
                .build();
    }

    @NonNull
    public PaymentMethodCreateParams.Card toPaymentMethodParamsCard() {
        return new PaymentMethodCreateParams.Card.Builder()
                .setNumber(number)
                .setCvc(cvc)
                .setExpiryMonth(expMonth)
                .setExpiryYear(expYear)
                .build();
    }

    /**
     * @return a {@link Card.Builder} populated with the fields of this {@link Card} instance
     */
    @NonNull
    public Builder toBuilder() {
        return new Builder(number, expMonth, expYear, cvc)
                .name(name)
                .addressLine1(addressLine1)
                .addressLine1Check(addressLine1Check)
                .addressLine2(addressLine2)
                .addressCity(addressCity)
                .addressState(addressState)
                .addressZip(addressZip)
                .addressZipCheck(addressZipCheck)
                .addressCountry(addressCountry)
                .brand(brand)
                .fingerprint(fingerprint)
                .funding(funding)
                .country(country)
                .currency(currency)
                .customer(customerId)
                .cvcCheck(cvcCheck)
                .last4(last4)
                .id(id)
                .tokenizationMethod(tokenizationMethod)
                .metadata(metadata)
                .loggingTokens(loggingTokens);
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
                || (CardBrand.AMERICAN_EXPRESS.equals(updatedType) && cvcValue.length() == 4)
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
    boolean validateExpYear(@NonNull Calendar now) {
        return expYear != null && !ModelUtils.hasYearPassed(expYear, now);
    }

    /**
     * @return the {@link #number} of this card
     */
    @Nullable
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
     * @return the {@link #cvc} for this card
     */
    @Nullable
    public String getCVC() {
        return cvc;
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
     * @return the {@link #expYear} for this card
     */
    @Nullable
    public Integer getExpYear() {
        return expYear;
    }

    /**
     * @return the cardholder {@link #name} for this card
     */
    @Nullable
    public String getName() {
        return name;
    }

    /**
     * @return the {@link #addressLine1} of this card
     */
    @Nullable
    public String getAddressLine1() {
        return addressLine1;
    }

    /**
     * @return the {@link #addressLine2} of this card
     */
    @Nullable
    public String getAddressLine2() {
        return addressLine2;
    }

    /**
     * @return the {@link #addressCity} for this card
     */
    @Nullable
    public String getAddressCity() {
        return addressCity;
    }

    /**
     * @return the {@link #addressZip} of this card
     */
    @Nullable
    public String getAddressZip() {
        return addressZip;
    }

    /**
     * @return the {@link #addressState} of this card
     */
    @Nullable
    public String getAddressState() {
        return addressState;
    }

    /**
     * @return the {@link #addressCountry} of this card
     */
    @Nullable
    public String getAddressCountry() {
        return addressCountry;
    }

    /**
     * @return the {@link #currency} of this card. Only supported for Managed accounts.
     */
    @Nullable
    public String getCurrency() {
        return currency;
    }

    /**
     * @return the {@link #metadata} of this card
     */
    @Nullable
    public Map<String, String> getMetadata() {
        return this.metadata;
    }

    /**
     * @return the {@link #last4} digits of this card.
     */
    @Nullable
    public String getLast4() {
        return last4;
    }

    @Nullable
    private String calculateLast4(@Nullable String number, @Nullable String last4) {
        if (!StripeTextUtils.isBlank(last4)) {
            return last4;
        } else if (number != null && number.length() > 4) {
            return number.substring(number.length() - 4);
        } else {
            return null;
        }
    }

    /**
     * Gets the {@link #brand} of this card. Updates the value if none has yet been set, or
     * if the {@link #number} has been changed.
     *
     * @return the {@link #brand} of this card
     */
    @Nullable
    @CardBrand
    public String getBrand() {
        return brand;
    }

    @Nullable
    private String calculateBrand(@Nullable String brand) {
        if (StripeTextUtils.isBlank(brand) && !StripeTextUtils.isBlank(number)) {
            return CardUtils.getPossibleCardType(number);
        } else {
            return brand;
        }
    }

    /**
     * @return the {@link #fingerprint} of this card
     */
    @Nullable
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
    @Nullable
    public String getCountry() {
        return country;
    }

    /**
     * @return the {@link #id} of this card
     */
    @Nullable
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

    @Nullable
    String getTokenizationMethod() {
        return this.tokenizationMethod;
    }

    boolean validateCard(@NonNull Calendar now) {
        if (cvc == null) {
            return validateNumber() && validateExpiryDate(now);
        } else {
            return validateNumber() && validateExpiryDate(now) && validateCVC();
        }
    }

    boolean validateExpiryDate(@NonNull Calendar now) {
        if (!validateExpMonth()) {
            return false;
        }
        if (expYear == null || !validateExpYear(now)) {
            return false;
        }
        return !ModelUtils.hasMonthPassed(expYear, expMonth, now);
    }

    private Card(@NonNull Builder builder) {
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
                ? calculateLast4(number, builder.last4)
                : builder.last4;
        this.brand = asCardBrand(builder.brand) == null
                ? calculateBrand(builder.brand)
                : builder.brand;
        this.fingerprint = StripeTextUtils.nullIfBlank(builder.fingerprint);
        this.funding = asFundingType(builder.funding);
        this.country = StripeTextUtils.nullIfBlank(builder.country);
        this.currency = StripeTextUtils.nullIfBlank(builder.currency);
        this.customerId = StripeTextUtils.nullIfBlank(builder.customerId);
        this.cvcCheck = StripeTextUtils.nullIfBlank(builder.cvcCheck);
        this.id = StripeTextUtils.nullIfBlank(builder.id);
        this.tokenizationMethod = StripeTextUtils.nullIfBlank(builder.tokenizationMethod);
        this.metadata = builder.metadata;

        if (builder.loggingTokens != null) {
            this.loggingTokens.addAll(builder.loggingTokens);
        }
    }

    @Nullable
    private String normalizeCardNumber(@Nullable String number) {
        if (number == null) {
            return null;
        }
        return number.trim().replaceAll("\\s+|-", "");
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return this == obj || (obj instanceof Card && typedEquals((Card) obj));
    }
    
    private boolean typedEquals(@NonNull Card card) {
        return ObjectUtils.equals(number, card.number)
                && ObjectUtils.equals(cvc, card.cvc)
                && ObjectUtils.equals(expMonth, card.expMonth)
                && ObjectUtils.equals(expYear, card.expYear)
                && ObjectUtils.equals(name, card.name)
                && ObjectUtils.equals(addressLine1, card.addressLine1)
                && ObjectUtils.equals(addressLine1Check, card.addressLine1Check)
                && ObjectUtils.equals(addressLine2, card.addressLine2)
                && ObjectUtils.equals(addressCity, card.addressCity)
                && ObjectUtils.equals(addressState, card.addressState)
                && ObjectUtils.equals(addressZip, card.addressZip)
                && ObjectUtils.equals(addressZipCheck, card.addressZipCheck)
                && ObjectUtils.equals(addressCountry, card.addressCountry)
                && ObjectUtils.equals(last4, card.last4)
                && ObjectUtils.equals(brand, card.brand)
                && ObjectUtils.equals(funding, card.funding)
                && ObjectUtils.equals(fingerprint, card.fingerprint)
                && ObjectUtils.equals(country, card.country)
                && ObjectUtils.equals(currency, card.currency)
                && ObjectUtils.equals(customerId, card.customerId)
                && ObjectUtils.equals(cvcCheck, card.cvcCheck)
                && ObjectUtils.equals(id, card.id)
                && ObjectUtils.equals(loggingTokens, card.loggingTokens)
                && ObjectUtils.equals(tokenizationMethod, card.tokenizationMethod)
                && ObjectUtils.equals(metadata, card.metadata);
    }

    @Override
    public int hashCode() {
        return ObjectUtils.hash(number, cvc, expMonth, expYear, name, addressLine1,
                addressLine1Check, addressLine2, addressCity, addressState, addressZip,
                addressZipCheck, addressCountry, last4, brand, funding, fingerprint,
                country, currency, customerId, cvcCheck, id, loggingTokens, tokenizationMethod,
                metadata);
    }

    /**
     * Builder class for a {@link Card} model.
     */
    public static final class Builder implements ObjectBuilder<Card> {
        @Nullable private final String number;
        @Nullable private final String cvc;
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
        private String customerId;
        private String cvcCheck;
        private String id;
        private String tokenizationMethod;
        private Map<String, String> metadata;
        private List<String> loggingTokens;

        /**
         * Constructor with most common {@link Card} fields.
         *
         * @param number the credit card number
         * @param expMonth the expiry month, as an integer value between 1 and 12
         * @param expYear the expiry year
         * @param cvc the card CVC number
         */
        public Builder(
                @Nullable String number,
                @IntRange(from = 1, to = 12) @Nullable Integer expMonth,
                @IntRange(from = 0) @Nullable Integer expYear,
                @Nullable String cvc) {
            this.number = number;
            this.expMonth = expMonth;
            this.expYear = expYear;
            this.cvc = cvc;
        }

        @NonNull
        public Builder name(@Nullable String name) {
            this.name = name;
            return this;
        }

        @NonNull
        public Builder addressLine1(@Nullable String address) {
            this.addressLine1 = address;
            return this;
        }

        @NonNull
        public Builder addressLine1Check(@Nullable String addressLine1Check) {
            this.addressLine1Check = addressLine1Check;
            return this;
        }

        @NonNull
        public Builder addressLine2(@Nullable String address) {
            this.addressLine2 = address;
            return this;
        }

        @NonNull
        public Builder addressCity(@Nullable String city) {
            this.addressCity = city;
            return this;
        }

        @NonNull
        public Builder addressState(@Nullable String state) {
            this.addressState = state;
            return this;
        }

        @NonNull
        public Builder addressZip(@Nullable String zip) {
            this.addressZip = zip;
            return this;
        }

        @NonNull
        public Builder addressZipCheck(@Nullable String zipCheck) {
            this.addressZipCheck = zipCheck;
            return this;
        }

        @NonNull
        public Builder addressCountry(@Nullable String country) {
            this.addressCountry = country;
            return this;
        }

        @NonNull
        public Builder brand(@Nullable @CardBrand String brand) {
            this.brand = brand;
            return this;
        }

        @NonNull
        public Builder fingerprint(@Nullable String fingerprint) {
            this.fingerprint = fingerprint;
            return this;
        }

        @NonNull
        public Builder funding(@Nullable @FundingType String funding) {
            this.funding = funding;
            return this;
        }

        @NonNull
        public Builder country(@Nullable String country) {
            this.country = country;
            return this;
        }

        @NonNull
        public Builder currency(@Nullable String currency) {
            this.currency = currency;
            return this;
        }

        @NonNull
        public Builder customer(@Nullable String customerId) {
            this.customerId = customerId;
            return this;
        }

        @NonNull
        public Builder cvcCheck(@Nullable String cvcCheck) {
            this.cvcCheck = cvcCheck;
            return this;
        }

        @NonNull
        public Builder last4(@Nullable String last4) {
            this.last4 = last4;
            return this;
        }

        @NonNull
        public Builder id(@Nullable String id) {
            this.id = id;
            return this;
        }

        @NonNull
        public Builder tokenizationMethod(@Nullable String tokenizationMethod) {
            this.tokenizationMethod = tokenizationMethod;
            return this;
        }

        @NonNull
        public Builder metadata(@Nullable Map<String, String> metadata) {
            this.metadata = metadata;
            return this;
        }

        @NonNull
        public Builder loggingTokens(@NonNull List<String> loggingTokens) {
            this.loggingTokens = loggingTokens;
            return this;
        }

        /**
         * Generate a new {@link Card} object based on the arguments held by this Builder.
         *
         * @return the newly created {@link Card} object
         */
        @NonNull
        public Card build() {
            return new Card(this);
        }
    }
}
