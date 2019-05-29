package com.stripe.android.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.stripe.android.utils.ObjectUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.stripe.android.model.StripeJsonUtils.optString;

public final class Stripe3ds2AuthResult {
    private static final String FIELD_ID = "id";
    private static final String FIELD_OBJECT = "object";
    private static final String FIELD_ARES = "ares";
    private static final String FIELD_CREATED = "created";
    private static final String FIELD_ERROR = "error";
    private static final String FIELD_LIVEMODE = "livemode";
    private static final String FIELD_SOURCE = "source";
    private static final String FIELD_STATE = "state";

    @NonNull public final String id;
    @NonNull public final String objectType;
    @Nullable public final Ares ares;
    @NonNull public final Long created;
    @NonNull public final String source;
    @Nullable public final String state;
    public final boolean liveMode;
    @Nullable public final ThreeDS2Error error;


    private Stripe3ds2AuthResult(@NonNull String id, @NonNull String objectType,
                                 @Nullable Ares ares, @NonNull Long created,
                                 @NonNull String source, @Nullable String state,
                                 boolean liveMode, @Nullable ThreeDS2Error error) {
        this.id = id;
        this.objectType = objectType;
        this.ares = ares;
        this.created = created;
        this.source = source;
        this.state = state;
        this.liveMode = liveMode;
        this.error = error;
    }

    @NonNull
    public static Stripe3ds2AuthResult fromJson(@NonNull JSONObject authResultJson) throws JSONException {
        return new Stripe3ds2AuthResult.Builder()
                .setId(authResultJson.getString(FIELD_ID))
                .setObjectType(authResultJson.getString(FIELD_OBJECT))
                .setCreated(authResultJson.getLong(FIELD_CREATED))
                .setLiveMode(authResultJson.getBoolean(FIELD_LIVEMODE))
                .setSource(authResultJson.getString(FIELD_SOURCE))
                .setState(authResultJson.optString(FIELD_STATE))
                .setAres(Ares.fromJson(authResultJson.optJSONObject(FIELD_ARES)))
                .setError(authResultJson.isNull(FIELD_ERROR) ? null :
                        ThreeDS2Error.fromJson(authResultJson.optJSONObject(FIELD_ERROR)))
                .build();
    }

    @Override
    public int hashCode() {
        return ObjectUtils
                .hash(id, objectType, ares, created, source, state, liveMode, error);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return super.equals(obj)
                || (obj instanceof Stripe3ds2AuthResult && typedEquals((Stripe3ds2AuthResult) obj));
    }

    private boolean typedEquals(@NonNull Stripe3ds2AuthResult obj) {
        return Objects.equals(id, obj.id)
                && Objects.equals(objectType, obj.objectType)
                && Objects.equals(ares, obj.ares)
                && Objects.equals(created, obj.created)
                && Objects.equals(source, obj.source)
                && Objects.equals(state, obj.state)
                && liveMode == obj.liveMode
                && Objects.equals(error, obj.error);
    }

    static class Builder {
        private String mId;
        private String mObjectType;
        @Nullable private Stripe3ds2AuthResult.Ares mAres;
        private Long mCreated;
        private String mSource;
        @Nullable private String mState;
        private boolean mLiveMode;
        @Nullable private ThreeDS2Error mError;

        @NonNull
        Builder setId(@NonNull String id) {
            mId = id;
            return this;
        }

        @NonNull
        Builder setObjectType(@NonNull String objectType) {
            mObjectType = objectType;
            return this;
        }

        @NonNull
        Builder setAres(@Nullable Stripe3ds2AuthResult.Ares ares) {
            mAres = ares;
            return this;
        }

        @NonNull
        Builder setCreated(@NonNull Long created) {
            mCreated = created;
            return this;
        }

        @NonNull
        Builder setSource(@NonNull String source) {
            mSource = source;
            return this;
        }

        @NonNull
        Builder setState(@Nullable String state) {
            mState = state;
            return this;
        }

        @NonNull
        Builder setLiveMode(boolean liveMode) {
            mLiveMode = liveMode;
            return this;
        }

        @NonNull
        Builder setError(@Nullable ThreeDS2Error error) {
            mError = error;
            return this;
        }

        @NonNull
        Stripe3ds2AuthResult build() {
            return new Stripe3ds2AuthResult(mId, mObjectType, mAres, mCreated, mSource, mState,
                    mLiveMode, mError);
        }
    }

