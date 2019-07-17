package com.stripe.android.exception;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Locale;

/**
 * An {@link Exception} that represents a failure to connect to Stripe's API.
 */
public class APIConnectionException extends StripeException {
    private static final int STATUS_CODE = 0;

    @NonNull
    public static APIConnectionException create(@NonNull String url, @NonNull Exception e) {
        final String message = String.format(Locale.ENGLISH,
                "IOException during API request to Stripe (%s): %s. "
                        + "Please check your internet connection and try again. "
                        + "If this problem persists, you should check Stripe's "
                        + "service status at https://twitter.com/stripestatus, "
                        + "or let us know at support@stripe.com.",
                url, e.getMessage());
        return new APIConnectionException(message, e);
    }

    public APIConnectionException(@Nullable String message, @Nullable Throwable e) {
        super(null, message, null, STATUS_CODE, e);
    }

}
