package com.stripe.android.model;

import android.support.annotation.NonNull;

import java.io.ByteArrayInputStream;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@RunWith(RobolectricTestRunner.class)
public class Stripe3ds2FingerprintTest {

    @NonNull
    public static final String DS_CERT_DATA_RSA = "-----BEGIN CERTIFICATE-----\n" +
            "MIIE0TCCA7mgAwIBAgIUXbeqM1duFcHk4dDBwT8o7Ln5wX8wDQYJKoZIhvcNAQEL\n" +
            "BQAwXjELMAkGA1UEBhMCVVMxITAfBgNVBAoTGEFtZXJpY2FuIEV4cHJlc3MgQ29t\n" +
            "cGFueTEsMCoGA1UEAxMjQW1lcmljYW4gRXhwcmVzcyBTYWZla2V5IElzc3Vpbmcg\n" +
            "Q0EwHhcNMTgwMjIxMjM0OTMxWhcNMjAwMjIxMjM0OTMwWjCB0DELMAkGA1UEBhMC\n" +
            "VVMxETAPBgNVBAgTCE5ldyBZb3JrMREwDwYDVQQHEwhOZXcgWW9yazE/MD0GA1UE\n" +
            "ChM2QW1lcmljYW4gRXhwcmVzcyBUcmF2ZWwgUmVsYXRlZCBTZXJ2aWNlcyBDb21w\n" +
            "YW55LCBJbmMuMTkwNwYDVQQLEzBHbG9iYWwgTmV0d29yayBUZWNobm9sb2d5IC0g\n" +
            "TmV0d29yayBBUEkgUGxhdGZvcm0xHzAdBgNVBAMTFlNESy5TYWZlS2V5LkVuY3J5\n" +
            "cHRLZXkwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQDSFF9kTYbwRrxX\n" +
            "C6WcJJYio5TZDM62+CnjQRfggV3GMI+xIDtMIN8LL/jbWBTycu97vrNjNNv+UPhI\n" +
            "WzhFDdUqyRfrY337A39uE8k1xhdDI3dNeZz6xgq8r9hn2NBou78YPBKidpN5oiHn\n" +
            "TxcFq1zudut2fmaldaa9a4ZKgIQo+02heiJfJ8XNWkoWJ17GcjJ59UU8C1KF/y1G\n" +
            "ymYO5ha2QRsVZYI17+ZFsqnpcXwK4Mr6RQKV6UimmO0nr5++CgvXfekcWAlLV6Xq\n" +
            "juACWi3kw0haepaX/9qHRu1OSyjzWNcSVZ0On6plB5Lq6Y9ylgmxDrv+zltz3MrT\n" +
            "K7txIAFFAgMBAAGjggESMIIBDjAMBgNVHRMBAf8EAjAAMCEGA1UdEQQaMBiCFlNE\n" +
            "Sy5TYWZlS2V5LkVuY3J5cHRLZXkwRQYJKwYBBAGCNxQCBDgeNgBBAE0ARQBYAF8A\n" +
            "UwBBAEYARQBLAEUAWQAyAF8ARABTAF8ARQBOAEMAUgBZAFAAVABJAE8ATjAOBgNV\n" +
            "HQ8BAf8EBAMCBJAwHwYDVR0jBBgwFoAU7k/rXuVMhTBxB1zSftPgmLFuDIgwRAYD\n" +
            "VR0fBD0wOzA5oDegNYYzaHR0cDovL2FtZXhzay5jcmwuY29tLXN0cm9uZy1pZC5u\n" +
            "ZXQvYW1leHNhZmVrZXkuY3JsMB0GA1UdDgQWBBQHclVTo5nwZGH8labJ2F2P45xi\n" +
            "fDANBgkqhkiG9w0BAQsFAAOCAQEAWY6b77VBoGLs3k5vOqSU7QRqT+4v6y77T8LA\n" +
            "BKrSZ58DiVZWVyDSxyftQUiRRgFHt2gTN0yfJTP50Fyp84nCEWC0tugZ4iIhgPss\n" +
            "HzL+4/u4eG/MTzK2ESxvPgr6YHajyuU+GXA89u8+bsFrFmojOjhTgFKli7YUeV/0\n" +
            "xoiYZf2utlns800ofJrcrfiFoqE6PvK4Od0jpeMgfSKv71nK5ihA1+wTk76ge1fs\n" +
            "PxL23hEdRpWW11ofaLfJGkLFXMM3/LHSXWy7HhsBgDELdzLSHU4VkSv8yTOZxsRO\n" +
            "ByxdC5v3tXGcK56iQdtKVPhFGOOEBugw7AcuRzv3f1GhvzAQZg==\n" +
            "-----END CERTIFICATE-----\n";

