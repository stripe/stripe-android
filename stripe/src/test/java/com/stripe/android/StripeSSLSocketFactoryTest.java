package com.stripe.android;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;

public class StripeSSLSocketFactoryTest {

    @Test
    public void getEnabledProtocols() {
        final String[] defaultProtocols = {"protocol"};

        assertArrayEquals(new String[]{"protocol"},
                new StripeSSLSocketFactory(false, false)
                        .getEnabledProtocols(defaultProtocols));
        assertArrayEquals(new String[]{"protocol", "TLSv1.2"},
                new StripeSSLSocketFactory(false, true)
                        .getEnabledProtocols(defaultProtocols));
        assertArrayEquals(new String[]{"protocol", "TLSv1.1", "TLSv1.2"},
                new StripeSSLSocketFactory(true, true)
                        .getEnabledProtocols(defaultProtocols));
    }
}
