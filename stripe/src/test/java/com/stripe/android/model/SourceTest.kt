package com.stripe.android.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.json.JSONObject

/**
 * Test class for [Source] model.
 */
class SourceTest {
    @Test
    fun fromJsonStringWithoutNulls_isNotNull() {
        assertNotNull(Source.fromJson(EXAMPLE_JSON_SOURCE_WITHOUT_NULLS))
    }

    @Test
    fun fromJsonString_withCustomType_createsSourceWithCustomType() {
        val customSource = requireNotNull(Source.fromJson(EXAMPLE_JSON_SOURCE_CUSTOM_TYPE))
        assertEquals(Source.SourceType.UNKNOWN, customSource.type)
        assertEquals(DOGE_COIN, customSource.typeRaw)
        assertNull(customSource.sourceTypeModel)
        requireNotNull(customSource.sourceTypeData)

        assertNotNull(customSource.receiver)
        assertNotNull(customSource.codeVerification)
    }

    @Test
    fun fromJsonString_withDeletedCardJson_shouldReturnSourceWithCardId() {
        val source = Source.fromJson(DELETED_CARD_JSON)
        assertEquals("card_1ELdAlCRMbs6FrXfNbmZEOb7", source?.id)
    }

    @Test
    fun fromJsonString_withCreatedCardJson_shouldReturnSourceWithCardId() {
        val source = SourceFixtures.CARD
        assertEquals("card_1ELxrOCRMbs6FrXfdxOGjnaD", source.id)
        assertEquals(Source.SourceType.CARD, source.type)
        assertTrue(source.sourceTypeModel is SourceCardData)

        val sourceCardData = source.sourceTypeModel as SourceCardData?
        assertEquals(Card.CardBrand.VISA, sourceCardData?.brand)
    }

    @Test
    fun fromJsonString_withWeChatSourceJson() {
        val source = SourceFixtures.WECHAT
        assertNotNull(source)

        assertEquals(Source.USD, source.currency)
        assertTrue(source.isLiveMode!!)

        val weChat = source.weChat
        assertNotNull(weChat)
        assertEquals("wxa0df8has9d78ce", weChat.appId)
    }

    companion object {
        @JvmField
        internal val EXAMPLE_JSON_SOURCE_WITHOUT_NULLS = JSONObject(
            """
            {
                "id": "src_19t3xKBZqEXluyI4uz2dxAfQ",
                "object": "source",
                "amount": 1000,
                "client_secret": "src_client_secret_of43INi1HteJwXVe3djAUosN",
                "code_verification": {
                    "attempts_remaining": 3,
                    "status": "pending"
                },
                "created": 1488499654,
                "currency": "usd",
                "flow": "receiver",
                "livemode": false,
                "metadata": {},
                "owner": {
                    "verified_phone": "4158675309",
                    "address": {
                        "country": "US",
                        "city": "San Francisco",
                        "state": "CA",
                        "postal_code": "94107",
                        "line2": "#345",
                        "line1": "123 Market St"
                    },
                    "phone": "4158675309",
                    "name": "Jenny Rosen",
                    "verified_name": "Jenny Rosen",
                    "verified_email": "jenny.rosen@example.com",
                    "verified_address": {
                        "country": "US",
                        "city": "San Francisco",
                        "state": "CA",
                        "postal_code": "94107",
                        "line2": "#345",
                        "line1": "123 Market St"
                    },
                    "email": "jenny.rosen@example.com"
                },
                "redirect": {
                    "return_url": "https://google.com",
                    "url": "examplecompany://redirect-link",
                    "status": "succeeded"
                },
                "receiver": {
                    "address": "test_1MBhWS3uv4ynCfQXF3xQjJkzFPukr4K56N",
                    "amount_charged": 0,
                    "amount_received": 0,
                    "amount_returned": 0
                },
                "status": "pending",
                "type": "card",
                "usage": "single_use",
                "card": {
                    "address_zip_check": "unchecked",
                    "tokenization_method": "apple_pay",
                    "country": "US",
                    "last4": "4242",
                    "funding": "credit",
                    "cvc_check": "unchecked",
                    "exp_month": 12,
                    "exp_year": 2050,
                    "address_line1_check": "unchecked",
                    "three_d_secure": "optional",
                    "dynamic_last4": "4242",
                    "brand": "Visa"
                }
            }
            """.trimIndent()
        )

        private const val DOGE_COIN = "dogecoin"

        private val EXAMPLE_JSON_SOURCE_CUSTOM_TYPE = JSONObject(
            """
            {
                "id": "src_19t3xKBZqEXluyI4uz2dxAfQ",
                "object": "source",
                "amount": 1000,
                "client_secret": "src_client_secret_of43INi1HteJwXVe3djAUosN",
                "code_verification": {
                    "attempts_remaining": 3,
                    "status": "pending"
                },
                "created": 1488499654,
                "currency": "usd",
                "flow": "receiver",
                "livemode": false,
                "metadata": {},
                "owner": {
                    "verified_phone": "4158675309",
                    "address": {
                        "country": "US",
                        "city": "San Francisco",
                        "state": "CA",
                        "postal_code": "94107",
                        "line2": "#345",
                        "line1": "123 Market St"
                    },
                    "phone": "4158675309",
                    "name": "Jenny Rosen",
                    "verified_name": "Jenny Rosen",
                    "verified_email": "jenny.rosen@example.com",
                    "verified_address": {
                        "country": "US",
                        "city": "San Francisco",
                        "state": "CA",
                        "postal_code": "94107",
                        "line2": "#345",
                        "line1": "123 Market St"
                    },
                    "email": "jenny.rosen@example.com"
                },
                "redirect": {
                    "return_url": "https://google.com",
                    "url": "examplecompany://redirect-link",
                    "status": "succeeded"
                },
                "receiver": {
                    "address": "test_1MBhWS3uv4ynCfQXF3xQjJkzFPukr4K56N",
                    "amount_charged": 0,
                    "amount_received": 0,
                    "amount_returned": 0
                },
                "status": "pending",
                "type": "dogecoin",
                "usage": "single_use",
                "dogecoin": {
                    "address": "test_1MBhWS3uv4ynCfQXF3xQjJkzFPukr4K56N",
                    "amount": 2371000,
                    "amount_charged": 0,
                    "amount_received": 0,
                    "amount_returned": 0,
                    "uri": "dogecoin:test_1MBhWS3uv4ynCfQXF3xQjJkzFPukr4K56N?amount=0.02371000"
                }
            }
            """.trimIndent()
        )

        private val DELETED_CARD_JSON = JSONObject(
            """
            {
                "id": "card_1ELdAlCRMbs6FrXfNbmZEOb7",
                "object": "card",
                "deleted": true
            }
            """.trimIndent()
        )
    }
}
