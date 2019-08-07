package com.stripe.android.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringDef;
import android.support.annotation.VisibleForTesting;

import com.stripe.android.utils.ObjectUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static com.stripe.android.model.StripeJsonUtils.optString;

/**
 * Model for a <a href="https://stripe.com/docs/api/sources/object#source_object-redirect">
 *     redirect</a> object in the source api.
 */
public final class SourceRedirect extends StripeModel {

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({
            Status.PENDING,
            Status.SUCCEEDED,
            Status.FAILED,
            Status.NOT_REQUIRED
    })
    @interface Status {
        String PENDING = "pending";
        String SUCCEEDED = "succeeded";
        String FAILED = "failed";
        String NOT_REQUIRED = "not_required";
    }

    private static final String FIELD_RETURN_URL = "return_url";
    private static final String FIELD_STATUS = "status";
    private static final String FIELD_URL = "url";

    @Nullable private final String mReturnUrl;
    @Nullable @Status private final String mStatus;
    @Nullable private final String mUrl;

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

    @Nullable
    @Status
    public String getStatus() {
        return mStatus;
    }

    @Nullable
    public String getUrl() {
        return mUrl;
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
    @VisibleForTesting
    static String asStatus(@Nullable String stringStatus) {
        if (Status.PENDING.equals(stringStatus)) {
            return Status.PENDING;
        } else if (Status.SUCCEEDED.equals(stringStatus)) {
            return Status.SUCCEEDED;
        } else if (Status.FAILED.equals(stringStatus)) {
            return Status.FAILED;
        } else if (Status.NOT_REQUIRED.equals(stringStatus)) {
            return Status.NOT_REQUIRED;
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
