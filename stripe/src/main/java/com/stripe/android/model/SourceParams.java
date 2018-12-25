package com.stripe.android.model;

import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.Size;

import java.util.HashMap;
import java.util.Map;

import static com.stripe.android.StripeNetworkUtils.removeNullAndEmptyParams;
import static com.stripe.android.model.Source.SourceType;

/**
 * Represents a grouping of parameters needed to create a {@link Source} object on the server.
 */
public class SourceParams {

    static final String API_PARAM_AMOUNT = "amount";
    static final String API_PARAM_CURRENCY = "currency";
    static final String API_PARAM_METADATA = "metadata";
    static final String API_PARAM_OWNER = "owner";
    static final String API_PARAM_REDIRECT = "redirect";
    static final String API_PARAM_TYPE = "type";
    static final String API_PARAM_TOKEN = "token";
    static final String API_PARAM_USAGE = "usage";

    static final String API_PARAM_CLIENT_SECRET = "client_secret";

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
    static final String FIELD_PREFERRED_LANGUAGE = "preferred_language";

    static final String VISA_CHECKOUT = "visa_checkout";
    static final String CALL_ID = "callid";

    static final String MASTERPASS = "masterpass";
    static final String TRANSACTION_ID = "transaction_id";
    static final String CART_ID = "cart_id";

    @IntRange(from = 0) private Long mAmount;
    private Map<String, Object> mApiParameterMap;
    private String mCurrency;
    @Nullable private String mTypeRaw;
    private Map<String, Object> mOwner;
    private Map<String, String> mMetaData;
    private Map<String, Object> mRedirect;
    private Map<String, Object> mExtraParams;
    private String mToken;
    @Nullable @Source.Usage private String mUsage;
    @SourceType private String mType;

    private SourceParams() {}

    /**
     * Create parameters necessary for creating a P24 source.
     *
     * @param amount A positive integer in the smallest currency unit representing the amount to
     *               charge the customer (e.g., 1099 for a €10.99 payment).
     * @param currency `eur` or `pln` (P24 payments must be in either Euros or Polish Zloty).
     * @param name The name of the account holder (optional).
     * @param email The email address of the account holder.
     * @param returnUrl The URL the customer should be redirected to after the authorization
     *                  process.
     * @return a {@link SourceParams} that can be used to create a P24 source
     *
     * @see <a href="https://stripe.com/docs/sources/p24">https://stripe.com/docs/sources/p24</a>
     */
    public static SourceParams createP24Params(
            @IntRange(from = 0) long amount,
            @NonNull String currency,
            @Nullable String name,
            @NonNull String email,
            @NonNull String returnUrl) {
        SourceParams params = new SourceParams()
                .setAmount(amount)
                .setType(Source.P24)
                .setCurrency(currency)
                .setRedirect(createSimpleMap(FIELD_RETURN_URL, returnUrl));

        Map<String, Object> ownerMap = new HashMap<>();
        ownerMap.put(FIELD_NAME, name);
        ownerMap.put(FIELD_EMAIL, email);
        removeNullAndEmptyParams(ownerMap);
        if (ownerMap.keySet().size() > 0) {
            params.setOwner(ownerMap);
        }

        return params;
    }

    /**
     * Create parameters necessary for creating a reusable Alipay source.
     *
     * @param currency The currency of the payment. Must be the default currency for your country.
     *                 Can be aud, cad, eur, gbp, hkd, jpy, nzd, sgd, or usd. Users in Denmark,
     *                 Norway, Sweden, or Switzerland must use eur.
     * @param name The name of the account holder (optional).
     * @param email The email address of the account holder (optional).
     * @param returnUrl The URL the customer should be redirected to after the authorization
     *                 process.
     * @return a {@link SourceParams} that can be used to create an Alipay reusable source
     *
     * @see <a href="https://stripe.com/docs/sources/alipay">https://stripe.com/docs/sources/alipay</a>
     */
    public static SourceParams createAlipayReusableParams(
            @NonNull String currency,
            @Nullable String name,
            @Nullable String email,
            @NonNull String returnUrl) {
        SourceParams params = new SourceParams()
                .setType(Source.ALIPAY)
                .setCurrency(currency)
                .setRedirect(createSimpleMap(FIELD_RETURN_URL, returnUrl))
                .setUsage(Source.REUSABLE);

        Map<String, Object> ownerMap = new HashMap<>();
        ownerMap.put(FIELD_NAME, name);
        ownerMap.put(FIELD_EMAIL, email);
        removeNullAndEmptyParams(ownerMap);
        if (ownerMap.keySet().size() > 0) {
            params.setOwner(ownerMap);
        }

        return params;
    }

