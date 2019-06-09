package com.stripe.android.model;

import org.junit.Test;

import java.util.Map;
import java.util.Objects;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Test class for {@link SourceCardData}.
 */
public class SourceCardDataTest {

    static final String EXAMPLE_JSON_SOURCE_CARD_DATA_WITH_APPLE_PAY =
            "{\"exp_month\":12,\"exp_year\":2050," +
            "\"address_line1_check\":\"unchecked\",\"address_zip_check\":" +
            "\"unchecked\",\"brand\":\"Visa\",\"country\":\"US\",\"cvc_check\"" +
            ":\"unchecked\",\"funding\":\"credit\",\"last4\":\"4242\",\"three_d_secure\"" +
            ":\"optional\",\"tokenization_method\":\"apple_pay\",\"dynamic_last4\":\"4242\"}";


    private static final SourceCardData CARD_DATA =
            SourceCardData.fromString(EXAMPLE_JSON_SOURCE_CARD_DATA_WITH_APPLE_PAY);

    @Test
    public void fromExampleJsonCard_createsExpectedObject() {
        assertNotNull(CARD_DATA);
        assertEquals(Card.VISA, CARD_DATA.getBrand());
        assertEquals(Card.FUNDING_CREDIT, CARD_DATA.getFunding());
        assertEquals("4242", CARD_DATA.getLast4());
        assertNotNull(CARD_DATA.getExpiryMonth());
        assertNotNull(CARD_DATA.getExpiryYear());
        assertEquals(12, CARD_DATA.getExpiryMonth().intValue());
        assertEquals(2050, CARD_DATA.getExpiryYear().intValue());
        assertEquals("US", CARD_DATA.getCountry());
        assertEquals("optional", CARD_DATA.getThreeDSecureStatus());
        assertEquals("apple_pay", CARD_DATA.getTokenizationMethod());
    }

    @Test
    public void fromExampleJsonCard_toMap_createsExpectedMapping() {
        assertNotNull(CARD_DATA);
        final Map<String, Object> cardDataMap = CARD_DATA.toMap();
        assertNotNull(cardDataMap);
        assertEquals("US", cardDataMap.get("country"));
        assertEquals("4242", cardDataMap.get("last4"));
        assertEquals(12, cardDataMap.get("exp_month"));
        assertEquals(2050, cardDataMap.get("exp_year"));
        assertEquals(Card.FUNDING_CREDIT, cardDataMap.get("funding"));
        assertEquals(Card.VISA, cardDataMap.get("brand"));
        assertEquals("optional", cardDataMap.get("three_d_secure"));
        assertEquals("apple_pay", cardDataMap.get("tokenization_method"));
        assertEquals("4242", cardDataMap.get("dynamic_last4"));
    }

    @Test
    public void testEquals() {
        assertEquals(CARD_DATA,
                SourceCardData.fromString(EXAMPLE_JSON_SOURCE_CARD_DATA_WITH_APPLE_PAY));
    }

    @Test
    public void testHashCode() {
        assertNotNull(CARD_DATA);
        assertEquals(CARD_DATA.hashCode(),
                Objects.requireNonNull(
                        SourceCardData.fromString(EXAMPLE_JSON_SOURCE_CARD_DATA_WITH_APPLE_PAY))
                        .hashCode());
    }

    @Test
    public void testAsThreeDSecureStatus() {
        assertEquals(SourceCardData.REQUIRED, SourceCardData.asThreeDSecureStatus("required"));
        assertEquals(SourceCardData.OPTIONAL, SourceCardData.asThreeDSecureStatus("optional"));
        assertEquals(SourceCardData.NOT_SUPPORTED,
                SourceCardData.asThreeDSecureStatus("not_supported"));
        assertEquals(SourceCardData.RECOMMENDED,
                SourceCardData.asThreeDSecureStatus("recommended"));
        assertEquals(SourceCardData.UNKNOWN, SourceCardData.asThreeDSecureStatus("unknown"));
        assertNull(SourceCardData.asThreeDSecureStatus(""));
    }
}
