package com.stripe.android.view;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import com.stripe.android.R;

import java.util.Set;

/**
 * A {@link WebView} used for authenticating payment and deep-linking the user back to the
 * PaymentIntent's return_url[0].
 * <p>
 * [0] https://stripe.com/docs/api/payment_intents/confirm#confirm_payment_intent-return_url
 */
class PaymentAuthWebView extends WebView {
    @SuppressWarnings("RedundantModifier")
    public PaymentAuthWebView(@NonNull Context context) {
        this(context, null);
    }

    @SuppressWarnings("RedundantModifier")
    public PaymentAuthWebView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        configureSettings();
    }

    @SuppressWarnings("RedundantModifier")
    public PaymentAuthWebView(@NonNull Context context, @Nullable AttributeSet attrs,
                              int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        configureSettings();
    }

    void init(@NonNull Activity activity, @NonNull String clientSecret, @NonNull String returnUrl) {
        setWebViewClient(new PaymentAuthWebViewClient(activity, clientSecret, returnUrl));
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void configureSettings() {
        getSettings().setJavaScriptEnabled(true);
        getSettings().setAllowContentAccess(false);
    }

    static class PaymentAuthWebViewClient extends WebViewClient {
        static final String PARAM_PAYMENT_CLIENT_SECRET = "payment_intent_client_secret";
        static final String PARAM_SETUP_CLIENT_SECRET = "setup_intent_client_secret";

        @NonNull private final Activity mActivity;
        @NonNull private final String mClientSecret;
        @Nullable private final Uri mReturnUrl;
        @NonNull private final ProgressBar mProgressBar;

        PaymentAuthWebViewClient(@NonNull Activity activity, @NonNull String clientSecret,
                                 @Nullable String returnUrl) {
            mActivity = activity;
            mClientSecret = clientSecret;
            mReturnUrl = returnUrl != null ? Uri.parse(returnUrl) : null;
            mProgressBar = activity.findViewById(R.id.auth_web_view_progress_bar);
        }

        @Override
        public void onPageCommitVisible(@NonNull WebView view, @NonNull String url) {
            super.onPageCommitVisible(view, url);
            mProgressBar.setVisibility(GONE);
        }

        @Override
        public boolean shouldOverrideUrlLoading(@NonNull WebView view,
                                                @NonNull String urlString) {
            final Uri uri = Uri.parse(urlString);
            if (isReturnUrl(uri)) {
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

        private boolean isReturnUrl(@NonNull Uri uri) {
            if (mReturnUrl != null) {
                // If the `returnUrl` is known, look for URIs that match it.

                return mReturnUrl.getScheme() != null &&
                        mReturnUrl.getScheme().equals(uri.getScheme()) &&
                        mReturnUrl.getHost() != null &&
                        mReturnUrl.getHost().equals(uri.getHost());
            } else {
                // If the `returnUrl` is unknown, look for URIs that contain a
                // `payment_intent_client_secret` or `setup_intent_client_secret`
                // query parameter, and check if its values matches the given `clientSecret`
                // as a query parameter.

                final Set<String> paramNames = uri.getQueryParameterNames();
                final String clientSecret;
                if (paramNames.contains(PARAM_PAYMENT_CLIENT_SECRET)) {
                    clientSecret = uri.getQueryParameter(PARAM_PAYMENT_CLIENT_SECRET);
                } else if (paramNames.contains(PARAM_SETUP_CLIENT_SECRET)) {
                    clientSecret = uri.getQueryParameter(PARAM_SETUP_CLIENT_SECRET);
                } else {
                    clientSecret = null;
                }
                return mClientSecret.equals(clientSecret);
            }
        }
    }
}
