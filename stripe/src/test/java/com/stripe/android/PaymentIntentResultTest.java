package com.stripe.android;

import com.stripe.android.model.PaymentIntentFixtures;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class PaymentIntentResultTest {

    @Test
    public void testBuilder() {
        assertEquals(PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2,
                new PaymentIntentResult.Builder()
                        .setPaymentIntent(PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2)
                        .build()
                        .getIntent());
    }
}