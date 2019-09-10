package com.stripe.android.model;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.stripe.android.ObjectBuilder;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.json.JSONException;
import org.json.JSONObject;

import static com.stripe.android.model.StripeJsonUtils.optBoolean;
import static com.stripe.android.model.StripeJsonUtils.optCurrency;
import static com.stripe.android.model.StripeJsonUtils.optLong;
import static com.stripe.android.model.StripeJsonUtils.optMap;
import static com.stripe.android.model.StripeJsonUtils.optString;

/**
 * A PaymentIntent tracks the process of collecting a payment from your customer.
 *
 * <ul>
 * <li><a href="https://stripe.com/docs/payments/payment-intents">Payment Intents Overview</a></li>
 * <li><a href="https://stripe.com/docs/api/payment_intents">PaymentIntents API</a></li>
 * </ul>
 */
@SuppressWarnings("WeakerAccess")
public final class PaymentIntent extends StripeModel implements StripeIntent {
    private static final String VALUE_PAYMENT_INTENT = "payment_intent";

    private static final String FIELD_ID = "id";
    private static final String FIELD_OBJECT = "object";
    private static final String FIELD_AMOUNT = "amount";
    private static final String FIELD_CREATED = "created";
    private static final String FIELD_CANCELED_AT = "canceled_at";
    private static final String FIELD_CANCELLATION_REASON = "cancellation_reason";
    private static final String FIELD_CAPTURE_METHOD = "capture_method";
    private static final String FIELD_CLIENT_SECRET = "client_secret";
    private static final String FIELD_CONFIRMATION_METHOD = "confirmation_method";
    private static final String FIELD_CURRENCY = "currency";
    private static final String FIELD_DESCRIPTION = "description";
    private static final String FIELD_LAST_PAYMENT_ERROR = "last_payment_error";
    private static final String FIELD_LIVEMODE = "livemode";
    private static final String FIELD_NEXT_ACTION = "next_action";
    private static final String FIELD_PAYMENT_METHOD_ID = "payment_method_id";
    private static final String FIELD_PAYMENT_METHOD_TYPES = "payment_method_types";
    private static final String FIELD_RECEIPT_EMAIL = "receipt_email";
    private static final String FIELD_STATUS = "status";
    private static final String FIELD_SETUP_FUTURE_USAGE = "setup_future_usage";

    private static final String FIELD_NEXT_ACTION_TYPE = "type";

    @Nullable private final String mId;
    @Nullable private final String mObjectType;
    @NonNull private final List<String> mPaymentMethodTypes;
    @Nullable private final Long mAmount;
    private final long mCanceledAt;
    @Nullable private final CancellationReason mCancellationReason;
    @Nullable private final String mCaptureMethod;
    @Nullable private final String mClientSecret;
    @Nullable private final String mConfirmationMethod;
    private final long mCreated;
    @Nullable private final String mCurrency;
    @Nullable private final String mDescription;
    private final boolean mLiveMode;
    @Nullable private final Map<String, Object> mNextAction;
    @Nullable private final NextActionType mNextActionType;
    @Nullable private final String mPaymentMethodId;
    @Nullable private final String mReceiptEmail;
    @Nullable private final Status mStatus;
    @Nullable private final Usage mSetupFutureUsage;
    @Nullable private final Error mLastPaymentError;

    /**
     * @return Unique identifier for the object.
     */
    @Nullable
    @Override
    public String getId() {
        return mId;
    }

    /**
     * @return The list of payment method types (e.g. card) that this PaymentIntent is allowed to
     * use.
     */
    @NonNull
    public List<String> getPaymentMethodTypes() {
        return mPaymentMethodTypes;
    }

    /**
     * @return Amount intended to be collected by this PaymentIntent.
     */
    @Nullable
    public Long getAmount() {
        return mAmount;
    }

    /**
     * @return Populated when status is canceled, this is the time at which the PaymentIntent
     * was canceled. Measured in seconds since the Unix epoch. If unavailable, will return 0.
     */
    public long getCanceledAt() {
        return mCanceledAt;
    }

