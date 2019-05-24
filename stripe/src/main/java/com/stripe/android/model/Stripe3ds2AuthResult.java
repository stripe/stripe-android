package com.stripe.android.model;

/*
{
    "id": "threeds2_1Ecwz3CRMbs6FrXfThtfogua",
    "object": "three_d_secure_2",
    "ares": {
        "acsChallengeMandated": "Y",
        "acsSignedContent": "eyJhbGciOiJFUzI1NiJ9.eyJhY3NFcGhlbVB1YktleSI6eyJjcnYiOiJQLTI1NiIsImt0eSI6IkVDIiwieCI6IjNoU0tOdml0STVrVUtkVGVHSUZjSFJ1cjNlSGx1ZEEtaDRPbTA0djZUd2MiLCJ5IjoicWpoSjlISTFDZWN3SFktSmRqNXozWXIyNXVCLXZFakU2S3djRExhSTA5YyJ9LCJzZGtFcGhlbVB1YktleSI6eyJrdHkiOiJFQyIsInVzZSI6InNpZyIsImNydiI6IlAtMjU2Iiwia2lkIjoiM2ZlMGNiMjAtNDhiZi00NGY5LTk5YWUtMTI2OTFlYmQwMzgyIiwieCI6IkpQdWhiSTRpaTd6MVg4U1RpVU9FVTVsWTdqaHFYMk1zV1NnN3RIbU5RUFEiLCJ5IjoiU2txSDJPMmlwbUJzS2F4czN3SnpDbC0wNDlkR2xTQXA3QS1kcnVSTlJuTSJ9LCJhY3NVUkwiOiJodHRwczovL2hvb2tzLnN0cmlwZS5jb20vM2Rfc2VjdXJlXzJfdGVzdC9hcHBfY2hhbGxlbmdlL2hCbUR6MlFSWTNVTlBQU1N5QWRNTXQxSXNYZWN3YzhVNlRlckJiNEJfY289In0.aER-DQLnhHTjReHDkneOlDjVUcxg_TZjv41X6VjpKvzOcEpwPzxDo6xFTYONNoQbjK3j8Q9YTzX4jLjup2cWSQ",
        "acsTransID": "dd23c757-211a-4c1b-add5-06a1450a642e",
        "acsURL": null,
        "authenticationType": "02",
        "cardholderInfo": null,
        "messageExtension": null,
        "messageType": "ARes",
        "messageVersion": "2.1.0",
        "sdkTransID": "20158862-9d9d-4d71-83d4-9e65554ed92c",
        "threeDSServerTransID": "e8ea0b42-0e74-42b2-92b4-1b27005f0596"
    },
    "created": 1558541285,
    "error": null,
    "livemode": false,
    "source": "src_1Ecwz1CRMbs6FrXfUwt98lxf",
    "state": "challenge_required"
}
 */


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
    @Nullable public final StripeError error;


    private Stripe3ds2AuthResult(@Nullable String id, @Nullable String objectType,
                                 @Nullable Ares ares, @Nullable Long created,
                                 @Nullable String source, @Nullable String state,
                                 @Nullable Boolean liveMode, @Nullable StripeError error) {
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
        @Nullable private StripeError mError;

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
        Builder setError(@Nullable StripeError error) {
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

            Builder setThreeDSServerTransId(@NonNull String threeDSServerTransId) {
                mThreeDSServerTransId = threeDSServerTransId;
                return this;
            }

            Builder setAcsChallengeMandated(@Nullable String acsChallengeMandated) {
                mAcsChallengeMandated = acsChallengeMandated;
                return this;
            }

            Builder setAcsSignedContent(@Nullable String acsSignedContent) {
                mAcsSignedContent = acsSignedContent;
                return this;
            }

            Builder setAcsTransId(@NonNull String acsTransId) {
                mAcsTransId = acsTransId;
                return this;
            }

            Builder setAcsUrl(@Nullable String acsUrl) {
                mAcsUrl = acsUrl;
                return this;
            }

            Builder setAuthenticationType(@Nullable String authenticationType) {
                mAuthenticationType = authenticationType;
                return this;
            }

            Builder setCardholderInfo(@Nullable String cardholderInfo) {
                mCardholderInfo = cardholderInfo;
                return this;
            }

            Builder setMessageExtension(@Nullable List<MessageExtension> messageExtension) {
                mMessageExtension = messageExtension;
                return this;
            }

            Builder setMessageType(@NonNull String messageType) {
                mMessageType = messageType;
                return this;
            }

            Builder setMessageVersion(@NonNull String messageVersion) {
                mMessageVersion = messageVersion;
                return this;
            }

            Builder setSdkTransId(@Nullable String sdkTransId) {
                mSdkTransId = sdkTransId;
                return this;
            }

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
        // Note: Payment System Registered Application Provider Identifier (RID) is required as prefix
        // of the ID.
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
            private String mName;
            private boolean mCriticalityIndicator;
            private String mId;
            private Map<String, String> mData;

            Builder setName(String name) {
                mName = name;
                return this;
            }

            Builder setCriticalityIndicator(boolean criticalityIndicator) {
                mCriticalityIndicator = criticalityIndicator;
                return this;
            }

            Builder setId(String id) {
                mId = id;
                return this;
            }

            Builder setData(Map<String, String> data) {
                mData = data;
                return this;
            }

            MessageExtension build() {
                return new MessageExtension(mName, mCriticalityIndicator, mId, mData);
            }
        }
    }
}
