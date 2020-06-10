package com.stripe.android.model

import com.stripe.android.model.CardTest.Companion.JSON_CARD_USD
import com.stripe.android.model.SourceFixtures.CUSTOMER_SOURCE_CARD
import com.stripe.android.model.parsers.CustomerSourceJsonParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.json.JSONException
import org.json.JSONObject

/**
 * Test class for [CustomerSource] model class.
 */
class CustomerSourceTest {

    @Test
    fun fromJson_whenCard_createsCustomerSourceData() {
        val jsonCard = JSON_CARD_USD
        val sourceData = parse(jsonCard)
        assertNotNull(sourceData)
        assertNotNull(sourceData.asCard())
        assertEquals("card_189fi32eZvKYlo2CHK8NPRME", sourceData.id)
        assertNull(sourceData.tokenizationMethod)
    }

    @Test
    fun fromJson_whenCardWithTokenization_createsSourceDataWithTokenization() {
        val jsonCard = SourceFixtures.APPLE_PAY
        val sourceData = parse(jsonCard)
        assertNotNull(sourceData)
        assertNotNull(sourceData.asCard())
        assertEquals("card_189fi32eZvKYlo2CHK8NPRME", sourceData.id)
        assertEquals(TokenizationMethod.ApplePay, sourceData.tokenizationMethod)
    }

    @Test
    fun fromJson_whenSource_createsCustomerSourceData() {
        assertNotNull(CUSTOMER_SOURCE_CARD.asSource())
        assertEquals("src_19t3xKBZqEXluyI4uz2dxAfQ", CUSTOMER_SOURCE_CARD.id)
    }

    @Test
    fun fromExampleJsonSource_toJson_createsSameObject() {
        assertNotNull(CUSTOMER_SOURCE_CARD)
    }

    @Test
    @Throws(JSONException::class)
    fun getSourceType_whenCard_returnsCard() {
        val sourceData = parse(JSON_CARD_USD)
        assertNotNull(sourceData)
        assertEquals(Source.SourceType.CARD, sourceData.sourceType)
    }

    @Test
    fun getSourceType_whenSourceThatIsNotCard_returnsSourceType() {
        val alipaySource = parse(SourceFixtures.ALIPAY_JSON)
        assertNotNull(alipaySource)
        assertEquals(Source.SourceType.ALIPAY, alipaySource.sourceType)
    }

    private companion object {
        private fun parse(jsonObject: JSONObject): CustomerSource? {
            return CustomerSourceJsonParser().parse(jsonObject)
        }
    }
}