    /**
     * Create parameters necessary for creating a single use Alipay source.
     *
     * @param amount A positive integer in the smallest currency unit representing the amount to
     *              charge the customer (e.g., 1099 for a $10.99 payment).
     * @param currency The currency of the payment. Must be the default currency for your country.
     *                Can be aud, cad, eur, gbp, hkd, jpy, nzd, sgd, or usd. Users in Denmark,
     *                Norway, Sweden, or Switzerland must use eur.
     * @param name The name of the account holder (optional).
     * @param email The email address of the account holder (optional).
     * @param returnUrl The URL the customer should be redirected to after the authorization
     *                 process.
     * @return a {@link SourceParams} that can be used to create an Alipay single-use source
     *
     * @see <a href="https://stripe.com/docs/sources/alipay">https://stripe.com/docs/sources/alipay</a>
     */
    @NonNull
    public static SourceParams createAlipaySingleUseParams(
            @IntRange(from = 0) long amount,
            @NonNull String currency,
            @Nullable String name,
            @Nullable String email,
            @NonNull String returnUrl) {
        SourceParams params = new SourceParams()
                .setType(Source.ALIPAY)
                .setCurrency(currency)
                .setAmount(amount)
                .setRedirect(createSimpleMap(FIELD_RETURN_URL, returnUrl));

        Map<String, Object> ownerMap = new HashMap<>();
        ownerMap.put(FIELD_NAME, name);
        ownerMap.put(FIELD_EMAIL, email);
        removeNullAndEmptyParams(ownerMap);
        if (ownerMap.keySet().size() > 0) {
            params.setOwner(ownerMap);
        }

        return params;
    }

    /**
     * Create parameters necessary for creating a Bancontact source.
     *
     * @param amount A positive integer in the smallest currency unit representing the amount to
     *              charge the customer (e.g., 1099 for a €10.99 payment). The charge amount must be
     *              at least €1 or its equivalent in the given currency.
     * @param name The full name of the account holder.
     * @param returnUrl The URL the customer should be redirected to after the authorization
     *                 process.
     * @param statementDescriptor A custom statement descriptor for the payment (optional).
     * @param preferredLanguage The preferred language of the Bancontact authorization page that the
     *                          customer is redirected to. Supported values are: en, de, fr, or nl
     *                          (optional).
     * @return a {@link SourceParams} object that can be used to create a Bancontact source
     *
     * @see <a href="https://stripe.com/docs/sources/bancontact">https://stripe.com/docs/sources/bancontact</a>
     */
    @NonNull
    public static SourceParams createBancontactParams(
            @IntRange(from = 0) long amount,
            @NonNull String name,
            @NonNull String returnUrl,
            @Nullable String statementDescriptor,
            @Nullable String preferredLanguage) {
        SourceParams params = new SourceParams()
                .setType(Source.BANCONTACT)
                .setCurrency(Source.EURO)
                .setAmount(amount)
                .setOwner(createSimpleMap(FIELD_NAME, name))
                .setRedirect(createSimpleMap(FIELD_RETURN_URL, returnUrl));

        if (statementDescriptor != null || preferredLanguage != null) {
            Map<String, Object> additionalParamsMap = new HashMap<>();

            additionalParamsMap.put(FIELD_STATEMENT_DESCRIPTOR, statementDescriptor);
            additionalParamsMap.put(FIELD_PREFERRED_LANGUAGE, preferredLanguage);
            removeNullAndEmptyParams(additionalParamsMap);

            params.setApiParameterMap(additionalParamsMap);
        }

        return params;
    }

    /**
     * Create a custom {@link SourceParams} object. Incorrect attributes may result in errors
     * when connecting to Stripe's API.
     *
     * @return an empty {@link SourceParams} object.
     */
    @NonNull
    public static SourceParams createCustomParams() {
        return new SourceParams();
    }

