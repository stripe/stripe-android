package com.stripe.android.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Test class for {@link SourceSepaDebitData}.
 */
public class SourceSepaDebitDataTest {

    private static final String EXAMPLE_SEPA_JSON_DATA = "{\"bank_code\":\"37040044\"," +
            "\"country\":\"DE\",\"fingerprint\":\"R8MJxzkSUv1Kv07L\",\"last4\":\"3000\"," +
            "\"mandate_reference\":\"CK4K2GFVPOVR4T5B\"," +
            "\"mandate_url\":\"https:\\/\\/hooks.stripe.com\\/" +
            "adapter\\/sepa_debit\\/file\\/" +
            "src_1A0burBbvEcIpqUbyTfDmJPk\\/src_client_secret_5Dgw1AQGTABOh0vlnKyxgboh\"}";

    private static final String MANDATE_URL = "https://hooks.stripe.com/" +
            "adapter/sepa_debit/file/src_1A0burBbvEcIpqUbyTfDmJPk/" +
            "src_client_secret_5Dgw1AQGTABOh0vlnKyxgboh";

    @Test
    public void fromJson_withExampleData_returnsExpectedObject() {
        final SourceSepaDebitData sepaData = SourceSepaDebitData.fromString(EXAMPLE_SEPA_JSON_DATA);
        assertNotNull(sepaData);
        assertEquals("37040044", sepaData.getBankCode());
        assertEquals("R8MJxzkSUv1Kv07L", sepaData.getFingerPrint());
        assertEquals("CK4K2GFVPOVR4T5B", sepaData.getMandateReference());
        assertEquals("DE", sepaData.getCountry());
        assertEquals("3000", sepaData.getLast4());
        assertEquals(MANDATE_URL, sepaData.getMandateUrl());
        assertNull(sepaData.getBranchCode());
    }

    @Test
    public void testEquals() {
        assertEquals(
                SourceSepaDebitData.fromString(EXAMPLE_SEPA_JSON_DATA),
                SourceSepaDebitData.fromString(EXAMPLE_SEPA_JSON_DATA)
        );
    }

    @Test
    public void testHashCode() {
        assertEquals(
                SourceSepaDebitData.fromString(EXAMPLE_SEPA_JSON_DATA).hashCode(),
                SourceSepaDebitData.fromString(EXAMPLE_SEPA_JSON_DATA).hashCode()
        );
    }
}
