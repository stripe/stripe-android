package com.stripe.android.model;

import org.junit.Test;

import static com.stripe.android.model.AddressTest.JSON_ADDRESS;
import static org.junit.Assert.assertNotNull;

/**
 * Test class for {@link SourceOwner} model.
 */
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
            "\"address\":"+ JSON_ADDRESS + "," +
            "\"email\": \"jenny.rosen@example.com\"," +
            "\"name\": \"Jenny Rosen\"," +
            "\"phone\": \"4158675309\"," +
            "\"verified_address\":"+ JSON_ADDRESS + "," +
            "\"verified_email\": \"jenny.rosen@example.com\"," +
            "\"verified_name\": \"Jenny Rosen\"," +
            "\"verified_phone\": \"4158675309\"" +
            "}";

    @Test
    public void fromJsonStringWithoutNulls_isNotNull() {
        assertNotNull(SourceOwner.fromString(EXAMPLE_JSON_OWNER_WITHOUT_NULLS));
    }

    @Test
    public void fromJsonStringWithNulls_IsNotNull() {
        assertNotNull(SourceOwner.fromString(EXAMPLE_JSON_OWNER_WITH_NULLS));
    }
}
