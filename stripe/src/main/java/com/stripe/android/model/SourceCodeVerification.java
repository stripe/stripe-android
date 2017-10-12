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

import static com.stripe.android.model.StripeJsonUtils.optString;
/**
 * Model for a
 * {@url https://stripe.com/docs/api#source_object-code_verification code_verification}
 * object in the source api, <em>not</em> source code verification
 */
public class SourceCodeVerification extends StripeJsonModel {

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

    private int mAttemptsRemaining;
    private @Status String mStatus;

    SourceCodeVerification(int attemptsRemaining, @Status String status) {
        mAttemptsRemaining = attemptsRemaining;
        mStatus = status;
    }

    public int getAttemptsRemaining() {
        return mAttemptsRemaining;
    }

    void setAttemptsRemaining(int attemptsRemaining) {
        mAttemptsRemaining = attemptsRemaining;
    }

    @Status
    public String getStatus() {
        return mStatus;
    }

    void setStatus(@Status String status) {
        mStatus = status;
    }

    @NonNull
    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> hashMap = new HashMap<>();
        hashMap.put(FIELD_ATTEMPTS_REMAINING, mAttemptsRemaining);
        if (mStatus != null) {
            hashMap.put(FIELD_STATUS, mStatus);
        }
        return hashMap;
    }

    @NonNull
    @Override
    public JSONObject toJson() {
        JSONObject jsonObject = new JSONObject();

        try {
            jsonObject.put(FIELD_ATTEMPTS_REMAINING, mAttemptsRemaining);
            StripeJsonUtils.putStringIfNotNull(jsonObject, FIELD_STATUS, mStatus);
        } catch (JSONException ignored) { }

        return jsonObject;
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
}
