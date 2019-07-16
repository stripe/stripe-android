package com.stripe.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

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
    private static final String CLIENT_SECRET =
            "pi_1EceMnCRMbs6FrXfCXdF8dnx_secret_vew0L3IGaO0x9o0eyRMGzKr0k";
    private static final PaymentAuthWebViewStarter.Data DATA =
            new PaymentAuthWebViewStarter.Data(
                    CLIENT_SECRET,
                    "https://hooks.stripe.com/",
                    "stripe://payment-auth"
            );

    @Mock private Activity mActivity;
    @Captor private ArgumentCaptor<Intent> mIntentArgumentCaptor;
    @Captor private ArgumentCaptor<Integer> mRequestCodeCaptor;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void start_startsWithCorrectIntentAndRequestCode() {
        new PaymentAuthWebViewStarter(mActivity, 50000).start(DATA);
        verify(mActivity).startActivityForResult(mIntentArgumentCaptor.capture(),
                mRequestCodeCaptor.capture());

        final Intent intent = mIntentArgumentCaptor.getValue();
        final Bundle extras = intent.getExtras();
        assertNotNull(extras);
        assertNull(extras.getParcelable(PaymentAuthWebViewStarter.EXTRA_UI_CUSTOMIZATION));
        assertEquals(4, extras.size());
        assertEquals(CLIENT_SECRET,
                extras.getString(PaymentAuthWebViewStarter.EXTRA_CLIENT_SECRET));
    }

    @Test
    public void start_startsWithCorrectIntentAndRequestCodeAndCustomization() {
        new PaymentAuthWebViewStarter(mActivity, 50000,
                new StripeToolbarCustomization()).start(DATA);
        verify(mActivity).startActivityForResult(mIntentArgumentCaptor.capture(),
                mRequestCodeCaptor.capture());

        final Intent intent = mIntentArgumentCaptor.getValue();
        final Bundle extras = intent.getExtras();
        assertNotNull(extras);
        assertNotNull(extras.getParcelable(PaymentAuthWebViewStarter.EXTRA_UI_CUSTOMIZATION));
        assertEquals(4, extras.size());
        assertEquals(CLIENT_SECRET,
                extras.getString(PaymentAuthWebViewStarter.EXTRA_CLIENT_SECRET));
    }
}
