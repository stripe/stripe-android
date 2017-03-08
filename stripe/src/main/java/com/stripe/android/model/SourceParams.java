package com.stripe.android.model;

import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.Size;

import com.stripe.android.util.StripeNetworkUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.stripe.android.model.Source.SourceType;
import static com.stripe.android.util.StripeNetworkUtils.removeNullParams;

/**
 * Represents a grouping of parameters needed to create a {@link Source} object on the server.
 */
public class SourceParams {

    static final String API_PARAM_AMOUNT = "amount";
    static final String API_PARAM_CURRENCY = "currency";
    static final String API_PARAM_OWNER = "owner";
    static final String API_PARAM_REDIRECT = "redirect";
    static final String API_PARAM_TYPE = "type";

    static final String FIELD_ADDRESS = "address";
    static final String FIELD_BANK = "bank";
    static final String FIELD_CARD = "card";
    static final String FIELD_CITY = "city";
    static final String FIELD_COUNTRY = "country";
    static final String FIELD_CVC = "cvc";
    static final String FIELD_EMAIL = "email";
    static final String FIELD_EXP_MONTH = "exp_month";
    static final String FIELD_EXP_YEAR = "exp_year";
    static final String FIELD_IBAN = "iban";
    static final String FIELD_LINE_1 = "line1";
    static final String FIELD_LINE_2 = "line2";
    static final String FIELD_NAME = "name";
    static final String FIELD_NUMBER = "number";
    static final String FIELD_POSTAL_CODE = "postal_code";
    static final String FIELD_RETURN_URL = "return_url";
    static final String FIELD_STATE = "state";
    static final String FIELD_STATEMENT_DESCRIPTOR = "statement_descriptor";

    @IntRange(from = 0) private Long mAmount;
    private Map<String, Object> mApiParameterMap;
    private String mCurrency;
    private Map<String, Object> mOwner;
    private Map<String, Object> mRedirect;
    @SourceType private String mType;

    private SourceParams() {}

    /**
     * Create a set of parameters used for a Bancontact source.
     *
     * @param amount amount to be charged
     * @param name account owner name
     * @param returnUrl a URL redirect
     * @param statementDescriptor a description of the transaction to put on the customer statement
     * @return a {@link SourceParams} object that can be used to create a Bancontact source
     */
    @NonNull
    public static SourceParams createBancontactParams(
            @IntRange(from = 0) long amount,
            @NonNull String name,
            @NonNull String returnUrl,
            @Nullable String statementDescriptor) {
        SourceParams params = new SourceParams()
                .setType(Source.BANCONTACT)
                .setCurrency(Source.EURO)
                .setAmount(amount)
                .setOwner(createSimpleMap(FIELD_NAME, name))
                .setRedirect(createSimpleMap(FIELD_RETURN_URL, returnUrl));

        if (statementDescriptor != null) {
            Map<String, Object> additionalParamsMap =
                    createSimpleMap(FIELD_STATEMENT_DESCRIPTOR, statementDescriptor);
            params.setApiParameterMap(additionalParamsMap);
        }

        return params;
    }

    /**
     * Create a set of parameters for a Bitcoin source.
     *
     * @param amount amount for the transaction
     * @param currency currency of the transaction (value is converted to Bitcoin on the server)
     * @param email owner email for the account
     * @return a {@link SourceParams} object that can be used to create a Bitcoin source
     */
    @NonNull
    public static SourceParams createBitcoinParams(
            @IntRange(from = 0) long amount,
            @NonNull String currency,
            @NonNull String email) {
        return new SourceParams()
                .setType(Source.BITCOIN)
                .setAmount(amount)
                .setCurrency(currency)
                .setOwner(createSimpleMap(FIELD_EMAIL, email));
    }

    /**
     * Create a set of parameters for a credit card source.
     *
     * @param card a {@link Card} object containing the details necessary for the source
     * @return a {@link SourceParams} object that can be used to create a card source
     */
    @NonNull
    public static SourceParams createCardParams(@NonNull Card card) {
        SourceParams params = new SourceParams().setType(Source.CARD);

        // Not enforcing all fields to exist at this level.
        // Instead, the server will return an error for invalid data.
        Map<String, Object> basicInfoMap = new HashMap<>();
        basicInfoMap.put(FIELD_NUMBER, card.getNumber());
        basicInfoMap.put(FIELD_EXP_MONTH, card.getExpMonth());
        basicInfoMap.put(FIELD_EXP_YEAR, card.getExpYear());
        basicInfoMap.put(FIELD_CVC, card.getCVC());
        removeNullParams(basicInfoMap);

        params.setApiParameterMap(basicInfoMap);

        Map<String, Object> addressMap = new HashMap<>();
        addressMap.put(FIELD_LINE_1, card.getAddressLine1());
        addressMap.put(FIELD_LINE_2, card.getAddressLine2());
        addressMap.put(FIELD_CITY, card.getAddressCity());
        addressMap.put(FIELD_COUNTRY, card.getAddressCountry());
        addressMap.put(FIELD_STATE, card.getAddressState());
        addressMap.put(FIELD_POSTAL_CODE, card.getAddressZip());
        removeNullParams(addressMap);

        // If there are any keys left...
        if (addressMap.keySet().size() > 0) {
            params.setOwner(createSimpleMap(FIELD_ADDRESS, addressMap));
        }

        return params;
    }

