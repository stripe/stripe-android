package com.stripe.android;

import com.stripe.android.stripe3ds2.init.ui.StripeUiCustomization;
import com.stripe.android.stripe3ds2.init.ui.UiCustomization;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class PaymentAuthConfigTest {

    @Before
    public void setup() {
        PaymentAuthConfig.reset();
    }

    @Test
    public void get_whenNotInited_returnsDefault() {
        final PaymentAuthConfig paymentAuthConfig = PaymentAuthConfig.get();
        assertEquals(PaymentAuthConfig.Stripe3ds2Config.DEFAULT_TIMEOUT,
                paymentAuthConfig.stripe3ds2Config.timeout);
        assertNotNull(paymentAuthConfig.stripe3ds2Config.uiCustomization);
    }

    @Test
    public void get_whenInit_returnsInstance() {
        PaymentAuthConfig.init(new PaymentAuthConfig.Builder()
                .set3ds2Config(new PaymentAuthConfig.Stripe3ds2Config.Builder()
                        .setTimeout(20)
                        .build())
                .build());
        assertEquals(20, PaymentAuthConfig.get().stripe3ds2Config.timeout);
    }

    @Test
    public void testStripe3ds2ConfigBuilder() {
        final UiCustomization uiCustomization = new StripeUiCustomization();
        PaymentAuthConfig.init(new PaymentAuthConfig.Builder()
                .set3ds2Config(new PaymentAuthConfig.Stripe3ds2Config.Builder()
                        .setTimeout(20)
                        .setUiCustomization(uiCustomization)
                        .build())
                .build());
        assertEquals(uiCustomization,
                PaymentAuthConfig.get().stripe3ds2Config.uiCustomization);
    }
}
