package com.stripe.android.model;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static com.stripe.android.model.SourceCardDataTest.EXAMPLE_JSON_SOURCE_CARD_DATA_WITH_APPLE_PAY;
import static com.stripe.android.model.SourceCodeVerificationTest.EXAMPLE_JSON_CODE_VERIFICATION;
import static com.stripe.android.model.SourceOwnerTest.EXAMPLE_JSON_OWNER_WITHOUT_NULLS;
import static com.stripe.android.model.SourceOwnerTest.EXAMPLE_MAP_OWNER;
import static com.stripe.android.model.SourceReceiverTest.EXAMPLE_MAP_RECEIVER;
import static com.stripe.android.model.SourceRedirectTest.EXAMPLE_JSON_REDIRECT;
import static com.stripe.android.testharness.JsonTestUtils.assertMapEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Test class for {@link Source} model.
 */
public class SourceTest {

    static final String EXAMPLE_ALIPAY_SOURCE = "{\n" +
                    "  \"id\": \"src_1AtlSXBZqEXluyI4JgBYTq5W\",\n" +
                    "  \"object\": \"source\",\n" +
                    "  \"amount\": 1000,\n" +
                    "  \"client_secret\": \"src_client_secret_BGI2mBjd810BJEbvWRd83jac\",\n" +
                    "  \"created\": 1503443217,\n" +
                    "  \"currency\": \"usd\",\n" +
                    "  \"flow\": \"receiver\",\n" +
                    "  \"livemode\": false,\n" +
                    "  \"metadata\": {\n" +
                    "  },\n" +
                    "  \"owner\": {\n" +
                    "    \"address\": null,\n" +
                    "    \"email\": \"jenny.rosen@example.com\",\n" +
                    "    \"name\": null,\n" +
                    "    \"phone\": null,\n" +
                    "    \"verified_address\": null,\n" +
                    "    \"verified_email\": null,\n" +
                    "    \"verified_name\": null,\n" +
                    "    \"verified_phone\": null\n" +
                    "  },\n" +
                    "  \"receiver\": {\n" +
                    "    \"address\": \"test_1MBhWS3uv4ynCfQXF3xQjJkzFPukr4K56N\",\n" +
                    "    \"amount_charged\": 0,\n" +
                    "    \"amount_received\": 0,\n" +
                    "    \"amount_returned\": 0,\n" +
                    "    \"refund_attributes_method\": \"email\",\n" +
                    "    \"refund_attributes_status\": \"missing\"\n" +
                    "  },\n" +
                    "  \"statement_descriptor\": null,\n" +
                    "  \"status\": \"pending\",\n" +
                    "  \"type\": \"alipay\",\n" +
                    "  \"usage\": \"single_use\"\n" +
                    "}";

    static final String EXAMPLE_JSON_SOURCE_WITHOUT_NULLS = "{\n"+
            "\"id\": \"src_19t3xKBZqEXluyI4uz2dxAfQ\",\n"+
            "\"object\": \"source\",\n"+
            "\"amount\": 1000,\n"+
            "\"client_secret\": \"src_client_secret_of43INi1HteJwXVe3djAUosN\",\n"+
            "\"code_verification\": " + EXAMPLE_JSON_CODE_VERIFICATION + ",\n"+
            "\"created\": 1488499654,\n"+
            "\"currency\": \"usd\",\n"+
            "\"flow\": \"receiver\",\n"+
            "\"livemode\": false,\n"+
            "\"metadata\": {\n"+
            "},\n"+
            "\"owner\": " + EXAMPLE_JSON_OWNER_WITHOUT_NULLS +",\n"+
            "\"redirect\": " + EXAMPLE_JSON_REDIRECT + ",\n"+
            "\"receiver\": {\n"+
            "\"address\": \"test_1MBhWS3uv4ynCfQXF3xQjJkzFPukr4K56N\",\n"+
            "\"amount_charged\": 0,\n"+
            "\"amount_received\": 0,\n"+
            "\"amount_returned\": 0\n"+
            "},\n"+
            "\"status\": \"pending\",\n"+
            "\"type\": \"card\",\n"+
            "\"usage\": \"single_use\",\n"+
            "\"card\": " + EXAMPLE_JSON_SOURCE_CARD_DATA_WITH_APPLE_PAY + "\n"+
            "}";

