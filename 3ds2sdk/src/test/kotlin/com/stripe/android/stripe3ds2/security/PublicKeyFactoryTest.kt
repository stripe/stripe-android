package com.stripe.android.stripe3ds2.security

import androidx.test.core.app.ApplicationProvider
import com.stripe.android.stripe3ds2.exceptions.SDKRuntimeException
import com.stripe.android.stripe3ds2.observability.FakeErrorReporter
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.security.interfaces.ECPublicKey
import java.security.interfaces.RSAPublicKey
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class PublicKeyFactoryTest {
    private val errorReporter = FakeErrorReporter()

    @Test
    fun create_withRsaDirectoryServerId_shouldReturnRsaPublicKey() {
        val publicKey = createFactory()
            .create(DirectoryServer.TestRsa.ids.first())
        assertTrue(publicKey is RSAPublicKey)
    }

    @Test
    fun create_withEcDirectoryServerId_shouldReturnECPublicKey() {
        val publicKey = createFactory()
            .create(DirectoryServer.TestEc.ids.first())
        assertTrue(publicKey is ECPublicKey)
    }

    @Test
    fun create_withVisaDirectoryServerId_shouldReturnRsaPublicKey() {
        val publicKey = createFactory()
            .create(DirectoryServer.Visa.ids.first())
        assertTrue(publicKey is RSAPublicKey)
    }

    @Test
    fun create_withMastercardDirectoryServerId_shouldReturnRsaPublicKey() {
        val publicKey = createFactory()
            .create(DirectoryServer.Mastercard.ids.first())
        assertTrue(publicKey is RSAPublicKey)
    }

    @Test
    fun create_withAmexDirectoryServerId_shouldReturnRsaPublicKey() {
        val publicKey = createFactory()
            .create(DirectoryServer.Amex.ids.first())
        assertTrue(publicKey is RSAPublicKey)
    }

    @Test
    fun create_withDiscoverDirectoryServerId_shouldReturnRsaPublicKey() {
        val publicKey = createFactory()
            .create(DirectoryServer.Discover.ids.first())
        assertTrue(publicKey is RSAPublicKey)
    }

    @Test
    fun create_withInvalidDirectoryServerId() {
        assertFailsWith<SDKRuntimeException> {
            createFactory().create("invalid")
        }
    }

    private fun createFactory(): PublicKeyFactory {
        return PublicKeyFactory(ApplicationProvider.getApplicationContext(), errorReporter)
    }
}
