package com.stripe.android.model;

import org.junit.Test;

import static com.stripe.android.model.SourceCardDataTest.EXAMPLE_JSON_SOURCE_CARD_DATA_WITH_APPLE_PAY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Test class for {@link Source} model.
 */
public class SourceTest {


    static final String EXAMPLE_JSON_SOURCE_WITHOUT_NULLS = "{\n"+
            "\"id\": \"src_19t3xKBZqEXluyI4uz2dxAfQ\",\n"+
            "\"object\": \"source\",\n"+
            "\"amount\": 1000,\n"+
            "\"client_secret\": \"src_client_secret_of43INi1HteJwXVe3djAUosN\",\n"+
            "\"code_verification\": " + SourceFixtures.SOURCE_CODE_VERIFICATION_JSON.toString() + ",\n"+
            "\"created\": 1488499654,\n"+
            "\"currency\": \"usd\",\n"+
            "\"flow\": \"receiver\",\n"+
            "\"livemode\": false,\n"+
            "\"metadata\": {\n"+
            "},\n"+
            "\"owner\": " + SourceFixtures.SOURCE_OWNER_WITHOUT_NULLS.toString() +",\n"+
            "\"redirect\": " + SourceFixtures.SOURCE_REDIRECT_JSON.toString() + ",\n"+
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

    private static final String EXAMPLE_JSON_SOURCE_CUSTOM_TYPE = "{\n"+
            "\"id\": \"src_19t3xKBZqEXluyI4uz2dxAfQ\",\n"+
            "\"object\": \"source\",\n"+
            "\"amount\": 1000,\n"+
            "\"client_secret\": \"src_client_secret_of43INi1HteJwXVe3djAUosN\",\n"+
            "\"code_verification\": " + SourceFixtures.SOURCE_CODE_VERIFICATION_JSON.toString() + ",\n"+
            "\"created\": 1488499654,\n"+
            "\"currency\": \"usd\",\n"+
            "\"flow\": \"receiver\",\n"+
            "\"livemode\": false,\n"+
            "\"metadata\": {\n"+
            "},\n"+
            "\"owner\": " + SourceFixtures.SOURCE_OWNER_WITHOUT_NULLS.toString() +",\n"+
            "\"redirect\": " + SourceFixtures.SOURCE_REDIRECT_JSON.toString() + ",\n"+
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



    @Test
    public void fromJsonStringWithoutNulls_isNotNull() {
        assertNotNull(Source.fromString(EXAMPLE_JSON_SOURCE_WITHOUT_NULLS));
    }

    @Test
    public void fromJsonString_withCustomType_createsSourceWithCustomType() {
        Source customSource = Source.fromString(EXAMPLE_JSON_SOURCE_CUSTOM_TYPE);
        assertNotNull("Parsing failure", customSource);
        assertEquals(Source.SourceType.UNKNOWN, customSource.getType());
        assertEquals(DOGE_COIN, customSource.getTypeRaw());
        assertNull(customSource.getSourceTypeModel());
        assertNotNull("Failed to find custom api params",
                customSource.getSourceTypeData());

        assertNotNull(customSource.getReceiver());
        assertNotNull(customSource.getCodeVerification());
    }

    @Test
    public void fromJsonString_withDeletedCardJson_shouldReturnSourceWithCardId() {
        final Source source = Source.fromString(DELETED_CARD_JSON);
        assertNotNull(source);
        assertEquals("card_1ELdAlCRMbs6FrXfNbmZEOb7", source.getId());
    }

    @Test
    public void fromJsonString_withCreatedCardJson_shouldReturnSourceWithCardId() {
        final Source source = SourceFixtures.CARD;
        assertNotNull(source);
        assertEquals("card_1ELxrOCRMbs6FrXfdxOGjnaD", source.getId());
        assertEquals(Source.SourceType.CARD, source.getType());
        assertTrue(source.getSourceTypeModel() instanceof SourceCardData);

        final SourceCardData sourceCardData = (SourceCardData) source.getSourceTypeModel();
        assertEquals(Card.CardBrand.VISA, sourceCardData.getBrand());
    }

    @Test
    public void fromJsonString_withWeChatSourceJson() {
        final Source source = SourceFixtures.WECHAT;
        assertNotNull(source);

        assertEquals(Source.USD, source.getCurrency());
        assertTrue(source.isLiveMode());

        final WeChat weChat = source.getWeChat();
        assertNotNull(weChat);
        assertEquals("wxa0df8has9d78ce", weChat.appId);
    }
}
