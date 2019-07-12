package com.stripe.android.model;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Objects;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class Stripe3dsRedirectTest {

    @Test
    public void testCreate() {
        final Stripe3dsRedirect redirect = Stripe3dsRedirect.create(
                Objects.requireNonNull(PaymentIntentFixtures.PI_REQUIRES_3DS1.getStripeSdkData()));
        assertEquals(
                "https://hooks.stripe.com/3d_secure_2_eap/begin_test/src_1Ecve7CRMbs6FrXfm8AxXMIh/src_client_secret_F79yszOBAiuaZTuIhbn3LPUW",
                redirect.getRedirectData().url.toString()
        );
    }
}