    /**
     * @return Reason for cancellation of this PaymentIntent
     */
    @Nullable
    public CancellationReason getCancellationReason() {
        return mCancellationReason;
    }

    /**
     * @return One of <code>automatic</code> (default) or <code>manual</code>.
     *
     * <p>When the capture method is <code>automatic</code>,
     * Stripe automatically captures funds when the customer authorizes the payment.
     */
    @Nullable
    public String getCaptureMethod() {
        return mCaptureMethod;
    }

    /**
     * @return The client secret of this PaymentIntent. Used for client-side retrieval using a
     * publishable key.
     *
     * <p>The client secret can be used to complete a payment from your frontend.
     * It should not be stored, logged, embedded in URLs, or exposed to anyone other than the
     * customer. Make sure that you have TLS enabled on any page that includes the client
     * secret.</p>
     */
    @Nullable
    @Override
    public String getClientSecret() {
        return mClientSecret;
    }

    /**
     * @return One of automatic (default) or manual.
     *
     * <p>When the confirmation method is <code>automatic</code>, a PaymentIntent can be confirmed
     * using a publishable key. After <code>next_action</code>s are handled, no additional
     * confirmation is required to complete the payment.</p>
     *
     * <p>When the confirmation method is <code>manual</code>, all payment attempts must be made
     * using a secret key. The PaymentIntent returns to the <code>requires_confirmation</code>
     * state after handling <code>next_action</code>s, and requires your server to initiate each
     * payment attempt with an explicit confirmation.</p>
     */
    @Nullable
    public String getConfirmationMethod() {
        return mConfirmationMethod;
    }

    /**
     * @return Time at which the object was created. Measured in seconds since the Unix epoch.
     */
    @Override
    public long getCreated() {
        return mCreated;
    }

    /**
     * @return Three-letter ISO currency code, in lowercase. Must be a supported currency.
     */
    @Nullable
    public String getCurrency() {
        return mCurrency;
    }

