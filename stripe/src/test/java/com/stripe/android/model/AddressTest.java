package com.stripe.android.model;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.HashMap;
import java.util.Map;

import static com.stripe.android.testharness.JsonTestUtils.assertJsonEquals;
import static com.stripe.android.testharness.JsonTestUtils.assertMapEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * Test class for {@link Address}.
 */
@RunWith(RobolectricTestRunner.class)
public class AddressTest {

    static final String JSON_ADDRESS = "{" +
            "\"city\": \"San Francisco\"," +
            "\"country\": \"US\",\n" +
            "\"line1\": \"123 Market St\"," +
            "\"line2\": \"#345\"," +
            "\"postal_code\": \"94107\"," +
            "\"state\": \"CA\"" +
            "}";

    private static final Map<String, Object> MAP_ADDRESS = new HashMap<String, Object>() {{
        put("city", "San Francisco");
        put("country", "US");
        put("line1", "123 Market St");
        put("line2", "#345");
        put("postal_code", "94107");
        put("state", "CA");
    }};

    private static final Address ADDRESS = Address.fromString(JSON_ADDRESS);

    @Test
    public void fromJsonString_backToJson_createsIdenticalElement() {
        assertNotNull(ADDRESS);
        try {
            JSONObject rawConversion = new JSONObject(JSON_ADDRESS);
            assertJsonEquals(rawConversion, ADDRESS.toJson());
        } catch (JSONException jsonException) {
            fail("Test Data failure: " + jsonException.getLocalizedMessage());
        }
    }

    @Test
    public void fromJsonString_toMap_createsExpectedMap() {
        assertNotNull(ADDRESS);
        assertMapEquals(MAP_ADDRESS, ADDRESS.toMap());
    }

    @Test
    public void builderConstructor_whenCalled_createsExpectedAddress() {
        final Address address = new Address.Builder()
                .setCity("San Francisco")
                .setCountry("US")
                .setLine1("123 Market St")
                .setLine2("#345")
                .setPostalCode("94107")
                .setState("CA")
                .build();
        assertEquals(address, ADDRESS);
    }
}
