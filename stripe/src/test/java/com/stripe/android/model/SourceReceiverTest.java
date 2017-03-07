package com.stripe.android.model;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.HashMap;
import java.util.Map;

import static com.stripe.android.testharness.JsonTestUtils.assertJsonEquals;
import static com.stripe.android.testharness.JsonTestUtils.assertMapEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * Test class for {@link SourceReceiver}.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 23)
public class SourceReceiverTest {

    static final String EXAMPLE_JSON_RECEIVER = "{" +
            "\"address\": \"test_1MBhWS3uv4ynCfQXF3xQjJkzFPukr4K56N\"," +
            "\"amount_charged\": 0," +
            "\"amount_received\": 0," +
            "\"amount_returned\": 0" +
            "}";

    static final Map<String, Object> EXAMPLE_MAP_RECEIVER =
            new HashMap<String, Object>() {{
                put("address", "test_1MBhWS3uv4ynCfQXF3xQjJkzFPukr4K56N");
                put("amount_charged", 0L);
                put("amount_received", 0L);
                put("amount_returned", 0L);
            }};

    private SourceReceiver mSourceReceiver;

    @Before
    public void setup() {
        mSourceReceiver = SourceReceiver.fromString(EXAMPLE_JSON_RECEIVER);
        assertNotNull(mSourceReceiver);
    }

    @Test
    public void fromJsonString_backToJson_createsIdenticalElement() {
        try {
            JSONObject rawConversion = new JSONObject(EXAMPLE_JSON_RECEIVER);
            assertJsonEquals(rawConversion, mSourceReceiver.toJson());
        } catch (JSONException jsonException) {
            fail("Test Data failure: " + jsonException.getLocalizedMessage());
        }
    }

    @Test
    public void fromJsonString_toMap_createsExpectedMap() {
        assertMapEquals(EXAMPLE_MAP_RECEIVER, mSourceReceiver.toMap());
    }

}