    public static class Ares {
        static final String FIELD_ACS_CHALLENGE_MANDATED = "acsChallengeMandated";
        static final String FIELD_ACS_SIGNED_CONTENT = "acsSignedContent";
        static final String FIELD_ACS_TRANS_ID = "acsTransID";
        static final String FIELD_ACS_URL = "acsURL";
        static final String FIELD_AUTHENTICATION_TYPE = "authenticationType";
        static final String FIELD_CARDHOLDER_INFO = "cardholderInfo";
        static final String FIELD_MESSAGE_EXTENSION = "messageExtension";
        static final String FIELD_MESSAGE_TYPE = "messageType";
        static final String FIELD_MESSAGE_VERSION = "messageVersion";
        static final String FIELD_SDK_TRANS_ID = "sdkTransID";
        static final String FIELD_THREE_DS_SERVER_TRANS_ID = "threeDSServerTransID";

        @NonNull public final String threeDSServerTransId;
        @Nullable public final String acsChallengeMandated;
        @Nullable public final String acsSignedContent;
        @NonNull public final String acsTransId;
        @Nullable public final String acsUrl;
        @Nullable public final String authenticationType;
        @Nullable public final String cardholderInfo;
        @Nullable public final List<MessageExtension> messageExtension;
        @NonNull public final String messageType;
        @NonNull public final String messageVersion;
        @Nullable public final String sdkTransId;

        private Ares(@NonNull String threeDSServerTransId, @Nullable String acsChallengeMandated,
                     @Nullable String acsSignedContent, @NonNull String acsTransId,
                     @Nullable String acsUrl, @Nullable String authenticationType,
                     @Nullable String cardholderInfo,
                     @Nullable List<MessageExtension> messageExtension, @NonNull String messageType,
                     @NonNull String messageVersion, @Nullable String sdkTransId) {
            this.threeDSServerTransId = threeDSServerTransId;
            this.acsChallengeMandated = acsChallengeMandated;
            this.acsSignedContent = acsSignedContent;
            this.acsTransId = acsTransId;
            this.acsUrl = acsUrl;
            this.authenticationType = authenticationType;
            this.cardholderInfo = cardholderInfo;
            this.messageExtension = messageExtension;
            this.messageType = messageType;
            this.messageVersion = messageVersion;
            this.sdkTransId = sdkTransId;
        }

        @NonNull
        static Ares fromJson(@NonNull JSONObject aresJson) throws JSONException {
            return new Ares.Builder()
                    .setThreeDSServerTransId(aresJson.getString(FIELD_THREE_DS_SERVER_TRANS_ID))
                    .setAcsChallengeMandated(optString(aresJson, FIELD_ACS_CHALLENGE_MANDATED))
                    .setAcsSignedContent(optString(aresJson, FIELD_ACS_SIGNED_CONTENT))
                    .setAcsTransId(aresJson.getString(FIELD_ACS_TRANS_ID))
                    .setAcsUrl(optString(aresJson, FIELD_ACS_URL))
                    .setAuthenticationType(optString(aresJson, FIELD_AUTHENTICATION_TYPE))
                    .setCardholderInfo(optString(aresJson, FIELD_CARDHOLDER_INFO))
                    .setMessageType(aresJson.getString(FIELD_MESSAGE_TYPE))
                    .setMessageVersion(aresJson.getString(FIELD_MESSAGE_VERSION))
                    .setSdkTransId(optString(aresJson, FIELD_SDK_TRANS_ID))
                    .setMessageExtension(MessageExtension.fromJson(
                            aresJson.optJSONArray(FIELD_MESSAGE_EXTENSION)))
                    .build();
        }

        @Override
        public int hashCode() {
            return ObjectUtils.hash(threeDSServerTransId, acsChallengeMandated,
                    acsSignedContent, acsTransId, acsUrl, authenticationType, cardholderInfo
                    , messageExtension, messageType, messageVersion, sdkTransId);
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            return super.equals(obj)
                    || (obj instanceof Ares && typedEquals((Ares) obj));
        }

        private boolean typedEquals(@NonNull Ares obj) {
            return Objects.equals(threeDSServerTransId, obj.threeDSServerTransId)
                    && Objects.equals(acsChallengeMandated, obj.acsChallengeMandated)
                    && Objects.equals(acsSignedContent, obj.acsSignedContent)
                    && Objects.equals(acsTransId, obj.acsTransId)
                    && Objects.equals(acsUrl, obj.acsUrl)
                    && Objects.equals(authenticationType, obj.authenticationType)
                    && Objects.equals(cardholderInfo, obj.cardholderInfo)
                    && Objects.equals(messageExtension, obj.messageExtension)
                    && Objects.equals(messageType, obj.messageType)
                    && Objects.equals(messageVersion, obj.messageVersion)
                    && Objects.equals(sdkTransId, obj.sdkTransId);
        }

