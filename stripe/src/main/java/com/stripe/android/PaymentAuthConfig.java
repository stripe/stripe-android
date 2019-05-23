package com.stripe.android;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import com.stripe.android.stripe3ds2.init.ui.StripeUiCustomization;
import com.stripe.android.stripe3ds2.init.ui.UiCustomization;

/**
 * Configuration for {@link PaymentAuthenticationController}
 */
class PaymentAuthConfig {

    @Nullable
    private static PaymentAuthConfig INSTANCE;

    @NonNull
    private static final PaymentAuthConfig DEFAULT = new PaymentAuthConfig.Builder()
            .set3ds2Config(new Stripe3ds2Config.Builder().build())
            .build();

    static void init(@NonNull PaymentAuthConfig config) {
        INSTANCE = config;
    }

    @NonNull
    static PaymentAuthConfig get() {
        return INSTANCE != null ? INSTANCE : DEFAULT;
    }

    @VisibleForTesting
    static void reset() {
        INSTANCE = null;
    }

    @NonNull
    final Stripe3ds2Config stripe3ds2Config;

    private PaymentAuthConfig(@NonNull Builder builder) {
        stripe3ds2Config = builder.mStripe3ds2Config;
    }

    static final class Builder {
        private Stripe3ds2Config mStripe3ds2Config;

        @NonNull
        Builder set3ds2Config(@NonNull Stripe3ds2Config stripe3ds2Config) {
            this.mStripe3ds2Config = stripe3ds2Config;
            return this;
        }

        @NonNull
        PaymentAuthConfig build() {
            return new PaymentAuthConfig(this);
        }
    }

    static final class Stripe3ds2Config {
        static final int TIMEOUT = 5;

        final int timeout;
        @NonNull final UiCustomization uiCustomization;

        private Stripe3ds2Config(@NonNull Builder builder) {
            timeout = builder.mTimeout;
            uiCustomization = builder.mUiCustomization;
        }

        static final class Builder {
            private int mTimeout = TIMEOUT;
            private UiCustomization mUiCustomization = new StripeUiCustomization();

            @NonNull
            Builder setTimeout(int timeout) {
                this.mTimeout = timeout;
                return this;
            }

            @NonNull
            Builder setUiCustomization(@NonNull UiCustomization uiCustomization) {
                this.mUiCustomization = uiCustomization;
                return this;
            }

            @NonNull
            Stripe3ds2Config build() {
                return new Stripe3ds2Config(this);
            }
        }
    }
}
