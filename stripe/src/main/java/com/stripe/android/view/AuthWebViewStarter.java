package com.stripe.android.view;

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.NonNull;

import com.stripe.android.model.PaymentIntent;

/**
 * A class that manages starting a {@link AuthWebViewActivity} instance with the correct arguments.
 */
public class AuthWebViewStarter {
    static final String EXTRA_AUTH_URL = "auth_url";
    static final String EXTRA_RETURN_URL = "return_url";

    @NonNull private final Activity mActivity;

    public AuthWebViewStarter(@NonNull Activity activity) {
        mActivity = activity;
    }

    /**
     * @param redirectData typically obtained through {@link PaymentIntent#getRedirectData()}
     */
    public void start(@NonNull PaymentIntent.RedirectData redirectData) {
        mActivity.startActivity(new Intent(mActivity, AuthWebViewActivity.class)
                .putExtra(EXTRA_AUTH_URL, redirectData.url.toString())
                .putExtra(EXTRA_RETURN_URL, redirectData.returnUrl.toString()));
    }
}