        static class Builder {
            private String mThreeDSServerTransId;
            private String mAcsChallengeMandated;
            private String mAcsSignedContent;
            private String mAcsTransId;
            private String mAcsUrl;
            private String mAuthenticationType;
            private String mCardholderInfo;
            private List<Stripe3ds2AuthResult.MessageExtension> mMessageExtension;
            private String mMessageType;
            private String mMessageVersion;
            private String mSdkTransId;

            @NonNull
            Builder setThreeDSServerTransId(@Nullable String threeDSServerTransId) {
                mThreeDSServerTransId = threeDSServerTransId;
                return this;
            }

            @NonNull
            Builder setAcsChallengeMandated(@Nullable String acsChallengeMandated) {
                mAcsChallengeMandated = acsChallengeMandated;
                return this;
            }

            @NonNull
            Builder setAcsSignedContent(@Nullable String acsSignedContent) {
                mAcsSignedContent = acsSignedContent;
                return this;
            }

            @NonNull
            Builder setAcsTransId(@Nullable String acsTransId) {
                mAcsTransId = acsTransId;
                return this;
            }

            @NonNull
            Builder setAcsUrl(@Nullable String acsUrl) {
                mAcsUrl = acsUrl;
                return this;
            }

            @NonNull
            Builder setAuthenticationType(@Nullable String authenticationType) {
                mAuthenticationType = authenticationType;
                return this;
            }

            @NonNull
            Builder setCardholderInfo(@Nullable String cardholderInfo) {
                mCardholderInfo = cardholderInfo;
                return this;
            }

            @NonNull
            Builder setMessageExtension(@Nullable List<MessageExtension> messageExtension) {
                mMessageExtension = messageExtension;
                return this;
            }

            @NonNull
            Builder setMessageType(@Nullable String messageType) {
                mMessageType = messageType;
                return this;
            }

            @NonNull
            Builder setMessageVersion(@Nullable String messageVersion) {
                mMessageVersion = messageVersion;
                return this;
            }

            @NonNull
            Builder setSdkTransId(@Nullable String sdkTransId) {
                mSdkTransId = sdkTransId;
                return this;
            }

            @NonNull
            Ares build() {
                return new Stripe3ds2AuthResult.Ares(mThreeDSServerTransId, mAcsChallengeMandated,
                        mAcsSignedContent, mAcsTransId, mAcsUrl, mAuthenticationType,
                        mCardholderInfo, mMessageExtension, mMessageType, mMessageVersion,
                        mSdkTransId);
            }
        }
    }

    public static class MessageExtension {

        static final String FIELD_NAME = "name";
        static final String FIELD_ID = "id";
        static final String FIELD_CRITICALITY_INDICATOR = "criticalityIndicator";
        static final String FIELD_DATA = "data";

        // The name of the extension data set as defined by the extension owner.
        @NonNull public final String name;

        // A boolean value indicating whether the recipient must understand the contents of the
        // extension to interpret the entire message.
        public final boolean criticalityIndicator;

        // A unique identifier for the extension.
        // Note: Payment System Registered Application Provider Identifier (RID) is required as
        // prefix of the ID.
        @NonNull public final String id;

        // The data carried in the extension.
        @NonNull public final Map<String, String> data;

        private MessageExtension(@NonNull String name, boolean criticalityIndicator,
                                 @NonNull String id,
                                 @NonNull Map<String, String> data) {
            this.name = name;
            this.criticalityIndicator = criticalityIndicator;
            this.id = id;
            this.data = data;
        }

        @Nullable
        static List<MessageExtension> fromJson(@Nullable JSONArray messageExtensionsJson)
                throws JSONException {
            if (messageExtensionsJson == null) {
                return null;
            }

            final List<MessageExtension> messageExtensions = new ArrayList<>();
            for (int i = 0; i < messageExtensionsJson.length(); i++) {
                final JSONObject messageExtensionJson = messageExtensionsJson.optJSONObject(i);
                if (messageExtensionJson != null) {
                    messageExtensions.add(fromJson(messageExtensionJson));
                }
            }

            return messageExtensions;
        }

