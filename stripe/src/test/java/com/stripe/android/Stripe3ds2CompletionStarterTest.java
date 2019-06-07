package com.stripe.android;

import android.app.Activity;
import android.content.Intent;

import com.stripe.android.model.PaymentIntentFixtures;
import com.stripe.android.view.PaymentResultExtras;

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
import static org.mockito.Mockito.verify;

@RunWith(RobolectricTestRunner.class)
public class Stripe3ds2CompletionStarterTest {

    private Stripe3ds2CompletionStarter mStarter;

    @Mock private Activity mActivity;

    @Captor private ArgumentCaptor<Intent> mIntentArgumentCaptor;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mStarter = new Stripe3ds2CompletionStarter(mActivity, 500);
    }

    @Test
    public void start_withCompletion_shouldAddClientSecretAndAuthStatusToIntent() {
        mStarter.start(Stripe3ds2CompletionStarter.StartData.createForComplete(
                PaymentIntentFixtures.PI_REQUIRES_3DS2, "Y"));
        verify(mActivity).startActivityForResult(mIntentArgumentCaptor.capture(), eq(500));
        final Intent intent = mIntentArgumentCaptor.getValue();
        assertEquals(PaymentIntentFixtures.PI_REQUIRES_3DS2.getClientSecret(),
                intent.getStringExtra(PaymentResultExtras.CLIENT_SECRET));
        assertEquals(PaymentIntentResult.Status.SUCCEEDED,
                intent.getIntExtra(PaymentResultExtras.AUTH_STATUS,
                        PaymentIntentResult.Status.UNKNOWN));
    }

    @Test
    public void start_withProtocolError_shouldAddClientSecretAndAuthStatusToIntent() {
        mStarter.start(new Stripe3ds2CompletionStarter.StartData(
                PaymentIntentFixtures.PI_REQUIRES_3DS2,
                Stripe3ds2CompletionStarter.ChallengeFlowOutcome.PROTOCOL_ERROR));
        verify(mActivity).startActivityForResult(mIntentArgumentCaptor.capture(), eq(500));
        final Intent intent = mIntentArgumentCaptor.getValue();
        assertEquals(PaymentIntentFixtures.PI_REQUIRES_3DS2.getClientSecret(),
                intent.getStringExtra(PaymentResultExtras.CLIENT_SECRET));
        assertEquals(PaymentIntentResult.Status.FAILED,
                intent.getIntExtra(PaymentResultExtras.AUTH_STATUS,
                        PaymentIntentResult.Status.UNKNOWN));
    }
}
