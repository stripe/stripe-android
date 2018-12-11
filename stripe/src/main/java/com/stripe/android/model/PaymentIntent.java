package com.stripe.android.model;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.stripe.android.StripeNetworkUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.stripe.android.model.StripeJsonUtils.listToJsonArray;
import static com.stripe.android.model.StripeJsonUtils.optBoolean;
import static com.stripe.android.model.StripeJsonUtils.optCurrency;
import static com.stripe.android.model.StripeJsonUtils.optLong;
import static com.stripe.android.model.StripeJsonUtils.optMap;
import static com.stripe.android.model.StripeJsonUtils.optString;
import static com.stripe.android.model.StripeJsonUtils.putArrayIfNotNull;
import static com.stripe.android.model.StripeJsonUtils.putBooleanIfNotNull;
import static com.stripe.android.model.StripeJsonUtils.putLongIfNotNull;
import static com.stripe.android.model.StripeJsonUtils.putMapIfNotNull;
import static com.stripe.android.model.StripeJsonUtils.putStringIfNotNull;

public class PaymentIntent extends StripeJsonModel {
    static final String VALUE_PAYMENT_INTENT = "payment_intent";

    static final String FIELD_ID = "id";
    static final String FIELD_OBJECT = "object";
    static final String FIELD_ALLOWED_SOURCE_TYPES = "allowed_source_types";
    static final String FIELD_AMOUNT = "amount";
    static final String FIELD_CREATED = "created";
    static final String FIELD_CANCELED = "canceled_at";
    static final String FIELD_CAPTURE_METHOD = "capture_method";
    static final String FIELD_CLIENT_SECRET = "client_secret";
    static final String FIELD_CONFIRMATION_METHOD = "confirmation_method";
    static final String FIELD_CURRENCY = "currency";
    static final String FIELD_DESCRIPTION = "description";
    static final String FIELD_LIVEMODE = "livemode";
    static final String FIELD_NEXT_SOURCE_ACTION = "next_source_action";
    static final String FIELD_RECEIPT_EMAIL = "receipt_email";
    static final String FIELD_SOURCE = "source";
    static final String FIELD_STATUS = "status";

    private String mId;
    private String mObjectType;
    private List<String> mAllowedSourceTypes;
    private Long mAmount;
    private Long mCanceledAt;
    private String mCaptureMethod;
    private String mClientSecret;
    private String mConfirmationMethod;
    private Long mCreated;
    private String mCurrency;
    private String mDescription;
    private Boolean mLiveMode;
    private Map<String, Object> mNextSourceAction;
    private String mReceiptEmail;
    private String mSource;
    private String mStatus;

    public String getId() {
        return mId;
    }

    public List<String> getAllowedSourceTypes() {
        return mAllowedSourceTypes;
    }

    public Long getAmount() {
        return mAmount;
    }

    public Long getCanceledAt() {
        return mCanceledAt;
    }

    public String getCaptureMethod() {
        return mCaptureMethod;
    }

    public String getClientSecret() {
        return mClientSecret;
    }

    public String getConfirmationMethod() {
        return mConfirmationMethod;
    }

    public Long getCreated() {
        return mCreated;
    }

    public String getCurrency() {
        return mCurrency;
    }

    public String getDescription() {
        return mDescription;
    }

    public Boolean isLiveMode() {
        return mLiveMode;
    }

    public Map<String, Object> getNextSourceAction() {
        return mNextSourceAction;
    }

    public Uri getAuthorizationUrl() {
        if ("requires_source_action".equals(mStatus) &&
                mNextSourceAction.containsKey("authorize_with_url") &&
                mNextSourceAction.get("authorize_with_url") instanceof Map) {
            Map<String, Object> authorizeWithUrlMap =
                    (Map) mNextSourceAction.get("authorize_with_url");
            if (authorizeWithUrlMap.containsKey("url") &&
                    authorizeWithUrlMap.get("url") instanceof String) {
                return Uri.parse((String) authorizeWithUrlMap.get("url"));
            }
        }
        return null;
    }

    @Nullable
    public String getReceiptEmail() {
        return mReceiptEmail;
    }

    @Nullable
    public String getSource() {
        return mSource;
    }

    @NonNull
    public String getStatus() {
        return mStatus;
    }

    PaymentIntent(
            String id,
            String objectType,
            List<String> allowedSourceTypes,
            Long amount,
            Long canceledAt,
            String captureMethod,
            String clientSecret,
            String confirmationMethod,
            Long created,
            String currency,
            String description,
            Boolean liveMode,
            Map<String, Object> nextSourceAction,
            String receiptEmail,
            String source,
            String status
    ) {
        mId = id;
        mObjectType = objectType;
        mAllowedSourceTypes = allowedSourceTypes;
        mAmount = amount;
        mCanceledAt = canceledAt;
        mCaptureMethod = captureMethod;
        mClientSecret = clientSecret;
        mConfirmationMethod = confirmationMethod;
        mCreated = created;
        mCurrency = currency;
        mDescription = description;
        mLiveMode = liveMode;
        mNextSourceAction = nextSourceAction;
        mReceiptEmail = receiptEmail;
        mSource = source;
        mStatus = status;
    }

    public static String parseIdFromClientSecret(String clientSecret) {
        return clientSecret.split("_secret")[0];
    }

