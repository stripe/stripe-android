package com.stripe.android;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertNotNull;

@RunWith(RobolectricTestRunner.class)
public class PaymentAuthConfigJavaTest {

    @Test
    public void testCreate() {
        PaymentAuthConfig.init(new PaymentAuthConfig.Builder()
                .set3ds2Config(new PaymentAuthConfig.Stripe3ds2Config.Builder()
                        .setTimeout(20)
                        .setUiCustomization(
                                new PaymentAuthConfig.Stripe3ds2UiCustomization.Builder()
                                        .setAccentColor("#ffffff")
                                        .build())
                        .build())
                .build());
        assertNotNull(PaymentAuthConfig.get());
    }
}
