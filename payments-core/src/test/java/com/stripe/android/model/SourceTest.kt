package com.stripe.android.model

import com.stripe.android.model.SourceFixtures.CUSTOMER_SOURCE_CARD_JSON
import com.stripe.android.model.SourceFixtures.DELETED_CARD_JSON
import com.stripe.android.model.SourceFixtures.DOGE_COIN
import com.stripe.android.model.SourceFixtures.EXAMPLE_JSON_SOURCE_CUSTOM_TYPE
import com.stripe.android.model.parsers.SourceJsonParser
import org.json.JSONObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Test class for [Source] model.
 */
class SourceTest {
    @Test
    fun fromJsonStringWithoutNulls_isNotNull() {
        assertNotNull(parse(CUSTOMER_SOURCE_CARD_JSON))
    }

    @Test
    fun fromJsonString_withCustomType_createsSourceWithCustomType() {
        val customSource = requireNotNull(parse(EXAMPLE_JSON_SOURCE_CUSTOM_TYPE))
        assertEquals(Source.SourceType.UNKNOWN, customSource.type)
        assertEquals(DOGE_COIN, customSource.typeRaw)
        assertNull(customSource.sourceTypeModel)
        requireNotNull(customSource.sourceTypeData)

        assertNotNull(customSource.receiver)
        assertNotNull(customSource.codeVerification)
    }

    @Test
    fun fromJsonString_withDeletedCardJson_shouldReturnSourceWithCardId() {
        val source = parse(DELETED_CARD_JSON)
        assertEquals("card_1ELdAlCRMbs6FrXfNbmZEOb7", source?.id)
    }

    @Test
    fun fromJsonString_withCreatedCardJson_shouldReturnSourceWithCardId() {
        val source = SourceFixtures.CARD
        assertEquals("card_1ELxrOCRMbs6FrXfdxOGjnaD", source.id)
        assertEquals(Source.SourceType.CARD, source.type)
        assertTrue(source.sourceTypeModel is SourceTypeModel.Card)

        val sourceCardData = source.sourceTypeModel as SourceTypeModel.Card?
        assertEquals(CardBrand.Visa, sourceCardData?.brand)
    }

    @Test
    fun fromJsonString_withWeChatSourceJson() {
        val source = SourceFixtures.WECHAT
        assertNotNull(source)

        assertEquals(Source.USD, source.currency)
        assertTrue(source.isLiveMode == true)

        val weChat = source.weChat
        assertNotNull(weChat)
        assertEquals("wxa0df8has9d78ce", weChat.appId)
    }

    @Test
    fun fromJson_withSourceOrderAndStatementDescriptor() {
        assertEquals(
            SourceOrderFixtures.SOURCE_ORDER,
            SourceFixtures.SOURCE_WITH_SOURCE_ORDER.sourceOrder
        )

        assertEquals(
            "WIDGET FACTORY",
            SourceFixtures.SOURCE_WITH_SOURCE_ORDER.statementDescriptor
        )
    }

    private companion object {
        private fun parse(jsonObject: JSONObject): Source? {
            return SourceJsonParser().parse(jsonObject)
        }
    }
}
