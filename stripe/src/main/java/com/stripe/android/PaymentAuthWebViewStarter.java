package com.stripe.android;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.stripe.android.stripe3ds2.init.ui.StripeToolbarCustomization;
import com.stripe.android.view.AuthActivityStarter;
import com.stripe.android.view.PaymentAuthWebViewActivity;

/**
 * A class that manages starting a {@link PaymentAuthWebViewActivity} instance with the correct
 * arguments.
 */
public class PaymentAuthWebViewStarter
        implements AuthActivityStarter<PaymentAuthWebViewStarter.Data> {
    public static final String EXTRA_AUTH_URL = "auth_url";
    public static final String EXTRA_CLIENT_SECRET = "client_secret";
    public static final String EXTRA_RETURN_URL = "return_url";
    public static final String EXTRA_UI_CUSTOMIZATION = "ui_customization";

    @NonNull private final Host mHost;
    private final int mRequestCode;
    @Nullable private final StripeToolbarCustomization mToolbarCustomization;

    PaymentAuthWebViewStarter(@NonNull Host host, int requestCode) {
        this(host, requestCode, null);
    }

    PaymentAuthWebViewStarter(@NonNull Host host, int requestCode,
                              @Nullable StripeToolbarCustomization toolbarCustomization) {
        mHost = host;
        mRequestCode = requestCode;
        mToolbarCustomization = toolbarCustomization;
    }

    public void start(@NonNull PaymentAuthWebViewStarter.Data data) {
        final Bundle extras = new Bundle();
        extras.putString(EXTRA_CLIENT_SECRET, data.mClientSecret);
        extras.putString(EXTRA_AUTH_URL, data.mUrl);
        extras.putString(EXTRA_RETURN_URL, data.mReturnUrl);
        extras.putParcelable(EXTRA_UI_CUSTOMIZATION, mToolbarCustomization);

        mHost.startActivityForResult(PaymentAuthWebViewActivity.class, extras, mRequestCode);
    }

    static final class Data {
        @NonNull private final String mClientSecret;
        @NonNull private final String mUrl;
        @Nullable private final String mReturnUrl;

        Data(@NonNull String clientSecret,
             @NonNull String url,
             @Nullable String returnUrl) {
            mClientSecret = clientSecret;
            mUrl = url;
            mReturnUrl = returnUrl;
        }
    }
}
