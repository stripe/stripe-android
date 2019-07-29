package com.stripe.android.view;

import android.webkit.WebView;
import android.widget.ProgressBar;

import com.stripe.android.StripeIntentResult;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@RunWith(RobolectricTestRunner.class)
public class PaymentAuthWebViewTest {

    @Mock private PaymentAuthWebView.PaymentAuthWebViewClient.Listener mListener;
    @Mock private ProgressBar mProgressBar;
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
                new PaymentAuthWebView.PaymentAuthWebViewClient(mListener, mProgressBar,
                        "pi_123_secret_456",
                        "stripe://payment_intent_return");
        paymentAuthWebViewClient.shouldOverrideUrlLoading(mWebView, deepLink);
        verify(mListener).onAuthCompleted(StripeIntentResult.Status.SUCCEEDED);
    }

    @Test
    public void shouldOverrideUrlLoading_withSetupIntent_shouldSetResult() {
        final String deepLink = "stripe://payment_auth?setup_intent=seti_1234" +
                "&setup_intent_client_secret=seti_1234_secret_5678&source_type=card";

        final PaymentAuthWebView.PaymentAuthWebViewClient paymentAuthWebViewClient =
                new PaymentAuthWebView.PaymentAuthWebViewClient(mListener, mProgressBar,
                        "seti_1234_secret_5678",
                        "stripe://payment_auth");
        paymentAuthWebViewClient.shouldOverrideUrlLoading(mWebView, deepLink);
        verify(mListener).onAuthCompleted(StripeIntentResult.Status.SUCCEEDED);
    }

    @Test
    public void shouldOverrideUrlLoading_withoutReturnUrl_onPaymentIntentImplicitReturnUrl_shouldSetResult() {
        final String deepLink = "stripe://payment_intent_return?payment_intent=pi_123&" +
                "payment_intent_client_secret=pi_123_secret_456&source_type=card";
        final PaymentAuthWebView.PaymentAuthWebViewClient paymentAuthWebViewClient =
                new PaymentAuthWebView.PaymentAuthWebViewClient(mListener, mProgressBar,
                        "pi_123_secret_456", null);
        paymentAuthWebViewClient.shouldOverrideUrlLoading(mWebView, deepLink);
        verify(mListener).onAuthCompleted(StripeIntentResult.Status.SUCCEEDED);
    }

    @Test
    public void shouldOverrideUrlLoading_withoutReturnUrl_onSetupIntentImplicitReturnUrl_shouldSetResult() {
        final String deepLink = "stripe://payment_auth?setup_intent=seti_1234" +
                "&setup_intent_client_secret=seti_1234_secret_5678&source_type=card";
        final PaymentAuthWebView.PaymentAuthWebViewClient paymentAuthWebViewClient =
                new PaymentAuthWebView.PaymentAuthWebViewClient(mListener, mProgressBar,
                        "seti_1234_secret_5678", null);
        paymentAuthWebViewClient.shouldOverrideUrlLoading(mWebView, deepLink);
        verify(mListener).onAuthCompleted(StripeIntentResult.Status.SUCCEEDED);
    }

    @Test
    public void shouldOverrideUrlLoading_withoutReturnUrl_shouldNotAutoFinishActivity() {
        final PaymentAuthWebView.PaymentAuthWebViewClient paymentAuthWebViewClient =
                new PaymentAuthWebView.PaymentAuthWebViewClient(mListener, mProgressBar,
                        "pi_123_secret_456", null);
        paymentAuthWebViewClient.shouldOverrideUrlLoading(mWebView,
                "https://example.com");
        verify(mListener, never()).onAuthCompleted(anyInt());
    }

    @Test
    public void shouldOverrideUrlLoading_witKnownReturnUrl_shouldFinish() {
        final PaymentAuthWebView.PaymentAuthWebViewClient paymentAuthWebViewClient =
                new PaymentAuthWebView.PaymentAuthWebViewClient(mListener, mProgressBar,
                        "pi_123_secret_456", null);
        paymentAuthWebViewClient.shouldOverrideUrlLoading(mWebView,
                "stripejs://use_stripe_sdk/return_url");
        verify(mListener).onAuthCompleted(StripeIntentResult.Status.SUCCEEDED);
    }

    @Test
    public void onPageFinished_wit3DSecureCompleteUrl_shouldFinish() {
        final PaymentAuthWebView.PaymentAuthWebViewClient paymentAuthWebViewClient =
                new PaymentAuthWebView.PaymentAuthWebViewClient(mListener, mProgressBar,
                        "pi_123_secret_456", null);
        paymentAuthWebViewClient.onPageFinished(mWebView,
                "https://hooks.stripe.com/3d_secure/complete/tdsrc_1ExLWoCRMbs6FrXfjPJRYtng");
        verify(mListener).onAuthCompleted(StripeIntentResult.Status.SUCCEEDED);
    }

    @Test
    public void onPageFinished_witRedirectCompleteUrl_shouldFinish() {
        final PaymentAuthWebView.PaymentAuthWebViewClient paymentAuthWebViewClient =
                new PaymentAuthWebView.PaymentAuthWebViewClient(mListener, mProgressBar,
                        "pi_123_secret_456", null);
        paymentAuthWebViewClient.onPageFinished(mWebView,
                "https://hooks.stripe.com/redirect/complete/src_1ExLWoCRMbs6FrXfjPJRYtng");
        verify(mListener).onAuthCompleted(StripeIntentResult.Status.SUCCEEDED);
    }
}
