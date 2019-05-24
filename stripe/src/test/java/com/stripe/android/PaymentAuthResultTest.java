package com.stripe.android;

import com.stripe.android.model.PaymentIntentFixtures;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class PaymentAuthResultTest {

    @Test
    public void testBuilder() {
        assertEquals(PaymentIntentFixtures.PI_REQUIRES_3DS2,
                new PaymentAuthResult.Builder()
                        .setPaymentIntent(PaymentIntentFixtures.PI_REQUIRES_3DS2)
                        .build()
                        .paymentIntent);
    }
}