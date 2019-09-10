package com.stripe.android.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Size;
import androidx.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;

import static com.stripe.android.model.StripeJsonUtils.optLong;
import static com.stripe.android.model.StripeJsonUtils.optString;

/**
 * Model for a <a href="https://stripe.com/docs/sources">Sources API</a> object.
 *
 * See <a href="https://stripe.com/docs/api/sources/object">Sources API Reference</a>.
 */
@SuppressWarnings({"WeakerAccess"})
public final class Source extends StripeModel implements StripePaymentSource {

    static final String VALUE_SOURCE = "source";
    private static final String VALUE_CARD = "card";

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({
            SourceType.ALIPAY,
            SourceType.CARD,
            SourceType.THREE_D_SECURE,
            SourceType.GIROPAY,
            SourceType.SEPA_DEBIT,
            SourceType.IDEAL,
            SourceType.SOFORT,
            SourceType.BANCONTACT,
            SourceType.P24,
            SourceType.EPS,
            SourceType.MULTIBANCO,
            SourceType.WECHAT,
            SourceType.UNKNOWN
    })
    public @interface SourceType {
        String ALIPAY = "alipay";
        String CARD = "card";
        String THREE_D_SECURE = "three_d_secure";
        String GIROPAY = "giropay";
        String SEPA_DEBIT = "sepa_debit";
        String IDEAL = "ideal";
        String SOFORT = "sofort";
        String BANCONTACT = "bancontact";
        String P24 = "p24";
        String EPS = "eps";
        String MULTIBANCO = "multibanco";
        String WECHAT = "wechat";
        String UNKNOWN = "unknown";
    }

    private static final Set<String> MODELED_TYPES = new HashSet<>(
            Arrays.asList(SourceType.CARD, SourceType.SEPA_DEBIT));

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({
            SourceStatus.PENDING,
            SourceStatus.CHARGEABLE,
            SourceStatus.CONSUMED,
            SourceStatus.CANCELED,
            SourceStatus.FAILED
    })
    public @interface SourceStatus {
        String PENDING = "pending";
        String CHARGEABLE = "chargeable";
        String CONSUMED = "consumed";
        String CANCELED = "canceled";
        String FAILED = "failed";
    }

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({
            Usage.REUSABLE,
            Usage.SINGLE_USE
    })
    public @interface Usage {
        String REUSABLE = "reusable";
        String SINGLE_USE = "single_use";
    }

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({
            SourceFlow.REDIRECT,
            SourceFlow.RECEIVER,
            SourceFlow.CODE_VERIFICATION,
            SourceFlow.NONE
    })
    public @interface SourceFlow {
        String REDIRECT = "redirect";
        String RECEIVER = "receiver";
        String CODE_VERIFICATION = "code_verification";
        String NONE = "none";
    }

    static final String EURO = "eur";
    static final String USD = "usd";

    private static final String FIELD_ID = "id";
    private static final String FIELD_OBJECT = "object";
    private static final String FIELD_AMOUNT = "amount";
    private static final String FIELD_CLIENT_SECRET = "client_secret";
    private static final String FIELD_CODE_VERIFICATION = "code_verification";
    private static final String FIELD_CREATED = "created";
    private static final String FIELD_CURRENCY = "currency";
    private static final String FIELD_FLOW = "flow";
    private static final String FIELD_LIVEMODE = "livemode";
    private static final String FIELD_METADATA = "metadata";
    private static final String FIELD_OWNER = "owner";
    private static final String FIELD_RECEIVER = "receiver";
    private static final String FIELD_REDIRECT = "redirect";
    private static final String FIELD_STATUS = "status";
    private static final String FIELD_TYPE = "type";
    private static final String FIELD_USAGE = "usage";
    private static final String FIELD_WECHAT = "wechat";

    @NonNull @SourceType private final String mType;
    @NonNull private final String mTypeRaw;

    @Nullable private final String mId;
    @Nullable private final Long mAmount;
    @Nullable private final String mClientSecret;
    @Nullable private final SourceCodeVerification mCodeVerification;
    @Nullable private final Long mCreated;
    @Nullable private final String mCurrency;
    @Nullable @SourceFlow private final String mFlow;
    @Nullable private final Boolean mLiveMode;
    @Nullable private final Map<String, String> mMetaData;
    @Nullable private final SourceOwner mOwner;
    @Nullable private final SourceReceiver mReceiver;
    @Nullable private final SourceRedirect mRedirect;
    @Nullable @SourceStatus private final String mStatus;
    @Nullable private final Map<String, Object> mSourceTypeData;
    @Nullable private final StripeSourceTypeModel mSourceTypeModel;
    @Nullable @Usage private final String mUsage;
    @Nullable private final WeChat mWeChat;

