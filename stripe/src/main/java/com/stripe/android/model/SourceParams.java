package com.stripe.android.model;

import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.Size;

import com.stripe.android.utils.ObjectUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static com.stripe.android.StripeNetworkUtils.removeNullAndEmptyParams;
import static com.stripe.android.model.Source.SourceType;

/**
 * Represents a grouping of parameters needed to create a {@link Source} object on the server.
 */
public final class SourceParams implements StripeParamsModel {

    private static final String API_PARAM_AMOUNT = "amount";
    private static final String API_PARAM_CURRENCY = "currency";
    private static final String API_PARAM_METADATA = "metadata";
    private static final String API_PARAM_OWNER = "owner";
    private static final String API_PARAM_REDIRECT = "redirect";
    private static final String API_PARAM_TYPE = "type";
    private static final String API_PARAM_TOKEN = "token";
    private static final String API_PARAM_USAGE = "usage";
    private static final String API_PARAM_WECHAT = "wechat";

    private static final String API_PARAM_CLIENT_SECRET = "client_secret";

    private static final String FIELD_ADDRESS = "address";
    private static final String FIELD_BANK = "bank";
    private static final String FIELD_CARD = "card";
    private static final String FIELD_CITY = "city";
    private static final String FIELD_COUNTRY = "country";
    private static final String FIELD_CVC = "cvc";
    private static final String FIELD_EMAIL = "email";
    private static final String FIELD_EXP_MONTH = "exp_month";
    private static final String FIELD_EXP_YEAR = "exp_year";
    private static final String FIELD_IBAN = "iban";
    private static final String FIELD_LINE_1 = "line1";
    private static final String FIELD_LINE_2 = "line2";
    private static final String FIELD_NAME = "name";
    private static final String FIELD_NUMBER = "number";
    private static final String FIELD_PHONE = "phone";
    private static final String FIELD_POSTAL_CODE = "postal_code";
    private static final String FIELD_RETURN_URL = "return_url";
    private static final String FIELD_STATE = "state";
    private static final String FIELD_STATEMENT_DESCRIPTOR = "statement_descriptor";
    private static final String FIELD_PREFERRED_LANGUAGE = "preferred_language";

    private static final String VISA_CHECKOUT = "visa_checkout";
    private static final String CALL_ID = "callid";

    private static final String MASTERPASS = "masterpass";
    private static final String TRANSACTION_ID = "transaction_id";
    private static final String CART_ID = "cart_id";

    @NonNull @SourceType private final String mType;
    @NonNull private final String mTypeRaw;

    @Nullable @IntRange(from = 0) private Long mAmount;
    @Nullable private Map<String, Object> mApiParameterMap;
    @Nullable private String mCurrency;
    @Nullable private Map<String, Object> mOwner;
    @Nullable private Map<String, String> mMetaData;
    @Nullable private Map<String, Object> mRedirect;
    @Nullable private Map<String, Object> mExtraParams;
    @Nullable private String mToken;
    @Nullable @Source.Usage private String mUsage;
    @Nullable private WeChatParams mWeChatParams;

