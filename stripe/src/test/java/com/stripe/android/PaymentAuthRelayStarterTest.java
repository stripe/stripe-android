package com.stripe.android;

import android.app.Activity;
import android.content.Intent;

import com.stripe.android.model.PaymentIntentFixtures;
import com.stripe.android.view.PaymentAuthenticationExtras;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@RunWith(RobolectricTestRunner.class)
public class PaymentAuthRelayStarterTest {
    @Mock private Activity mActivity;
    @Captor private ArgumentCaptor<Intent> mIntentArgumentCaptor;

    private PaymentAuthRelayStarter mStarter;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mStarter = new PaymentAuthRelayStarter(mActivity, 500);
    }

    @Test
    public void start_withPaymentIntent_shouldSetCorrectIntentExtras() {
        mStarter.start(new PaymentAuthRelayStarter.Data(PaymentIntentFixtures.PI_REQUIRES_3DS2));
        verify(mActivity).startActivityForResult(mIntentArgumentCaptor.capture(), eq(500));
        final Intent intent = mIntentArgumentCaptor.getValue();
        assertEquals(PaymentIntentFixtures.PI_REQUIRES_3DS2.getClientSecret(),
                intent.getStringExtra(PaymentAuthenticationExtras.CLIENT_SECRET));
        assertNull(intent.getSerializableExtra(PaymentAuthenticationExtras.AUTH_EXCEPTION));
    }

    @Test
    public void start_withException_shouldSetCorrectIntentExtras() {
        final Exception exception = new RuntimeException();
        mStarter.start(new PaymentAuthRelayStarter.Data(exception));
        verify(mActivity).startActivityForResult(mIntentArgumentCaptor.capture(), eq(500));
        final Intent intent = mIntentArgumentCaptor.getValue();
        assertNull(intent.getStringExtra(PaymentAuthenticationExtras.CLIENT_SECRET));
        assertEquals(exception,
                intent.getSerializableExtra(PaymentAuthenticationExtras.AUTH_EXCEPTION));
    }
}