    private Source(
            @Nullable String id,
            @Nullable Long amount,
            @Nullable String clientSecret,
            @Nullable SourceCodeVerification codeVerification,
            @Nullable Long created,
            @Nullable String currency,
            @Nullable @SourceFlow String flow,
            @Nullable Boolean liveMode,
            @Nullable Map<String, String> metaData,
            @Nullable SourceOwner owner,
            @Nullable SourceReceiver receiver,
            @Nullable SourceRedirect redirect,
            @Nullable @SourceStatus String status,
            @Nullable Map<String, Object> sourceTypeData,
            @Nullable StripeSourceTypeModel sourceTypeModel,
            @NonNull @SourceType String type,
            @NonNull String rawType,
            @Nullable @Usage String usage,
            @Nullable WeChat weChat) {
        mId = id;
        mAmount = amount;
        mClientSecret = clientSecret;
        mCodeVerification = codeVerification;
        mCreated = created;
        mCurrency = currency;
        mFlow = flow;
        mLiveMode = liveMode;
        mMetaData = metaData;
        mOwner = owner;
        mReceiver = receiver;
        mRedirect = redirect;
        mStatus = status;
        mSourceTypeData = sourceTypeData;
        mSourceTypeModel = sourceTypeModel;
        mType = type;
        mTypeRaw = rawType;
        mUsage = usage;
        mWeChat = weChat;
    }

    @Nullable
    @Override
    public String getId() {
        return mId;
    }

    @Nullable
    public Long getAmount() {
        return mAmount;
    }

    @Nullable
    public String getClientSecret() {
        return mClientSecret;
    }

    @Nullable
    public SourceCodeVerification getCodeVerification() {
        return mCodeVerification;
    }

    @Nullable
    public Long getCreated() {
        return mCreated;
    }

    public String getCurrency() {
        return mCurrency;
    }

    @SourceFlow
    public String getFlow() {
        return mFlow;
    }

    @Nullable
    public Boolean isLiveMode() {
        return mLiveMode;
    }

    @Nullable
    public Map<String, String> getMetaData() {
        return mMetaData;
    }

    @Nullable
    public SourceOwner getOwner() {
        return mOwner;
    }

    @Nullable
    public SourceReceiver getReceiver() {
        return mReceiver;
    }

    @Nullable
    public SourceRedirect getRedirect() {
        return mRedirect;
    }

    @SourceStatus
    public String getStatus() {
        return mStatus;
    }

    @Nullable
    public Map<String, Object> getSourceTypeData() {
        return mSourceTypeData;
    }

    @Nullable
    public StripeSourceTypeModel getSourceTypeModel() {
        return mSourceTypeModel;
    }

    /**
     * Gets the {@link SourceType} of this Source, as one of the enumerated values.
     * If a custom source type has been created, this returns {@link SourceType#UNKNOWN}. To get
     * the raw value of an {@link SourceType#UNKNOWN} type, use {@link #getTypeRaw()}.
     *
     * @return the {@link SourceType} of this Source
     */
    @NonNull
    @SourceType
    public String getType() {
        return mType;
    }

    /**
     * Gets the type of this source as a String. If it is a known type, this will return
     * a string equal to the {@link SourceType} returned from {@link #getType()}. This
     * method is not restricted to known types
     *
     * @return the type of this Source as a string
     */
    @NonNull
    public String getTypeRaw() {
        return mTypeRaw;
    }

    @Usage
    @Nullable
    public String getUsage() {
        return mUsage;
    }

    @NonNull
    public WeChat getWeChat() {
        if (!SourceType.WECHAT.equals(mType)) {
            throw new IllegalStateException(
                    String.format(Locale.ENGLISH, "Source type must be '%s'", SourceType.WECHAT)
            );
        }

        return Objects.requireNonNull(mWeChat);
    }

    @Nullable
    public static Source fromString(@Nullable String jsonString) {
        try {
            return fromJson(new JSONObject(jsonString));
        } catch (JSONException ignored) {
            return null;
        }
    }

    @Nullable
    public static Source fromJson(@Nullable JSONObject jsonObject) {
        if (jsonObject == null) {
            return null;
        }

        final String objectType = jsonObject.optString(FIELD_OBJECT);
        if (VALUE_CARD.equals(objectType)) {
            return fromCardJson(jsonObject);
        } else if (VALUE_SOURCE.equals(objectType)) {
            return fromSourceJson(jsonObject);
        } else {
            return null;
        }
    }