    private static final String DOGE_COIN = "dogecoin";

    private static final String EXAMPLE_JSON_SOURCE_WITH_NULLS = "{\n"+
            "\"id\": \"src_19t3xKBZqEXluyI4uz2dxAfQ\",\n"+
            "\"object\": \"source\",\n"+
            "\"amount\": 1000,\n"+
            "\"client_secret\": \"src_client_secret_of43INi1HteJwXVe3djAUosN\",\n"+
            "\"created\": 1488499654,\n"+
            "\"currency\": \"usd\",\n"+
            "\"flow\": \"receiver\",\n"+
            "\"livemode\": false,\n"+
            "\"metadata\": {\n"+
            "},\n"+
            "\"owner\": {\n"+
            "\"address\": null,\n"+
            "\"email\": \"jenny.rosen@example.com\",\n"+
            "\"name\": \"Jenny Rosen\",\n"+
            "\"phone\": \"4158675309\",\n"+
            "\"verified_address\": null,\n"+
            "\"verified_email\": null,\n"+
            "\"verified_name\": null,\n"+
            "\"verified_phone\": null\n"+
            "},\n"+
            "\"receiver\": {\n"+
            "\"address\": \"test_1MBhWS3uv4ynCfQXF3xQjJkzFPukr4K56N\",\n"+
            "\"amount_charged\": 0,\n"+
            "\"amount_received\": 0,\n"+
            "\"amount_returned\": 0\n"+
            "},\n"+
            "\"status\": \"pending\",\n"+
            "\"type\": \"bitcoin\",\n"+
            "\"usage\": \"single_use\"\n"+
            "}";

    private static final Map<String, Object> EXAMPLE_SOURCE_MAP = new HashMap<String, Object>() {{
        put("id", "src_19t3xKBZqEXluyI4uz2dxAfQ");
        put("object", "source");
        put("amount", 1000L);
        put("client_secret", "src_client_secret_of43INi1HteJwXVe3djAUosN");
        put("created", 1488499654L);
        put("currency", "usd");
        put("flow", "receiver");
        put("livemode", false);
        put("metadata", new HashMap<String, Object>());
        put("owner", EXAMPLE_MAP_OWNER);
        put("receiver", EXAMPLE_MAP_RECEIVER);
        put("status", "pending");
        put("type", "bitcoin");
        put("usage", "single_use");
    }};

    private static final String EXAMPLE_JSON_SOURCE_CUSTOM_TYPE = "{\n"+
            "\"id\": \"src_19t3xKBZqEXluyI4uz2dxAfQ\",\n"+
            "\"object\": \"source\",\n"+
            "\"amount\": 1000,\n"+
            "\"client_secret\": \"src_client_secret_of43INi1HteJwXVe3djAUosN\",\n"+
            "\"code_verification\": " + EXAMPLE_JSON_CODE_VERIFICATION + ",\n"+
            "\"created\": 1488499654,\n"+
            "\"currency\": \"usd\",\n"+
            "\"flow\": \"receiver\",\n"+
            "\"livemode\": false,\n"+
            "\"metadata\": {\n"+
            "},\n"+
            "\"owner\": " + EXAMPLE_JSON_OWNER_WITHOUT_NULLS +",\n"+
            "\"redirect\": " + EXAMPLE_JSON_REDIRECT + ",\n"+
            "\"receiver\": {\n"+
            "\"address\": \"test_1MBhWS3uv4ynCfQXF3xQjJkzFPukr4K56N\",\n"+
            "\"amount_charged\": 0,\n"+
            "\"amount_received\": 0,\n"+
            "\"amount_returned\": 0\n"+
            "},\n"+
            "\"status\": \"pending\",\n"+
            "\"type\": \"dogecoin\",\n"+
            "\"usage\": \"single_use\",\n"+
            "\"dogecoin\": {\n" +
            "\"address\": \"test_1MBhWS3uv4ynCfQXF3xQjJkzFPukr4K56N\",\n" +
            "\"amount\": 2371000,\n" +
            "\"amount_charged\": 0,\n" +
            "\"amount_received\": 0,\n" +
            "\"amount_returned\": 0,\n" +
            "\"uri\": \"dogecoin:test_1MBhWS3uv4ynCfQXF3xQjJkzFPukr4K56N?amount=0.02371000\"\n" +
            "}" +
            "}";

