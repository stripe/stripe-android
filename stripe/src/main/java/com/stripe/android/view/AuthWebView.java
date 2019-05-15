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
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import com.stripe.android.R;

/**
 * A {@link WebView} used for authenticating payment and deep-linking the user back to the
 * PaymentIntent's return_url[0].
 * <p>
 * [0] https://stripe.com/docs/api/payment_intents/confirm#confirm_payment_intent-return_url
 */
class AuthWebView extends WebView {
    @SuppressWarnings("RedundantModifier")
    public AuthWebView(@NonNull Context context) {
        this(context, null);
    }

    @SuppressWarnings("RedundantModifier")
    public AuthWebView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        configureSettings();
    }

    @SuppressWarnings("RedundantModifier")
    public AuthWebView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        configureSettings();
    }

    void init(@NonNull Activity activity, @NonNull String returnUrl) {
        setWebViewClient(new AuthWebViewClient(activity, returnUrl));
        setWebChromeClient(new AuthWebViewChromeClient(activity));
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void configureSettings() {
        getSettings().setJavaScriptEnabled(true);
        getSettings().setAllowContentAccess(false);
    }

    static class AuthWebViewChromeClient extends WebChromeClient {
        @NonNull private final ProgressBar mProgressBar;

        AuthWebViewChromeClient(@NonNull Activity activity) {
            mProgressBar = activity.findViewById(R.id.auth_web_view_progress_bar);
        }

        @Override
        public void onProgressChanged(@NonNull WebView view, int progress) {
            if (progress < 100) {
                if (mProgressBar.getVisibility() == GONE) {
                    mProgressBar.setVisibility(VISIBLE);
                }
                mProgressBar.setProgress(progress);
            } else if (progress == 100) {
                mProgressBar.setVisibility(GONE);
                mProgressBar.setProgress(0);
            }
        }
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
            if (isReturnUrl(urlString)) {
                mActivity.setResult(Activity.RESULT_OK,
                        new Intent()
                                .setData(Uri.parse(urlString)));
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
