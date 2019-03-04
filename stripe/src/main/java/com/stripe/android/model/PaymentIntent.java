package com.stripe.android.model;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.stripe.android.StripeNetworkUtils;
import com.stripe.android.utils.ObjectUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.AbstractMap;
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
    private static final String VALUE_PAYMENT_INTENT = "payment_intent";

    private static final String FIELD_ID = "id";
    private static final String FIELD_OBJECT = "object";
    private static final String FIELD_ALLOWED_SOURCE_TYPES = "allowed_source_types";
    private static final String FIELD_AMOUNT = "amount";
    private static final String FIELD_CREATED = "created";
    private static final String FIELD_CANCELED = "canceled_at";
    private static final String FIELD_CAPTURE_METHOD = "capture_method";
    private static final String FIELD_CLIENT_SECRET = "client_secret";
    private static final String FIELD_CONFIRMATION_METHOD = "confirmation_method";
    private static final String FIELD_CURRENCY = "currency";
    private static final String FIELD_DESCRIPTION = "description";
    private static final String FIELD_LIVEMODE = "livemode";
    private static final String FIELD_NEXT_SOURCE_ACTION = "next_source_action";
    private static final String FIELD_RECEIPT_EMAIL = "receipt_email";
    private static final String FIELD_SOURCE = "source";
    private static final String FIELD_STATUS = "status";

    @Nullable private final String mId;
    @Nullable private final String mObjectType;
    @NonNull private final List<String> mAllowedSourceTypes;
    @Nullable private final Long mAmount;
    @Nullable private final Long mCanceledAt;
    @Nullable private final String mCaptureMethod;
    @Nullable private final String mClientSecret;
    @Nullable private final String mConfirmationMethod;
    @Nullable private final Long mCreated;
    @Nullable private final String mCurrency;
    @Nullable private final String mDescription;
    @Nullable private final Boolean mLiveMode;
    @Nullable private final Map<String, Object> mNextSourceAction;
    @Nullable private final String mReceiptEmail;
    @Nullable private final String mSource;
    @Nullable private final String mStatus;

    @Nullable
    public String getId() {
        return mId;
    }

    @NonNull
    public List<String> getAllowedSourceTypes() {
        return mAllowedSourceTypes;
    }

    @Nullable
    public Long getAmount() {
        return mAmount;
    }

    @Nullable
    public Long getCanceledAt() {
        return mCanceledAt;
    }

    @Nullable
    public String getCaptureMethod() {
        return mCaptureMethod;
    }

    @Nullable
    public String getClientSecret() {
        return mClientSecret;
    }

    @Nullable
    public String getConfirmationMethod() {
        return mConfirmationMethod;
    }

    @Nullable
    public Long getCreated() {
        return mCreated;
    }

    @Nullable
    public String getCurrency() {
        return mCurrency;
    }

    @Nullable
    public String getDescription() {
        return mDescription;
    }

    @Nullable
    public Boolean isLiveMode() {
        return mLiveMode;
    }

    @Nullable
    public Map<String, Object> getNextSourceAction() {
        return mNextSourceAction;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public Uri getAuthorizationUrl() {
        if ("requires_source_action".equals(mStatus) &&
                mNextSourceAction != null &&
                mNextSourceAction.containsKey("authorize_with_url") &&
                mNextSourceAction.get("authorize_with_url") instanceof Map) {
            final Map<String, Object> authorizeWithUrlMap =
                    (Map) mNextSourceAction.get("authorize_with_url");
            if (authorizeWithUrlMap != null &&
                    authorizeWithUrlMap.containsKey("url") &&
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

    @Nullable
    public String getStatus() {
        return mStatus;
    }

    private PaymentIntent(
            @Nullable String id,
            @Nullable String objectType,
            @NonNull List<String> allowedSourceTypes,
            @Nullable Long amount,
            @Nullable Long canceledAt,
            @Nullable String captureMethod,
            @Nullable String clientSecret,
            @Nullable String confirmationMethod,
            @Nullable Long created,
            @Nullable String currency,
            @Nullable String description,
            @Nullable Boolean liveMode,
            @Nullable Map<String, Object> nextSourceAction,
            @Nullable String receiptEmail,
            @Nullable String source,
            @Nullable String status
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

    @NonNull
    public static String parseIdFromClientSecret(@NonNull String clientSecret) {
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
        final List<String> allowedSourceTypes = new ArrayList<>();
        if (allowedSourceTypesJSONArray != null) {
            for (int i = 0; i < allowedSourceTypesJSONArray.length(); i++) {
                try {
                    allowedSourceTypes.add(allowedSourceTypesJSONArray.getString(i));
                } catch (JSONException ignored) {
                }
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
        final JSONObject jsonObject = new JSONObject();
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
        final AbstractMap<String, Object> map = new HashMap<>();
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

    @Override
    public boolean equals(@Nullable Object obj) {
        return this == obj || (obj instanceof PaymentIntent && typedEquals((PaymentIntent) obj));
    }

    private boolean typedEquals(@NonNull PaymentIntent paymentIntent) {
        return ObjectUtils.equals(mId, paymentIntent.mId)
                && ObjectUtils.equals(mObjectType, paymentIntent.mObjectType)
                && ObjectUtils.equals(mAllowedSourceTypes, paymentIntent.mAllowedSourceTypes)
                && ObjectUtils.equals(mAmount, paymentIntent.mAmount)
                && ObjectUtils.equals(mCanceledAt, paymentIntent.mCanceledAt)
                && ObjectUtils.equals(mCaptureMethod, paymentIntent.mCaptureMethod)
                && ObjectUtils.equals(mClientSecret, paymentIntent.mClientSecret)
                && ObjectUtils.equals(mConfirmationMethod, paymentIntent.mConfirmationMethod)
                && ObjectUtils.equals(mCreated, paymentIntent.mCreated)
                && ObjectUtils.equals(mCurrency, paymentIntent.mCurrency)
                && ObjectUtils.equals(mDescription, paymentIntent.mDescription)
                && ObjectUtils.equals(mLiveMode, paymentIntent.mLiveMode)
                && ObjectUtils.equals(mNextSourceAction, paymentIntent.mNextSourceAction)
                && ObjectUtils.equals(mReceiptEmail, paymentIntent.mReceiptEmail)
                && ObjectUtils.equals(mSource, paymentIntent.mSource)
                && ObjectUtils.equals(mStatus, paymentIntent.mStatus);
    }

    @Override
    public int hashCode() {
        return ObjectUtils.hash(mId, mObjectType, mAllowedSourceTypes, mAmount, mCanceledAt,
                mCaptureMethod, mClientSecret, mConfirmationMethod, mCreated, mCurrency,
                mDescription, mLiveMode, mNextSourceAction, mReceiptEmail, mSource, mStatus);
    }
}