    public static final Certificate DS_CERTIFICATE_RSA = generateCertificate(DS_CERT_DATA_RSA);

    @Test
    public void create_with3ds2SdkData_shouldCreateObject() throws CertificateException {
        final PaymentIntent.SdkData sdkData = PaymentIntentFixtures.PI_REQUIRES_VISA_3DS2
                .getStripeSdkData();
        assertNotNull(sdkData);
        final Stripe3ds2Fingerprint stripe3ds2Fingerprint = Stripe3ds2Fingerprint.create(sdkData);
        assertEquals("src_1EceOlCRMbs6FrXf2hqrI1g5",
                stripe3ds2Fingerprint.source);
        assertEquals(Stripe3ds2Fingerprint.DirectoryServer.Visa,
                stripe3ds2Fingerprint.directoryServer);
        assertEquals("e64bb72f-60ac-4845-b8b6-47cfdb0f73aa",
                stripe3ds2Fingerprint.serverTransactionId);

        assertNotNull(stripe3ds2Fingerprint.directoryServerEncryption);
        assertEquals(Stripe3ds2Fingerprint.DirectoryServer.Visa.id,
                stripe3ds2Fingerprint.directoryServerEncryption.directoryServerId);
        assertEquals(DS_CERTIFICATE_RSA,
                stripe3ds2Fingerprint.directoryServerEncryption.certificate);
        assertNull(stripe3ds2Fingerprint.directoryServerEncryption.keyId);
    }

    @Test
    public void create_with3ds2AmexSdkData_shouldCreateObject() throws CertificateException {
        final PaymentIntent.SdkData sdkData = PaymentIntentFixtures.PI_REQUIRES_AMEX_3DS2
                .getStripeSdkData();
        assertNotNull(sdkData);
        final Stripe3ds2Fingerprint stripe3ds2Fingerprint = Stripe3ds2Fingerprint.create(sdkData);
        assertEquals("src_1EceOlCRMbs6FrXf2hqrI1g5",
                stripe3ds2Fingerprint.source);
        assertEquals(Stripe3ds2Fingerprint.DirectoryServer.Amex,
                stripe3ds2Fingerprint.directoryServer);
        assertEquals("e64bb72f-60ac-4845-b8b6-47cfdb0f73aa",
                stripe3ds2Fingerprint.serverTransactionId);

        assertNotNull(stripe3ds2Fingerprint.directoryServerEncryption);
        assertEquals(Stripe3ds2Fingerprint.DirectoryServer.Amex.id,
                stripe3ds2Fingerprint.directoryServerEncryption.directoryServerId);
        assertEquals(DS_CERTIFICATE_RSA,
                stripe3ds2Fingerprint.directoryServerEncryption.certificate);
        assertEquals("7c4debe3f4af7f9d1569a2ffea4343c2566826ee",
                stripe3ds2Fingerprint.directoryServerEncryption.keyId);
    }

    @Test(expected = IllegalArgumentException.class)
    public void create_with3ds1SdkData_shouldThrowException() throws CertificateException {
        final PaymentIntent.SdkData sdkData = PaymentIntentFixtures.PI_REQUIRES_3DS1
                .getStripeSdkData();
        assertNotNull(sdkData);
        Stripe3ds2Fingerprint.create(sdkData);
    }

    private static Certificate generateCertificate(@NonNull String certificate) {
        try {
            final CertificateFactory factory = CertificateFactory.getInstance("X.509");
            return factory.generateCertificate(new ByteArrayInputStream(certificate.getBytes()));
        } catch (CertificateException e) {
            return null;
        }
    }
}