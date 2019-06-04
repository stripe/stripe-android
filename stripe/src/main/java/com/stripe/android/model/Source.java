package com.stripe.android.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.Size;
import android.support.annotation.StringDef;

import com.stripe.android.utils.ObjectUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.stripe.android.StripeNetworkUtils.removeNullAndEmptyParams;
import static com.stripe.android.model.StripeJsonUtils.optLong;
import static com.stripe.android.model.StripeJsonUtils.optString;

/**
 * A model class representing a source in the Android SDK. More detailed information and interaction
 * can be seen at <a href="https://stripe.com/docs/api/sources/object?lang=java">
 * https://stripe.com/docs/api/sources/object?lang=java</a>.
 */
@SuppressWarnings({"WeakerAccess", "unused"})
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

    @Nullable private String mId;
    @Nullable private Long mAmount;
    @Nullable private String mClientSecret;
    @Nullable private SourceCodeVerification mCodeVerification;
    @Nullable private Long mCreated;
    @Nullable private String mCurrency;
    @Nullable private String mTypeRaw;
    @Nullable @SourceFlow private String mFlow;
    @Nullable private Boolean mLiveMode;
    @Nullable private Map<String, String> mMetaData;
    @Nullable private SourceOwner mOwner;
    @Nullable private SourceReceiver mReceiver;
    @Nullable private SourceRedirect mRedirect;
    @Nullable @SourceStatus private String mStatus;
    @Nullable private Map<String, Object> mSourceTypeData;
    @Nullable private final StripeSourceTypeModel mSourceTypeModel;
    @Nullable @SourceType private String mType;
    @Nullable @Usage private String mUsage;

    private Source(@Nullable String id, @Nullable SourceCardData sourceTypeModel) {
        mId = id;
        mType = SourceType.CARD;
        mSourceTypeModel = sourceTypeModel;
    }

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
            @Nullable @Usage String usage
    ) {
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
    }

    @Nullable
    @Override
    public String getId() {
        return mId;
    }

    public Long getAmount() {
        return mAmount;
    }

    public String getClientSecret() {
        return mClientSecret;
    }

    public SourceCodeVerification getCodeVerification() {
        return mCodeVerification;
    }

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

    public Boolean isLiveMode() {
        return mLiveMode;
    }

    public Map<String, String> getMetaData() {
        return mMetaData;
    }

    public SourceOwner getOwner() {
        return mOwner;
    }

    public SourceReceiver getReceiver() {
        return mReceiver;
    }

    public SourceRedirect getRedirect() {
        return mRedirect;
    }

    @SourceStatus
    public String getStatus() {
        return mStatus;
    }

    public Map<String, Object> getSourceTypeData() {
        return mSourceTypeData;
    }

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
    public String getTypeRaw() {
        return mTypeRaw;
    }

    @Usage
    public String getUsage() {
        return mUsage;
    }

    public void setId(String id) {
        mId = id;
    }

    public void setAmount(long amount) {
        mAmount = amount;
    }

    public void setClientSecret(String clientSecret) {
        mClientSecret = clientSecret;
    }

    public void setCodeVerification(SourceCodeVerification codeVerification) {
        mCodeVerification = codeVerification;
    }

    public void setCreated(long created) {
        mCreated = created;
    }

    public void setCurrency(String currency) {
        mCurrency = currency;
    }

    public void setFlow(@SourceFlow String flow) {
        mFlow = flow;
    }

    public void setLiveMode(boolean liveMode) {
        mLiveMode = liveMode;
    }

    public void setMetaData(Map<String, String> metaData) {
        mMetaData = metaData;
    }

    public void setOwner(SourceOwner owner) {
        mOwner = owner;
    }

    public void setReceiver(SourceReceiver receiver) {
        mReceiver = receiver;
    }

    public void setRedirect(SourceRedirect redirect) {
        mRedirect = redirect;
    }

    public void setStatus(@SourceStatus String status) {
        mStatus = status;
    }

    public void setSourceTypeData(Map<String, Object> sourceTypeData) {
        mSourceTypeData = sourceTypeData;
    }

    public void setTypeRaw(@NonNull @Size(min = 1) String typeRaw) {
        mTypeRaw = typeRaw;
        setType(SourceType.UNKNOWN);
    }

    public void setType(@SourceType String type) {
        mType = type;
    }

    public void setUsage(@Usage String usage) {
        mUsage = usage;
    }

    @NonNull
    @Override
    public Map<String, Object> toMap() {
        final AbstractMap<String, Object> map = new HashMap<>();
        map.put(FIELD_ID, mId);
        map.put(FIELD_OBJECT, VALUE_SOURCE);
        map.put(FIELD_AMOUNT, mAmount);
        map.put(FIELD_CLIENT_SECRET, mClientSecret);
        if (mCodeVerification != null) {
            map.put(FIELD_CODE_VERIFICATION, mCodeVerification.toMap());
        }

        map.put(FIELD_CREATED, mCreated);
        map.put(FIELD_CURRENCY, mCurrency);
        map.put(FIELD_FLOW, mFlow);
        map.put(FIELD_LIVEMODE, mLiveMode);
        map.put(FIELD_METADATA, mMetaData);

        if (mOwner != null) {
            map.put(FIELD_OWNER, mOwner.toMap());
        }
        if (mReceiver != null) {
            map.put(FIELD_RECEIVER, mReceiver.toMap());
        }
        if (mRedirect != null) {
            map.put(FIELD_REDIRECT, mRedirect.toMap());
        }

        map.put(mTypeRaw, mSourceTypeData);

        map.put(FIELD_STATUS, mStatus);
        map.put(FIELD_TYPE, mTypeRaw);
        map.put(FIELD_USAGE, mUsage);
        removeNullAndEmptyParams(map);
        return map;
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
        return new Source(
                optString(jsonObject, FIELD_ID),
                SourceCardData.fromJson(jsonObject)
        );
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
                usage);
    }

    @Nullable
    private static <T extends StripeModel> T optStripeJsonModel(
            @NonNull JSONObject jsonObject,
            @NonNull @Size(min = 1) String key,
            Class<T> type) {
        if (!jsonObject.has(key)) {
            return null;
        }

        switch (key) {
            case FIELD_CODE_VERIFICATION:
                return type.cast(SourceCodeVerification.fromJson(
                        jsonObject.optJSONObject(FIELD_CODE_VERIFICATION)));
            case FIELD_OWNER:
                return type.cast(
                        SourceOwner.fromJson(jsonObject.optJSONObject(FIELD_OWNER)));
            case FIELD_RECEIVER:
                return type.cast(
                        SourceReceiver.fromJson(jsonObject.optJSONObject(FIELD_RECEIVER)));
            case FIELD_REDIRECT:
                return type.cast(
                        SourceRedirect.fromJson(jsonObject.optJSONObject(FIELD_REDIRECT)));
            case SourceType.CARD:
                return type.cast(
                        SourceCardData.fromJson(jsonObject.optJSONObject(SourceType.CARD)));
            case SourceType.SEPA_DEBIT:
                return type.cast(SourceSepaDebitData.fromJson(
                        jsonObject.optJSONObject(SourceType.SEPA_DEBIT)));
            default:
                return null;
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
        } else if (SourceType.P24.equals(sourceType)) {
            return SourceType.P24;
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
        return ObjectUtils.equals(mId, source.mId)
                && ObjectUtils.equals(mAmount, source.mAmount)
                && ObjectUtils.equals(mClientSecret, source.mClientSecret)
                && ObjectUtils.equals(mCodeVerification, source.mCodeVerification)
                && ObjectUtils.equals(mCreated, source.mCreated)
                && ObjectUtils.equals(mCurrency, source.mCurrency)
                && ObjectUtils.equals(mTypeRaw, source.mTypeRaw)
                && ObjectUtils.equals(mFlow, source.mFlow)
                && ObjectUtils.equals(mLiveMode, source.mLiveMode)
                && ObjectUtils.equals(mMetaData, source.mMetaData)
                && ObjectUtils.equals(mOwner, source.mOwner)
                && ObjectUtils.equals(mReceiver, source.mReceiver)
                && ObjectUtils.equals(mRedirect, source.mRedirect)
                && ObjectUtils.equals(mStatus, source.mStatus)
                && ObjectUtils.equals(mSourceTypeData, source.mSourceTypeData)
                && ObjectUtils.equals(mSourceTypeModel, source.mSourceTypeModel)
                && ObjectUtils.equals(mType, source.mType)
                && ObjectUtils.equals(mUsage, source.mUsage);
    }

    @Override
    public int hashCode() {
        return ObjectUtils.hash(mId, mAmount, mClientSecret, mCodeVerification, mCreated, mCurrency,
                mTypeRaw, mFlow, mLiveMode, mMetaData, mOwner, mReceiver, mRedirect, mStatus,
                mSourceTypeData, mSourceTypeModel, mType, mUsage);
    }
}
