package com.stripe.android.model

import com.stripe.android.model.CardTest.Companion.JSON_CARD_USD
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.json.JSONException
import org.json.JSONObject

/**
 * Test class for [Customer] model object.
 */
class CustomerTest {

    @Test
    fun fromString_whenStringIsNull_returnsNull() {
        assertNull(Customer.fromString(null))
    }

    @Test
    fun fromJson_whenNotACustomer_returnsNull() {
        assertNull(Customer.fromJson(NON_CUSTOMER_OBJECT))
    }

    @Test
    fun fromJson_whenCustomer_returnsExpectedCustomer() {
        val customer = CustomerFixtures.CUSTOMER
        assertNotNull(customer)
        assertEquals("cus_AQsHpvKfKwJDrF", customer.id)
        assertEquals("abc123", customer.defaultSource)
        assertNull(customer.shippingInformation)
        assertNotNull(customer.sources)
        assertEquals("/v1/customers/cus_AQsHpvKfKwJDrF/sources", customer.url)
        assertFalse(customer.hasMore)
        assertEquals(0, customer.totalCount)
    }

    @Test
    @Throws(JSONException::class)
    fun fromJson_whenCustomerHasApplePay_returnsCustomerWithoutApplePaySources() {
        val customer = Customer.fromString(createTestCustomerObjectWithApplePaySource())
        assertNotNull(customer)
        assertEquals(2, customer.sources.size)
        // Note that filtering the apple_pay sources intentionally does not change the total
        // count value.
        assertEquals(5, customer.totalCount)
    }

    @Test
    fun fromJson_createsSameObject() {
        val expectedCustomer = Customer.fromJson(CustomerFixtures.CUSTOMER_JSON)
        assertNotNull(expectedCustomer)
        assertEquals(
            expectedCustomer,
            Customer.fromJson(CustomerFixtures.CUSTOMER_JSON)
        )
    }

    @Throws(JSONException::class)
    private fun createTestCustomerObjectWithApplePaySource(): String {
        val rawJsonCustomer = CustomerFixtures.CUSTOMER_JSON
        val sourcesObject = rawJsonCustomer.getJSONObject("sources")
        val sourcesArray = sourcesObject.getJSONArray("data")

        sourcesObject.put("total_count", 5)
        sourcesArray.put(SourceFixtures.APPLE_PAY)

        val manipulatedCard = JSONObject(JSON_CARD_USD.toString())
        manipulatedCard.put("id", "card_id55555")
        manipulatedCard.put("tokenization_method", "apple_pay")

        // Note that we don't yet explicitly support bitcoin sources, but this data is
        // convenient for the test because it is not an apple pay source.
        sourcesArray.put(SourceTest.EXAMPLE_JSON_SOURCE_WITHOUT_NULLS)
        sourcesArray.put(SourceFixtures.ALIPAY_JSON)
        sourcesArray.put(JSONObject(JSON_CARD_USD.toString()))
        sourcesArray.put(manipulatedCard)
        sourcesObject.put("data", sourcesArray)

        rawJsonCustomer.put("sources", sourcesObject)

        // Verify JSON manipulation
        assertTrue(rawJsonCustomer.has("sources"))
        assertTrue(rawJsonCustomer.getJSONObject("sources").has("data"))
        assertEquals(5,
            rawJsonCustomer.getJSONObject("sources").getJSONArray("data").length())

        return rawJsonCustomer.toString()
    }

    companion object {

        private val NON_CUSTOMER_OBJECT = JSONObject(
            """
            {
                "object": "not_a_customer",
                "has_more": false,
                "total_count": 22,
                "url": "http://google.com"
            }
            """.trimIndent()
        )
    }
}
