package com.stripe.android.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.stripe.android.StripeError;
import com.stripe.android.utils.ObjectUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class Stripe3ds2AuthResult {
    private static final String FIELD_ID = "id";
    private static final String FIELD_OBJECT = "object";
    private static final String FIELD_ARES = "ares";
    private static final String FIELD_CREATED = "created";
    private static final String FIELD_ERROR = "error";
    private static final String FIELD_LIVEMODE = "livemode";
    private static final String FIELD_SOURCE = "source";
    private static final String FIELD_STATE = "state";

    @Nullable public final String id;
    @Nullable public final String objectType;
    @Nullable public final Ares ares;
    @Nullable public final Long created;
    @Nullable public final String source;
    @Nullable public final String state;
    @Nullable public final Boolean liveMode;
    @Nullable public final ThreeDS2Error error;


    private Stripe3ds2AuthResult(@Nullable String id, @Nullable String objectType,
                                 @Nullable Ares ares, @Nullable Long created,
                                 @Nullable String source, @Nullable String state,
                                 @Nullable Boolean liveMode, @Nullable ThreeDS2Error error) {
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
    static Stripe3ds2AuthResult fromJson(@NonNull JSONObject authResultJson) {
        return new Stripe3ds2AuthResult.Builder()
                .setId(authResultJson.optString(FIELD_ID))
                .setObjectType(authResultJson.optString(FIELD_OBJECT))
                .setCreated(authResultJson.optLong(FIELD_CREATED))
                .setLiveMode(authResultJson.optBoolean(FIELD_LIVEMODE))
                .setSource(authResultJson.optString(FIELD_SOURCE))
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
                && Objects.equals(liveMode, obj.liveMode)
                && Objects.equals(error, obj.error);
    }

    static class Builder {
        @Nullable private String mId;
        @Nullable private String mObjectType;
        @Nullable private Stripe3ds2AuthResult.Ares mAres;
        @Nullable private Long mCreated;
        @Nullable private String mSource;
        @Nullable private String mState;
        @Nullable private Boolean mLiveMode;
        @Nullable private ThreeDS2Error mError;

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
        Builder setAres(@Nullable Stripe3ds2AuthResult.Ares ares) {
            mAres = ares;
            return this;
        }

        @NonNull
        Builder setCreated(@Nullable Long created) {
            mCreated = created;
            return this;
        }

        @NonNull
        Builder setSource(@Nullable String source) {
            mSource = source;
            return this;
        }

        @NonNull
        Builder setState(@Nullable String state) {
            mState = state;
            return this;
        }

        @NonNull
        Builder setLiveMode(@Nullable Boolean liveMode) {
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
        static Ares fromJson(@NonNull JSONObject aresJson) {
            return new Ares.Builder()
                    .setThreeDSServerTransId(aresJson.optString(FIELD_THREE_DS_SERVER_TRANS_ID))
                    .setAcsChallengeMandated(aresJson.isNull(FIELD_ACS_CHALLENGE_MANDATED) ? null :
                            aresJson.optString(FIELD_ACS_CHALLENGE_MANDATED))
                    .setAcsSignedContent(aresJson.isNull(FIELD_ACS_SIGNED_CONTENT) ? null :
                            aresJson.optString(FIELD_ACS_SIGNED_CONTENT))
                    .setAcsTransId(aresJson.optString(FIELD_ACS_TRANS_ID))
                    .setAcsUrl(aresJson.isNull(FIELD_ACS_URL) ? null :
                            aresJson.optString(FIELD_ACS_URL))
                    .setAuthenticationType(aresJson.isNull(FIELD_AUTHENTICATION_TYPE) ? null :
                            aresJson.optString(FIELD_AUTHENTICATION_TYPE))
                    .setCardholderInfo(aresJson.isNull(FIELD_CARDHOLDER_INFO) ? null :
                            aresJson.optString(FIELD_CARDHOLDER_INFO))
                    .setMessageType(aresJson.optString(FIELD_MESSAGE_TYPE))
                    .setMessageVersion(aresJson.optString(FIELD_MESSAGE_VERSION))
                    .setSdkTransId(aresJson.isNull(FIELD_SDK_TRANS_ID) ? null :
                            aresJson.optString(FIELD_SDK_TRANS_ID))
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
            @Nullable private String mThreeDSServerTransId;
            @Nullable private String mAcsChallengeMandated;
            @Nullable private String mAcsSignedContent;
            @Nullable private String mAcsTransId;
            @Nullable private String mAcsUrl;
            @Nullable private String mAuthenticationType;
            @Nullable private String mCardholderInfo;
            @Nullable private List<Stripe3ds2AuthResult.MessageExtension> mMessageExtension;
            @Nullable private String mMessageType;
            @Nullable private String mMessageVersion;
            @Nullable private String mSdkTransId;

            @NonNull
            Builder setThreeDSServerTransId(@NonNull String threeDSServerTransId) {
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
            Builder setAcsTransId(@NonNull String acsTransId) {
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
            Builder setMessageType(@NonNull String messageType) {
                mMessageType = messageType;
                return this;
            }

            @NonNull
            Builder setMessageVersion(@NonNull String messageVersion) {
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
        static List<MessageExtension> fromJson(@Nullable JSONArray messageExtensionsJson) {
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
        private static MessageExtension fromJson(@NonNull JSONObject messageExtensionJson) {
            final Map<String, String> data = new HashMap<>();
            final JSONObject dataJson = messageExtensionJson.optJSONObject(FIELD_DATA);
            if (dataJson != null) {
                final Iterator<String> keys = dataJson.keys();
                while (keys.hasNext()) {
                    final String key = keys.next();
                    data.put(key, dataJson.optString(key));
                }
            }

            return new Builder()
                    .setName(messageExtensionJson.optString(FIELD_NAME))
                    .setCriticalityIndicator(
                            messageExtensionJson.optBoolean(FIELD_CRITICALITY_INDICATOR))
                    .setId(messageExtensionJson.optString(FIELD_ID))
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
            @Nullable private String mName;
            private boolean mCriticalityIndicator;
            @Nullable private String mId;
            @Nullable private Map<String, String> mData;

            @NonNull
            Builder setName(@NonNull String name) {
                mName = name;
                return this;
            }

            @NonNull
            Builder setCriticalityIndicator(boolean criticalityIndicator) {
                mCriticalityIndicator = criticalityIndicator;
                return this;
            }

            @NonNull
            Builder setId(@NonNull String id) {
                mId = id;
                return this;
            }

            @NonNull
            Builder setData(@NonNull Map<String, String> data) {
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

        @Nullable public final String threeDSServerTransId;
        @Nullable public final String acsTransId;
        @Nullable public final String dsTransId;
        @Nullable public final String errorCode;
        @Nullable public final String errorComponent;
        @Nullable public final String errorDescription;
        @Nullable public final String errorDetail;
        @Nullable public final String errorMessageType;
        @Nullable public final String messageType;
        @Nullable public final String messageVersion;
        @Nullable public final String sdkTransId;

        private ThreeDS2Error(@Nullable String threeDSServerTransId, @Nullable String acsTransId,
                      @Nullable String dsTransId, @Nullable String errorCode,
                      @Nullable String errorComponent, @Nullable String errorDescription,
                      @Nullable String errorDetail, @Nullable String errorMessageType,
                      @Nullable String messageType, @Nullable String messageVersion,
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
        static ThreeDS2Error fromJson(@NonNull JSONObject errorJson) {
            return new ThreeDS2Error.Builder()
                    .setThreeDSServerTransId(errorJson.optString(FIELD_THREE_DS_SERVER_TRANS_ID))
                    .setAcsTransId(errorJson.isNull(FIELD_ACS_TRANS_ID) ? null :
                            errorJson.optString(FIELD_ACS_TRANS_ID))
                    .setDsTransId(errorJson.isNull(FIELD_DS_TRANS_ID) ? null :
                            errorJson.optString(FIELD_DS_TRANS_ID))
                    .setErrorCode(errorJson.optString(FIELD_ERROR_CODE))
                    .setErrorComponent(errorJson.optString(FIELD_ERROR_COMPONENT))
                    .setErrorDescription(errorJson.optString(FIELD_ERROR_DESCRIPTION))
                    .setErrorDetail(errorJson.optString(FIELD_ERROR_DETAIL))
                    .setErrorMessageType(errorJson.isNull(FIELD_ERROR_MESSAGE_TYPE) ? null :
                            errorJson.optString(FIELD_ERROR_MESSAGE_TYPE))
                    .setMessageType(errorJson.optString(FIELD_MESSAGE_TYPE))
                    .setMessageVersion(errorJson.optString(FIELD_MESSAGE_VERSION))
                    .setSdkTransId(errorJson.isNull(FIELD_SDK_TRANS_ID) ? null :
                            errorJson.optString(FIELD_SDK_TRANS_ID))
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
            @Nullable private String mThreeDSServerTransId;
            @Nullable private String mAcsTransId;
            @Nullable private String mDsTransId;
            @Nullable private String mErrorCode;
            @Nullable private String mErrorComponent;
            @Nullable private String mErrorDescription;
            @Nullable private String mErrorDetail;
            @Nullable private String mErrorMessageType;
            @Nullable private String mMessageType;
            @Nullable private String mMessageVersion;
            @Nullable private String mSdkTransId;

            @NonNull
            Builder setThreeDSServerTransId(@NonNull String threeDSServerTransId) {
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
            Builder setErrorCode(@NonNull String errorCode) {
                mErrorCode = errorCode;
                return this;
            }

            @NonNull
            Builder setErrorComponent(@NonNull String errorComponent) {
                mErrorComponent = errorComponent;
                return this;
            }

            @NonNull
            Builder setErrorDescription(@NonNull String errorDescription) {
                mErrorDescription = errorDescription;
                return this;
            }

            @NonNull
            Builder setErrorDetail(@NonNull String errorDetail) {
                mErrorDetail = errorDetail;
                return this;
            }

            @NonNull
            Builder setErrorMessageType(@Nullable String errorMessageType) {
                mErrorMessageType = errorMessageType;
                return this;
            }

            @NonNull
            Builder setMessageType(@NonNull String messageType) {
                mMessageType = messageType;
                return this;
            }

            @NonNull
            Builder setMessageVersion(@NonNull String messageVersion) {
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
