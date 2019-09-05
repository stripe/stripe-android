package com.stripe.android.model;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.stripe.android.ObjectBuilder;
import com.stripe.android.utils.ObjectUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.json.JSONException;
import org.json.JSONObject;

import static com.stripe.android.model.StripeJsonUtils.optMap;
import static com.stripe.android.model.StripeJsonUtils.optString;


/**
 * A SetupIntent guides you through the process of setting up a customer's payment credentials for
 * future payments.
 */
@SuppressWarnings("WeakerAccess")
public final class SetupIntent extends StripeModel implements StripeIntent {
    private static final String VALUE_SETUP_INTENT = "setup_intent";

    private static final String FIELD_ID = "id";
    private static final String FIELD_OBJECT = "object";
    private static final String FIELD_CANCELLATION_REASON = "cancellation_reason";
    private static final String FIELD_CREATED = "created";
    private static final String FIELD_CLIENT_SECRET = "client_secret";
    private static final String FIELD_DESCRIPTION = "description";
    private static final String FIELD_LAST_SETUP_ERROR = "last_setup_error";
    private static final String FIELD_LIVEMODE = "livemode";
    private static final String FIELD_NEXT_ACTION = "next_action";
    private static final String FIELD_PAYMENT_METHOD_TYPES = "payment_method_types";
    private static final String FIELD_STATUS = "status";
    private static final String FIELD_USAGE = "usage";
    private static final String FIELD_PAYMENT_METHOD = "payment_method";

    private static final String FIELD_NEXT_ACTION_TYPE = "type";

    @Nullable private final String mId;
    @Nullable private final String mObjectType;
    @Nullable private final CancellationReason mCancellationReason;
    private final long mCreated;
    @Nullable private final String mClientSecret;
    @Nullable private final String mDescription;
    private final boolean mLiveMode;
    @Nullable private final Map<String, Object> mNextAction;
    @Nullable private final NextActionType mNextActionType;
    @Nullable private final String mPaymentMethodId;
    @NonNull private final List<String> mPaymentMethodTypes;
    @Nullable private final Status mStatus;
    @Nullable private final Usage mUsage;
    @Nullable private final Error mLastSetupError;

    private SetupIntent(@NonNull Builder builder) {
        mId = builder.mId;
        mObjectType = builder.mObjectType;
        mCancellationReason = builder.mCancellationReason;
        mCreated = builder.mCreated;
        mClientSecret = builder.mClientSecret;
        mDescription = builder.mDescription;
        mLiveMode = builder.mLiveMode;
        mNextAction = builder.mNextAction;
        mNextActionType = mNextAction != null ?
                NextActionType.fromCode((String) mNextAction.get(FIELD_NEXT_ACTION_TYPE)) : null;
        mPaymentMethodId = builder.mPaymentMethodId;
        mPaymentMethodTypes = Objects.requireNonNull(builder.mPaymentMethodTypes);
        mStatus = builder.mStatus;
        mUsage = builder.mUsage;
        mLastSetupError = builder.mLastSetupError;
    }

    @NonNull
    public static String parseIdFromClientSecret(@NonNull String clientSecret) {
        return clientSecret.split("_secret")[0];
    }

    /**
     * @return Unique identifier for the object.
     */
    @Nullable
    @Override
    public String getId() {
        return mId;
    }

    /**
     * @return Reason for cancellation of this SetupIntent.
     */
    @Nullable
    public CancellationReason getCancellationReason() {
        return mCancellationReason;
    }

    /**
     * @return Time at which the object was created. Measured in seconds since the Unix epoch.
     */
    @Override
    public long getCreated() {
        return mCreated;
    }

    /**
     * @return An arbitrary string attached to the object. Often useful for displaying to users.
     */
    @Nullable
    public String getDescription() {
        return mDescription;
    }

    /**
     * @return The error encountered in the previous SetupIntent confirmation.
     */
    @Nullable
    public Error getLastSetupError() {
        return mLastSetupError;
    }

    /**
     * @return Has the value <code>true</code> if the object exists in live mode or the value
     * <code>false</code> if the object exists in test mode.
     */
    @Override
    public boolean isLiveMode() {
        return mLiveMode;
    }

    /**
     * @return ID of the payment method used with this SetupIntent.
     */
    @Nullable
    @Override
    public String getPaymentMethodId() {
        return mPaymentMethodId;
    }

    /**
     * @return The list of payment method types (e.g. card) that this SetupIntent is allowed to
     * set up.
     */
    @NonNull
    @Override
    public List<String> getPaymentMethodTypes() {
        return mPaymentMethodTypes;
    }

