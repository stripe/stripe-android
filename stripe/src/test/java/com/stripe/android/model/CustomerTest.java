package com.stripe.android.model;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import static com.stripe.android.model.CardTest.JSON_CARD_USD;
import static com.stripe.android.model.CustomerSourceTest.JSON_APPLE_PAY_CARD;
import static com.stripe.android.model.SourceTest.EXAMPLE_ALIPAY_SOURCE;
import static com.stripe.android.model.SourceTest.EXAMPLE_JSON_SOURCE_WITHOUT_NULLS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Test class for {@link Customer} model object.
 */
public class CustomerTest {

    private static final String NON_CUSTOMER_OBJECT =
            "{\n" +
                    "    \"object\": \"not_a_customer\",\n" +
                    "    \"has_more\": false,\n" +
                    "    \"total_count\": 22,\n" +
                    "    \"url\": \"http://google.com\"\n" +
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
        final Customer customer = CustomerFixtures.CUSTOMER;
        assertNotNull(customer);
        assertEquals("cus_AQsHpvKfKwJDrF", customer.getId());
        assertEquals("abc123", customer.getDefaultSource());
        assertNull(customer.getShippingInformation());
        assertNotNull(customer.getSources());
        assertEquals("/v1/customers/cus_AQsHpvKfKwJDrF/sources", customer.getUrl());
        assertEquals(Boolean.FALSE, customer.getHasMore());
        assertEquals(Integer.valueOf(0), customer.getTotalCount());
    }

    @Test
    public void fromJson_whenCustomerHasApplePay_returnsCustomerWithoutApplePaySources()
            throws JSONException {
        final Customer customer = Customer.fromString(createTestCustomerObjectWithApplePaySource());
        assertNotNull(customer);
        assertEquals(2, customer.getSources().size());
        // Note that filtering the apple_pay sources intentionally does not change the total
        // count value.
        assertEquals(Integer.valueOf(5), customer.getTotalCount());
    }

    @Test
    public void fromJson_createsSameObject() {
        final Customer expectedCustomer = Customer.fromJson(CustomerFixtures.CUSTOMER_JSON);
        assertNotNull(expectedCustomer);
        assertEquals(
                expectedCustomer,
                Customer.fromJson(CustomerFixtures.CUSTOMER_JSON)
        );
    }

    @NonNull
    private String createTestCustomerObjectWithApplePaySource() throws JSONException {
        final JSONObject rawJsonCustomer = CustomerFixtures.CUSTOMER_JSON;
        final JSONObject sourcesObject = rawJsonCustomer.getJSONObject("sources");
        final JSONArray sourcesArray = sourcesObject.getJSONArray("data");

        sourcesObject.put("total_count", 5);
        sourcesArray.put(new JSONObject(JSON_APPLE_PAY_CARD));

        final JSONObject manipulatedCard = new JSONObject(JSON_CARD_USD);
        manipulatedCard.put("id", "card_id55555");
        manipulatedCard.put("tokenization_method", "apple_pay");

        // Note that we don't yet explicitly support bitcoin sources, but this data is
        // convenient for the test because it is not an apple pay source.
        sourcesArray.put(new JSONObject(EXAMPLE_JSON_SOURCE_WITHOUT_NULLS));
        sourcesArray.put(new JSONObject(EXAMPLE_ALIPAY_SOURCE));
        sourcesArray.put(new JSONObject(JSON_CARD_USD));
        sourcesArray.put(manipulatedCard);
        sourcesObject.put("data", sourcesArray);

        rawJsonCustomer.put("sources", sourcesObject);

        // Verify JSON manipulation
        assertTrue(rawJsonCustomer.has("sources"));
        assertTrue(rawJsonCustomer.getJSONObject("sources").has("data"));
        assertEquals(5,
                rawJsonCustomer.getJSONObject("sources").getJSONArray("data").length());

        return rawJsonCustomer.toString();
    }
}
