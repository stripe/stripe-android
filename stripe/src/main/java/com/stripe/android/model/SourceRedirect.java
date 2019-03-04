package com.stripe.android.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringDef;

import com.stripe.android.utils.ObjectUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.AbstractMap;
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

    private static final String FIELD_RETURN_URL = "return_url";
    private static final String FIELD_STATUS = "status";
    private static final String FIELD_URL = "url";

    @Nullable private String mReturnUrl;
    @Nullable @Status private String mStatus;
    @Nullable private String mUrl;

    private SourceRedirect(@Nullable String returnUrl, @Status @Nullable String status,
                           @Nullable String url) {
        mReturnUrl = returnUrl;
        mStatus = status;
        mUrl = url;
    }

    @Nullable
    public String getReturnUrl() {
        return mReturnUrl;
    }

    public void setReturnUrl(@Nullable String returnUrl) {
        mReturnUrl = returnUrl;
    }

    @Nullable
    @Status
    public String getStatus() {
        return mStatus;
    }

    public void setStatus(@Nullable @Status String status) {
        mStatus = status;
    }

    @Nullable
    public String getUrl() {
        return mUrl;
    }

    public void setUrl(@Nullable String url) {
        mUrl = url;
    }

    @NonNull
    @Override
    public Map<String, Object> toMap() {
        final AbstractMap<String, Object> map = new HashMap<>();
        map.put(FIELD_RETURN_URL, mReturnUrl);
        map.put(FIELD_STATUS, mStatus);
        map.put(FIELD_URL, mUrl);
        removeNullAndEmptyParams(map);
        return map;
    }

    @NonNull
    @Override
    public JSONObject toJson() {
        final JSONObject jsonObject = new JSONObject();
        putStringIfNotNull(jsonObject, FIELD_RETURN_URL, mReturnUrl);
        putStringIfNotNull(jsonObject, FIELD_STATUS, mStatus);
        putStringIfNotNull(jsonObject, FIELD_URL, mUrl);
        return jsonObject;
    }

    @Nullable
    public static SourceRedirect fromString(@Nullable String jsonString) {
        try {
            return fromJson(new JSONObject(jsonString));
        } catch (JSONException ignored) {
            return null;
        }
    }

    @Nullable
    public static SourceRedirect fromJson(@Nullable JSONObject jsonObject) {
        if (jsonObject == null) {
            return null;
        }

        final String returnUrl = optString(jsonObject, FIELD_RETURN_URL);
        @Status final String status = asStatus(optString(jsonObject, FIELD_STATUS));
        final String url = optString(jsonObject, FIELD_URL);
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

    @Override
    public boolean equals(@Nullable Object obj) {
        return this == obj || (obj instanceof SourceRedirect && typedEquals((SourceRedirect) obj));
    }

    private boolean typedEquals(@NonNull SourceRedirect sourceRedirect) {
        return ObjectUtils.equals(mReturnUrl, sourceRedirect.mReturnUrl)
                && ObjectUtils.equals(mStatus, sourceRedirect.mStatus)
                && ObjectUtils.equals(mUrl, sourceRedirect.mUrl);
    }

    @Override
    public int hashCode() {
        return ObjectUtils.hash(mReturnUrl, mStatus, mUrl);
    }
}