    private SourceParams(@NonNull @SourceType String typeRaw) {
        mTypeRaw = Objects.requireNonNull(typeRaw);
        mType = Source.asSourceType(typeRaw);
    }

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
    @NonNull
    public static SourceParams createP24Params(
            @IntRange(from = 0) long amount,
            @NonNull String currency,
            @Nullable String name,
            @NonNull String email,
            @NonNull String returnUrl) {
        final SourceParams params = new SourceParams(SourceType.P24)
                .setAmount(amount)
                .setCurrency(currency)
                .setRedirect(createSimpleMap(FIELD_RETURN_URL, returnUrl));

        final AbstractMap<String, Object> ownerMap = new HashMap<>();
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
    @NonNull
    public static SourceParams createAlipayReusableParams(
            @NonNull String currency,
            @Nullable String name,
            @Nullable String email,
            @NonNull String returnUrl) {
        final SourceParams params = new SourceParams(SourceType.ALIPAY)
                .setCurrency(currency)
                .setRedirect(createSimpleMap(FIELD_RETURN_URL, returnUrl))
                .setUsage(Source.Usage.REUSABLE);

        final AbstractMap<String, Object> ownerMap = new HashMap<>();
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
        final SourceParams params = new SourceParams(SourceType.ALIPAY)
                .setCurrency(currency)
                .setAmount(amount)
                .setRedirect(createSimpleMap(FIELD_RETURN_URL, returnUrl));

        final AbstractMap<String, Object> ownerMap = new HashMap<>();
        ownerMap.put(FIELD_NAME, name);
        ownerMap.put(FIELD_EMAIL, email);
        removeNullAndEmptyParams(ownerMap);
        if (ownerMap.keySet().size() > 0) {
            params.setOwner(ownerMap);
        }

        return params;
    }

    @NonNull
    public static SourceParams createWeChatPayParams(
            @IntRange(from = 0) long amount,
            @NonNull String currency,
            @NonNull String weChatAppId,
            @NonNull String statementDescriptor) {
        return new SourceParams(SourceType.WECHAT)
                .setCurrency(currency)
                .setAmount(amount)
                .setWeChatParams(new WeChatParams(weChatAppId, statementDescriptor));
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
        final SourceParams params = new SourceParams(SourceType.BANCONTACT)
                .setCurrency(Source.EURO)
                .setAmount(amount)
                .setOwner(createSimpleMap(FIELD_NAME, name))
                .setRedirect(createSimpleMap(FIELD_RETURN_URL, returnUrl));

        if (statementDescriptor != null || preferredLanguage != null) {
            final AbstractMap<String, Object> additionalParamsMap = new HashMap<>();

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
     * @param type a custom type
     * @return an empty {@link SourceParams} object.
     */
    @NonNull
    public static SourceParams createCustomParams(@NonNull String type) {
        return new SourceParams(type);
    }

    /**
     * Create parameters necessary for converting a token into a source
     *
     * @param tokenId the id of the {@link Token} to be converted into a source.
     * @return a {@link SourceParams} object that can be used to create a source.
     */
    @NonNull
    public static SourceParams createSourceFromTokenParams(@NonNull String tokenId) {
        return new SourceParams(SourceType.CARD)
                .setToken(tokenId);
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
        final SourceParams params = new SourceParams(SourceType.CARD);

        // Not enforcing all fields to exist at this level.
        // Instead, the server will return an error for invalid data.
        final AbstractMap<String, Object> basicInfoMap = new HashMap<>();
        basicInfoMap.put(FIELD_NUMBER, card.getNumber());
        basicInfoMap.put(FIELD_EXP_MONTH, card.getExpMonth());
        basicInfoMap.put(FIELD_EXP_YEAR, card.getExpYear());
        basicInfoMap.put(FIELD_CVC, card.getCVC());
        removeNullAndEmptyParams(basicInfoMap);

        params.setApiParameterMap(basicInfoMap);

        final AbstractMap<String, Object> addressMap = new HashMap<>();
        addressMap.put(FIELD_LINE_1, card.getAddressLine1());
        addressMap.put(FIELD_LINE_2, card.getAddressLine2());
        addressMap.put(FIELD_CITY, card.getAddressCity());
        addressMap.put(FIELD_COUNTRY, card.getAddressCountry());
        addressMap.put(FIELD_STATE, card.getAddressState());
        addressMap.put(FIELD_POSTAL_CODE, card.getAddressZip());
        removeNullAndEmptyParams(addressMap);

        // If there are any keys left...
        final AbstractMap<String, Object> ownerMap = new HashMap<>();
        ownerMap.put(FIELD_NAME, card.getName());
        if (addressMap.keySet().size() > 0) {
            ownerMap.put(FIELD_ADDRESS, addressMap);
        }
        removeNullAndEmptyParams(ownerMap);
        if (ownerMap.keySet().size() > 0) {
            params.setOwner(ownerMap);
        }

        final Map<String, String> metadata = card.getMetadata();
        if (metadata != null) {
            params.setMetaData(metadata);
        }

        return params;
    }

    /**
     * @param googlePayPaymentData a {@link JSONObject} derived from Google Pay's
     *                             <a href="https://developers.google.com/pay/api/android/reference/client#tojson">PaymentData#toJson()</a>
     */
    @NonNull
    public static SourceParams createCardParamsFromGooglePay(
            @NonNull JSONObject googlePayPaymentData)
            throws JSONException {
        final JSONObject paymentMethodData = googlePayPaymentData
                .getJSONObject("paymentMethodData");
        final JSONObject googlePayBillingAddress = paymentMethodData
                .getJSONObject("info")
                .optJSONObject("billingAddress");
        final String paymentToken = paymentMethodData
                .getJSONObject("tokenizationData")
                .getString("token");
        final Token stripeToken = Token.fromJson(new JSONObject(paymentToken));
        final String stripeTokenId = Objects.requireNonNull(stripeToken).getId();

        final SourceParams params = new SourceParams(SourceType.CARD)
                .setToken(stripeTokenId);
        final Map<String, Object> addressMap;
        final String phone;
        final String name;
        if (googlePayBillingAddress != null) {
            name = googlePayBillingAddress.optString("name");
            phone = googlePayBillingAddress.optString("phoneNumber");
            addressMap = new HashMap<>();
            addressMap.put(FIELD_LINE_1,
                    googlePayBillingAddress.optString("address1"));
            addressMap.put(FIELD_LINE_2,
                    googlePayBillingAddress.optString("address2"));
            addressMap.put(FIELD_CITY,
                    googlePayBillingAddress.optString("locality"));
            addressMap.put(FIELD_COUNTRY,
                    googlePayBillingAddress.optString("countryCode"));
            addressMap.put(FIELD_STATE,
                    googlePayBillingAddress.optString("administrativeArea"));
            addressMap.put(FIELD_POSTAL_CODE,
                    googlePayBillingAddress.optString("postalCode"));
            removeNullAndEmptyParams(addressMap);
        } else {
            name = null;
            phone = null;
            addressMap = null;
        }

        final Map<String, Object> ownerMap = new HashMap<>();
        ownerMap.put(FIELD_EMAIL, googlePayPaymentData.optString("email"));
        if (name != null) {
            ownerMap.put(FIELD_NAME, name);
        }
        if (phone != null) {
            ownerMap.put(FIELD_PHONE, phone);
        }
        if (addressMap != null && !addressMap.isEmpty()) {
            ownerMap.put(FIELD_ADDRESS, addressMap);
        }
        removeNullAndEmptyParams(ownerMap);
        if (!ownerMap.isEmpty()) {
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
        final SourceParams params = new SourceParams(SourceType.EPS)
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
        final SourceParams params = new SourceParams(SourceType.GIROPAY)
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
        final SourceParams params = new SourceParams(SourceType.IDEAL)
                .setCurrency(Source.EURO)
                .setAmount(amount)
                .setRedirect(createSimpleMap(FIELD_RETURN_URL, returnUrl));
        if (name != null) {
            params.setOwner(createSimpleMap(FIELD_NAME, name));
        }
        final Map<String, Object> additionalParamsMap = new HashMap<>();
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
        return new SourceParams(SourceType.MULTIBANCO)
                .setCurrency(Source.EURO)
                .setAmount(amount)
                .setRedirect(createSimpleMap(FIELD_RETURN_URL, returnUrl))
                .setOwner(createSimpleMap(FIELD_EMAIL, email));
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
        final SourceParams params = new SourceParams(SourceType.SEPA_DEBIT)
                .setCurrency(Source.EURO);

        final AbstractMap<String, Object> address = new HashMap<>();
        address.put(FIELD_LINE_1, addressLine1);
        address.put(FIELD_CITY, city);
        address.put(FIELD_POSTAL_CODE, postalCode);
        address.put(FIELD_COUNTRY, country);

        final AbstractMap<String, Object> ownerMap = new HashMap<>();
        ownerMap.put(FIELD_NAME, name);
        ownerMap.put(FIELD_EMAIL, email);
        ownerMap.put(FIELD_ADDRESS, address);

        return params
                .setOwner(ownerMap)
                .setApiParameterMap(createSimpleMap(FIELD_IBAN, iban));
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
        final SourceParams params = new SourceParams(SourceType.SOFORT)
                .setCurrency(Source.EURO)
                .setAmount(amount)
                .setRedirect(createSimpleMap(FIELD_RETURN_URL, returnUrl));

        final Map<String, Object> sofortMap = createSimpleMap(FIELD_COUNTRY, country);
        if (statementDescriptor != null) {
            sofortMap.put(FIELD_STATEMENT_DESCRIPTOR, statementDescriptor);
        }

        return params
                .setApiParameterMap(sofortMap);
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
        return new SourceParams(SourceType.THREE_D_SECURE)
                .setCurrency(currency)
                .setAmount(amount)
                .setRedirect(createSimpleMap(FIELD_RETURN_URL, returnUrl))
                .setApiParameterMap(createSimpleMap(FIELD_CARD, cardID));
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
    @NonNull
    public static SourceParams createVisaCheckoutParams(@NonNull String callId) {
        return new SourceParams(SourceType.CARD)
                .setApiParameterMap(
                        createSimpleMap(VISA_CHECKOUT, createSimpleMap(CALL_ID, callId)));
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
    @NonNull
    public static SourceParams createMasterpassParams(
            @NonNull String transactionId,
            @NonNull String cartID) {
        final Map<String, Object> map = createSimpleMap(TRANSACTION_ID, transactionId);
        map.put(CART_ID, cartID);

        return new SourceParams(SourceType.CARD)
                .setApiParameterMap(createSimpleMap(MASTERPASS, map));
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
    public static Map<String, String> createRetrieveSourceParams(
            @NonNull @Size(min = 1) String clientSecret) {
        final Map<String, String> params = new HashMap<>();
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
    @Nullable
    public Map<String, String> getMetaData() {
        return mMetaData;
    }

    /*---- Setters ----*/

    /**
     * @param amount currency amount for this source, in the lowest denomination.
     * @return {@code this}, for chaining purposes
     */
    @NonNull
    public SourceParams setAmount(long amount) {
        mAmount = amount;
        return this;
    }

    /**
     * @param apiParameterMap a map of parameters specific for this type of source
     * @return {@code this}, for chaining purposes
     */
    @NonNull
    public SourceParams setApiParameterMap(
            @NonNull Map<String, Object> apiParameterMap) {
        mApiParameterMap = apiParameterMap;
        return this;
    }

    /**
     * @param currency currency code for this source (i.e. "EUR")
     * @return {@code this}, for chaining purposes
     */
    @NonNull
    public SourceParams setCurrency(String currency) {
        mCurrency = currency;
        return this;
    }

    /**
     * @param owner an {@link SourceOwner} object for this source
     * @return {@code this}, for chaining purposes
     */
    @NonNull
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
    @NonNull
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
    @NonNull
    public SourceParams setExtraParams(final Map<String, Object> extraParams) {
        mExtraParams = extraParams;
        return this;
    }

    /**
     * @param returnUrl a redirect URL for this source.
     * @return {@code this}, for chaining purposes
     */
    @NonNull
    public SourceParams setReturnUrl(@NonNull @Size(min = 1) String returnUrl) {
        if (mRedirect == null) {
            setRedirect(createSimpleMap(FIELD_RETURN_URL, returnUrl));
        } else {
            mRedirect.put(FIELD_RETURN_URL, returnUrl);
        }
        return this;
    }

    /**
     * Set custom metadata on the parameters.
     *
     * @return {@code this}, for chaining purposes
     */
    @NonNull
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
    @NonNull
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
    @NonNull
    public SourceParams setUsage(@NonNull @Source.Usage String usage) {
        mUsage = usage;
        return this;
    }

    @NonNull
    public SourceParams setWeChatParams(@NonNull WeChatParams weChatParams) {
        mWeChatParams = weChatParams;
        return this;
    }

    /**
     * Create a string-keyed map representing this object that is
     * ready to be sent over the network.
     *
     * @return a String-keyed map
     */
    @NonNull
    @Override
    public Map<String, Object> toParamMap() {
        final AbstractMap<String, Object> networkReadyMap = new HashMap<>();

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
        if (mWeChatParams != null) {
            networkReadyMap.put(API_PARAM_WECHAT, mWeChatParams.toParamMap());
        }
        removeNullAndEmptyParams(networkReadyMap);
        return networkReadyMap;
    }

    @NonNull
    private static <T> Map<String, Object> createSimpleMap(@NonNull String key, @NonNull T value) {
        final Map<String, Object> simpleMap = new HashMap<>();
        simpleMap.put(key, value);
        return simpleMap;
    }

    @NonNull
    private static <T> Map<String, Object> createSimpleMap(
            @NonNull String key1, @NonNull T value1,
            @NonNull String key2, @NonNull T value2) {
        final Map<String, Object> simpleMap = new HashMap<>();
        simpleMap.put(key1, value1);
        simpleMap.put(key2, value2);
        return simpleMap;
    }

    @Override
    public int hashCode() {
        return ObjectUtils.hash(mAmount, mApiParameterMap, mCurrency, mTypeRaw, mOwner, mMetaData,
                mRedirect, mExtraParams, mToken, mUsage, mType);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return this == obj || (obj instanceof SourceParams && typedEquals((SourceParams) obj));
    }

    private boolean typedEquals(@NonNull SourceParams params) {
        return ObjectUtils.equals(mAmount, params.mAmount) &&
                ObjectUtils.equals(mApiParameterMap, params.mApiParameterMap) &&
                ObjectUtils.equals(mCurrency, params.mCurrency) &&
                ObjectUtils.equals(mTypeRaw, params.mTypeRaw) &&
                ObjectUtils.equals(mOwner, params.mOwner) &&
                ObjectUtils.equals(mMetaData, params.mMetaData) &&
                ObjectUtils.equals(mRedirect, params.mRedirect) &&
                ObjectUtils.equals(mExtraParams, params.mExtraParams) &&
                ObjectUtils.equals(mToken, params.mToken) &&
                ObjectUtils.equals(mUsage, params.mUsage) &&
                ObjectUtils.equals(mType, params.mType);
    }

    static final class WeChatParams implements StripeParamsModel {
        private static final String FIELD_APPID = "appid";
        private static final String FIELD_STATEMENT_DESCRIPTOR = "statement_descriptor";

        @Nullable private final String appId;
        @Nullable private final String statementDescriptor;

        WeChatParams(@Nullable String appId, @Nullable String statementDescriptor) {
            this.appId = appId;
            this.statementDescriptor = statementDescriptor;
        }

        @NonNull
        @Override
        public Map<String, Object> toParamMap() {
            final Map<String, Object> params = new HashMap<>();
            if (appId != null) {
                params.put(FIELD_APPID, appId);
            }
            if (statementDescriptor != null) {
                params.put(FIELD_STATEMENT_DESCRIPTOR, statementDescriptor);
            }
            return params;
        }
    }
}
