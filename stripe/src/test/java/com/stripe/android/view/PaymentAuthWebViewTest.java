package com.stripe.android.view;

import android.app.Activity;
import android.webkit.WebView;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@RunWith(RobolectricTestRunner.class)
public class PaymentAuthWebViewTest {

    @Mock private Activity mActivity;
    @Mock private WebView mWebView;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void shouldOverrideUrlLoading_withPaymentIntent_shouldSetResult() {
        final String deepLink = "stripe://payment_intent_return?payment_intent=pi_123&" +
                "payment_intent_client_secret=pi_123_secret_456&source_type=card";
        final PaymentAuthWebView.PaymentAuthWebViewClient paymentAuthWebViewClient =
                new PaymentAuthWebView.PaymentAuthWebViewClient(mActivity,
                        "pi_123_secret_456",
                        "stripe://payment_intent_return");
        paymentAuthWebViewClient.shouldOverrideUrlLoading(mWebView, deepLink);
        verify(mActivity).finish();
    }

    @Test
    public void shouldOverrideUrlLoading_withSetupIntent_shouldSetResult() {
        final String deepLink = "stripe://payment_auth?setup_intent=seti_1234" +
                "&setup_intent_client_secret=seti_1234_secret_5678&source_type=card";

        final PaymentAuthWebView.PaymentAuthWebViewClient paymentAuthWebViewClient =
                new PaymentAuthWebView.PaymentAuthWebViewClient(mActivity,
                        "seti_1234_secret_5678",
                        "stripe://payment_auth");
        paymentAuthWebViewClient.shouldOverrideUrlLoading(mWebView, deepLink);
        verify(mActivity).finish();
    }

    @Test
    public void shouldOverrideUrlLoading_withoutReturnUrl_onPaymentIntentImplicitReturnUrl_shouldSetResult() {
        final String deepLink = "stripe://payment_intent_return?payment_intent=pi_123&" +
                "payment_intent_client_secret=pi_123_secret_456&source_type=card";
        final PaymentAuthWebView.PaymentAuthWebViewClient paymentAuthWebViewClient =
                new PaymentAuthWebView.PaymentAuthWebViewClient(mActivity,
                        "pi_123_secret_456", null);
        paymentAuthWebViewClient.shouldOverrideUrlLoading(mWebView, deepLink);
        verify(mActivity).finish();
    }

    @Test
    public void shouldOverrideUrlLoading_withoutReturnUrl_onSetupIntentImplicitReturnUrl_shouldSetResult() {
        final String deepLink = "stripe://payment_auth?setup_intent=seti_1234" +
                "&setup_intent_client_secret=seti_1234_secret_5678&source_type=card";
        final PaymentAuthWebView.PaymentAuthWebViewClient paymentAuthWebViewClient =
                new PaymentAuthWebView.PaymentAuthWebViewClient(mActivity,
                        "seti_1234_secret_5678", null);
        paymentAuthWebViewClient.shouldOverrideUrlLoading(mWebView, deepLink);
        verify(mActivity).finish();
    }

    @Test
    public void shouldOverrideUrlLoading_withoutReturnUrl_shouldNotAutoFinishActivity() {
        final PaymentAuthWebView.PaymentAuthWebViewClient paymentAuthWebViewClient =
                new PaymentAuthWebView.PaymentAuthWebViewClient(mActivity,
                        "pi_123_secret_456", null);
        paymentAuthWebViewClient.shouldOverrideUrlLoading(mWebView,
                "https://example.com");
        verify(mActivity, never()).finish();
    }

    @Test
    public void shouldOverrideUrlLoading_witKnownReturnUrl_shouldFinish() {
        final PaymentAuthWebView.PaymentAuthWebViewClient paymentAuthWebViewClient =
                new PaymentAuthWebView.PaymentAuthWebViewClient(mActivity,
                        "pi_123_secret_456", null);
        paymentAuthWebViewClient.shouldOverrideUrlLoading(mWebView,
                "stripejs://use_stripe_sdk/return_url");
        verify(mActivity).finish();
    }
}
