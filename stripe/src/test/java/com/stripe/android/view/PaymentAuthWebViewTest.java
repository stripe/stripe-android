package com.stripe.android.view;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.webkit.WebView;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@RunWith(RobolectricTestRunner.class)
public class PaymentAuthWebViewTest {

    @Mock private Activity mActivity;
    @Mock private ProgressBar mProgressBar;
    @Mock private WebView mWebView;
    @Mock private PackageManager mPackageManager;

    @Captor private ArgumentCaptor<Intent> mIntentArgumentCaptor;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void shouldOverrideUrlLoading_withPaymentIntent_shouldSetResult() {
        final String deepLink = "stripe://payment_intent_return?payment_intent=pi_123&" +
                "payment_intent_client_secret=pi_123_secret_456&source_type=card";
        final PaymentAuthWebView.PaymentAuthWebViewClient paymentAuthWebViewClient =
                createWebViewClient(
                        "pi_123_secret_456",
                        "stripe://payment_intent_return"
                );
        paymentAuthWebViewClient.shouldOverrideUrlLoading(mWebView, deepLink);
        verify(mActivity).finish();
    }

    @Test
    public void shouldOverrideUrlLoading_withSetupIntent_shouldSetResult() {
        final String deepLink = "stripe://payment_auth?setup_intent=seti_1234" +
                "&setup_intent_client_secret=seti_1234_secret_5678&source_type=card";

        final PaymentAuthWebView.PaymentAuthWebViewClient paymentAuthWebViewClient =
                createWebViewClient("seti_1234_secret_5678", "stripe://payment_auth");
        paymentAuthWebViewClient.shouldOverrideUrlLoading(mWebView, deepLink);
        verify(mActivity).finish();
    }

    @Test
    public void shouldOverrideUrlLoading_withoutReturnUrl_onPaymentIntentImplicitReturnUrl_shouldSetResult() {
        final String deepLink = "stripe://payment_intent_return?payment_intent=pi_123&" +
                "payment_intent_client_secret=pi_123_secret_456&source_type=card";
        final PaymentAuthWebView.PaymentAuthWebViewClient paymentAuthWebViewClient =
                createWebViewClient("pi_123_secret_456");
        paymentAuthWebViewClient.shouldOverrideUrlLoading(mWebView, deepLink);
        verify(mActivity).finish();
    }

    @Test
    public void shouldOverrideUrlLoading_withoutReturnUrl_onSetupIntentImplicitReturnUrl_shouldSetResult() {
        final String deepLink = "stripe://payment_auth?setup_intent=seti_1234" +
                "&setup_intent_client_secret=seti_1234_secret_5678&source_type=card";
        final PaymentAuthWebView.PaymentAuthWebViewClient paymentAuthWebViewClient =
                createWebViewClient("seti_1234_secret_5678");
        paymentAuthWebViewClient.shouldOverrideUrlLoading(mWebView, deepLink);
        verify(mActivity).finish();
    }

    @Test
    public void shouldOverrideUrlLoading_withoutReturnUrl_shouldNotAutoFinishActivity() {
        final PaymentAuthWebView.PaymentAuthWebViewClient paymentAuthWebViewClient =
                createWebViewClient("pi_123_secret_456");
        paymentAuthWebViewClient.shouldOverrideUrlLoading(mWebView,
                "https://example.com");
        verify(mActivity, never()).finish();
    }

    @Test
    public void shouldOverrideUclientSecretrlLoading_witKnownReturnUrl_shouldFinish() {
        final PaymentAuthWebView.PaymentAuthWebViewClient paymentAuthWebViewClient =
                createWebViewClient("pi_123_secret_456");
        paymentAuthWebViewClient.shouldOverrideUrlLoading(mWebView,
                "stripejs://use_stripe_sdk/return_url");
        verify(mActivity).finish();
    }

    @Test
    public void onPageFinished_wit3DSecureCompleteUrl_shouldFinish() {
        final PaymentAuthWebView.PaymentAuthWebViewClient paymentAuthWebViewClient =
                createWebViewClient("pi_123_secret_456");
        paymentAuthWebViewClient.onPageFinished(mWebView,
                "https://hooks.stripe.com/3d_secure/complete/tdsrc_1ExLWoCRMbs6FrXfjPJRYtng");
        verify(mActivity).finish();
    }

    @Test
    public void onPageFinished_witRedirectCompleteUrl_shouldFinish() {
        final PaymentAuthWebView.PaymentAuthWebViewClient paymentAuthWebViewClient =
                createWebViewClient("pi_123_secret_456");
        paymentAuthWebViewClient.onPageFinished(mWebView,
                "https://hooks.stripe.com/redirect/complete/src_1ExLWoCRMbs6FrXfjPJRYtng");
        verify(mActivity).finish();
    }

    @Test
    public void shouldOverrideUrlLoading_withOpaqueUri_shouldNotCrash() {
        final String deepLink = "mailto:patrick@example.com?payment_intent=pi_123&" +
                "payment_intent_client_secret=pi_123_secret_456&source_type=card";
        final PaymentAuthWebView.PaymentAuthWebViewClient paymentAuthWebViewClient =
                createWebViewClient("pi_123_secret_456");
        paymentAuthWebViewClient.shouldOverrideUrlLoading(mWebView, deepLink);
    }

    @Test
    public void shouldOverrideUrlLoading_withUnsupportedDeeplink_shouldFinish() {
        final String deepLink = "deep://link";
        final PaymentAuthWebView.PaymentAuthWebViewClient paymentAuthWebViewClient =
                createWebViewClient("pi_123_secret_456");
        paymentAuthWebViewClient.shouldOverrideUrlLoading(mWebView, deepLink);
        verify(mActivity).finish();
    }

    @Test
    public void shouldOverrideUlLoading_withIntentUri_shouldParseUri() {
        final String deepLink =
                "intent://example.com/#Intent;scheme=https;action=android.intent.action.VIEW;end";
        final PaymentAuthWebView.PaymentAuthWebViewClient paymentAuthWebViewClient =
                createWebViewClient("pi_123_secret_456");
        paymentAuthWebViewClient.shouldOverrideUrlLoading(mWebView, deepLink);
        verify(mPackageManager).resolveActivity(
                mIntentArgumentCaptor.capture(),
                eq(PackageManager.MATCH_DEFAULT_ONLY)
        );
        final Intent intent = mIntentArgumentCaptor.getValue();
        assertEquals("https://example.com/", intent.getDataString());
        verify(mActivity).finish();
    }

    @NonNull
    private PaymentAuthWebView.PaymentAuthWebViewClient createWebViewClient(
            @NonNull String clientSecret
    ) {
        return createWebViewClient(clientSecret, null);
    }

    @NonNull
    private PaymentAuthWebView.PaymentAuthWebViewClient createWebViewClient(
            @NonNull String clientSecret,
            @Nullable String returnUrl
    ) {
        return new PaymentAuthWebView.PaymentAuthWebViewClient(
                mActivity,
                mPackageManager,
                mProgressBar,
                clientSecret,
                returnUrl
        );
    }
}
