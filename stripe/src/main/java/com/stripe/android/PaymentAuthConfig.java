package com.stripe.android;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import com.stripe.android.view.threeds2.ThreeDS2UiCustomization;

import java.util.Objects;

/**
 * Configuration for authentication mechanisms via {@link PaymentController}
 */
public final class PaymentAuthConfig {

    @Nullable
    private static PaymentAuthConfig sInstance;

    @NonNull
    private static final PaymentAuthConfig DEFAULT = new PaymentAuthConfig.Builder()
            .set3ds2Config(new Stripe3ds2Config.Builder().build())
            .build();

    public static void init(@NonNull PaymentAuthConfig config) {
        sInstance = config;
    }

    @NonNull
    public static PaymentAuthConfig get() {
        return sInstance != null ? sInstance : DEFAULT;
    }

    @VisibleForTesting
    static void reset() {
        sInstance = null;
    }

    @NonNull final Stripe3ds2Config stripe3ds2Config;

    private PaymentAuthConfig(@NonNull Builder builder) {
        stripe3ds2Config = builder.mStripe3ds2Config;
    }

    public static final class Builder {
        private Stripe3ds2Config mStripe3ds2Config;

        @NonNull
        public Builder set3ds2Config(@NonNull Stripe3ds2Config stripe3ds2Config) {
            this.mStripe3ds2Config = stripe3ds2Config;
            return this;
        }

        @NonNull
        public PaymentAuthConfig build() {
            return new PaymentAuthConfig(this);
        }
    }

    public static final class Stripe3ds2Config {
        static final int DEFAULT_TIMEOUT = 5;

        final int timeout;
        @NonNull final ThreeDS2UiCustomization uiCustomization;

        private Stripe3ds2Config(@NonNull Builder builder) {
            timeout = builder.mTimeout;
            uiCustomization = Objects.requireNonNull(builder.mUiCustomization);
        }

        public static final class Builder {
            private int mTimeout = DEFAULT_TIMEOUT;
            private ThreeDS2UiCustomization mUiCustomization = new ThreeDS2UiCustomization();

            @NonNull
            public Builder setTimeout(int timeout) {
                this.mTimeout = timeout;
                return this;
            }

            @NonNull
            public Builder setUiCustomization(@NonNull ThreeDS2UiCustomization uiCustomization) {
                this.mUiCustomization = uiCustomization;
                return this;
            }

            @NonNull
            public Stripe3ds2Config build() {
                return new Stripe3ds2Config(this);
            }
        }
    }
}