    /**
     * Create parameters necessary for converting a token into a source
     *
     * @param tokenId the id of the {@link Token} to be converted into a source.
     * @return a {@link SourceParams} object that can be used to create a source.
     */
    @NonNull
    public static SourceParams createSourceFromTokenParams(String tokenId) {
        SourceParams sourceParams = SourceParams.createCustomParams();
        sourceParams.setType(Source.CARD);
        sourceParams.setToken(tokenId);
        return sourceParams;
    }

    /**
     * Create parameters necessary for creating a card source.
     *
     * @param card A {@link Card} object containing the details necessary for the source.
     * @return a {@link SourceParams} object that can be used to create a card source.
     *
     * @see <a href="https://stripe.com/docs/sources/cards">https://stripe.com/docs/sources/cards</a>
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
        removeNullAndEmptyParams(basicInfoMap);

        params.setApiParameterMap(basicInfoMap);

        Map<String, Object> addressMap = new HashMap<>();
        addressMap.put(FIELD_LINE_1, card.getAddressLine1());
        addressMap.put(FIELD_LINE_2, card.getAddressLine2());
        addressMap.put(FIELD_CITY, card.getAddressCity());
        addressMap.put(FIELD_COUNTRY, card.getAddressCountry());
        addressMap.put(FIELD_STATE, card.getAddressState());
        addressMap.put(FIELD_POSTAL_CODE, card.getAddressZip());
        removeNullAndEmptyParams(addressMap);

        // If there are any keys left...
        Map<String, Object> ownerMap = new HashMap<>();
        ownerMap.put(FIELD_NAME, card.getName());
        if (addressMap.keySet().size() > 0) {
            ownerMap.put(FIELD_ADDRESS, addressMap);
        }
        removeNullAndEmptyParams(ownerMap);
        if (ownerMap.keySet().size() > 0) {
            params.setOwner(ownerMap);
        }

        return params;
    }

    /**
     * Create parameters necessary for creating an EPS source.
     *
     * @param amount A positive integer in the smallest currency unit representing the amount to
     *               charge the customer (e.g., 1099 for a €10.99 payment).
     * @param name The full name of the account holder.
     * @param returnUrl The URL the customer should be redirected to after the authorization
     *                  process.
     * @param statementDescriptor A custom statement descriptor for the payment (optional).
     * @return a {@link SourceParams} object that can be used to create an EPS source
     *
     * @see <a href="https://stripe.com/docs/sources/eps">https://stripe.com/docs/sources/eps</a>
     */
    @NonNull
    public static SourceParams createEPSParams(
            @IntRange(from = 0) long amount,
            @NonNull String name,
            @NonNull String returnUrl,
            @Nullable String statementDescriptor) {
        SourceParams params = new SourceParams()
                .setType(Source.EPS)
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
     * Create parameters necessary for creating a Giropay source
     *
     * @param amount A positive integer in the smallest currency unit representing the amount to
     *              charge the customer (e.g., 1099 for a €10.99 payment).
     * @param name The full name of the account holder.
     * @param returnUrl The URL the customer should be redirected to after the authorization
     *                  process.
     * @param statementDescriptor A custom statement descriptor for the payment (optional).
     * @return a {@link SourceParams} object that can be used to create a Giropay source
     *
     * @see <a href="https://stripe.com/docs/sources/giropay">https://stripe.com/docs/sources/giropay</a>
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
     * Create parameters necessary for creating an iDEAL source.
     *
     * @param amount A positive integer in the smallest currency unit representing the amount to
     *               charge the customer (e.g., 1099 for a €10.99 payment).
     * @param name The full name of the account holder (optional).
     * @param returnUrl The URL the customer should be redirected to after the authorization
     *                 process.
     * @param statementDescriptor A custom statement descriptor for the payment (optional).
     * @param bank The customer’s bank (optional).
     * @return a {@link SourceParams} object that can be used to create an iDEAL source
     *
     * @see <a href="https://stripe.com/docs/sources/ideal">https://stripe.com/docs/sources/ideal</a>
     */
    @NonNull
    public static SourceParams createIdealParams(
            @IntRange(from = 0) long amount,
            @Nullable String name,
            @NonNull String returnUrl,
            @Nullable String statementDescriptor,
            @Nullable String bank) {
        SourceParams params = new SourceParams()
                .setType(Source.IDEAL)
                .setCurrency(Source.EURO)
                .setAmount(amount)
                .setRedirect(createSimpleMap(FIELD_RETURN_URL, returnUrl));
        if (name != null) {
            params.setOwner(createSimpleMap(FIELD_NAME, name));
        }
        Map<String, Object> additionalParamsMap = new HashMap<>();
        if (statementDescriptor != null) {
            additionalParamsMap.put(FIELD_STATEMENT_DESCRIPTOR, statementDescriptor);
        }
        if (bank != null) {
            additionalParamsMap.put(FIELD_BANK, bank);
        }
        if (!additionalParamsMap.isEmpty()) {
            params.setApiParameterMap(additionalParamsMap);
        }
        return params;
    }

    /**
     * Create parameters necessary to create a Multibanco source.
     *
     * @param amount A positive integer in the smallest currency unit representing the amount to
     *               charge the customer (e.g., 1099 for a €10.99 payment).
     * @param returnUrl The URL the customer should be redirected to after the authorization
     *                  process.
     * @param email The full email address of the customer.
     * @return a {@link SourceParams} object that can be used to create a Multibanco source
     *
     * @see <a href="https://stripe.com/docs/sources/multibanco">https://stripe.com/docs/sources/multibanco</a>
     */
    @NonNull
    public static SourceParams createMultibancoParams(
            @IntRange(from = 0) long amount,
            @NonNull String returnUrl,
            @NonNull String email) {
        SourceParams params = new SourceParams()
                .setType(Source.MULTIBANCO)
                .setCurrency(Source.EURO)
                .setAmount(amount)
                .setRedirect(createSimpleMap(FIELD_RETURN_URL, returnUrl))
                .setOwner(createSimpleMap(FIELD_EMAIL, email));
        return params;
    }

    @NonNull
    public static SourceParams createSepaDebitParams(
            @NonNull String name,
            @NonNull String iban,
            @Nullable String addressLine1,
            @NonNull String city,
            @NonNull String postalCode,
            @NonNull @Size(2) String country) {
        return createSepaDebitParams(name, iban, null, addressLine1, city, postalCode, country);
    }

    /**
     * Create parameters necessary to create a SEPA debit source
     *
     * @param name The full name of the account holder.
     * @param iban The IBAN number for the bank account that you wish to debit.
     * @param email The full email address of the owner (optional).
     * @param addressLine1 The first line of the owner's address (optional).
     * @param city The city of the owner's address.
     * @param postalCode The postal code of the owner's address.
     * @param country The ISO-3166 2-letter country code of the owner's address.
     * @return a {@link SourceParams} object that can be used to create a SEPA debit source
     *
     * @see <a href="https://stripe.com/docs/sources/sepa-debit">https://stripe.com/docs/sources/sepa-debit</a>
     */
    @NonNull
    public static SourceParams createSepaDebitParams(
            @NonNull String name,
            @NonNull String iban,
            @Nullable String email,
            @Nullable String addressLine1,
            @Nullable String city,
            @Nullable String postalCode,
            @Nullable @Size(2) String country) {
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
        ownerMap.put(FIELD_EMAIL, email);
        ownerMap.put(FIELD_ADDRESS, address);

        params.setOwner(ownerMap).setApiParameterMap(createSimpleMap(FIELD_IBAN, iban));
        return params;
    }

    /**
     * Create parameters necessary to create a SOFORT source.
     *
     * @param amount A positive integer in the smallest currency unit representing the amount to
     *              charge the customer (e.g., 1099 for a €10.99 payment).
     * @param returnUrl The URL the customer should be redirected to after the authorization
     *                  process.
     * @param country The ISO-3166 2-letter country code of the customer’s bank.
     * @param statementDescriptor A custom statement descriptor for the payment (optional).
     * @return a {@link SourceParams} object that can be used to create a SOFORT source
     *
     * @see <a href="https://stripe.com/docs/sources/sofort">https://stripe.com/docs/sources/sofort</a>
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
     * Create parameters necessary to create a 3D Secure source.
     *
     * @param amount A positive integer in the smallest currency unit representing the amount to
     *              charge the customer (e.g., 1099 for a €10.99 payment).
     * @param currency The currency the payment is being created in (e.g., eur).
     * @param returnUrl The URL the customer should be redirected to after the verification process.
     * @param cardID The ID of the card source.
     * @return a {@link SourceParams} object that can be used to create a 3D Secure source
     *
     * @see <a href="https://stripe.com/docs/sources/three-d-secure">https://stripe.com/docs/sources/three-d-secure</a>
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
     * Create parameters needed to make a Visa Checkout source.
     *
     * @param callId The payment request ID (callId) from the Visa Checkout SDK.
     * @return a {@link SourceParams} object that can be used to create a Visa Checkout Card Source.
     *
     * @see <a href="https://stripe.com/docs/visa-checkout">https://stripe.com/docs/visa-checkout</a>
     * @see <a href="https://developer.visa.com/capabilities/visa_checkout/docs">https://developer.visa.com/capabilities/visa_checkout/docs</a>
     */
    public static SourceParams createVisaCheckoutParams(@NonNull String callId) {
        SourceParams params = new SourceParams().setType(Source.CARD);
        Map<String, Object> visaCheckoutMap =
                createSimpleMap(VISA_CHECKOUT, createSimpleMap(CALL_ID, callId));
        params.setApiParameterMap(visaCheckoutMap);
        return params;
    }

    /**
     * Create parameters needed to make a Masterpass source
     *
     * @param transactionId The transaction ID from the Masterpass SDK.
     * @param cartID A unique string that you generate to identify the purchase when creating a cart
     *               for checkout in the Masterpass SDK.
     *
     * @return a {@link SourceParams} object that can be used to create a Masterpass Card Source.
     *
     * @see <a href="https://stripe.com/docs/masterpass">https://stripe.com/docs/masterpass</a>
     * @see <a href="https://developer.mastercard.com/product/masterpass">https://developer.mastercard.com/product/masterpass</a>
     * @see <a href="https://developer.mastercard.com/page/masterpass-merchant-mobile-checkout-sdk-for-android-v2">https://developer.mastercard.com/page/masterpass-merchant-mobile-checkout-sdk-for-android-v2</a>
     */
    public static SourceParams createMasterpassParams(
            @NonNull String transactionId,
            @NonNull String cartID) {
        SourceParams params = new SourceParams().setType(Source.CARD);
        Map map = createSimpleMap(TRANSACTION_ID, transactionId);
        map.put(CART_ID, cartID);
        Map<String, Object> masterpassMap =
                createSimpleMap(MASTERPASS, map);
        params.setApiParameterMap(masterpassMap);
        return params;
    }

    /**
     * Create parameters needed to retrieve a source.
     *
     * @param clientSecret the client secret for the source, needed because the Android SDK uses
     *                     a public key
     * @return a {@link Map} matching the parameter name to the client secret, ready to send to
     * the server.
     */
    @NonNull
    public static Map<String, Object> createRetrieveSourceParams(
            @NonNull @Size(min = 1) String clientSecret) {
        Map<String, Object> params = new HashMap<>();
        params.put(API_PARAM_CLIENT_SECRET, clientSecret);
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
    @NonNull
    @SourceType
    public String getType() {
        return mType;
    }

    /**
     * @return a custom type of this source, if one has been set
     */
    @Nullable
    public String getTypeRaw() {
        return mTypeRaw;
    }

    /**
     * @return the current usage of this source, if one has been set
     */
    @Nullable
    @Source.Usage
    public String getUsage() {
        return mUsage;
    }

    /**
     * @return the custom metadata set on these params
     */
    public Map<String, String> getMetaData() {
        return mMetaData;
    }

    /*---- Setters ----*/

    /**
     * @param amount currency amount for this source, in the lowest denomination.
     * @return {@code this}, for chaining purposes
     */
    public SourceParams setAmount(long amount) {
        mAmount = amount;
        return this;
    }

    /**
     * @param apiParameterMap a map of parameters specific for this type of source
     * @return {@code this}, for chaining purposes
     */
    public SourceParams setApiParameterMap(
            @NonNull Map<String, Object> apiParameterMap) {
        mApiParameterMap = apiParameterMap;
        return this;
    }

    /**
     * @param currency currency code for this source (i.e. "EUR")
     * @return {@code this}, for chaining purposes
     */
    public SourceParams setCurrency(String currency) {
        mCurrency = currency;
        return this;
    }

    /**
     * @param owner an {@link SourceOwner} object for this source
     * @return {@code this}, for chaining purposes
     */
    public SourceParams setOwner(Map<String, Object> owner) {
        mOwner = owner;
        return this;
    }

    /**
     * Sets a redirect property map for this source object. If you only want to
     * set a return url, use {@link #setReturnUrl(String)}.
     *
     * @param redirect a set of redirect parameters
     * @return {@code this}, for chaining purposes
     */
    public SourceParams setRedirect(Map<String, Object> redirect) {
        mRedirect = redirect;
        return this;
    }

    /**
     * Sets extra params for this source object.
     *
     * @param extraParams a set of params
     * @return {@code this}, for chaining purposes
     */
    public SourceParams setExtraParams(final Map<String, Object> extraParams) {
        mExtraParams = extraParams;
        return this;
    }

    /**
     * @param returnUrl a redirect URL for this source.
     * @return {@code this}, for chaining purposes
     */
    public SourceParams setReturnUrl(@NonNull @Size(min = 1) String returnUrl) {
        if (mRedirect == null) {
            setRedirect(createSimpleMap(FIELD_RETURN_URL, returnUrl));
        } else {
            mRedirect.put(FIELD_RETURN_URL, returnUrl);
        }
        return this;
    }

    /**
     * Sets the {@link SourceType} for this source. If you are creating a custom type,
     * use {@link #setTypeRaw(String)}.
     *
     * @param type the {@link SourceType}
     * @return {@code this}, for chaining purposes
     */
    public SourceParams setType(@Source.SourceType String type) {
        mType = type;
        mTypeRaw = type;
        return this;
    }

    /**
     * Sets a custom type for the source, and sets the {@link #mType type} for these parameters
     * to be {@link Source#UNKNOWN}.
     *
     * @param typeRaw the name of the source type
     * @return {@code this}, for chaining purposes
     */
    public SourceParams setTypeRaw(@NonNull String typeRaw) {
        mType = Source.asSourceType(typeRaw);
        if (mType == null) {
            mType = Source.UNKNOWN;
        }
        mTypeRaw = typeRaw;
        return this;
    }

    /**
     * Set custom metadata on the parameters.
     *
     * @param metaData
     * @return {@code this}, for chaining purposes
     */
    public SourceParams setMetaData(@NonNull Map<String, String> metaData) {
        mMetaData = metaData;
        return this;
    }

    /**
     * Sets a token ID on the parameters.
     *
     * @param token a token ID
     * @return {@code this}, for chaining purposes
     */
    public SourceParams setToken(@NonNull String token) {
        mToken = token;
        return this;
    }

    /**
     * Sets a usage value on the parameters. Used for Alipay, and should be
     * either "single_use" or "reusable". Not setting this value defaults
     * to "single_use".
     *
     * @param usage either "single_use" or "reusable"
     * @return {@code this} for chaining purposes
     */
    public SourceParams setUsage(@NonNull @Source.Usage String usage) {
        mUsage = usage;
        return this;
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

        networkReadyMap.put(API_PARAM_TYPE, mTypeRaw);
        networkReadyMap.put(mTypeRaw, mApiParameterMap);
        networkReadyMap.put(API_PARAM_AMOUNT, mAmount);
        networkReadyMap.put(API_PARAM_CURRENCY, mCurrency);
        networkReadyMap.put(API_PARAM_OWNER, mOwner);
        networkReadyMap.put(API_PARAM_REDIRECT, mRedirect);
        networkReadyMap.put(API_PARAM_METADATA, mMetaData);
        networkReadyMap.put(API_PARAM_TOKEN, mToken);
        networkReadyMap.put(API_PARAM_USAGE, mUsage);
        if (mExtraParams != null) {
            networkReadyMap.putAll(mExtraParams);
        }
        removeNullAndEmptyParams(networkReadyMap);
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
}