    /**
     * @return An arbitrary string attached to the object. Often useful for displaying to users.
     */
    @Nullable
    @Override
    public String getDescription() {
        return mDescription;
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
     * @return ID of the payment method (a PaymentMethod, Card, BankAccount, or saved Source object)
     * to attach to this PaymentIntent.
     */
    @Nullable
    @Override
    public String getPaymentMethodId() {
        return mPaymentMethodId;
    }

    @Override
    public boolean requiresAction() {
        return mStatus == Status.RequiresAction;
    }

    @Override
    public boolean requiresConfirmation() {
        return mStatus == Status.RequiresConfirmation;
    }

    /**
     * @return If present, this property tells you what actions you need to take in order for your
     * customer to fulfill a payment using the provided source.
     */
    @Nullable
    public Map<String, Object> getNextAction() {
        return mNextAction;
    }

    @Nullable
    @Override
    public NextActionType getNextActionType() {
        return mNextActionType;
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
     * @return Email address that the receipt for the resulting payment will be sent to.
     */
    @Nullable
    public String getReceiptEmail() {
        return mReceiptEmail;
    }

    /**
     * @return Status of this PaymentIntent.
     */
    @Nullable
    public Status getStatus() {
        return mStatus;
    }

    /**
     * @return The payment error encountered in the previous PaymentIntent confirmation.
     */
    @Nullable
    public Error getLastPaymentError() {
        return mLastPaymentError;
    }

    private PaymentIntent(
            @Nullable String id,
            @Nullable String objectType,
            @NonNull List<String> paymentMethodTypes,
            @Nullable Long amount,
            long canceledAt,
            @Nullable CancellationReason cancellationReason,
            @Nullable String captureMethod,
            @Nullable String clientSecret,
            @Nullable String confirmationMethod,
            long created,
            @Nullable String currency,
            @Nullable String description,
            boolean liveMode,
            @Nullable Map<String, Object> nextAction,
            @Nullable String paymentMethodId,
            @Nullable String receiptEmail,
            @Nullable Status status,
            @Nullable Usage setupFutureUsage,
            @Nullable Error lastPaymentError) {
        mId = id;
        mObjectType = objectType;
        mPaymentMethodTypes = paymentMethodTypes;
        mAmount = amount;
        mCanceledAt = canceledAt;
        mCancellationReason = cancellationReason;
        mCaptureMethod = captureMethod;
        mClientSecret = clientSecret;
        mConfirmationMethod = confirmationMethod;
        mCreated = created;
        mCurrency = currency;
        mDescription = description;
        mLiveMode = liveMode;
        mNextAction = nextAction;
        mPaymentMethodId = paymentMethodId;
        mReceiptEmail = receiptEmail;
        mStatus = status;
        mSetupFutureUsage = setupFutureUsage;
        mNextActionType = mNextAction != null ?
                NextActionType.fromCode((String) mNextAction.get(FIELD_NEXT_ACTION_TYPE)) : null;
        mLastPaymentError = lastPaymentError;
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
        final List<String> paymentMethodTypes = jsonArrayToList(
                jsonObject.optJSONArray(FIELD_PAYMENT_METHOD_TYPES));
        final Long amount = optLong(jsonObject, FIELD_AMOUNT);
        final long canceledAt = jsonObject.optLong(FIELD_CANCELED_AT);
        final CancellationReason cancellationReason =
                CancellationReason.fromCode(optString(jsonObject, FIELD_CANCELLATION_REASON));
        final String captureMethod = optString(jsonObject, FIELD_CAPTURE_METHOD);
        final String clientSecret = optString(jsonObject, FIELD_CLIENT_SECRET);
        final String confirmationMethod = optString(jsonObject, FIELD_CONFIRMATION_METHOD);
        final long created = jsonObject.optLong(FIELD_CREATED);
        final String currency = optCurrency(jsonObject, FIELD_CURRENCY);
        final String description = optString(jsonObject, FIELD_DESCRIPTION);
        final boolean livemode = Boolean.TRUE.equals(optBoolean(jsonObject, FIELD_LIVEMODE));
        final String paymentMethodId = optString(jsonObject, FIELD_PAYMENT_METHOD_ID);
        final String receiptEmail = optString(jsonObject, FIELD_RECEIPT_EMAIL);
        final Status status = Status.fromCode(optString(jsonObject, FIELD_STATUS));
        final Usage setupFutureUsage =
                Usage.fromCode(optString(jsonObject, FIELD_SETUP_FUTURE_USAGE));
        final Map<String, Object> nextAction = optMap(jsonObject, FIELD_NEXT_ACTION);
        final Error lastPaymentError =
                Error.fromJson(jsonObject.optJSONObject(FIELD_LAST_PAYMENT_ERROR));

        return new PaymentIntent(
                id,
                objectType,
                paymentMethodTypes,
                amount,
                canceledAt,
                cancellationReason,
                captureMethod,
                clientSecret,
                confirmationMethod,
                created,
                currency,
                description,
                livemode,
                nextAction,
                paymentMethodId,
                receiptEmail,
                status,
                setupFutureUsage,
                lastPaymentError
        );
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return this == obj || (obj instanceof PaymentIntent && typedEquals((PaymentIntent) obj));
    }

    private boolean typedEquals(@NonNull PaymentIntent paymentIntent) {
        return Objects.equals(mId, paymentIntent.mId)
                && Objects.equals(mObjectType, paymentIntent.mObjectType)
                && Objects.equals(mAmount, paymentIntent.mAmount)
                && Objects.equals(mCanceledAt, paymentIntent.mCanceledAt)
                && Objects.equals(mCancellationReason, paymentIntent.mCancellationReason)
                && Objects.equals(mCaptureMethod, paymentIntent.mCaptureMethod)
                && Objects.equals(mClientSecret, paymentIntent.mClientSecret)
                && Objects.equals(mConfirmationMethod, paymentIntent.mConfirmationMethod)
                && Objects.equals(mCreated, paymentIntent.mCreated)
                && Objects.equals(mCurrency, paymentIntent.mCurrency)
                && Objects.equals(mDescription, paymentIntent.mDescription)
                && Objects.equals(mLiveMode, paymentIntent.mLiveMode)
                && Objects.equals(mPaymentMethodId, paymentIntent.mPaymentMethodId)
                && Objects.equals(mReceiptEmail, paymentIntent.mReceiptEmail)
                && Objects.equals(mStatus, paymentIntent.mStatus)
                && Objects.equals(mSetupFutureUsage, paymentIntent.mSetupFutureUsage)
                && Objects.equals(mPaymentMethodTypes, paymentIntent.mPaymentMethodTypes)
                && Objects.equals(mNextAction, paymentIntent.mNextAction)
                && Objects.equals(mNextActionType, paymentIntent.mNextActionType)
                && Objects.equals(mLastPaymentError, paymentIntent.mLastPaymentError);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mId, mObjectType, mAmount, mCanceledAt, mCancellationReason,
                mCaptureMethod, mClientSecret, mConfirmationMethod, mCreated, mCurrency,
                mDescription, mLiveMode, mReceiptEmail, mStatus, mPaymentMethodTypes,
                mNextAction, mNextActionType, mPaymentMethodId, mSetupFutureUsage,
                mLastPaymentError);
    }

    /**
     * The payment error encountered in the previous PaymentIntent confirmation.
     *
     * See <a href="https://stripe.com/docs/api/payment_intents/object#payment_intent_object-last_payment_error">last_payment_error</a>.
     */
    @SuppressWarnings("WeakerAccess")
    public static final class Error {
        private static final String FIELD_CHARGE = "charge";
        private static final String FIELD_CODE = "code";
        private static final String FIELD_DECLINE_CODE = "decline_code";
        private static final String FIELD_DOC_URL = "doc_url";
        private static final String FIELD_MESSAGE = "message";
        private static final String FIELD_PARAM = "param";
        private static final String FIELD_PAYMENT_METHOD = "payment_method";
        private static final String FIELD_TYPE = "type";

        /**
         * For card errors, the ID of the failed charge.
         */
        @Nullable public final String charge;

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
            charge = builder.mCharge;
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
                    .setCharge(StripeJsonUtils.optString(errorJson, FIELD_CHARGE))
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
            return Objects.hash(charge, code, declineCode, docUrl, message, param,
                    paymentMethod, type);
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            return super.equals(obj) || (obj instanceof Error && typedEquals((Error) obj));
        }

        private boolean typedEquals(@NonNull Error error) {
            return Objects.equals(charge, error.charge) &&
                    Objects.equals(code, error.code) &&
                    Objects.equals(declineCode, error.declineCode) &&
                    Objects.equals(docUrl, error.docUrl) &&
                    Objects.equals(message, error.message) &&
                    Objects.equals(param, error.param) &&
                    Objects.equals(paymentMethod, error.paymentMethod) &&
                    Objects.equals(type, error.type);
        }

        private static final class Builder implements ObjectBuilder<Error> {
            @Nullable private String mCharge;
            @Nullable private String mCode;
            @Nullable private String mDeclineCode;
            @Nullable private String mDocUrl;
            @Nullable private String mMessage;
            @Nullable private String mParam;
            @Nullable private PaymentMethod mPaymentMethod;
            @Nullable private Type mType;

            @NonNull
            private Builder setCharge(@Nullable String charge) {
                this.mCharge = charge;
                return this;
            }

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
        Fraudulent("fraudulent"),
        RequestedByCustomer("requested_by_customer"),
        Abandoned("abandoned"),
        FailedInvoice("failed_invoice"),
        VoidInvoice("void_invoice"),
        Automatic("automatic");

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
