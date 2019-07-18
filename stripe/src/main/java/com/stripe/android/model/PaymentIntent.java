package com.stripe.android.model;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.stripe.android.StripeNetworkUtils;
import com.stripe.android.utils.ObjectUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
public final class PaymentIntent extends StripeModel implements StripeIntent {
    private static final String VALUE_PAYMENT_INTENT = "payment_intent";

    static final String FIELD_ID = "id";
    static final String FIELD_OBJECT = "object";
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
    static final String FIELD_PAYMENT_METHOD_TYPES = "payment_method_types";
    static final String FIELD_RECEIPT_EMAIL = "receipt_email";
    static final String FIELD_SOURCE = "source";
    static final String FIELD_STATUS = "status";
    static final String FIELD_SETUP_FUTURE_USAGE = "setup_future_usage";

    private static final String FIELD_NEXT_ACTION_TYPE = "type";

    @Nullable private final String mId;
    @Nullable private final String mObjectType;
    @NonNull private final List<String> mPaymentMethodTypes;
    @Nullable private final Long mAmount;
    @Nullable private final Long mCanceledAt;
    @Nullable private final String mCaptureMethod;
    @Nullable private final String mClientSecret;
    @Nullable private final String mConfirmationMethod;
    @Nullable private final Long mCreated;
    @Nullable private final String mCurrency;
    @Nullable private final String mDescription;
    private final boolean mLiveMode;
    @Nullable private final Map<String, Object> mNextAction;
    @Nullable private final NextActionType mNextActionType;
    @Nullable private final String mReceiptEmail;
    @Nullable private final String mSource;
    @Nullable private final Status mStatus;
    @Nullable private final Usage mSetupFutureUsage;

    @Nullable
    @Override
    public String getId() {
        return mId;
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

    @Override
    public boolean isLiveMode() {
        return mLiveMode;
    }

    public boolean requiresAction() {
        return mStatus == Status.RequiresAction;
    }

    public boolean requiresConfirmation() {
        return mStatus == Status.RequiresConfirmation;
    }

    @Nullable
    public Map<String, Object> getNextAction() {
        return mNextAction;
    }

    @Nullable
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
    public String getReceiptEmail() {
        return mReceiptEmail;
    }

    @Nullable
    public String getSource() {
        return mSource;
    }

    @Nullable
    public Status getStatus() {
        return mStatus;
    }

    private PaymentIntent(
            @Nullable String id,
            @Nullable String objectType,
            @NonNull List<String> paymentMethodTypes,
            @Nullable Long amount,
            @Nullable Long canceledAt,
            @Nullable String captureMethod,
            @Nullable String clientSecret,
            @Nullable String confirmationMethod,
            @Nullable Long created,
            @Nullable String currency,
            @Nullable String description,
            boolean liveMode,
            @Nullable Map<String, Object> nextAction,
            @Nullable String receiptEmail,
            @Nullable String source,
            @Nullable Status status,
            @Nullable Usage setupFutureUsage) {
        mId = id;
        mObjectType = objectType;
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
        mNextAction = nextAction;
        mReceiptEmail = receiptEmail;
        mSource = source;
        mStatus = status;
        mSetupFutureUsage = setupFutureUsage;
        mNextActionType = mNextAction != null ?
                NextActionType.fromCode((String) mNextAction.get(FIELD_NEXT_ACTION_TYPE)) : null;
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
        final Long canceledAt = optLong(jsonObject, FIELD_CANCELED);
        final String captureMethod = optString(jsonObject, FIELD_CAPTURE_METHOD);
        final String clientSecret = optString(jsonObject, FIELD_CLIENT_SECRET);
        final String confirmationMethod = optString(jsonObject, FIELD_CONFIRMATION_METHOD);
        final Long created = optLong(jsonObject, FIELD_CREATED);
        final String currency = optCurrency(jsonObject, FIELD_CURRENCY);
        final String description = optString(jsonObject, FIELD_DESCRIPTION);
        final boolean livemode = Boolean.TRUE.equals(optBoolean(jsonObject, FIELD_LIVEMODE));
        final String receiptEmail = optString(jsonObject, FIELD_RECEIPT_EMAIL);
        final Status status = Status.fromCode(optString(jsonObject, FIELD_STATUS));
        final Usage setupFutureUsage =
                Usage.fromCode(optString(jsonObject, FIELD_SETUP_FUTURE_USAGE));
        final Map<String, Object> nextAction = optMap(jsonObject, FIELD_NEXT_ACTION);
        final String source = optString(jsonObject, FIELD_SOURCE);

        return new PaymentIntent(
                id,
                objectType,
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
                nextAction,
                receiptEmail,
                source,
                status,
                setupFutureUsage);
    }

    @NonNull
    @Override
    public Map<String, Object> toMap() {
        final AbstractMap<String, Object> map = new HashMap<>();
        map.put(FIELD_ID, mId);
        map.put(FIELD_OBJECT, mObjectType);
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
        map.put(FIELD_NEXT_ACTION, mNextAction);
        map.put(FIELD_RECEIPT_EMAIL, mReceiptEmail);
        map.put(FIELD_STATUS, mStatus != null ? mStatus.code : null);
        map.put(FIELD_SETUP_FUTURE_USAGE,
                mSetupFutureUsage != null ? mSetupFutureUsage.code : null);
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
                && ObjectUtils.equals(mAmount, paymentIntent.mAmount)
                && ObjectUtils.equals(mCanceledAt, paymentIntent.mCanceledAt)
                && ObjectUtils.equals(mCaptureMethod, paymentIntent.mCaptureMethod)
                && ObjectUtils.equals(mClientSecret, paymentIntent.mClientSecret)
                && ObjectUtils.equals(mConfirmationMethod, paymentIntent.mConfirmationMethod)
                && ObjectUtils.equals(mCreated, paymentIntent.mCreated)
                && ObjectUtils.equals(mCurrency, paymentIntent.mCurrency)
                && ObjectUtils.equals(mDescription, paymentIntent.mDescription)
                && ObjectUtils.equals(mLiveMode, paymentIntent.mLiveMode)
                && ObjectUtils.equals(mReceiptEmail, paymentIntent.mReceiptEmail)
                && ObjectUtils.equals(mSource, paymentIntent.mSource)
                && ObjectUtils.equals(mStatus, paymentIntent.mStatus)
                && ObjectUtils.equals(mSetupFutureUsage, paymentIntent.mSetupFutureUsage)
                && ObjectUtils.equals(mPaymentMethodTypes, paymentIntent.mPaymentMethodTypes)
                && ObjectUtils.equals(mNextAction, paymentIntent.mNextAction)
                && ObjectUtils.equals(mNextActionType, paymentIntent.mNextActionType);
    }

    @Override
    public int hashCode() {
        return ObjectUtils.hash(mId, mObjectType, mAmount, mCanceledAt, mCaptureMethod,
                mClientSecret, mConfirmationMethod, mCreated, mCurrency, mDescription, mLiveMode,
                mReceiptEmail, mSource, mStatus, mPaymentMethodTypes, mNextAction,
                mNextActionType, mSetupFutureUsage);
    }

}
