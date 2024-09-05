package com.stripe.android.stripe3ds2.security

import com.google.common.truth.Truth.assertThat
import com.stripe.android.stripe3ds2.observability.FakeErrorReporter
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class StripeDiffieHellmanKeyGeneratorTest {
    private val errorReporter = FakeErrorReporter()

    @Test
    fun generateECDHSecret_shouldReturnValidSecretKey() {
        val keyPair = StripeEphemeralKeyPairGenerator(errorReporter).generate()
        val secretKey = StripeDiffieHellmanKeyGenerator(errorReporter)
            .generate(
                keyPair.public as ECPublicKey,
                keyPair.private as ECPrivateKey,
                SDK_REFERENCE_ID
            )
        assertThat(secretKey)
            .isNotNull()
    }

    private companion object {
        private const val SDK_REFERENCE_ID = "ABC123"
    }
}
