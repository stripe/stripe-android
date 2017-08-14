package com.stripe.android.model;

import com.stripe.android.testharness.JsonTestUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

/**
 * Test class for {@link Customer} model object.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 23)
public class CustomerTest {

    private static final String NON_CUSTOMER_OBJECT =
            "{\n" +
                    "    \"object\": \"not_a_customer\",\n" +
                    "    \"has_more\": false,\n" +
                    "    \"total_count\": 22,\n" +
                    "    \"url\": \"http://google.com\"\n" +
                    "}";

    private static final String TEST_CUSTOMER_OBJECT =
            "{\n" +
            "  \"id\": \"cus_AQsHpvKfKwJDrF\",\n" +
            "  \"object\": \"customer\",\n" +
            "  \"default_source\": \"abc123\",\n" +
            "  \"sources\": {\n" +
            "    \"object\": \"list\",\n" +
            "    \"data\": [\n" +
            "\n" +
            "    ],\n" +
            "    \"has_more\": false,\n" +
            "    \"total_count\": 0,\n" +
            "    \"url\": \"/v1/customers/cus_AQsHpvKfKwJDrF/sources\"\n" +
            "  }\n" +
            "}";

    @Test
    public void fromString_whenStringIsNull_returnsNull() {
        assertNull(Customer.fromString(null));
    }

    @Test
    public void fromJson_whenNotACustomer_returnsNull() {
        assertNull(Customer.fromString(NON_CUSTOMER_OBJECT));
    }

    @Test
    public void fromJson_whenCustomer_returnsExpectedCustomer() {
        Customer customer = Customer.fromString(TEST_CUSTOMER_OBJECT);
        assertNotNull(customer);
        assertEquals("cus_AQsHpvKfKwJDrF", customer.getId());
        assertEquals("abc123", customer.getDefaultSource());
        assertNull(customer.getShippingInformation());
        assertNotNull(customer.getSources());
        assertEquals("/v1/customers/cus_AQsHpvKfKwJDrF/sources", customer.getUrl());
        assertFalse(customer.getHasMore());
        assertEquals(Integer.valueOf(0), customer.getTotalCount());
    }

    @Test
    public void fromJson_toJson_createsSameObject() {
        try {
            JSONObject rawJsonCustomer = new JSONObject(TEST_CUSTOMER_OBJECT);
            Customer customer = Customer.fromString(TEST_CUSTOMER_OBJECT);
            assertNotNull(customer);
            JsonTestUtils.assertJsonEquals(rawJsonCustomer, customer.toJson());
        } catch (JSONException testDataException) {
            fail("Test data failure: " + testDataException.getMessage());
        }
    }
}
