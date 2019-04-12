package com.stripe.android.model;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

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
    static final String FIELD_NEXT_ACTION = "next_action";
    static final String FIELD_NEXT_SOURCE_ACTION = "next_source_action";
    static final String FIELD_PAYMENT_METHOD_TYPES = "payment_method_types";
    static final String FIELD_RECEIPT_EMAIL = "receipt_email";
    static final String FIELD_SOURCE = "source";
    static final String FIELD_STATUS = "status";
    static final String FIELD_TYPE = "type";

    @Nullable private final String mId;
    @Nullable private final String mObjectType;
    @NonNull private final List<String> mAllowedSourceTypes;
    @NonNull private final List<String> mPaymentMethodTypes;
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
    @Nullable private final Map<String, Object> mNextAction;

    @Nullable private final String mReceiptEmail;
    @Nullable private final String mSource;
    @Nullable private final String mStatus;

    @Nullable
    public String getId() {
        return mId;
    }

    /**
     * @deprecated use {@link #getPaymentMethodTypes()}
     */
    @Deprecated
    @NonNull
    public List<String> getAllowedSourceTypes() {
        return mAllowedSourceTypes;
    }

    @NonNull
    public List<String> getPaymentMethodTypes() {
        return mPaymentMethodTypes;
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

    /**
     * @deprecated use {@link #getNextAction()}
     */
    @Deprecated
    @Nullable
    public Map<String, Object> getNextSourceAction() {
        return mNextSourceAction;
    }

    @Nullable
    public Map<String, Object> getNextAction() {
        return mNextAction;
    }

    @Nullable
    public Uri getRedirectUrl() {
        final Map<String, Object> nextAction;

        final Status status = Status.fromCode(mStatus);
        if (Status.RequiresAction == status) {
            nextAction = mNextAction;
        } else if (Status.RequiresSourceAction == status) {
            nextAction = mNextSourceAction;
        } else {
            nextAction = null;
        }

        if (nextAction == null) {
            return null;
        }

        final NextActionType nextActionType = NextActionType
                .fromCode((String) nextAction.get(FIELD_TYPE));
        if (NextActionType.RedirectToUrl == nextActionType ||
                NextActionType.AuthorizeWithUrl == nextActionType) {
            final Object redirectToUrl = nextAction.get(nextActionType.code);
            if (redirectToUrl instanceof Map) {
                final Object url = ((Map) redirectToUrl).get("url");
                if (url instanceof String) {
                    return Uri.parse((String) url);
                }
            }
        }

        return null;
    }

    /**
     * @deprecated use {@link #getRedirectUrl()}
     */
    @Deprecated
    @Nullable
    public Uri getAuthorizationUrl() {
        return getRedirectUrl();
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
            @NonNull List<String> paymentMethodTypes,
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
            @Nullable Map<String, Object> nextAction,
            @Nullable String receiptEmail,
            @Nullable String source,
            @Nullable String status
    ) {
        mId = id;
        mObjectType = objectType;
        mAllowedSourceTypes = allowedSourceTypes;
        mPaymentMethodTypes = paymentMethodTypes;
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
        mNextAction = nextAction;
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

        final String id = optString(jsonObject, FIELD_ID);
        final String objectType = optString(jsonObject, FIELD_OBJECT);
        final List<String> allowedSourceTypes = jsonArrayToList(
                jsonObject.optJSONArray(FIELD_ALLOWED_SOURCE_TYPES));
        final List<String> paymentMethodTypes = jsonArrayToList(
                jsonObject.optJSONArray(FIELD_PAYMENT_METHOD_TYPES));
        final Long amount = optLong(jsonObject, FIELD_AMOUNT);
        final Long canceledAt = optLong(jsonObject, FIELD_CANCELED);
        final String captureMethod = optString(jsonObject, FIELD_CAPTURE_METHOD);
        final String clientSecret = optString(jsonObject, FIELD_CLIENT_SECRET);
        final String confirmationMethod = optString(jsonObject, FIELD_CONFIRMATION_METHOD);
        final Long created = optLong(jsonObject, FIELD_CREATED);
        final String currency = optCurrency(jsonObject, FIELD_CURRENCY);
        final String description = optString(jsonObject, FIELD_DESCRIPTION);
        final Boolean livemode = optBoolean(jsonObject, FIELD_LIVEMODE);
        final String receiptEmail = optString(jsonObject, FIELD_RECEIPT_EMAIL);
        final String status = optString(jsonObject, FIELD_STATUS);
        final Map<String, Object> nextSourceAction = optMap(jsonObject, FIELD_NEXT_SOURCE_ACTION);
        final Map<String, Object> nextAction = optMap(jsonObject, FIELD_NEXT_ACTION);
        final String source = optString(jsonObject, FIELD_SOURCE);

        return new PaymentIntent(
                id,
                objectType,
                allowedSourceTypes,
                paymentMethodTypes,
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
                nextAction,
                receiptEmail,
                source,
                status);
    }

    @NonNull
    private static List<String> jsonArrayToList(@Nullable JSONArray jsonArray) {
        final List<String> list = new ArrayList<>();
        if (jsonArray != null) {
            for (int i = 0; i < jsonArray.length(); i++) {
                try {
                    list.add(jsonArray.getString(i));
                } catch (JSONException ignored) {
                }
            }
        }

        return list;
    }

    @NonNull
    @Override
    public JSONObject toJson() {
        final JSONObject jsonObject = new JSONObject();
        putStringIfNotNull(jsonObject, FIELD_ID, mId);
        putStringIfNotNull(jsonObject, FIELD_OBJECT, mObjectType);
        putArrayIfNotNull(jsonObject, FIELD_ALLOWED_SOURCE_TYPES,
                listToJsonArray(mAllowedSourceTypes));
        putArrayIfNotNull(jsonObject, FIELD_PAYMENT_METHOD_TYPES,
                listToJsonArray(mPaymentMethodTypes));
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
        putMapIfNotNull(jsonObject, FIELD_NEXT_ACTION, mNextAction);
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
        map.put(FIELD_PAYMENT_METHOD_TYPES, mPaymentMethodTypes);
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
        map.put(FIELD_NEXT_ACTION, mNextAction);
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

    public enum NextActionType {
        RedirectToUrl("redirect_to_url"),
        AuthorizeWithUrl("authorize_with_url");

        @NonNull public final String code;

        NextActionType(@NonNull String code) {
            this.code = code;
        }

        @Nullable
        public static NextActionType fromCode(@Nullable String code) {
            if (code == null) {
                return null;
            }

            for (NextActionType nextActionType : values()) {
                if (nextActionType.code.equals(code)) {
                    return nextActionType;
                }
            }

            return null;
        }
    }

    /**
     * See https://stripe.com/docs/api/payment_intents/object#payment_intent_object-status
     */
    public enum Status {
        Canceled("canceled"),
        Processing("processing"),
        RequiresAction("requires_action"),
        RequiresAuthorization("requires_authorization"),
        RequiresCapture("requires_capture"),
        RequiresConfirmation("requires_confirmation"),
        RequiresPaymentMethod("requires_payment_method"),
        Succeeded("succeeded"),

        /**
         * @deprecated use {@link #RequiresPaymentMethod}
         */
        @Deprecated
        RequiresSource("requires_source"),

        /**
         * @deprecated use {@link #RequiresAction}
         */
        @Deprecated
        RequiresSourceAction("requires_source_action");

        @NonNull
        public final String code;

        Status(@NonNull String code) {
            this.code = code;
        }

        @Nullable
        public static Status fromCode(@Nullable String code) {
            if (code == null) {
                return null;
            }

            for (Status status : values()) {
                if (status.code.equals(code)) {
                    return status;
                }
            }

            return null;
        }
    }
}
