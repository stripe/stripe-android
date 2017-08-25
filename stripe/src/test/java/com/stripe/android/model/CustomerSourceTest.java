package com.stripe.android.model;

import com.stripe.android.testharness.JsonTestUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static com.stripe.android.model.CardTest.JSON_CARD;
import static com.stripe.android.model.SourceTest.EXAMPLE_BITCOIN_SOURCE;
import static com.stripe.android.model.SourceTest.EXAMPLE_JSON_SOURCE_WITHOUT_NULLS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * Test class for {@link CustomerSource} model class.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 23)
public class CustomerSourceTest {

    @Test
    public void fromJson_whenCard_createsCustomerSourceData() {
        try {
            JSONObject jsonCard = new JSONObject(JSON_CARD);
            CustomerSource sourceData = CustomerSource.fromJson(jsonCard);
            assertNotNull(sourceData);
            assertNotNull(sourceData.asCard());
            assertEquals("card_189fi32eZvKYlo2CHK8NPRME", sourceData.getId());
        } catch (JSONException jsonException) {
            fail("Test data failure: " + jsonException.getMessage());
        }
    }

    @Test
    public void fromJson_whenSource_createsCustomerSourceData() {
        CustomerSource sourceData =
                CustomerSource.fromString(EXAMPLE_JSON_SOURCE_WITHOUT_NULLS);
        assertNotNull(sourceData);
        assertNotNull(sourceData.asSource());
        assertEquals("src_19t3xKBZqEXluyI4uz2dxAfQ", sourceData.getId());
    }

    @Test
    public void fromExampleJsonSource_toJson_createsSameObject() {
        try {
            JSONObject original = new JSONObject(EXAMPLE_JSON_SOURCE_WITHOUT_NULLS);
            CustomerSource sourceData = CustomerSource.fromJson(original);
            assertNotNull(sourceData);
            JsonTestUtils.assertJsonEquals(original, sourceData.toJson());
        } catch (JSONException exception) {
            fail("Test data failure: " + exception.getMessage());
        }
    }

    @Test
    public void getSourceType_whenCard_returnsCard() {
        try {
            JSONObject jsonCard = new JSONObject(JSON_CARD);
            CustomerSource sourceData = CustomerSource.fromJson(jsonCard);
            assertNotNull(sourceData);
            assertEquals(Source.CARD, sourceData.getSourceType());
        } catch (JSONException jsonException) {
            fail("Test data failure: " + jsonException.getMessage());
        }
    }

    @Test
    public void getSourceType_whenSourceThatIsNotCard_returnsSourceType() {
        CustomerSource bitcoinSource = CustomerSource.fromString(EXAMPLE_BITCOIN_SOURCE);
        assertNotNull(bitcoinSource);
        assertEquals(Source.BITCOIN, bitcoinSource.getSourceType());
    }
}