    /**
     * Create parameters necessary for creating a Giropay source
     *
     * @param amount amount of the transaction
     * @param name source owner name
     * @param returnUrl redirect URL
     * @param statementDescriptor a description of the transaction
     * @return a {@link SourceParams} object that can be used to create a Giropay source
     */
    @NonNull
    public static SourceParams createGiropayParams(
            @IntRange(from = 0) long amount,
            @NonNull String name,
            @NonNull String returnUrl,
            @Nullable String statementDescriptor) {
        SourceParams params = new SourceParams()
                .setType(Source.GIROPAY)
                .setCurrency(Source.EURO)
                .setAmount(amount)
                .setOwner(createSimpleMap(FIELD_NAME, name))
                .setRedirect(createSimpleMap(FIELD_RETURN_URL, returnUrl));

        if (statementDescriptor != null) {
            Map<String, Object> additionalParamsMap =
                    createSimpleMap(FIELD_STATEMENT_DESCRIPTOR, statementDescriptor);
            params.setApiParameterMap(additionalParamsMap);
        }

        return params;
    }

    /**
     * Create parameters needed to make an iDEAL source.
     *
     * @param amount amount of the transaction
     * @param name owner name
     * @param returnUrl redirect URL
     * @param statementDescriptor a description of the transaction
     * @param bank bank id for the iDEAL source
     * @return a {@link SourceParams} object that can be used to create an iDEAL source
     */
    @NonNull
    public static SourceParams createIdealParams(
            @IntRange(from = 0) long amount,
            @NonNull String name,
            @NonNull String returnUrl,
            @Nullable String statementDescriptor,
            @Nullable String bank) {
        SourceParams params = new SourceParams()
                .setType(Source.IDEAL)
                .setCurrency(Source.EURO)
                .setAmount(amount)
                .setOwner(createSimpleMap(FIELD_NAME, name))
                .setRedirect(createSimpleMap(FIELD_RETURN_URL, returnUrl));

        if (statementDescriptor != null && bank != null) {
            Map<String, Object> additionalParamsMap =
                    createSimpleMap(
                            FIELD_STATEMENT_DESCRIPTOR, statementDescriptor,
                            FIELD_BANK, bank);
            params.setApiParameterMap(additionalParamsMap);
        }

        return params;
    }

    /**
     * Create parameters necessary to create a SEPA debit source
     *
     * @param name owner name
     * @param iban bank IBAN
     * @param addressLine1 1-line address of the owner
     * @param city city of source owner's address
     * @param postalCode postal code for source owner's address
     * @param country country code for source owner's address
     * @return a {@link SourceParams} object that can be used to create a SEPA debit source
     */
    @NonNull
    public static SourceParams createSepaDebitParams(
            @NonNull String name,
            @NonNull String iban,
            @NonNull String addressLine1,
            @NonNull String city,
            @NonNull String postalCode,
            @NonNull @Size(2) String country) {
        SourceParams params = new SourceParams()
                .setType(Source.SEPA_DEBIT)
                .setCurrency(Source.EURO);

        Map<String, Object> address = new HashMap<>();
        address.put(FIELD_LINE_1, addressLine1);
        address.put(FIELD_CITY, city);
        address.put(FIELD_POSTAL_CODE, postalCode);
        address.put(FIELD_COUNTRY, country);

        Map<String, Object> ownerMap = new HashMap<>();
        ownerMap.put(FIELD_NAME, name);
        ownerMap.put(FIELD_ADDRESS, address);

        params.setOwner(ownerMap).setApiParameterMap(createSimpleMap(FIELD_IBAN, iban));
        return params;
    }

