package com.stripe.android.model;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import java.util.Map;
import java.util.Objects;

/**
 * An interface for methods available in {@link PaymentIntent}
 */
public interface StripeIntent {
    boolean requiresAction();

    boolean requiresConfirmation();

    @Nullable
    PaymentIntent.NextActionType getNextActionType();

    @Nullable
    PaymentIntent.RedirectData getRedirectData();

    @Nullable
    String getClientSecret();

    @Nullable
    SdkData getStripeSdkData();

    @Nullable
    PaymentIntent.Status getStatus();

    /**
     * See https://stripe.com/docs/api/payment_intents/object#payment_intent_object-next_action-type
     */
    enum NextActionType {
        RedirectToUrl("redirect_to_url"),
        UseStripeSdk("use_stripe_sdk");

        @NonNull public final String code;

        NextActionType(@NonNull String code) {
            this.code = code;
        }

        @Nullable
        public static NextActionType fromCode(@Nullable String code) {
            if (code == null) {
                return null;
            }

            for (NextActionType nextActionType : values()) {
                if (nextActionType.code.equals(code)) {
                    return nextActionType;
                }
            }

            return null;
        }

        @NonNull
        @Override
        public String toString() {
            return code;
        }
    }

    /**
     * See https://stripe.com/docs/api/payment_intents/object#payment_intent_object-status
     */
    enum Status {
        Canceled("canceled"),
        Processing("processing"),
        RequiresAction("requires_action"),
        RequiresAuthorization("requires_authorization"),
        RequiresCapture("requires_capture"),
        RequiresConfirmation("requires_confirmation"),
        RequiresPaymentMethod("requires_payment_method"),
        Succeeded("succeeded");

        @NonNull
        public final String code;

        Status(@NonNull String code) {
            this.code = code;
        }

        @Nullable
        public static Status fromCode(@Nullable String code) {
            if (code == null) {
                return null;
            }

            for (Status status : values()) {
                if (status.code.equals(code)) {
                    return status;
                }
            }

            return null;
        }

        @NonNull
        @Override
        public String toString() {
            return code;
        }
    }

    final class SdkData {
        private static final String FIELD_TYPE = "type";

        private static final String TYPE_3DS2 = "stripe_3ds2_fingerprint";
        private static final String TYPE_3DS1 = "three_d_secure_redirect";

        @NonNull final String type;
        @NonNull final Map<String, ?> data;

        SdkData(@NonNull Map<String, ?> data) {
            this.type = Objects.requireNonNull((String) data.get(FIELD_TYPE));
            this.data = data;
        }

        public boolean is3ds2() {
            return TYPE_3DS2.equals(type);
        }

        public boolean is3ds1() {
            return TYPE_3DS1.equals(type);
        }
    }

    final class RedirectData {
        static final String FIELD_URL = "url";
        static final String FIELD_RETURN_URL = "return_url";

        /**
         * See <a href="https://stripe.com/docs/api
         * /payment_intents/object#payment_intent_object-next_action-redirect_to_url-url">
         * PaymentIntent.next_action.redirect_to_url.url
         * </a>
         */
        @NonNull public final Uri url;

        /**
         * See <a href="https://stripe.com/docs/api
         * /payment_intents/object#payment_intent_object-next_action-redirect_to_url-return_url">
         * PaymentIntent.next_action.redirect_to_url.return_url
         * </a>
         */
        @Nullable public final Uri returnUrl;

        @Nullable
        static RedirectData create(@NonNull Map<?, ?> redirectToUrlHash) {
            final Object urlObj = redirectToUrlHash.get(FIELD_URL);
            final Object returnUrlObj = redirectToUrlHash.get(FIELD_RETURN_URL);
            final String url = (urlObj instanceof String) ? urlObj.toString() : null;
            final String returnUrl = (returnUrlObj instanceof String) ?
                    returnUrlObj.toString() : null;
            if (url == null) {
                return null;
            }

            return new RedirectData(url, returnUrl);
        }

        @VisibleForTesting
        RedirectData(@NonNull String url, @Nullable String returnUrl) {
            this.url = Uri.parse(url);
            this.returnUrl = returnUrl != null ? Uri.parse(returnUrl) : null;
        }
    }
}
