package com.stripe.android.stripe3ds2

import com.nimbusds.jose.util.Base64
import com.nimbusds.jose.util.X509CertUtils
import java.io.ByteArrayInputStream
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

object CertificateFixtures {
    private val DS_CERT_DATA_RSA =
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

    private val DS_CERT_DATA_EC =
        """
        -----BEGIN CERTIFICATE-----
        MIICsTCCAlcCCQDNLAEQmlK+UzAKBggqhkjOPQQDAjCBkTELMAkGA1UEBhMCVVMx
        ETAPBgNVBAgMCFZpcmdpbmlhMREwDwYDVQQHDAhSaWNobW9uZDEQMA4GA1UECgwH
        TW9iZWx1eDEPMA0GA1UECwwGU3RyaXBlMRMwEQYDVQQDDApzdHJpcGUuY29tMSQw
        IgYJKoZIhvcNAQkBFhVqZW1lcmljay1jQHN0cmlwZS5jb20wHhcNMTkwNzEwMjI1
        MTE3WhcNMzkwNzA1MjI1MTE3WjCBkTELMAkGA1UEBhMCVVMxETAPBgNVBAgMCFZp
        cmdpbmlhMREwDwYDVQQHDAhSaWNobW9uZDEQMA4GA1UECgwHTW9iZWx1eDEPMA0G
        A1UECwwGU3RyaXBlMRMwEQYDVQQDDApzdHJpcGUuY29tMSQwIgYJKoZIhvcNAQkB
        FhVqZW1lcmljay1jQHN0cmlwZS5jb20wgfUwga4GByqGSM49AgEwgaICAQEwLAYH
        KoZIzj0BAQIhAP////////////////////////////////////7///wvMAYEAQAE
        AQcEQQR5vmZ++dy7rFWgYpXOhwsHApv82y3OKNlZ8oFbFvgXmEg62ncmo8RlXaT7
        /A4RCKj9F7RIpoVUGZxH0I/7ENS4AiEA/////////////////////rqu3OavSKA7
        v9JejNA2QUECAQEDQgAEVj9Aa2BEbQsmrPoK/pLsbHMZGZb2QfdVdA9Cc/ofNvc2
        6Z0B3M/1Cd2iLs5dPt2jFXFfkCfLJYVFfe9a93rRUjAKBggqhkjOPQQDAgNIADBF
        AiAX4s/1rIBFcTzNhhb1sDXTnoHedCoRkZX07off/GmqdAIhAMu/vv8mpxSiR4+N
        dh1iyV6Arry3ZO53AwsiSH6Xl5PY
        -----END CERTIFICATE-----
        """.trimIndent()

    val DS_CERTIFICATE_RSA: X509Certificate by lazy {
        generateCertificate(DS_CERT_DATA_RSA)
    }

    val DS_CERTIFICATE_EC: X509Certificate by lazy {
        generateCertificate(DS_CERT_DATA_EC)
    }

    private val ENCODED_ROOT_CERT = Base64(
        """
        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
        """.trimIndent()
    )

    val ROOT_CERTS: List<X509Certificate> by lazy {
        listOf(X509CertUtils.parse(ENCODED_ROOT_CERT.decode()))
    }

    private fun generateCertificate(certificateData: String): X509Certificate {
        val inputStream = ByteArrayInputStream(certificateData.toByteArray())
        val factory = CertificateFactory.getInstance("X.509")
        return factory.generateCertificate(inputStream) as X509Certificate
    }
}