    @NonNull
    private static Source fromCardJson(@NonNull JSONObject jsonObject) {
        final String id = optString(jsonObject, FIELD_ID);
        final SourceCardData sourceTypeModel = SourceCardData.fromJson(jsonObject);

        return new Source(id, null, null, null, null,
                null, null, null, null, null, null,
                null, null, null, sourceTypeModel, SourceType.CARD,
                SourceType.CARD, null, null);
    }

    @NonNull
    private static Source fromSourceJson(@NonNull JSONObject jsonObject) {
        final String id = optString(jsonObject, FIELD_ID);
        final Long amount = optLong(jsonObject, FIELD_AMOUNT);
        final String clientSecret = optString(jsonObject, FIELD_CLIENT_SECRET);
        final SourceCodeVerification codeVerification = optStripeJsonModel(
                jsonObject,
                FIELD_CODE_VERIFICATION,
                SourceCodeVerification.class);
        final Long created = optLong(jsonObject, FIELD_CREATED);
        final String currency = optString(jsonObject, FIELD_CURRENCY);
        @SourceFlow final String flow = asSourceFlow(optString(jsonObject, FIELD_FLOW));
        final Boolean liveMode = jsonObject.optBoolean(FIELD_LIVEMODE);
        final Map<String, String> metadata =
                StripeJsonUtils.jsonObjectToStringMap(jsonObject.optJSONObject(FIELD_METADATA));
        final SourceOwner owner = optStripeJsonModel(jsonObject, FIELD_OWNER, SourceOwner.class);
        final SourceReceiver receiver = optStripeJsonModel(
                jsonObject,
                FIELD_RECEIVER,
                SourceReceiver.class);
        final SourceRedirect redirect = optStripeJsonModel(
                jsonObject,
                FIELD_REDIRECT,
                SourceRedirect.class);
        @SourceStatus final String status = asSourceStatus(optString(jsonObject, FIELD_STATUS));

        final String typeRawOpt = optString(jsonObject, FIELD_TYPE);
        @SourceType final String typeRaw = typeRawOpt != null ? typeRawOpt : SourceType.UNKNOWN;
        @SourceType final String type = asSourceType(typeRaw);

        // Until we have models for all types, keep the original hash and the
        // model object. The customType variable can be any field, and is not altered by
        // trying to force it to be a type that we know of.
        final Map<String, Object> sourceTypeData =
                StripeJsonUtils.jsonObjectToMap(jsonObject.optJSONObject(typeRaw));
        final StripeSourceTypeModel sourceTypeModel = MODELED_TYPES.contains(typeRaw)
                ? optStripeJsonModel(jsonObject, typeRaw, StripeSourceTypeModel.class)
                : null;

        @Usage final String usage = asUsage(optString(jsonObject, FIELD_USAGE));

        final WeChat weChat;
        if (SourceType.WECHAT.equals(type)) {
            weChat = WeChat.fromJson(jsonObject.optJSONObject(FIELD_WECHAT));
        } else {
            weChat = null;
        }

        return new Source(
                id,
                amount,
                clientSecret,
                codeVerification,
                created,
                currency,
                flow,
                liveMode,
                metadata,
                owner,
                receiver,
                redirect,
                status,
                sourceTypeData,
                sourceTypeModel,
                type,
                typeRaw,
                usage,
                weChat
        );
    }

    @Nullable
    private static <T extends StripeModel> T optStripeJsonModel(
            @NonNull JSONObject jsonObject,
            @NonNull @Size(min = 1) String key,
            @NonNull Class<T> type) {
        if (!jsonObject.has(key)) {
            return null;
        }

        switch (key) {
            case FIELD_CODE_VERIFICATION: {
                return type.cast(SourceCodeVerification.fromJson(
                        jsonObject.optJSONObject(FIELD_CODE_VERIFICATION)));
            }
            case FIELD_OWNER: {
                return type.cast(
                        SourceOwner.fromJson(jsonObject.optJSONObject(FIELD_OWNER)));
            }
            case FIELD_RECEIVER: {
                return type.cast(
                        SourceReceiver.fromJson(jsonObject.optJSONObject(FIELD_RECEIVER)));
            }
            case FIELD_REDIRECT: {
                return type.cast(
                        SourceRedirect.fromJson(jsonObject.optJSONObject(FIELD_REDIRECT)));
            }
            case SourceType.CARD: {
                return type.cast(
                        SourceCardData.fromJson(jsonObject.optJSONObject(SourceType.CARD)));
            }
            case SourceType.SEPA_DEBIT: {
                return type.cast(SourceSepaDebitData.fromJson(
                        jsonObject.optJSONObject(SourceType.SEPA_DEBIT)));
            }
            default: {
                return null;
            }
        }
    }

