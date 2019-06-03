package com.stripe.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.stripe.android.model.PaymentIntentFixtures;
import com.stripe.android.stripe3ds2.init.ui.StripeToolbarCustomization;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.verify;

@RunWith(RobolectricTestRunner.class)
public class PaymentAuthWebViewStarterTest {
    @Mock private Activity mActivity;
    @Captor private ArgumentCaptor<Intent> mIntentArgumentCaptor;
    @Captor private ArgumentCaptor<Integer> mRequestCodeCaptor;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void start_startsWithCorrectIntentAndRequestCode() {
        new PaymentAuthWebViewStarter(mActivity, 50000)
                .start(PaymentIntentFixtures.REDIRECT_DATA);
        verify(mActivity).startActivityForResult(mIntentArgumentCaptor.capture(),
                mRequestCodeCaptor.capture());

        final Intent intent = mIntentArgumentCaptor.getValue();
        final Bundle extras = intent.getExtras();
        assertNotNull(extras);
        assertNull(extras.getParcelable(PaymentAuthWebViewStarter.EXTRA_UI_CUSTOMIZATION));
        assertEquals(3, extras.size());
    }

    @Test
    public void start_startsWithCorrectIntentAndRequestCodeAndCustomization() {
        new PaymentAuthWebViewStarter(mActivity, 50000,
                new StripeToolbarCustomization()).start(PaymentIntentFixtures.REDIRECT_DATA);
        verify(mActivity).startActivityForResult(mIntentArgumentCaptor.capture(),
                mRequestCodeCaptor.capture());

        final Intent intent = mIntentArgumentCaptor.getValue();
        final Bundle extras = intent.getExtras();
        assertNotNull(extras);
        assertNotNull(extras.getParcelable(PaymentAuthWebViewStarter.EXTRA_UI_CUSTOMIZATION));
        assertEquals(3, extras.size());
    }
}
