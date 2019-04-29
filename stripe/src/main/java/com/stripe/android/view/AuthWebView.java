package com.stripe.android.view;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * A {@link WebView} used for authenticating payment and deep-linking the user back to the
 * PaymentIntent's return_url[0].
 *
 * [0] https://stripe.com/docs/api/payment_intents/confirm#confirm_payment_intent-return_url
 */
class AuthWebView extends WebView {
    AuthWebView(@NonNull Context context) {
        this(context, null);
    }

    AuthWebView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        configureSettings();
    }

    AuthWebView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        configureSettings();
    }

    void init(@NonNull Activity activity, @NonNull String returnUrl) {
        setWebViewClient(new AuthWebViewClient(activity, returnUrl));
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void configureSettings() {
        getSettings().setJavaScriptEnabled(true);
        getSettings().setAllowContentAccess(false);
    }

    static class AuthWebViewClient extends WebViewClient {
        @NonNull private final Activity mActivity;
        @NonNull private final Uri mReturnUrl;

        AuthWebViewClient(@NonNull Activity activity, @NonNull String returnUrl) {
            mActivity = activity;
            mReturnUrl = Uri.parse(returnUrl);
        }

        @Override
        public boolean shouldOverrideUrlLoading(@NonNull WebView view,
                                                @NonNull String urlString) {
            final Uri uri = Uri.parse(urlString);
            if (isReturnUrl(urlString)) {
                mActivity.startActivity(new Intent(Intent.ACTION_VIEW)
                        .setData(uri));
                mActivity.finish();
                return true;
            }

            return super.shouldOverrideUrlLoading(view, urlString);
        }

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        @Override
        public boolean shouldOverrideUrlLoading(@NonNull WebView view,
                                                @NonNull WebResourceRequest request) {
            return shouldOverrideUrlLoading(view, request.getUrl().toString());
        }

        private boolean isReturnUrl(@NonNull String urlString) {
            final Uri uri = Uri.parse(urlString);
            return mReturnUrl.getScheme() != null &&
                    mReturnUrl.getScheme().equals(uri.getScheme()) &&
                    mReturnUrl.getHost() != null &&
                    mReturnUrl.getHost().equals(uri.getHost());
        }
    }
}
