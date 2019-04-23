package com.stripe.android;

import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

class Stripe3DS2AuthParams {

    static final String FIELD_APP = "app";
    static final String FIELD_SOURCE = "source";

    private static final String FIELD_SDK_APP_ID = "sdkAppID";
    private static final String FIELD_SDK_TRANS_ID = "sdkTransID";
    private static final String FIELD_SDK_ENC_DATA = "sdkEncData";
    private static final String FIELD_SDK_EPHEM_PUB_KEY = "sdkEphemPubKey";
    private static final String FIELD_SDK_MAX_TIMEOUT = "sdkMaxTimeout";
    private static final String FIELD_MESSAGE_VERSION = "messageVersion";
    private static final String FIELD_DEVICE_RENDER_OPTIONS = "deviceRenderOptions";

    private static final String FIELD_SDK_INTERFACE = "sdkInterface";
    private static final String FIELD_SDK_UI_TYPE = "sdkUiType";

    @NonNull private final String mSourceId;
    @NonNull private final String mDeviceData;
    @NonNull private final String mSdkAppId;
    @NonNull private final String mSdkTransactionId;
    @NonNull private final String mSdkEphemeralPublicKey;
    @NonNull private final String mMessageVersion;
    private final int mMaxTimeout;

    @VisibleForTesting
    Stripe3DS2AuthParams(@NonNull String sourceId,
                         @NonNull String sdkAppId,
                         @NonNull String sdkTransactionId,
                         @NonNull String deviceData,
                         @NonNull String sdkEphemeralPublicKey,
                         @NonNull String messageVersion,
                         int maxTimeout) {
        mSourceId = sourceId;
        mSdkAppId = sdkAppId;
        mDeviceData = deviceData;
        mSdkTransactionId = sdkTransactionId;
        mSdkEphemeralPublicKey = sdkEphemeralPublicKey;
        mMessageVersion = messageVersion;
        mMaxTimeout = maxTimeout;
    }

    @NonNull
    Map<String, Object> toParamMap() {
        final Map<String, Object> params = new HashMap<>();
        params.put(FIELD_SOURCE, mSourceId);
        params.put(FIELD_APP, createAppParams().toString());
        return params;
    }

    @NonNull
    private JSONObject createAppParams() {
        final JSONObject appParams = new JSONObject();
        try {
            appParams.put(FIELD_SDK_APP_ID, mSdkAppId);
            appParams.put(FIELD_SDK_TRANS_ID, mSdkTransactionId);
            appParams.put(FIELD_SDK_ENC_DATA, mDeviceData);
            appParams.put(FIELD_SDK_EPHEM_PUB_KEY, new JSONObject(mSdkEphemeralPublicKey));
            appParams.put(FIELD_SDK_MAX_TIMEOUT, mMaxTimeout);
            appParams.put(FIELD_MESSAGE_VERSION, mMessageVersion);
            appParams.put(FIELD_DEVICE_RENDER_OPTIONS, createDeviceRenderOptions());
        } catch (JSONException ignore) { }

        return appParams;
    }

    @NonNull
    private JSONObject createDeviceRenderOptions() {
        final JSONObject deviceRenderOptions = new JSONObject();
        try {
            deviceRenderOptions.put(FIELD_SDK_INTERFACE, "03");
            deviceRenderOptions.put(FIELD_SDK_UI_TYPE,
                    new JSONArray(Arrays.asList("01", "02", "03", "04", "05")));
        } catch (JSONException ignore) { }
        return deviceRenderOptions;
    }
}
