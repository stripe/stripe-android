package com.stripe.android.model;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.stripe.android.ObjectBuilder;
import com.stripe.android.StripeNetworkUtils;
import com.stripe.android.utils.ObjectUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.stripe.android.model.StripeJsonUtils.optBoolean;
import static com.stripe.android.model.StripeJsonUtils.optLong;
import static com.stripe.android.model.StripeJsonUtils.optMap;
import static com.stripe.android.model.StripeJsonUtils.optString;


/**
 * A SetupIntent guides you through the process of setting up a customer's payment credentials for
 * future payments.
 */
public final class SetupIntent extends StripeModel implements StripeIntent {
    private static final String VALUE_SETUP_INTENT = "setup_intent";

    static final String FIELD_ID = "id";
    static final String FIELD_OBJECT = "object";
    static final String FIELD_CREATED = "created";
    static final String FIELD_CLIENT_SECRET = "client_secret";
    static final String FIELD_CUSTOMER = "customer";
    static final String FIELD_DESCRIPTION = "description";
    static final String FIELD_LIVEMODE = "livemode";
    static final String FIELD_NEXT_ACTION = "next_action";
    static final String FIELD_PAYMENT_METHOD_TYPES = "payment_method_types";
    static final String FIELD_STATUS = "status";
    static final String FIELD_USAGE = "usage";
    static final String FIELD_PAYMENT_METHOD = "payment_method";

    private static final String FIELD_NEXT_ACTION_TYPE = "type";

    @Nullable private final String mId;
    @Nullable private final String mObjectType;
    private final long mCreated;
    @Nullable private final String mClientSecret;
    @Nullable private final String mCustomerId;
    @Nullable private final String mDescription;
    private final boolean mLiveMode;
    @Nullable private final Map<String, Object> mNextAction;
    @Nullable private final NextActionType mNextActionType;
    @Nullable private final String mPaymentMethodId;
    @Nullable private final List<String> mPaymentMethodTypes;
    @Nullable private final Status mStatus;
    @Nullable private final Usage mUsage;

    private SetupIntent(@NonNull Builder builder) {
        mId = builder.mId;
        mObjectType = builder.mObjectType;
        mCreated = builder.mCreated;
        mClientSecret = builder.mClientSecret;
        mCustomerId = builder.mCustomerId;
        mDescription = builder.mDescription;
        mLiveMode = builder.mLiveMode;
        mNextAction = builder.mNextAction;
        mNextActionType = mNextAction != null ?
                NextActionType.fromCode((String) mNextAction.get(FIELD_NEXT_ACTION_TYPE)) : null;
        mPaymentMethodId = builder.mPaymentMethodId;
        mPaymentMethodTypes = builder.mPaymentMethodTypes;
        mStatus = builder.mStatus;
        mUsage = builder.mUsage;
    }



    @NonNull
    public static String parseIdFromClientSecret(@NonNull String clientSecret) {
        return clientSecret.split("_secret")[0];
    }

    @Nullable
    public String getId() {
        return mId;
    }

    public long getCreated() {
        return mCreated;
    }

    @Nullable
    public String getCustomerId() {
        return mCustomerId;
    }

    @Nullable
    public String getDescription() {
        return mDescription;
    }

    public boolean getLiveMode() {
        return mLiveMode;
    }

    @Nullable
    public String getPaymentMethodId() {
        return mPaymentMethodId;
    }

    @NonNull
    public List<String> getPaymentMethodTypes() {
        return mPaymentMethodTypes;
    }

    @Nullable
    public Usage getUsage() {
        return mUsage;
    }

    @Override
    public boolean requiresAction() {
        return mStatus == Status.RequiresAction;
    }

    @Override
    public boolean requiresConfirmation() {
        return mStatus == Status.RequiresConfirmation;
    }

    @Nullable
    @Override
    public NextActionType getNextActionType() {
        return mNextActionType;
    }

