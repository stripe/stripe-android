package com.stripe.android.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;

import org.json.JSONException;
import org.json.JSONObject;

import static com.stripe.android.model.StripeJsonUtils.optString;

/**
 * Model for a
 * <a href="https://stripe.com/docs/api/sources/object#source_object-code_verification">
 *     code verification</a> object in the Sources API.
 *
 * <em>Not</em> source code verification.
 */
public final class SourceCodeVerification extends StripeModel {

    // Note: these are the same as the values for the @Redirect.Status StringDef.
    // They don't have to stay the same forever, so they are redefined here.
    @Retention(RetentionPolicy.SOURCE)
    @StringDef({
            Status.PENDING,
            Status.SUCCEEDED,
            Status.FAILED
    })
    @interface Status {
        String PENDING = "pending";
        String SUCCEEDED = "succeeded";
        String FAILED = "failed";
    }

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
        if (Status.PENDING.equals(stringStatus)) {
            return Status.PENDING;
        } else if (Status.SUCCEEDED.equals(stringStatus)) {
            return Status.SUCCEEDED;
        } else if (Status.FAILED.equals(stringStatus)) {
            return Status.FAILED;
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
                && Objects.equals(mStatus, sourceCodeVerification.mStatus);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mAttemptsRemaining, mStatus);
    }
}
