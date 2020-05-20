package com.stripe.android

import com.google.common.truth.Truth.assertThat
import kotlin.test.Test

class StripeSSLSocketFactoryTest {

    @Test
    fun getEnabledProtocols() {
        val defaultProtocols = arrayOf("protocol")

        assertThat(
            StripeSSLSocketFactory(tlsv11Supported = false, tlsv12Supported = false)
                .getEnabledProtocols(defaultProtocols)
        ).isEqualTo(
            arrayOf("protocol")
        )

        assertThat(
            StripeSSLSocketFactory(tlsv11Supported = false, tlsv12Supported = true)
                .getEnabledProtocols(defaultProtocols)
        ).isEqualTo(
            arrayOf("protocol", "TLSv1.2")
        )

        assertThat(
            StripeSSLSocketFactory(tlsv11Supported = true, tlsv12Supported = true)
                .getEnabledProtocols(defaultProtocols)
        ).isEqualTo(
            arrayOf("protocol", "TLSv1.1", "TLSv1.2")
        )
    }
}
