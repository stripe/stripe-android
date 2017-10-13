package com.stripe.android.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringDef;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashMap;
import java.util.Map;

import static com.stripe.android.StripeNetworkUtils.removeNullAndEmptyParams;
import static com.stripe.android.model.StripeJsonUtils.optString;
import static com.stripe.android.model.StripeJsonUtils.putStringIfNotNull;

/**
 * Model for a <a href="https://stripe.com/docs/api#source_object-redirect">redirect</a> object
 * in the source api.
 */
public class SourceRedirect extends StripeJsonModel {

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({
            PENDING,
            SUCCEEDED,
            FAILED
    })
    @interface Status { }
    public static final String PENDING = "pending";
    public static final String SUCCEEDED = "succeeded";
    public static final String FAILED = "failed";

    static final String FIELD_RETURN_URL = "return_url";
    static final String FIELD_STATUS = "status";
    static final String FIELD_URL = "url";

    private String mReturnUrl;
    private @Status String mStatus;
    private String mUrl;

    SourceRedirect(String returnUrl, @Status String status, String url) {
        mReturnUrl = returnUrl;
        mStatus = status;
        mUrl = url;
    }

    public String getReturnUrl() {
        return mReturnUrl;
    }

    public void setReturnUrl(String returnUrl) {
        mReturnUrl = returnUrl;
    }

    @Status
    public String getStatus() {
        return mStatus;
    }

    public void setStatus(@Status String status) {
        mStatus = status;
    }

    public String getUrl() {
        return mUrl;
    }

    public void setUrl(String url) {
        mUrl = url;
    }

    @NonNull
    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> hashMap = new HashMap<>();
        hashMap.put(FIELD_RETURN_URL, mReturnUrl);
        hashMap.put(FIELD_STATUS, mStatus);
        hashMap.put(FIELD_URL, mUrl);
        removeNullAndEmptyParams(hashMap);
        return hashMap;
    }

    @NonNull
    @Override
    public JSONObject toJson() {
        JSONObject jsonObject = new JSONObject();
        putStringIfNotNull(jsonObject, FIELD_RETURN_URL, mReturnUrl);
        putStringIfNotNull(jsonObject, FIELD_STATUS, mStatus);
        putStringIfNotNull(jsonObject, FIELD_URL, mUrl);
        return jsonObject;
    }

    @Nullable
    public static SourceRedirect fromString(@Nullable String jsonString) {
        try {
            JSONObject jsonObject = new JSONObject(jsonString);
            return fromJson(jsonObject);
        } catch (JSONException ignored) {
            return null;
        }
    }

    @Nullable
    public static SourceRedirect fromJson(@Nullable JSONObject jsonObject) {
        if (jsonObject == null) {
            return null;
        }

        String returnUrl = optString(jsonObject, FIELD_RETURN_URL);
        @Status String status = asStatus(optString(jsonObject, FIELD_STATUS));
        String url = optString(jsonObject, FIELD_URL);
        return new SourceRedirect(returnUrl, status, url);
    }

    @Nullable
    @Status
    private static String asStatus(@Nullable String stringStatus) {
        if (PENDING.equals(stringStatus)) {
            return PENDING;
        } else if (SUCCEEDED.equals(stringStatus)) {
            return SUCCEEDED;
        } else if (FAILED.equals(stringStatus)) {
            return FAILED;
        }

        return null;
    }
}
