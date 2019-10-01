package com.stripe.android.model

import java.io.ByteArrayInputStream
import java.security.PublicKey
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class Stripe3ds2FingerprintTest {

    @Test
    @Throws(CertificateException::class)
    fun create_with3ds2SdkData_shouldCreateObject() {
        val sdkData = PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2
            .stripeSdkData
        assertNotNull(sdkData)
        val stripe3ds2Fingerprint = Stripe3ds2Fingerprint.create(sdkData)
        assertEquals("src_1ExkUeAWhjPjYwPiLWUvXrSA", stripe3ds2Fingerprint.source)
        assertEquals(Stripe3ds2Fingerprint.DirectoryServer.Mastercard,
            stripe3ds2Fingerprint.directoryServer)
        assertEquals("34b16ea1-1206-4ee8-84d2-d292bc73c2ae",
            stripe3ds2Fingerprint.serverTransactionId)

        val directoryServerEncryption =
            stripe3ds2Fingerprint.directoryServerEncryption
        assertNotNull(stripe3ds2Fingerprint.directoryServerEncryption)
        assertEquals(Stripe3ds2Fingerprint.DirectoryServer.Mastercard.id,
            directoryServerEncryption.directoryServerId)
        assertNotNull(directoryServerEncryption.directoryServerPublicKey)
        assertEquals("7c4debe3f4af7f9d1569a2ffea4343c2566826ee",
            directoryServerEncryption.keyId)
        assertEquals(1, directoryServerEncryption.rootCerts.size.toLong())
    }

    @Test
    @Throws(CertificateException::class)
    fun create_with3ds2AmexSdkData_shouldCreateObject() {
        val sdkData = PaymentIntentFixtures.PI_REQUIRES_AMEX_3DS2
            .stripeSdkData
        assertNotNull(sdkData)
        val stripe3ds2Fingerprint = Stripe3ds2Fingerprint.create(sdkData)
        assertEquals("src_1EceOlCRMbs6FrXf2hqrI1g5", stripe3ds2Fingerprint.source)
        assertEquals(Stripe3ds2Fingerprint.DirectoryServer.Amex,
            stripe3ds2Fingerprint.directoryServer)
        assertEquals("e64bb72f-60ac-4845-b8b6-47cfdb0f73aa",
            stripe3ds2Fingerprint.serverTransactionId)

        assertNotNull(stripe3ds2Fingerprint.directoryServerEncryption)
        assertEquals(Stripe3ds2Fingerprint.DirectoryServer.Amex.id,
            stripe3ds2Fingerprint.directoryServerEncryption.directoryServerId)
        assertEquals(DS_RSA_PUBLIC_KEY,
            stripe3ds2Fingerprint.directoryServerEncryption.directoryServerPublicKey)
        assertEquals("7c4debe3f4af7f9d1569a2ffea4343c2566826ee",
            stripe3ds2Fingerprint.directoryServerEncryption.keyId)
    }

    @Test
    fun create_with3ds1SdkData_shouldThrowException() {
        val sdkData = PaymentIntentFixtures.PI_REQUIRES_3DS1
            .stripeSdkData
        assertNotNull(sdkData)
        assertFailsWith<IllegalArgumentException> { Stripe3ds2Fingerprint.create(sdkData) }
    }

    companion object {
        internal val DS_CERT_DATA_RSA =
            """
            -----BEGIN CERTIFICATE-----
            MIIE0TCCA7mgAwIBAgIUXbeqM1duFcHk4dDBwT8o7Ln5wX8wDQYJKoZIhvcNAQEL
            BQAwXjELMAkGA1UEBhMCVVMxITAfBgNVBAoTGEFtZXJpY2FuIEV4cHJlc3MgQ29t
            cGFueTEsMCoGA1UEAxMjQW1lcmljYW4gRXhwcmVzcyBTYWZla2V5IElzc3Vpbmcg
            Q0EwHhcNMTgwMjIxMjM0OTMxWhcNMjAwMjIxMjM0OTMwWjCB0DELMAkGA1UEBhMC
            VVMxETAPBgNVBAgTCE5ldyBZb3JrMREwDwYDVQQHEwhOZXcgWW9yazE/MD0GA1UE
            ChM2QW1lcmljYW4gRXhwcmVzcyBUcmF2ZWwgUmVsYXRlZCBTZXJ2aWNlcyBDb21w
            YW55LCBJbmMuMTkwNwYDVQQLEzBHbG9iYWwgTmV0d29yayBUZWNobm9sb2d5IC0g
            TmV0d29yayBBUEkgUGxhdGZvcm0xHzAdBgNVBAMTFlNESy5TYWZlS2V5LkVuY3J5
            cHRLZXkwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQDSFF9kTYbwRrxX
            C6WcJJYio5TZDM62+CnjQRfggV3GMI+xIDtMIN8LL/jbWBTycu97vrNjNNv+UPhI
            WzhFDdUqyRfrY337A39uE8k1xhdDI3dNeZz6xgq8r9hn2NBou78YPBKidpN5oiHn
            TxcFq1zudut2fmaldaa9a4ZKgIQo+02heiJfJ8XNWkoWJ17GcjJ59UU8C1KF/y1G
            ymYO5ha2QRsVZYI17+ZFsqnpcXwK4Mr6RQKV6UimmO0nr5++CgvXfekcWAlLV6Xq
            juACWi3kw0haepaX/9qHRu1OSyjzWNcSVZ0On6plB5Lq6Y9ylgmxDrv+zltz3MrT
            K7txIAFFAgMBAAGjggESMIIBDjAMBgNVHRMBAf8EAjAAMCEGA1UdEQQaMBiCFlNE
            Sy5TYWZlS2V5LkVuY3J5cHRLZXkwRQYJKwYBBAGCNxQCBDgeNgBBAE0ARQBYAF8A
            UwBBAEYARQBLAEUAWQAyAF8ARABTAF8ARQBOAEMAUgBZAFAAVABJAE8ATjAOBgNV
            HQ8BAf8EBAMCBJAwHwYDVR0jBBgwFoAU7k/rXuVMhTBxB1zSftPgmLFuDIgwRAYD
            VR0fBD0wOzA5oDegNYYzaHR0cDovL2FtZXhzay5jcmwuY29tLXN0cm9uZy1pZC5u
            ZXQvYW1leHNhZmVrZXkuY3JsMB0GA1UdDgQWBBQHclVTo5nwZGH8labJ2F2P45xi
            fDANBgkqhkiG9w0BAQsFAAOCAQEAWY6b77VBoGLs3k5vOqSU7QRqT+4v6y77T8LA
            BKrSZ58DiVZWVyDSxyftQUiRRgFHt2gTN0yfJTP50Fyp84nCEWC0tugZ4iIhgPss
            HzL+4/u4eG/MTzK2ESxvPgr6YHajyuU+GXA89u8+bsFrFmojOjhTgFKli7YUeV/0
            xoiYZf2utlns800ofJrcrfiFoqE6PvK4Od0jpeMgfSKv71nK5ihA1+wTk76ge1fs
            PxL23hEdRpWW11ofaLfJGkLFXMM3/LHSXWy7HhsBgDELdzLSHU4VkSv8yTOZxsRO
            ByxdC5v3tXGcK56iQdtKVPhFGOOEBugw7AcuRzv3f1GhvzAQZg==
            -----END CERTIFICATE-----
            """.trimIndent()

        val DS_RSA_PUBLIC_KEY: PublicKey = generateCertificate(DS_CERT_DATA_RSA).publicKey

        private fun generateCertificate(certificateData: String): X509Certificate {
            try {
                val factory = CertificateFactory.getInstance("X.509")
                return factory.generateCertificate(
                    ByteArrayInputStream(certificateData.toByteArray())
                ) as X509Certificate
            } catch (e: CertificateException) {
                throw RuntimeException(e)
            }
        }
    }
}