        @NonNull
        private static MessageExtension fromJson(@NonNull JSONObject messageExtensionJson)
                throws JSONException {
            final Map<String, String> data = new HashMap<>();
            final JSONObject dataJson = messageExtensionJson.optJSONObject(FIELD_DATA);
            if (dataJson != null) {
                final Iterator<String> keys = dataJson.keys();
                while (keys.hasNext()) {
                    final String key = keys.next();
                    data.put(key, dataJson.getString(key));
                }
            }

            return new Builder()
                    .setName(optString(messageExtensionJson, FIELD_NAME))
                    .setCriticalityIndicator(
                            messageExtensionJson.optBoolean(FIELD_CRITICALITY_INDICATOR))
                    .setId(optString(messageExtensionJson, FIELD_ID))
                    .setData(data)
                    .build();
        }

        @Override
        public int hashCode() {
            return ObjectUtils.hash(name, id, criticalityIndicator, data);
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            return super.equals(obj)
                    || (obj instanceof MessageExtension && typedEquals((MessageExtension) obj));
        }

        private boolean typedEquals(@NonNull MessageExtension obj) {
            return Objects.equals(name, obj.name)
                    && Objects.equals(id, obj.id)
                    && criticalityIndicator == obj.criticalityIndicator
                    && Objects.equals(data, obj.data);
        }

        static final class Builder {
            private String mName;
            private boolean mCriticalityIndicator;
            private String mId;
            private Map<String, String> mData;

            @NonNull
            Builder setName(@Nullable String name) {
                mName = name;
                return this;
            }

            @NonNull
            Builder setCriticalityIndicator(boolean criticalityIndicator) {
                mCriticalityIndicator = criticalityIndicator;
                return this;
            }

            @NonNull
            Builder setId(@Nullable String id) {
                mId = id;
                return this;
            }

            @NonNull
            Builder setData(@Nullable Map<String, String> data) {
                mData = data;
                return this;
            }

            @NonNull
            MessageExtension build() {
                return new MessageExtension(mName, mCriticalityIndicator, mId, mData);
            }
        }
    }

    public static class ThreeDS2Error {

        static final String FIELD_THREE_DS_SERVER_TRANS_ID = "threeDSServerTransID";
        static final String FIELD_ACS_TRANS_ID = "acsTransID";
        static final String FIELD_DS_TRANS_ID = "dsTransID";
        static final String FIELD_ERROR_CODE = "errorCode";
        static final String FIELD_ERROR_COMPONENT = "errorComponent";
        static final String FIELD_ERROR_DESCRIPTION = "errorDescription";
        static final String FIELD_ERROR_DETAIL = "errorDetail";
        static final String FIELD_ERROR_MESSAGE_TYPE = "errorMessageType";
        static final String FIELD_MESSAGE_TYPE = "messageType";
        static final String FIELD_MESSAGE_VERSION = "messageVersion";
        static final String FIELD_SDK_TRANS_ID = "sdkTransID";

        @NonNull public final String threeDSServerTransId;
        @Nullable public final String acsTransId;
        @Nullable public final String dsTransId;
        @NonNull public final String errorCode;
        @NonNull public final String errorComponent;
        @NonNull public final String errorDescription;
        @NonNull public final String errorDetail;
        @Nullable public final String errorMessageType;
        @NonNull public final String messageType;
        @NonNull public final String messageVersion;
        @Nullable public final String sdkTransId;

        private ThreeDS2Error(@NonNull String threeDSServerTransId, @Nullable String acsTransId,
                              @Nullable String dsTransId, @NonNull String errorCode,
                              @NonNull String errorComponent, @NonNull String errorDescription,
                              @NonNull String errorDetail, @Nullable String errorMessageType,
                              @NonNull String messageType, @NonNull String messageVersion,
                              @Nullable String sdkTransId) {
            this.threeDSServerTransId = threeDSServerTransId;
            this.acsTransId = acsTransId;
            this.dsTransId = dsTransId;
            this.errorCode = errorCode;
            this.errorComponent = errorComponent;
            this.errorDescription = errorDescription;
            this.errorDetail = errorDetail;
            this.errorMessageType = errorMessageType;
            this.messageType = messageType;
            this.messageVersion = messageVersion;
            this.sdkTransId = sdkTransId;
        }