    @Nullable
    public static PaymentIntent fromString(@Nullable String jsonString) {
        try {
            return fromJson(new JSONObject(jsonString));
        } catch (JSONException ignored) {
            return null;
        }
    }

    @Nullable
    public static PaymentIntent fromJson(@Nullable JSONObject jsonObject) {
        if (jsonObject == null ||
                !VALUE_PAYMENT_INTENT.equals(jsonObject.optString(FIELD_OBJECT))) {
            return null;
        }
        String id = optString(jsonObject, FIELD_ID);
        String objectType = optString(jsonObject, FIELD_OBJECT);
        JSONArray allowedSourceTypesJSONArray = jsonObject.optJSONArray(FIELD_ALLOWED_SOURCE_TYPES);
        List<String> allowedSourceTypes = new ArrayList<>();
        for (int i = 0; i < allowedSourceTypesJSONArray.length(); i++) {
            try {
                allowedSourceTypes.add(allowedSourceTypesJSONArray.getString(i));
            } catch (JSONException ignored) {
            }
        }
        Long amount = optLong(jsonObject, FIELD_AMOUNT);
        Long canceledAt = optLong(jsonObject, FIELD_CANCELED);
        String captureMethod = optString(jsonObject, FIELD_CAPTURE_METHOD);
        String clientSecret = optString(jsonObject, FIELD_CLIENT_SECRET);
        String confirmationMethod = optString(jsonObject, FIELD_CONFIRMATION_METHOD);
        Long created = optLong(jsonObject, FIELD_CREATED);
        String currency = optCurrency(jsonObject, FIELD_CURRENCY);
        String description = optString(jsonObject, FIELD_DESCRIPTION);
        Boolean livemode = optBoolean(jsonObject, FIELD_LIVEMODE);
        String receiptEmail = optString(jsonObject, FIELD_RECEIPT_EMAIL);
        String status = optString(jsonObject, FIELD_STATUS);
        Map<String, Object> nextSourceAction = optMap(jsonObject, FIELD_NEXT_SOURCE_ACTION);
        String source = optString(jsonObject, FIELD_SOURCE);

        return new PaymentIntent(
                id,
                objectType,
                allowedSourceTypes,
                amount,
                canceledAt,
                captureMethod,
                clientSecret,
                confirmationMethod,
                created,
                currency,
                description,
                livemode,
                nextSourceAction,
                receiptEmail,
                source,
                status);
    }

    @NonNull
    @Override
    public JSONObject toJson() {
        JSONObject jsonObject = new JSONObject();
        putStringIfNotNull(jsonObject, FIELD_ID, mId);
        putStringIfNotNull(jsonObject, FIELD_OBJECT, mObjectType);
        putArrayIfNotNull(jsonObject, FIELD_ALLOWED_SOURCE_TYPES,
                listToJsonArray(mAllowedSourceTypes));
        putLongIfNotNull(jsonObject, FIELD_AMOUNT, mAmount);
        putLongIfNotNull(jsonObject, FIELD_CANCELED, mCanceledAt);
        putStringIfNotNull(jsonObject, FIELD_CAPTURE_METHOD, mCaptureMethod);
        putStringIfNotNull(jsonObject, FIELD_CLIENT_SECRET, mClientSecret);
        putStringIfNotNull(jsonObject, FIELD_CONFIRMATION_METHOD, mConfirmationMethod);
        putLongIfNotNull(jsonObject, FIELD_CREATED, mCreated);
        putStringIfNotNull(jsonObject, FIELD_CURRENCY, mCurrency);
        putStringIfNotNull(jsonObject, FIELD_DESCRIPTION, mDescription);
        putBooleanIfNotNull(jsonObject, FIELD_LIVEMODE, mLiveMode);
        putMapIfNotNull(jsonObject, FIELD_NEXT_SOURCE_ACTION, mNextSourceAction);
        putStringIfNotNull(jsonObject, FIELD_RECEIPT_EMAIL, mReceiptEmail);
        putStringIfNotNull(jsonObject, FIELD_SOURCE, mSource);
        putStringIfNotNull(jsonObject, FIELD_STATUS, mStatus);
        return jsonObject;
    }

    @NonNull
    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put(FIELD_ID, mId);
        map.put(FIELD_OBJECT, mObjectType);
        map.put(FIELD_ALLOWED_SOURCE_TYPES, mAllowedSourceTypes);
        map.put(FIELD_AMOUNT, mAmount);
        map.put(FIELD_CANCELED, mCanceledAt);
        map.put(FIELD_CLIENT_SECRET, mClientSecret);
        map.put(FIELD_CAPTURE_METHOD, mCaptureMethod);
        map.put(FIELD_CONFIRMATION_METHOD, mConfirmationMethod);
        map.put(FIELD_CREATED, mCreated);
        map.put(FIELD_CURRENCY, mCurrency);
        map.put(FIELD_DESCRIPTION, mDescription);
        map.put(FIELD_LIVEMODE, mLiveMode);
        map.put(FIELD_NEXT_SOURCE_ACTION, mNextSourceAction);
        map.put(FIELD_RECEIPT_EMAIL, mReceiptEmail);
        map.put(FIELD_STATUS, mStatus);
        map.put(FIELD_SOURCE, mSource);
        StripeNetworkUtils.removeNullAndEmptyParams(map);
        return map;
    }

}