    @Nullable
    @Override
    public RedirectData getRedirectData() {
        if (NextActionType.RedirectToUrl != mNextActionType) {
            return null;
        }

        final Map<String, Object> nextAction;

        if (Status.RequiresAction == mStatus) {
            nextAction = mNextAction;
        } else {
            nextAction = null;
        }

        if (nextAction == null) {
            return null;
        }

        final NextActionType nextActionType = NextActionType
                .fromCode((String) nextAction.get(FIELD_NEXT_ACTION_TYPE));
        if (NextActionType.RedirectToUrl == nextActionType) {
            final Object redirectToUrl = nextAction.get(nextActionType.code);
            if (redirectToUrl instanceof Map) {
                return RedirectData.create((Map) redirectToUrl);
            }
        }

        return null;
    }

    @Nullable
    @Override
    public String getClientSecret() {
        return mClientSecret;
    }

    @Nullable
    public Uri getRedirectUrl() {
        final RedirectData redirectData = getRedirectData();
        if (redirectData == null) {
            return null;
        }

        return redirectData.url;
    }

    @Nullable
    @Override
    public SdkData getStripeSdkData() {
        if (mNextAction == null || NextActionType.UseStripeSdk != mNextActionType) {
            return null;
        }

        //noinspection ConstantConditions,unchecked
        return new SdkData((Map<String, ?>) mNextAction.get(NextActionType.UseStripeSdk.code));
    }

    @Nullable
    @Override
    public Status getStatus() {
        return mStatus;
    }

    @Nullable
    public static SetupIntent fromString(@Nullable String jsonString) {
        try {
            return fromJson(new JSONObject(jsonString));
        } catch (JSONException ignored) {
            return null;
        }
    }

    @Nullable
    public static SetupIntent fromJson(@Nullable JSONObject jsonObject) {
        if (jsonObject == null ||
                !VALUE_SETUP_INTENT.equals(jsonObject.optString(FIELD_OBJECT))) {
            return null;
        }

        return new Builder()
                .setId(optString(jsonObject, FIELD_ID))
                .setObjectType(optString(jsonObject, FIELD_OBJECT))
                .setCreated(jsonObject.optLong(FIELD_CREATED))
                .setClientSecret(optString(jsonObject, FIELD_CLIENT_SECRET))
                .setCustomerId(optString(jsonObject, FIELD_CUSTOMER))
                .setDescription(optString(jsonObject, FIELD_DESCRIPTION))
                .setLiveMode(jsonObject.optBoolean(FIELD_LIVEMODE))
                .setPaymentMethodId(optString(jsonObject, FIELD_PAYMENT_METHOD))
                .setPaymentMethodTypes(jsonArrayToList(
                        jsonObject.optJSONArray(FIELD_PAYMENT_METHOD_TYPES)))
                .setStatus(Status.fromCode(optString(jsonObject, FIELD_STATUS)))
                .setUsage(Usage.fromCode(optString(jsonObject, FIELD_USAGE)))
                .setNextAction(optMap(jsonObject, FIELD_NEXT_ACTION))
                .build();
    }

    @NonNull
    @Override
    public Map<String, Object> toMap() {
        final AbstractMap<String, Object> map = new HashMap<>();
        map.put(FIELD_ID, mId);
        map.put(FIELD_OBJECT, mObjectType);
        map.put(FIELD_PAYMENT_METHOD, mPaymentMethodId);
        map.put(FIELD_PAYMENT_METHOD_TYPES, mPaymentMethodTypes);
        map.put(FIELD_CLIENT_SECRET, mClientSecret);
        map.put(FIELD_CREATED, mCreated);
        map.put(FIELD_CUSTOMER, mCustomerId);
        map.put(FIELD_DESCRIPTION, mDescription);
        map.put(FIELD_LIVEMODE, mLiveMode);
        map.put(FIELD_NEXT_ACTION, mNextAction);
        map.put(FIELD_STATUS, mStatus != null ? mStatus.code : null);
        map.put(FIELD_USAGE, mUsage != null ? mUsage.code : null);
        StripeNetworkUtils.removeNullAndEmptyParams(map);
        return map;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return this == obj || (obj instanceof SetupIntent && typedEquals((SetupIntent) obj));
    }

