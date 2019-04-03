package com.stripe.android.view.i18n;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.stripe.android.StripeError;

public interface ErrorMessageTranslator {
    /**
     * See https://stripe.com/docs/api/errors for a list of error codes and associated messages
     *
     * @param httpCode The HTTP code associated with the error response.
     * @param errorMessage A human-readable message providing more details about the error.
     *                     For card errors, these messages can be shown to your users.
     * @param stripeError The {@link StripeError} that represents detailed information about the
     *                    error. Specifically, {@link StripeError#code} is useful for understanding
     *                    the underlying error (e.g. "payment_method_unactivated").
     *
     * @return a non-null error message
     */
    @NonNull
    String translate(int httpCode, @Nullable String errorMessage,
                     @Nullable StripeError stripeError);

    class Default implements ErrorMessageTranslator {
        @NonNull
        @Override
        public String translate(int httpCode, @Nullable String errorMessage,
                                @Nullable StripeError stripeError) {
            return errorMessage == null ? "" : errorMessage;
        }
    }
}