    /**
     * @return Indicates how the payment method is intended to be used in the future.
     *
     * <p>Use <code>on_session</code> if you intend to only reuse the payment method when the
     * customer is in your checkout flow. Use <code>off_session</code> if your customer may or
     * may not be in your checkout flow. If not provided, this value defaults to
     * <code>off_session</code>.</p>
     */
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

    /**
     * @return The client secret of this SetupIntent. Used for client-side retrieval using a
     * publishable key.
     *
     * <p>The client secret can be used to complete payment setup from your frontend. It should not
     * be stored, logged, embedded in URLs, or exposed to anyone other than the customer. Make
     * sure that you have TLS enabled on any page that includes the client secret.</p>
     */
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

    /**
     * @return Status of this SetupIntent.
     */
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
                .setCancellationReason(CancellationReason.fromCode(
                        optString(jsonObject, FIELD_CANCELLATION_REASON)
                ))
                .setDescription(optString(jsonObject, FIELD_DESCRIPTION))
                .setLiveMode(jsonObject.optBoolean(FIELD_LIVEMODE))
                .setPaymentMethodId(optString(jsonObject, FIELD_PAYMENT_METHOD))
                .setPaymentMethodTypes(jsonArrayToList(
                        jsonObject.optJSONArray(FIELD_PAYMENT_METHOD_TYPES)))
                .setStatus(Status.fromCode(optString(jsonObject, FIELD_STATUS)))
                .setUsage(Usage.fromCode(optString(jsonObject, FIELD_USAGE)))
                .setNextAction(optMap(jsonObject, FIELD_NEXT_ACTION))
                .setLastSetupError(Error.fromJson(jsonObject.optJSONObject(FIELD_LAST_SETUP_ERROR)))
                .build();
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
                && ObjectUtils.equals(mCancellationReason, setupIntent.mCancellationReason)
                && ObjectUtils.equals(mDescription, setupIntent.mDescription)
                && ObjectUtils.equals(mLastSetupError, setupIntent.mLastSetupError)
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
        return ObjectUtils.hash(mId, mObjectType, mCancellationReason, mClientSecret, mCreated,
                mDescription, mLastSetupError, mLiveMode, mStatus, mPaymentMethodId,
                mPaymentMethodTypes, mNextAction, mNextActionType, mUsage);
    }

    private static final class Builder implements ObjectBuilder<SetupIntent> {
        @Nullable private String mId;
        @Nullable private String mObjectType;
        @Nullable private CancellationReason mCancellationReason;
        private long mCreated;
        @Nullable private String mClientSecret;
        @Nullable private String mDescription;
        private boolean mLiveMode;
        @Nullable private Map<String, Object> mNextAction;
        @Nullable private String mPaymentMethodId;
        private List<String> mPaymentMethodTypes;
        @Nullable private Status mStatus;
        @Nullable private Usage mUsage;
        @Nullable private Error mLastSetupError;

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
        Builder setCancellationReason(@Nullable CancellationReason cancellationReason) {
            mCancellationReason = cancellationReason;
            return this;
        }

        @NonNull
        Builder setDescription(@Nullable String description) {
            mDescription = description;
            return this;
        }

        @NonNull
        Builder setLastSetupError(@Nullable Error lastSetupError) {
            mLastSetupError = lastSetupError;
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
        Builder setPaymentMethodTypes(@NonNull List<String> paymentMethodTypes) {
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

    /**
     * The error encountered in the previous SetupIntent confirmation.
     *
     * See <a href="https://stripe.com/docs/api/setup_intents/object#setup_intent_object-last_setup_error">last_setup_error</a>.
     */
    public static final class Error {
        private static final String FIELD_CODE = "code";
        private static final String FIELD_DECLINE_CODE = "decline_code";
        private static final String FIELD_DOC_URL = "doc_url";
        private static final String FIELD_MESSAGE = "message";
        private static final String FIELD_PARAM = "param";
        private static final String FIELD_PAYMENT_METHOD = "payment_method";
        private static final String FIELD_TYPE = "type";

        /**
         * For some errors that could be handled programmatically, a short string indicating the
         * <a href="https://stripe.com/docs/error-codes">error code</a> reported.
         */
        @Nullable public final String code;

        /**
         * For card errors resulting from a card issuer decline, a short string indicating the
         * <a href="https://stripe.com/docs/declines#issuer-declines">card issuer’s reason for the decline</a>
         * if they provide one.
         */
        @Nullable public final String declineCode;

        /**
         * A URL to more information about the
         * <a href="https://stripe.com/docs/error-codes">error code</a> reported.
         */
        @Nullable public final String docUrl;

        /**
         * A human-readable message providing more details about the error. For card errors,
         * these messages can be shown to your users.
         */
        @Nullable public final String message;

        /**
         * If the error is parameter-specific, the parameter related to the error.
         * For example, you can use this to display a message near the correct form field.
         */
        @Nullable public final String param;

        /**
         * The PaymentMethod object for errors returned on a request involving a PaymentMethod.
         */
        @Nullable public final PaymentMethod paymentMethod;

        /**
         * The type of error returned.
         */
        @Nullable public final Type type;

        private Error(@NonNull Builder builder) {
            code = builder.mCode;
            declineCode = builder.mDeclineCode;
            docUrl = builder.mDocUrl;
            message = builder.mMessage;
            param = builder.mParam;
            paymentMethod = builder.mPaymentMethod;
            type = builder.mType;
        }

        @Nullable
        private static Error fromJson(@Nullable JSONObject errorJson) {
            if (errorJson == null) {
                return null;
            }

            return new Builder()
                    .setCode(StripeJsonUtils.optString(errorJson, FIELD_CODE))
                    .setDeclineCode(StripeJsonUtils.optString(errorJson, FIELD_DECLINE_CODE))
                    .setDocUrl(StripeJsonUtils.optString(errorJson, FIELD_DOC_URL))
                    .setMessage(StripeJsonUtils.optString(errorJson, FIELD_MESSAGE))
                    .setParam(StripeJsonUtils.optString(errorJson, FIELD_PARAM))
                    .setPaymentMethod(
                            PaymentMethod.fromJson(errorJson.optJSONObject(FIELD_PAYMENT_METHOD)))
                    .setType(Type.fromCode(StripeJsonUtils.optString(errorJson, FIELD_TYPE)))
                    .build();
        }

        @Override
        public int hashCode() {
            return ObjectUtils.hash(code, declineCode, docUrl, message, param, paymentMethod, type);
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            return super.equals(obj) || (obj instanceof Error && typedEquals((Error) obj));
        }

        private boolean typedEquals(@NonNull Error error) {
            return ObjectUtils.equals(code, error.code) &&
                    ObjectUtils.equals(declineCode, error.declineCode) &&
                    ObjectUtils.equals(docUrl, error.docUrl) &&
                    ObjectUtils.equals(message, error.message) &&
                    ObjectUtils.equals(param, error.param) &&
                    ObjectUtils.equals(paymentMethod, error.paymentMethod) &&
                    ObjectUtils.equals(type, error.type);
        }

        private static final class Builder implements ObjectBuilder<Error> {
            @Nullable private String mCode;
            @Nullable private String mDeclineCode;
            @Nullable private String mDocUrl;
            @Nullable private String mMessage;
            @Nullable private String mParam;
            @Nullable private PaymentMethod mPaymentMethod;
            @Nullable private Type mType;

            @NonNull
            private Builder setCode(@Nullable String code) {
                this.mCode = code;
                return this;
            }

            @NonNull
            private Builder setDeclineCode(@Nullable String declineCode) {
                this.mDeclineCode = declineCode;
                return this;
            }

            @NonNull
            private Builder setDocUrl(@Nullable String docUrl) {
                this.mDocUrl = docUrl;
                return this;
            }

            @NonNull
            private Builder setMessage(@Nullable String message) {
                this.mMessage = message;
                return this;
            }

            @NonNull
            private Builder setParam(@Nullable String mParam) {
                this.mParam = mParam;
                return this;
            }

            @NonNull
            private Builder setPaymentMethod(@Nullable PaymentMethod paymentMethod) {
                this.mPaymentMethod = paymentMethod;
                return this;
            }

            @NonNull
            private Builder setType(@Nullable Type type) {
                this.mType = type;
                return this;
            }

            @NonNull
            @Override
            public Error build() {
                return new Error(this);
            }
        }

        public enum Type {
            ApiConnectionError("api_connection_error"),
            ApiError("api_error"),
            AuthenticationError("authentication_error"),
            CardError("card_error"),
            IdempotencyError("idempotency_error"),
            InvalidRequestError("invalid_request_error"),
            RateLimitError("rate_limit_error");

            @NonNull public final String code;

            Type(@NonNull String code) {
                this.code = code;
            }

            @Nullable
            private static Type fromCode(@Nullable String typeCode) {
                for (Type type : values()) {
                    if (type.code.equals(typeCode)) {
                        return type;
                    }
                }

                return null;
            }
        }
    }

    public enum CancellationReason {
        Duplicate("duplicate"),
        RequestedByCustomer("requested_by_customer"),
        Abandoned("abandoned");

        @NonNull private final String code;

        CancellationReason(@NonNull String code) {
            this.code = code;
        }

        @Nullable
        private static CancellationReason fromCode(@Nullable String code) {
            for (CancellationReason cancellationReason : values()) {
                if (cancellationReason.code.equals(code)) {
                    return cancellationReason;
                }
            }

            return null;
        }
    }
}
