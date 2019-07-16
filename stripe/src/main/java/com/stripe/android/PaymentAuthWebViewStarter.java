package com.stripe.android;

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.stripe.android.stripe3ds2.init.ui.StripeToolbarCustomization;
import com.stripe.android.view.ActivityStarter;
import com.stripe.android.view.PaymentAuthWebViewActivity;

/**
 * A class that manages starting a {@link PaymentAuthWebViewActivity} instance with the correct
 * arguments.
 */
public class PaymentAuthWebViewStarter implements ActivityStarter<PaymentAuthWebViewStarter.Data> {
    public static final String EXTRA_AUTH_URL = "auth_url";
    public static final String EXTRA_CLIENT_SECRET = "client_secret";
    public static final String EXTRA_RETURN_URL = "return_url";
    public static final String EXTRA_UI_CUSTOMIZATION = "ui_customization";

    @NonNull private final Activity mActivity;
    private final int mRequestCode;
    @Nullable private final StripeToolbarCustomization mToolbarCustomization;

    PaymentAuthWebViewStarter(@NonNull Activity activity, int requestCode) {
        this(activity, requestCode, null);
    }

    PaymentAuthWebViewStarter(@NonNull Activity activity, int requestCode,
                              @Nullable StripeToolbarCustomization toolbarCustomization) {
        mActivity = activity;
        mRequestCode = requestCode;
        mToolbarCustomization = toolbarCustomization;
    }

    public void start(@NonNull PaymentAuthWebViewStarter.Data data) {
        final Intent intent = new Intent(mActivity, PaymentAuthWebViewActivity.class)
                .putExtra(EXTRA_CLIENT_SECRET, data.mClientSecret)
                .putExtra(EXTRA_AUTH_URL, data.mUrl)
                .putExtra(EXTRA_RETURN_URL, data.mReturnUrl)
                .putExtra(EXTRA_UI_CUSTOMIZATION, mToolbarCustomization);

        mActivity.startActivityForResult(intent, mRequestCode);
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