    private static final String DELETED_CARD_JSON = "{\n" +
            "  \"id\": \"card_1ELdAlCRMbs6FrXfNbmZEOb7\",\n" +
            "  \"object\": \"card\",\n" +
            "  \"deleted\": true\n" +
            "}";

    private static final String CREATED_CARD_JSON = "{\n" +
            "  \"id\": \"card_1ELxrOCRMbs6FrXfdxOGjnaD\",\n" +
            "  \"object\": \"card\",\n" +
            "  \"address_city\": null,\n" +
            "  \"address_country\": null,\n" +
            "  \"address_line1\": null,\n" +
            "  \"address_line1_check\": null,\n" +
            "  \"address_line2\": null,\n" +
            "  \"address_state\": null,\n" +
            "  \"address_zip\": null,\n" +
            "  \"address_zip_check\": null,\n" +
            "  \"brand\": \"Visa\",\n" +
            "  \"country\": \"US\",\n" +
            "  \"customer\": \"cus_Epd7N0VR3cdjsr\",\n" +
            "  \"cvc_check\": null,\n" +
            "  \"dynamic_last4\": null,\n" +
            "  \"exp_month\": 4,\n" +
            "  \"exp_year\": 2020,\n" +
            "  \"funding\": \"credit\",\n" +
            "  \"last4\": \"4242\",\n" +
            "  \"metadata\": {\n" +
            "  },\n" +
            "  \"name\": null,\n" +
            "  \"tokenization_method\": null\n" +
            "}\n";

    @Test
    public void fromJsonStringWithoutNulls_isNotNull() {
        assertNotNull(Source.fromString(EXAMPLE_JSON_SOURCE_WITHOUT_NULLS));
    }

    @Test
    public void fromJsonStringWithNulls_toMap_createsExpectedMap() {
        Source sourceWithNulls = Source.fromString(EXAMPLE_JSON_SOURCE_WITH_NULLS);
        assertNotNull("Test Data failure during parsing", sourceWithNulls);
        assertMapEquals(EXAMPLE_SOURCE_MAP, sourceWithNulls.toMap());
    }

    @Test
    public void fromJsonString_withCustomType_createsSourceWithCustomType() {
        Source customSource = Source.fromString(EXAMPLE_JSON_SOURCE_CUSTOM_TYPE);
        assertNotNull("Parsing failure", customSource);
        assertEquals(Source.UNKNOWN, customSource.getType());
        assertEquals(DOGE_COIN, customSource.getTypeRaw());
        assertNull(customSource.getSourceTypeModel());
        assertNotNull("Failed to find custom api params", customSource.getSourceTypeData());
    }

    @Test
    public void fromJsonString_withDeletedCardJson_shouldReturnSourceWithCardId() {
        final Source source = Source.fromString(DELETED_CARD_JSON);
        assertNotNull(source);
        assertEquals("card_1ELdAlCRMbs6FrXfNbmZEOb7", source.getId());
    }

    @Test
    public void fromJsonString_withCreatedCardJson_shouldReturnSourceWithCardId() {
        final Source source = Source.fromString(CREATED_CARD_JSON);
        assertNotNull(source);
        assertEquals("card_1ELxrOCRMbs6FrXfdxOGjnaD", source.getId());
        assertEquals(Source.CARD, source.getType());
        assertTrue(source.getSourceTypeModel() instanceof SourceCardData);

        final SourceCardData sourceCardData = (SourceCardData) source.getSourceTypeModel();
        assertEquals(Card.VISA, sourceCardData.getBrand());
    }
}