        @NonNull
        static ThreeDS2Error fromJson(@NonNull JSONObject errorJson) throws JSONException {
            return new ThreeDS2Error.Builder()
                    .setThreeDSServerTransId(errorJson.getString(FIELD_THREE_DS_SERVER_TRANS_ID))
                    .setAcsTransId(optString(errorJson, FIELD_ACS_TRANS_ID))
                    .setDsTransId(optString(errorJson, FIELD_DS_TRANS_ID))
                    .setErrorCode(errorJson.getString(FIELD_ERROR_CODE))
                    .setErrorComponent(errorJson.getString(FIELD_ERROR_COMPONENT))
                    .setErrorDescription(errorJson.getString(FIELD_ERROR_DESCRIPTION))
                    .setErrorDetail(errorJson.getString(FIELD_ERROR_DETAIL))
                    .setErrorMessageType(optString(errorJson, FIELD_ERROR_MESSAGE_TYPE))
                    .setMessageType(errorJson.getString(FIELD_MESSAGE_TYPE))
                    .setMessageVersion(errorJson.getString(FIELD_MESSAGE_VERSION))
                    .setSdkTransId(optString(errorJson, FIELD_SDK_TRANS_ID))
                    .build();
        }

        @Override
        public int hashCode() {
            return ObjectUtils.hash(threeDSServerTransId, acsTransId, dsTransId, errorCode,
                    errorComponent, errorDescription, errorDetail, errorMessageType, messageType,
                    messageVersion, sdkTransId);
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            return super.equals(obj)
                    || (obj instanceof ThreeDS2Error && typedEquals((ThreeDS2Error) obj));
        }

        private boolean typedEquals(@NonNull ThreeDS2Error obj) {
            return Objects.equals(threeDSServerTransId, obj.threeDSServerTransId)
                    && Objects.equals(acsTransId, obj.acsTransId)
                    && Objects.equals(dsTransId, obj.dsTransId)
                    && Objects.equals(errorCode, obj.errorCode)
                    && Objects.equals(errorComponent, obj.errorComponent)
                    && Objects.equals(errorDescription, obj.errorDescription)
                    && Objects.equals(errorDetail, obj.errorDetail)
                    && Objects.equals(errorMessageType, obj.errorMessageType)
                    && Objects.equals(messageType, obj.messageType)
                    && Objects.equals(messageVersion, obj.messageVersion)
                    && Objects.equals(sdkTransId, obj.sdkTransId);
        }

        static class Builder {
            private String mThreeDSServerTransId;
            private String mAcsTransId;
            private String mDsTransId;
            private String mErrorCode;
            private String mErrorComponent;
            private String mErrorDescription;
            private String mErrorDetail;
            private String mErrorMessageType;
            private String mMessageType;
            private String mMessageVersion;
            private String mSdkTransId;

            @NonNull
            Builder setThreeDSServerTransId(@Nullable String threeDSServerTransId) {
                mThreeDSServerTransId = threeDSServerTransId;
                return this;
            }

            @NonNull
            Builder setAcsTransId(@Nullable String acsTransId) {
                mAcsTransId = acsTransId;
                return this;
            }

            @NonNull
            Builder setDsTransId(@Nullable String dsTransId) {
                mDsTransId = dsTransId;
                return this;
            }

            @NonNull
            Builder setErrorCode(@Nullable String errorCode) {
                mErrorCode = errorCode;
                return this;
            }

            @NonNull
            Builder setErrorComponent(@Nullable String errorComponent) {
                mErrorComponent = errorComponent;
                return this;
            }

            @NonNull
            Builder setErrorDescription(@Nullable String errorDescription) {
                mErrorDescription = errorDescription;
                return this;
            }

            @NonNull
            Builder setErrorDetail(@Nullable String errorDetail) {
                mErrorDetail = errorDetail;
                return this;
            }

            @NonNull
            Builder setErrorMessageType(@Nullable String errorMessageType) {
                mErrorMessageType = errorMessageType;
                return this;
            }

            @NonNull
            Builder setMessageType(@Nullable String messageType) {
                mMessageType = messageType;
                return this;
            }

            @NonNull
            Builder setMessageVersion(@Nullable String messageVersion) {
                mMessageVersion = messageVersion;
                return this;
            }

            @NonNull
            Builder setSdkTransId(@Nullable String sdkTransId) {
                mSdkTransId = sdkTransId;
                return this;
            }

            @NonNull
            Stripe3ds2AuthResult.ThreeDS2Error build() {
                return new Stripe3ds2AuthResult.ThreeDS2Error(mThreeDSServerTransId, mAcsTransId,
                        mDsTransId, mErrorCode, mErrorComponent, mErrorDescription, mErrorDetail,
                        mErrorMessageType, mMessageType, mMessageVersion, mSdkTransId);
            }
        }
    }
}
