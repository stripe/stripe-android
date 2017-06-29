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

import static com.stripe.android.model.AddressTest.EXAMPLE_JSON_ADDRESS;
import static com.stripe.android.testharness.JsonTestUtils.assertJsonEquals;
import static com.stripe.android.testharness.JsonTestUtils.assertMapEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * Test class for {@link SourceOwner} model.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 23)
public class SourceOwnerTest {

    private static final String EXAMPLE_JSON_OWNER_WITH_NULLS = "{" +
            "\"address\": null," +
            "\"email\": \"jenny.rosen@example.com\"," +
            "\"name\": \"Jenny Rosen\"," +
            "\"phone\": \"4158675309\"," +
            "\"verified_address\": null," +
            "\"verified_email\": null," +
            "\"verified_name\": null," +
            "\"verified_phone\": null" +
            "}";

    static final String EXAMPLE_JSON_OWNER_WITHOUT_NULLS = "{" +
            "\"address\":"+ EXAMPLE_JSON_ADDRESS + "," +
            "\"email\": \"jenny.rosen@example.com\"," +
            "\"name\": \"Jenny Rosen\"," +
            "\"phone\": \"4158675309\"," +
            "\"verified_address\":"+ EXAMPLE_JSON_ADDRESS + "," +
            "\"verified_email\": \"jenny.rosen@example.com\"," +
            "\"verified_name\": \"Jenny Rosen\"," +
            "\"verified_phone\": \"4158675309\"" +
            "}";

    static final Map<String, Object> EXAMPLE_MAP_OWNER = new HashMap<String, Object>() {{
        put("email", "jenny.rosen@example.com");
        put("name", "Jenny Rosen");
        put("phone", "4158675309");
    }};

    private SourceOwner mSourceOwner;

    @Before
    public void setup() {
        mSourceOwner = SourceOwner.fromString(EXAMPLE_JSON_OWNER_WITHOUT_NULLS);
        assertNotNull(mSourceOwner);
    }

    @Test
    public void fromJsonStringWithoutNulls_backToJson_createsIdenticalElement() {
        try {
            JSONObject rawConversion = new JSONObject(EXAMPLE_JSON_OWNER_WITHOUT_NULLS);
            assertJsonEquals(rawConversion, mSourceOwner.toJson());
        } catch (JSONException jsonException) {
            fail("Test Data failure: " + jsonException.getLocalizedMessage());
        }
    }

    @Test
    public void fromJsonStringWithNulls_toMap_createsExpectedMap() {
        SourceOwner ownerWithNulls = SourceOwner.fromString(EXAMPLE_JSON_OWNER_WITH_NULLS);
        assertNotNull("Test Data failure during parsing", ownerWithNulls);
        assertMapEquals(EXAMPLE_MAP_OWNER, ownerWithNulls.toMap());
    }
}
