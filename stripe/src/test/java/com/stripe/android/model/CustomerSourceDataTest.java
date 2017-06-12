package com.stripe.android.model;

import com.stripe.android.testharness.JsonTestUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static com.stripe.android.model.CardTest.JSON_CARD;
import static com.stripe.android.model.SourceTest.EXAMPLE_JSON_SOURCE_WITHOUT_NULLS;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * Test class for {@link CustomerSourceData} model class.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 23)
public class CustomerSourceDataTest {

    @Test
    public void fromJson_whenCard_createsCustomerSourceData() {
        try {
            JSONObject jsonCard = new JSONObject(JSON_CARD);
            CustomerSourceData sourceData = CustomerSourceData.fromJson(jsonCard);
            assertNotNull(sourceData);
            assertNotNull(sourceData.asCard());
        } catch (JSONException jsonException) {
            fail("Test data failure: " + jsonException.getMessage());
        }
    }

    @Test
    public void fromJson_whenSource_createsCustomerSourceData() {
        CustomerSourceData sourceData =
                CustomerSourceData.fromString(EXAMPLE_JSON_SOURCE_WITHOUT_NULLS);
        assertNotNull(sourceData);
        assertNotNull(sourceData.asSource());
    }

    @Test
    public void fromExampleJsonSource_toJson_createsSameObject() {
        try {
            JSONObject original = new JSONObject(EXAMPLE_JSON_SOURCE_WITHOUT_NULLS);
            CustomerSourceData sourceData = CustomerSourceData.fromJson(original);
            assertNotNull(sourceData);
            JsonTestUtils.assertJsonEquals(original, sourceData.toJson());
        } catch (JSONException exception) {
            fail("Test data failure: " + exception.getMessage());
        }
    }
}
