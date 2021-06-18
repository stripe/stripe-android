package com.stripe.android.model

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.parsers.CustomerJsonParser
import org.json.JSONObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Test class for [Customer] model object.
 */
class CustomerTest {

    @Test
    fun fromJson_whenNotACustomer_returnsNull() {
        assertThat(parse(NON_CUSTOMER_OBJECT))
            .isNull()
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
    fun fromJson_whenCustomerHasApplePay_returnsCustomerWithoutApplePaySources() {
        val customer = parse(createTestCustomerObjectWithApplePaySource())
        assertNotNull(customer)
        assertThat(customer.sources)
            .hasSize(2)
        // Note that filtering the apple_pay sources intentionally does not change the total
        // count value.
        assertEquals(5, customer.totalCount)
    }

    @Test
    fun fromJson_createsSameObject() {
        val expectedCustomer = parse(CustomerFixtures.CUSTOMER_JSON)
        assertNotNull(expectedCustomer)
        assertEquals(
            expectedCustomer,
            parse(CustomerFixtures.CUSTOMER_JSON)
        )
    }

    private fun createTestCustomerObjectWithApplePaySource(): JSONObject {
        val rawJsonCustomer = CustomerFixtures.CUSTOMER_JSON
        val sourcesObject = rawJsonCustomer.getJSONObject("sources")

        val sourcesArray = sourcesObject.getJSONArray("data").apply {
            put(SourceFixtures.APPLE_PAY)

            // Note that we don't yet explicitly support bitcoin sources, but this data is
            // convenient for the test because it is not an apple pay source.
            put(SourceFixtures.CUSTOMER_SOURCE_CARD_JSON)
            put(SourceFixtures.ALIPAY_JSON)
            put(JSONObject(CardFixtures.CARD_USD_JSON.toString()))
            put(
                JSONObject(CardFixtures.CARD_USD_JSON.toString()).apply {
                    put("id", "card_id55555")
                    put("tokenization_method", "apple_pay")
                }
            )
        }

        rawJsonCustomer.put(
            "sources",
            sourcesObject
                .put("data", sourcesArray)
                .put("total_count", 5)
        )

        // Verify JSON manipulation
        assertEquals(
            5,
            rawJsonCustomer.getJSONObject("sources").getJSONArray("data").length()
        )

        return JSONObject(rawJsonCustomer.toString())
    }

    private companion object {
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

        private fun parse(jsonObject: JSONObject): Customer? {
            return CustomerJsonParser().parse(jsonObject)
        }
    }
}
