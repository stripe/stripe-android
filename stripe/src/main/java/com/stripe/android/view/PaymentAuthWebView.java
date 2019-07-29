package com.stripe.android.view;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
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

import com.stripe.android.StripeIntentResult;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * A {@link WebView} used for authenticating payment details
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

    void init(@NonNull PaymentAuthWebViewClient.Listener listener, @NonNull ProgressBar progressBar,
              @NonNull String clientSecret, @NonNull String returnUrl) {
        setWebViewClient(new PaymentAuthWebViewClient(listener, progressBar, clientSecret,
                returnUrl));
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void configureSettings() {
        getSettings().setJavaScriptEnabled(true);
        getSettings().setAllowContentAccess(false);
    }

    static class PaymentAuthWebViewClient extends WebViewClient {
        static final String PARAM_PAYMENT_CLIENT_SECRET = "payment_intent_client_secret";
        static final String PARAM_SETUP_CLIENT_SECRET = "setup_intent_client_secret";

        private static final Set<String> COMPLETION_URLS =
                new HashSet<>(Arrays.asList(
                        "https://hooks.stripe.com/redirect/complete/src_",
                        "https://hooks.stripe.com/3d_secure/complete/tdsrc_"
                ));

        @NonNull private final String mClientSecret;
        @Nullable private final Uri mReturnUrl;
        @NonNull private final ProgressBar mProgressBar;
        @NonNull private final Listener mListener;

        PaymentAuthWebViewClient(@NonNull Listener listener, @NonNull ProgressBar progressBar,
                                 @NonNull String clientSecret, @Nullable String returnUrl) {
            mListener = listener;
            mClientSecret = clientSecret;
            mReturnUrl = returnUrl != null ? Uri.parse(returnUrl) : null;
            mProgressBar = progressBar;
        }

        @Override
        public void onPageCommitVisible(@NonNull WebView view, @NonNull String url) {
            super.onPageCommitVisible(view, url);
            mProgressBar.setVisibility(GONE);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            if (url != null && isCompletionUrl(url)) {
                onAuthCompleted();
            }
        }

        private boolean isCompletionUrl(@NonNull String url) {
            for (String completionUrl : COMPLETION_URLS) {
                if (url.startsWith(completionUrl)) {
                    return true;
                }
            }

            return false;
        }

        @Override
        public boolean shouldOverrideUrlLoading(@NonNull WebView view,
                                                @NonNull String urlString) {
            final Uri uri = Uri.parse(urlString);
            if (isReturnUrl(uri)) {
                onAuthCompleted();
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
            if (isPredefinedReturnUrl(uri)) {
                return true;
            } else if (mReturnUrl != null) {
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

        // pre-defined return URLs
        private boolean isPredefinedReturnUrl(@NonNull Uri uri) {
            return "stripejs://use_stripe_sdk/return_url".equals(uri.toString());
        }

        private void onAuthCompleted() {
            mListener.onAuthCompleted(StripeIntentResult.Status.SUCCEEDED);
        }

        interface Listener {
            void onAuthCompleted(@StripeIntentResult.Status int status);
        }
    }
}
