package com.stripe.android.model;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 * Test class for {@link SourceCardData}.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 23)
public class SourceCardDataTest {

    private static final String EXAMPLE_JSON_CARD = "{\"exp_month\":12,\"exp_year\":2050," +
            "\"address_line1_check\":\"unchecked\",\"address_zip_check\":" +
            "\"unchecked\",\"brand\":\"Visa\",\"country\":\"US\",\"cvc_check\"" +
            ":\"unchecked\",\"funding\":\"credit\",\"last4\":\"4242\",\"three_d_secure\"" +
            ":\"optional\",\"tokenization_method\":null,\"dynamic_last4\":null}";

    @Test
    public void fromExampleJsonCard_createsExpectedObject() {
        SourceCardData cardData = SourceCardData.fromString(EXAMPLE_JSON_CARD);
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
    }

    @Test
    public void fromExampleJsonCard_toMap_createsExpectedMapping() {
        SourceCardData cardData = SourceCardData.fromString(EXAMPLE_JSON_CARD);
        Map<String, Object> cardDataMap = cardData.toMap();

        assertNotNull(cardDataMap);
        assertEquals("US", cardDataMap.get("country"));
        assertEquals("4242", cardDataMap.get("last4"));
        assertEquals(12, cardDataMap.get("exp_month"));
        assertEquals(2050, cardDataMap.get("exp_year"));
        assertEquals(Card.FUNDING_CREDIT, cardDataMap.get("funding"));
        assertEquals(Card.VISA, cardDataMap.get("brand"));
        assertEquals("optional", cardDataMap.get("three_d_secure"));
        assertFalse(cardDataMap.containsKey("tokenization_method"));
        assertFalse(cardDataMap.containsKey("dynamic_last4"));
    }
}
