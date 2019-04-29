package com.stripe.android.view;

import android.app.Activity;
import android.content.Intent;
import android.webkit.WebView;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.*;
import static org.mockito.Mockito.verify;

@RunWith(RobolectricTestRunner.class)
public class AuthWebViewTest {

    @Mock private Activity mActivity;
    @Mock private WebView mWebView;
    @Captor private ArgumentCaptor<Intent> mIntentArgumentCaptor;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void shouldOverrideUrlLoading_withDeepLink_shouldStartDeepLink() {
        final String deepLink = "stripe://payment_intent_return?payment_intent=pi_123&" +
                        "payment_intent_client_secret=pi_123_secret_456&source_type=card";
        final AuthWebView.AuthWebViewClient authWebViewClient =
                new AuthWebView.AuthWebViewClient(mActivity,
                        "stripe://payment_intent_return");
        authWebViewClient.shouldOverrideUrlLoading(mWebView, deepLink);
        verify(mActivity).startActivity(mIntentArgumentCaptor.capture());
        verify(mActivity).finish();

        final Intent intent = mIntentArgumentCaptor.getValue();
        assertEquals(deepLink, intent.getDataString());
    }
}
