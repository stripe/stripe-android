package com.stripe.android.model;

import com.stripe.android.testharness.JsonTestUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static com.stripe.android.model.CardTest.JSON_CARD;
import static com.stripe.android.model.CustomerSourceTest.JSON_APPLE_PAY_CARD;
import static com.stripe.android.model.SourceTest.EXAMPLE_ALIPAY_SOURCE;
import static com.stripe.android.model.SourceTest.EXAMPLE_JSON_SOURCE_WITHOUT_NULLS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test class for {@link Customer} model object.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 25)
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
    public void fromJson_whenCustomerHasApplePay_returnsCustomerWithoutApplePaySources() {
        Customer customer = Customer.fromString(createTestCustomerObjectWithApplePaySource());
        assertNotNull(customer);
        assertEquals(2, customer.getSources().size());
        // Note that filtering the apple_pay sources intentionally does not change the total
        // count value.
        assertEquals(Integer.valueOf(5), customer.getTotalCount());
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

    private String createTestCustomerObjectWithApplePaySource() {
        try {
            JSONObject rawJsonCustomer = new JSONObject(TEST_CUSTOMER_OBJECT);
            JSONObject sourcesObject = rawJsonCustomer.getJSONObject("sources");
            JSONArray sourcesArray = sourcesObject.getJSONArray("data");

            sourcesObject.put("total_count", 5);
            CustomerSource applePayCard = CustomerSource.fromString(JSON_APPLE_PAY_CARD);
            assertNotNull(applePayCard);
            sourcesArray.put(applePayCard.toJson());

            Card testCard = Card.fromString(JSON_CARD);
            assertNotNull(testCard);

            JSONObject manipulatedCard = new JSONObject(JSON_CARD);
            manipulatedCard.put("id", "card_id55555");
            manipulatedCard.put("tokenization_method", "apple_pay");
            Card manipulatedApplePayCard = Card.fromJson(manipulatedCard);
            assertNotNull(manipulatedApplePayCard);

            Source sourceCardWithApplePay = Source.fromString(EXAMPLE_JSON_SOURCE_WITHOUT_NULLS);
            // Note that we don't yet explicitly support bitcoin sources, but this data is
            // convenient for the test because it is not an apple pay source.
            Source alipaySource = Source.fromString(EXAMPLE_ALIPAY_SOURCE);
            assertNotNull(sourceCardWithApplePay);
            assertNotNull(alipaySource);
            sourcesArray.put(sourceCardWithApplePay.toJson());
            sourcesArray.put(alipaySource.toJson());

            sourcesArray.put(testCard.toJson());
            sourcesArray.put(manipulatedApplePayCard.toJson());
            sourcesObject.put("data", sourcesArray);

            rawJsonCustomer.put("sources", sourcesObject);

            // Verify JSON manipulation
            assertTrue(rawJsonCustomer.has("sources"));
            assertTrue(rawJsonCustomer.getJSONObject("sources").has("data"));
            assertEquals(5, rawJsonCustomer.getJSONObject("sources").getJSONArray("data").length());
            return rawJsonCustomer.toString();
        } catch (JSONException testDataException) {
            fail("Test data failure: " + testDataException.getMessage());
        }
        return null;
    }
}
