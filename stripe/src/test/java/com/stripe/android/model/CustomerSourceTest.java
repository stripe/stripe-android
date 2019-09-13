package com.stripe.android.model;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import static com.stripe.android.model.CardTest.JSON_CARD_USD;
import static com.stripe.android.model.SourceTest.EXAMPLE_JSON_SOURCE_WITHOUT_NULLS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Test class for {@link CustomerSource} model class.
 */
public class CustomerSourceTest {

    @Test
    public void fromJson_whenCard_createsCustomerSourceData() throws JSONException {
        final JSONObject jsonCard = new JSONObject(JSON_CARD_USD);
        final CustomerSource sourceData = CustomerSource.fromJson(jsonCard);
        assertNotNull(sourceData);
        assertNotNull(sourceData.asCard());
        assertEquals("card_189fi32eZvKYlo2CHK8NPRME", sourceData.getId());
        assertNull(sourceData.getTokenizationMethod());
    }

    @Test
    public void fromJson_whenCardWithTokenization_createsSourceDataWithTokenization()
            throws JSONException {
        final JSONObject jsonCard = SourceFixtures.APPLE_PAY;
        final CustomerSource sourceData = CustomerSource.fromJson(jsonCard);
        assertNotNull(sourceData);
        assertNotNull(sourceData.asCard());
        assertEquals("card_189fi32eZvKYlo2CHK8NPRME", sourceData.getId());
        assertEquals("apple_pay", sourceData.getTokenizationMethod());
    }

    @Test
    public void fromJson_whenSource_createsCustomerSourceData() {
        final CustomerSource sourceData =
                CustomerSource.fromString(EXAMPLE_JSON_SOURCE_WITHOUT_NULLS);
        assertNotNull(sourceData);
        assertNotNull(sourceData.asSource());
        assertEquals("src_19t3xKBZqEXluyI4uz2dxAfQ", sourceData.getId());
    }

    @Test
    public void fromExampleJsonSource_toJson_createsSameObject() {
        assertNotNull(CustomerSource.fromString(EXAMPLE_JSON_SOURCE_WITHOUT_NULLS));
    }

    @Test
    public void getSourceType_whenCard_returnsCard() throws JSONException {
        final CustomerSource sourceData = CustomerSource.fromJson(new JSONObject(JSON_CARD_USD));
        assertNotNull(sourceData);
        assertEquals(Source.SourceType.CARD, sourceData.getSourceType());
    }

    @Test
    public void getSourceType_whenSourceThatIsNotCard_returnsSourceType() {
        final CustomerSource alipaySource = CustomerSource.fromJson(SourceFixtures.ALIPAY_JSON);
        assertNotNull(alipaySource);
        assertEquals(Source.SourceType.ALIPAY, alipaySource.getSourceType());
    }
}