    @Nullable
    @SourceStatus
    private static String asSourceStatus(@Nullable String sourceStatus) {
        if (SourceStatus.PENDING.equals(sourceStatus)) {
            return SourceStatus.PENDING;
        } else if (SourceStatus.CHARGEABLE.equals(sourceStatus)) {
            return SourceStatus.CHARGEABLE;
        } else if (SourceStatus.CONSUMED.equals(sourceStatus)) {
            return SourceStatus.CONSUMED;
        } else if (SourceStatus.CANCELED.equals(sourceStatus)) {
            return SourceStatus.CANCELED;
        } else if (SourceStatus.FAILED.equals(sourceStatus)) {
            return SourceStatus.FAILED;
        }
        return null;
    }

    @NonNull
    @SourceType
    static String asSourceType(@Nullable String sourceType) {
        if (SourceType.CARD.equals(sourceType)) {
            return SourceType.CARD;
        } else if (SourceType.THREE_D_SECURE.equals(sourceType)) {
            return SourceType.THREE_D_SECURE;
        } else if (SourceType.GIROPAY.equals(sourceType)) {
            return SourceType.GIROPAY;
        } else if (SourceType.SEPA_DEBIT.equals(sourceType)) {
            return SourceType.SEPA_DEBIT;
        } else if (SourceType.IDEAL.equals(sourceType)) {
            return SourceType.IDEAL;
        } else if (SourceType.SOFORT.equals(sourceType)) {
            return SourceType.SOFORT;
        } else if (SourceType.BANCONTACT.equals(sourceType)) {
            return SourceType.BANCONTACT;
        } else if (SourceType.ALIPAY.equals(sourceType)) {
            return SourceType.ALIPAY;
        } else if (SourceType.EPS.equals(sourceType)) {
            return SourceType.EPS;
        } else if (SourceType.P24.equals(sourceType)) {
            return SourceType.P24;
        } else if (SourceType.MULTIBANCO.equals(sourceType)) {
            return SourceType.MULTIBANCO;
        } else if (SourceType.WECHAT.equals(sourceType)) {
            return SourceType.WECHAT;
        } else if (SourceType.UNKNOWN.equals(sourceType)) {
            return SourceType.UNKNOWN;
        } else {
            return SourceType.UNKNOWN;
        }
    }

    @Nullable
    @Usage
    private static String asUsage(@Nullable String usage) {
        if (Usage.REUSABLE.equals(usage)) {
            return Usage.REUSABLE;
        } else if (Usage.SINGLE_USE.equals(usage)) {
            return Usage.SINGLE_USE;
        }
        return null;
    }

    @Nullable
    @SourceFlow
    private static String asSourceFlow(@Nullable String sourceFlow) {
        if (SourceFlow.REDIRECT.equals(sourceFlow)) {
            return SourceFlow.REDIRECT;
        } else if (SourceFlow.RECEIVER.equals(sourceFlow)) {
            return SourceFlow.RECEIVER;
        } else if (SourceFlow.CODE_VERIFICATION.equals(sourceFlow)) {
            return SourceFlow.CODE_VERIFICATION;
        } else if (SourceFlow.NONE.equals(sourceFlow)) {
            return SourceFlow.NONE;
        }
        return null;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return this == obj || (obj instanceof Source && typedEquals((Source) obj));
    }

    private boolean typedEquals(@NonNull Source source) {
        return Objects.equals(mId, source.mId)
                && Objects.equals(mAmount, source.mAmount)
                && Objects.equals(mClientSecret, source.mClientSecret)
                && Objects.equals(mCodeVerification, source.mCodeVerification)
                && Objects.equals(mCreated, source.mCreated)
                && Objects.equals(mCurrency, source.mCurrency)
                && Objects.equals(mTypeRaw, source.mTypeRaw)
                && Objects.equals(mFlow, source.mFlow)
                && Objects.equals(mLiveMode, source.mLiveMode)
                && Objects.equals(mMetaData, source.mMetaData)
                && Objects.equals(mOwner, source.mOwner)
                && Objects.equals(mReceiver, source.mReceiver)
                && Objects.equals(mRedirect, source.mRedirect)
                && Objects.equals(mStatus, source.mStatus)
                && Objects.equals(mSourceTypeData, source.mSourceTypeData)
                && Objects.equals(mSourceTypeModel, source.mSourceTypeModel)
                && Objects.equals(mType, source.mType)
                && Objects.equals(mUsage, source.mUsage)
                && Objects.equals(mWeChat, source.mWeChat);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mId, mAmount, mClientSecret, mCodeVerification, mCreated, mCurrency,
                mTypeRaw, mFlow, mLiveMode, mMetaData, mOwner, mReceiver, mRedirect, mStatus,
                mSourceTypeData, mSourceTypeModel, mType, mUsage);
    }
}
