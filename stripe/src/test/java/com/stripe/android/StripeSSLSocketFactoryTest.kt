package com.stripe.android

import kotlin.test.Test
import kotlin.test.assertTrue

class StripeSSLSocketFactoryTest {

    @Test
    fun getEnabledProtocols() {
        val defaultProtocols = arrayOf("protocol")

        assertTrue(
            StripeSSLSocketFactory(tlsv11Supported = false, tlsv12Supported = false)
                .getEnabledProtocols(defaultProtocols)
                .contentEquals(arrayOf("protocol"))
        )
        assertTrue(
            StripeSSLSocketFactory(tlsv11Supported = false, tlsv12Supported = true)
                .getEnabledProtocols(defaultProtocols)
                .contentEquals(arrayOf("protocol", "TLSv1.2"))
        )
        assertTrue(
            StripeSSLSocketFactory(tlsv11Supported = true, tlsv12Supported = true)
                .getEnabledProtocols(defaultProtocols)
                .contentEquals(arrayOf("protocol", "TLSv1.1", "TLSv1.2"))
        )
    }
}
