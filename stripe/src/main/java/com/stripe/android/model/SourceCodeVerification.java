package com.stripe.android.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringDef;

import com.stripe.android.utils.ObjectUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashMap;
import java.util.Map;

import static com.stripe.android.model.StripeJsonUtils.optString;

/**
 * Model for a
 * https://stripe.com/docs/api/sources/object#source_object-code_verification
 * object in the source api, <em>not</em> source code verification
 */
public final class SourceCodeVerification extends StripeModel {

    // Note: these are the same as the values for the @Redirect.Status StringDef.
    // They don't have to stay the same forever, so they are redefined here.
    @Retention(RetentionPolicy.SOURCE)
    @StringDef({
            PENDING,
            SUCCEEDED,
            FAILED
    })
    @interface Status { }
    static final String PENDING = "pending";
    static final String SUCCEEDED = "succeeded";
    static final String FAILED = "failed";

    private static final String FIELD_ATTEMPTS_REMAINING = "attempts_remaining";
    private static final String FIELD_STATUS = "status";
    private static final int INVALID_ATTEMPTS_REMAINING = -1;

    private final int mAttemptsRemaining;
    @Nullable @Status private final String mStatus;

    private SourceCodeVerification(int attemptsRemaining, @Nullable @Status String status) {
        mAttemptsRemaining = attemptsRemaining;
        mStatus = status;
    }

    public int getAttemptsRemaining() {
        return mAttemptsRemaining;
    }

    @Nullable
    @Status
    public String getStatus() {
        return mStatus;
    }

    @NonNull
    @Override
    public Map<String, Object> toMap() {
        final Map<String, Object> map = new HashMap<>();
        map.put(FIELD_ATTEMPTS_REMAINING, mAttemptsRemaining);
        if (mStatus != null) {
            map.put(FIELD_STATUS, mStatus);
        }
        return map;
    }

    @Nullable
    public static SourceCodeVerification fromString(@Nullable String jsonString) {
        try {
            return fromJson(new JSONObject(jsonString));
        } catch (JSONException ignored) {
            return null;
        }
    }

    @Nullable
    public static SourceCodeVerification fromJson(@Nullable JSONObject jsonObject) {
        if (jsonObject == null) {
            return null;
        }

        return new SourceCodeVerification(
                jsonObject.optInt(FIELD_ATTEMPTS_REMAINING, INVALID_ATTEMPTS_REMAINING),
                asStatus(optString(jsonObject, FIELD_STATUS)));
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
        return this == obj || (obj instanceof SourceCodeVerification
                && typedEquals((SourceCodeVerification) obj));
    }

    private boolean typedEquals(@NonNull SourceCodeVerification sourceCodeVerification) {
        return mAttemptsRemaining == sourceCodeVerification.mAttemptsRemaining
                && ObjectUtils.equals(mStatus, sourceCodeVerification.mStatus);
    }

    @Override
    public int hashCode() {
        return ObjectUtils.hash(mAttemptsRemaining, mStatus);
    }
}
