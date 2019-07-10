package com.stripe.android.model;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@RunWith(RobolectricTestRunner.class)
public class Stripe3ds2FingerprintTest {

    @Test
    public void create_with3ds2SdkData_shouldCreateObject() {
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
        assertEquals("RSA", stripe3ds2Fingerprint.directoryServerEncryption.algorithm);
        assertEquals("MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMI",
                stripe3ds2Fingerprint.directoryServerEncryption.certificate);
        assertNull(stripe3ds2Fingerprint.directoryServerEncryption.keyId);
    }

    @Test
    public void create_with3ds2AmexSdkData_shouldCreateObject() {
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
        assertEquals("RSA", stripe3ds2Fingerprint.directoryServerEncryption.algorithm);
        assertEquals("MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMI",
                stripe3ds2Fingerprint.directoryServerEncryption.certificate);
        assertEquals("7c4debe3f4af7f9d1569a2ffea4343c2566826ee",
                stripe3ds2Fingerprint.directoryServerEncryption.keyId);
    }


    @Test(expected = IllegalArgumentException.class)
    public void create_with3ds1SdkData_shouldThrowException() {
        final PaymentIntent.SdkData sdkData = PaymentIntentFixtures.PI_REQUIRES_3DS1
                .getStripeSdkData();
        assertNotNull(sdkData);
        Stripe3ds2Fingerprint.create(sdkData);
    }
}