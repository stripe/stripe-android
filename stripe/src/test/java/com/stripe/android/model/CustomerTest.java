package com.stripe.android.model;

import com.stripe.android.testharness.JsonTestUtils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

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

    public static final String TEST_CUSTOMER_OBJECT =
            "{\n" +
            "  \"id\": \"cus_AQsHpvKfKwJDrF\",\n" +
            "  \"object\": \"customer\",\n" +
            "  \"account_balance\": 0,\n" +
            "  \"created\": 1491584731,\n" +
            "  \"currency\": \"usd\",\n" +
            "  \"default_source\": null,\n" +
            "  \"delinquent\": false,\n" +
            "  \"description\": \"iOS SDK example customer\",\n" +
            "  \"discount\": null,\n" +
            "  \"email\": \"abc@example.com\",\n" +
            "  \"livemode\": false,\n" +
            "  \"metadata\": {\n" +
            "  },\n" +
            "  \"shipping\": null,\n" +
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
    public void fromJson_whenNotACustomer_returnsNull() {
        assertNull(Customer.fromString(NON_CUSTOMER_OBJECT));
    }

    @Test
    public void fromJson_whenCustomer_returnsExpectedCustomer() {
        Customer customer = Customer.fromString(TEST_CUSTOMER_OBJECT);
        assertNotNull(customer);
        assertEquals("cus_AQsHpvKfKwJDrF", customer.getId());
        assertEquals(Integer.valueOf(0), customer.getAccountBalance());
        assertEquals(Long.valueOf(1491584731), customer.getCreated());
        assertEquals("usd", customer.getCurrency());
        assertNull(customer.getDefaultSource());
        assertFalse(customer.getDelinquent());
        assertNull(customer.getDiscount());
        assertEquals("abc@example.com", customer.getEmail());
        assertFalse(customer.getLivemode());
        JsonTestUtils.assertMapEquals(new HashMap<String, String>(), customer.getMetadata());
        assertNull(customer.getShippingInformation());
        assertNotNull(customer.getCustomerSources());
    }
}