    private boolean typedEquals(@NonNull SetupIntent setupIntent) {
        return ObjectUtils.equals(mId, setupIntent.mId)
                && ObjectUtils.equals(mObjectType, setupIntent.mObjectType)
                && ObjectUtils.equals(mClientSecret, setupIntent.mClientSecret)
                && ObjectUtils.equals(mCreated, setupIntent.mCreated)
                && ObjectUtils.equals(mCustomerId, setupIntent.mCustomerId)
                && ObjectUtils.equals(mDescription, setupIntent.mDescription)
                && ObjectUtils.equals(mLiveMode, setupIntent.mLiveMode)
                && ObjectUtils.equals(mStatus, setupIntent.mStatus)
                && ObjectUtils.equals(mUsage, setupIntent.mUsage)
                && ObjectUtils.equals(mPaymentMethodId, setupIntent.mPaymentMethodId)
                && ObjectUtils.equals(mPaymentMethodTypes, setupIntent.mPaymentMethodTypes)
                && ObjectUtils.equals(mNextAction, setupIntent.mNextAction)
                && ObjectUtils.equals(mNextActionType, setupIntent.mNextActionType);
    }

    @Override
    public int hashCode() {
        return ObjectUtils.hash(mId, mObjectType, mCustomerId, mClientSecret, mCreated,
                mDescription, mLiveMode, mStatus, mPaymentMethodId, mPaymentMethodTypes,
                mNextAction, mNextActionType, mUsage);
    }

    static final class Builder implements ObjectBuilder<SetupIntent> {
        @Nullable String mId;
        @Nullable String mObjectType;
        long mCreated;
        @Nullable String mClientSecret;
        @Nullable String mCustomerId;
        @Nullable String mDescription;
        boolean mLiveMode;
        @Nullable Map<String, Object> mNextAction;
        @Nullable String mPaymentMethodId;
        @Nullable List<String> mPaymentMethodTypes;
        @Nullable Status mStatus;
        @Nullable Usage mUsage;

        @NonNull
        Builder setId(@Nullable String id) {
            mId = id;
            return this;
        }

        @NonNull
        Builder setObjectType(@Nullable String objectType) {
            mObjectType = objectType;
            return this;
        }

        @NonNull
        Builder setCreated(long created) {
            mCreated = created;
            return this;
        }

        @NonNull
        Builder setClientSecret(@Nullable String clientSecret) {
            mClientSecret = clientSecret;
            return this;
        }

        @NonNull
        Builder setCustomerId(@Nullable String customerId) {
            mCustomerId = customerId;
            return this;
        }

        @NonNull
        Builder setDescription(@Nullable String description) {
            mDescription = description;
            return this;
        }

        @NonNull
        Builder setLiveMode(boolean liveMode) {
            mLiveMode = liveMode;
            return this;
        }

        @NonNull
        Builder setNextAction(@Nullable Map<String, Object> nextAction) {
            mNextAction = nextAction;
            return this;
        }

        @NonNull
        Builder setPaymentMethodId(@Nullable String paymentMethodId) {
            mPaymentMethodId = paymentMethodId;
            return this;
        }

        @NonNull
        Builder setPaymentMethodTypes(@Nullable List<String> paymentMethodTypes) {
            mPaymentMethodTypes = paymentMethodTypes;
            return this;
        }

        @NonNull
        Builder setStatus(@Nullable Status status) {
            mStatus = status;
            return this;
        }

        @NonNull
        Builder setUsage(@Nullable Usage usage) {
            mUsage = usage;
            return this;
        }

        @NonNull
        @Override
        public SetupIntent build() {
            return new SetupIntent(this);
        }
    }
}
