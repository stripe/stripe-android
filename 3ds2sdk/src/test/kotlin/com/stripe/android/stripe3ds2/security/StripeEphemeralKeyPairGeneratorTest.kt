package com.stripe.android.stripe3ds2.security

import com.google.common.truth.Truth.assertThat
import com.stripe.android.stripe3ds2.observability.FakeErrorReporter
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import kotlin.test.Test

class StripeEphemeralKeyPairGeneratorTest {

    @Test
    fun generate_shouldCreateEcKeys() {
        val keyPair = StripeEphemeralKeyPairGenerator(
            FakeErrorReporter()
        ).generate()

        assertThat(keyPair.public)
            .isInstanceOf(ECPublicKey::class.java)
        assertThat(keyPair.private)
            .isInstanceOf(ECPrivateKey::class.java)
    }
}