    /**
     * Create parameters needed to make a SOFORT source.
     *
     * @param amount the amount of the transaction
     * @param returnUrl the redirect URL
     * @param country the country code for the source
     * @param statementDescriptor a description of the transaction
     * @return a {@link SourceParams} object that can be used to create a SOFORT source
     */
    @NonNull
    public static SourceParams createSofortParams(
            @IntRange(from = 0) long amount,
            @NonNull String returnUrl,
            @NonNull @Size(2) String country,
            @Nullable String statementDescriptor) {
        SourceParams params = new SourceParams()
                .setType(Source.SOFORT)
                .setCurrency(Source.EURO)
                .setAmount(amount)
                .setRedirect(createSimpleMap(FIELD_RETURN_URL, returnUrl));

        Map<String, Object> sofortMap = createSimpleMap(FIELD_COUNTRY, country);
        if (statementDescriptor != null) {
            sofortMap.put(FIELD_STATEMENT_DESCRIPTOR, statementDescriptor);
        }

        params.setApiParameterMap(sofortMap);

        return params;
    }

    /**
     * Create parameters needed to make a 3D-Secure source.
     *
     * @param amount amount of the transaction
     * @param currency currency code for the transaction
     * @param returnUrl the redirect url
     * @param cardID the ID from the card source object used to create this 3DS source
     * @return a {@link SourceParams} object that can be used to create a 3D-Secure source
     */
    @NonNull
    public static SourceParams createThreeDSecureParams(
            @IntRange(from = 0) long amount,
            @NonNull String currency,
            @NonNull String returnUrl,
            @NonNull String cardID) {
        SourceParams params = new SourceParams()
                .setType(Source.THREE_D_SECURE)
                .setCurrency(currency)
                .setAmount(amount)
                .setRedirect(createSimpleMap(FIELD_RETURN_URL, returnUrl));
        params.setApiParameterMap(createSimpleMap(FIELD_CARD, cardID));
        return params;
    }

    /**
     * @return the amount of the transaction
     */
    @Nullable
    public Long getAmount() {
        return mAmount;
    }

    /**
     * @return a {@link Map} of the parameters specific to this type of source
     */
    @Nullable
    public Map<String, Object> getApiParameterMap() {
        return mApiParameterMap;
    }

    /**
     * @return the currency code for the transaction
     */
    @Nullable
    public String getCurrency() {
        return mCurrency;
    }

    /**
     * @return details about the source owner (map contents are specific to source type)
     */
    @Nullable
    public Map<String, Object> getOwner() {
        return mOwner;
    }

    /**
     * @return redirect map for the source
     */
    @Nullable
    public Map<String, Object> getRedirect() {
        return mRedirect;
    }

    /**
     * @return the {@link SourceType Type} of this source
     */
    public String getType() {
        return mType;
    }

    /**
     * Create a string-keyed map representing this object that is
     * ready to be sent over the network.
     *
     * @return a String-keyed map
     */
    @NonNull
    public Map<String, Object> toParamMap() {
        Map<String, Object> networkReadyMap = new HashMap<>();
        networkReadyMap.put(API_PARAM_TYPE, getType());
        networkReadyMap.put(getType(), getApiParameterMap());
        networkReadyMap.put(API_PARAM_AMOUNT, getAmount());
        networkReadyMap.put(API_PARAM_CURRENCY, getCurrency());
        networkReadyMap.put(API_PARAM_OWNER, getOwner());
        networkReadyMap.put(API_PARAM_REDIRECT, getRedirect());
        StripeNetworkUtils.removeNullParams(networkReadyMap);
        return networkReadyMap;
    }

    @NonNull
    private static Map<String, Object> createSimpleMap(
            @NonNull String key, @NonNull Object value) {
        Map<String, Object> simpleMap = new HashMap<>();
        simpleMap.put(key, value);
        return simpleMap;
    }

    @NonNull
    private static Map<String, Object> createSimpleMap(
            @NonNull String key1, @NonNull Object value1,
            @NonNull String key2, @NonNull Object value2) {
        Map<String, Object> simpleMap = new HashMap<>();
        simpleMap.put(key1, value1);
        simpleMap.put(key2, value2);
        return simpleMap;
    }

    /*---- Setters ----*/

    private SourceParams setAmount(long amount) {
        mAmount = amount;
        return this;
    }

    private SourceParams setApiParameterMap(
            @NonNull Map<String, Object> apiParameterMap) {
        mApiParameterMap = apiParameterMap;
        return this;
    }

    private SourceParams setCurrency(String currency) {
        mCurrency = currency;
        return this;
    }

    private SourceParams setOwner(Map<String, Object> owner) {
        mOwner = owner;
        return this;
    }

    private SourceParams setRedirect(Map<String, Object> redirect) {
        mRedirect = redirect;
        return this;
    }

    private SourceParams setType(@Source.SourceType String type) {
        mType = type;
        return this;
    }

}
