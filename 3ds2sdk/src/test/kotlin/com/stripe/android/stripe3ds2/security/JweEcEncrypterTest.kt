package com.stripe.android.stripe3ds2.security

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.stripe3ds2.observability.FakeErrorReporter
import org.json.JSONObject
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.security.interfaces.ECPublicKey
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class JweEcEncrypterTest {

    private val errorReporter = FakeErrorReporter()
    private val publicKeyFactory = PublicKeyFactory(
        ApplicationProvider.getApplicationContext(),
        errorReporter
    )
    private val jweEcEncrypter = JweEcEncrypter(
        StripeEphemeralKeyPairGenerator(errorReporter),
        errorReporter
    )

    @Test
    fun encrypt_shouldReturnStringWithCorrectLength() {
        val originalPayload = JSONObject()
            .put("color", "blue")
            .put("shape", "square")

        val publicKey = publicKeyFactory.create(EC_DIRECTORY_SERVER_ID) as ECPublicKey
        val encryptedPayload = jweEcEncrypter.encrypt(
            originalPayload.toString(),
            publicKey,
            EC_DIRECTORY_SERVER_ID
        )
        assertThat(encryptedPayload)
            .hasLength(336)
    }

    private companion object {
        private const val EC_DIRECTORY_SERVER_ID = "F000000001"
    }
}
