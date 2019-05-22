package com.stripe.android;

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.NonNull;

import com.stripe.android.model.PaymentIntent;
import com.stripe.android.view.ActivityStarter;
import com.stripe.android.view.PaymentAuthWebViewActivity;

/**
 * A class that manages starting a {@link PaymentAuthWebViewActivity} instance with the correct
 * arguments.
 */
public class PaymentAuthWebViewStarter implements ActivityStarter<PaymentIntent.RedirectData> {
    public static final String EXTRA_AUTH_URL = "auth_url";
    public static final String EXTRA_RETURN_URL = "return_url";

    @NonNull private final Activity mActivity;
    private final int mRequestCode;

    PaymentAuthWebViewStarter(@NonNull Activity activity, int requestCode) {
        mActivity = activity;
        mRequestCode = requestCode;
    }

    /**
     * @param redirectData typically obtained through {@link PaymentIntent#getRedirectData()}
     *
     */
    public void start(@NonNull PaymentIntent.RedirectData redirectData) {
        final Intent intent = new Intent(mActivity, PaymentAuthWebViewActivity.class)
                .putExtra(EXTRA_AUTH_URL, redirectData.url.toString())
                .putExtra(EXTRA_RETURN_URL, redirectData.returnUrl != null ?
                        redirectData.returnUrl.toString() : null);
        mActivity.startActivityForResult(intent, mRequestCode);
    }
}
