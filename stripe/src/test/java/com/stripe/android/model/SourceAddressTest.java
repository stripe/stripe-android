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
 * Test class for {@link SourceAddress}.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 23)
public class SourceAddressTest {

    static final String EXAMPLE_JSON_ADDRESS = "{" +
            "\"city\": \"San Francisco\"," +
            "\"country\": \"US\",\n" +
            "\"line1\": \"123 Market St\"," +
            "\"line2\": \"#345\"," +
            "\"postal_code\": \"94107\"," +
            "\"state\": \"CA\"" +
            "}";

    private static final Map<String, Object> EXAMPLE_MAP_ADDRESS = new HashMap<String, Object>() {{
        put("city", "San Francisco");
        put("country", "US");
        put("line1", "123 Market St");
        put("line2", "#345");
        put("postal_code", "94107");
        put("state", "CA");
    }};

    private SourceAddress mSourceAddress;

    @Before
    public void setup() {
        mSourceAddress = SourceAddress.fromString(EXAMPLE_JSON_ADDRESS);
        assertNotNull(mSourceAddress);
    }

    @Test
    public void fromJsonString_backToJson_createsIdenticalElement() {
        try {
            JSONObject rawConversion = new JSONObject(EXAMPLE_JSON_ADDRESS);
            assertJsonEquals(rawConversion, mSourceAddress.toJson());
        } catch (JSONException jsonException) {
            fail("Test Data failure: " + jsonException.getLocalizedMessage());
        }
    }

    @Test
    public void fromJsonString_toMap_createsExpectedMap() {
        assertMapEquals(EXAMPLE_MAP_ADDRESS, mSourceAddress.toMap());
    }
}
