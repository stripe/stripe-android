package com.stripe.android.stripe3ds2.security

import com.stripe.android.stripe3ds2.observability.FakeErrorReporter
import kotlin.test.Test
import kotlin.test.assertEquals

class EcKeyFactoryTest {

    private val ecKeyFactory = EcKeyFactory(FakeErrorReporter())

    @Test
    fun testCreation() {
        val keyPair = StripeEphemeralKeyPairGenerator(FakeErrorReporter()).generate()

        val publicKey = keyPair.public
        assertEquals(
            publicKey,
            ecKeyFactory.createPublic(publicKey.encoded)
        )

        val privateKey = keyPair.private
        assertEquals(
            privateKey,
            ecKeyFactory.createPrivate(privateKey.encoded)
        )
    }
}
