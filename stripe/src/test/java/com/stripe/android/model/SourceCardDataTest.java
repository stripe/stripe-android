package com.stripe.android.model;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Test class for {@link SourceCardData}.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 25)
public class SourceCardDataTest {

    static final String EXAMPLE_JSON_SOURCE_CARD_DATA_WITH_APPLE_PAY =
            "{\"exp_month\":12,\"exp_year\":2050," +
            "\"address_line1_check\":\"unchecked\",\"address_zip_check\":" +
            "\"unchecked\",\"brand\":\"Visa\",\"country\":\"US\",\"cvc_check\"" +
            ":\"unchecked\",\"funding\":\"credit\",\"last4\":\"4242\",\"three_d_secure\"" +
            ":\"optional\",\"tokenization_method\":\"apple_pay\",\"dynamic_last4\":\"4242\"}";

    @Test
    public void fromExampleJsonCard_createsExpectedObject() {
        SourceCardData cardData = SourceCardData.fromString(EXAMPLE_JSON_SOURCE_CARD_DATA_WITH_APPLE_PAY);
        assertNotNull(cardData);
        assertEquals(Card.VISA, cardData.getBrand());
        assertEquals(0, cardData.getAdditionalFields().size());
        assertEquals(Card.FUNDING_CREDIT, cardData.getFunding());
        assertEquals("4242", cardData.getLast4());
        assertNotNull(cardData.getExpiryMonth());
        assertNotNull(cardData.getExpiryYear());
        assertEquals(12, cardData.getExpiryMonth().intValue());
        assertEquals(2050, cardData.getExpiryYear().intValue());
        assertEquals("US", cardData.getCountry());
        assertEquals("optional", cardData.getThreeDSecureStatus());
        assertEquals("apple_pay", cardData.getTokenizationMethod());
    }

    @Test
    public void fromExampleJsonCard_toMap_createsExpectedMapping() {
        SourceCardData cardData = SourceCardData.fromString(EXAMPLE_JSON_SOURCE_CARD_DATA_WITH_APPLE_PAY);
        Map<String, Object> cardDataMap = cardData.toMap();

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
}
