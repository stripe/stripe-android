package com.stripe.android.model

import com.stripe.android.model.SourceOrderFixtures.SOURCE_ORDER_JSON
import com.stripe.android.model.parsers.SourceJsonParser
import org.json.JSONObject

@Suppress("MaxLineLength")
internal object SourceFixtures {
    private val PARSER = SourceJsonParser()

    val ALIPAY_JSON = JSONObject(
        """
        {
            "id": "src_1AtlSXBZqEXluyI4JgBYTq5W",
            "object": "source",
            "amount": 1000,
            "client_secret": "src_client_secret_BGI2mBjd810BJEbvWRd83jac",
            "created": 1503443217,
            "currency": "usd",
            "flow": "receiver",
            "livemode": false,
            "metadata": {},
            "owner": {
                "address": null,
                "email": "jenny.rosen@example.com",
                "name": null,
                "phone": null,
                "verified_address": null,
                "verified_email": null,
                "verified_name": null,
                "verified_phone": null
            },
            "receiver": {
                "address": "test_1MBhWS3uv4ynCfQXF3xQjJkzFPukr4K56N",
                "amount_charged": 0,
                "amount_received": 0,
                "amount_returned": 0,
                "refund_attributes_method": "email",
                "refund_attributes_status": "missing"
            },
            "statement_descriptor": null,
            "status": "pending",
            "type": "alipay",
            "usage": "single_use"
        }
        """.trimIndent()
    )

    val WECHAT = requireNotNull(
        PARSER.parse(
            JSONObject(
                """
        {
            "id": "src_1F4ZSkBNJ02",
            "object": "source",
            "amount": 100,
            "client_secret": "src_client_secret_FZiuAs6g3ri",
            "created": 1565124054,
            "currency": "usd",
            "flow": "none",
            "livemode": true,
            "metadata": {},
            "owner": {
                "address": null,
                "email": null,
                "name": null,
                "phone": null,
                "verified_address": null,
                "verified_email": null,
                "verified_name": null,
                "verified_phone": null
            },
            "statement_descriptor": null,
            "status": "pending",
            "type": "wechat",
            "usage": "single_use",
            "wechat": {
                "statement_descriptor": "ORDER AT11990",
                "android_appId": "wxa0df8has9d78ce",
                "android_nonceStr": "yFNjg8d9hsfaEPYID",
                "android_package": "Sign=WXPay",
                "android_partnerId": "268716457",
                "android_prepayId": "wx070440550af89hAh8941913701900",
                "android_sign": "1A98A09EA74DCF006598h89433DED3FF6BCED1C062C63B43AE773D8",
                "android_timeStamp": "1565124055",
                "ios_native_url": "weixin://app/wxa0df8has9d78ce/pay/",
                "qr_code_url": null
            }
        }
                """.trimIndent()
            )
        )
    )

    val SOURCE_CARD_JSON = JSONObject(
        """
        {
            "id": "src_19t3xKBZqEXluyI4uz2dxAfQ",
            "object": "source",
            "amount": 1000,
            "client_secret": "src_client_secret_of43INi1HteJwXVe3djAUosN",
            "created": 1488499654,
            "currency": "usd",
            "flow": "receiver",
            "livemode": false,
            "metadata": {},
            "owner": {
                "address": null,
                "email": "jenny.rosen@example.com",
                "name": "Jenny Rosen",
                "phone": "4158675309",
                "verified_address": null,
                "verified_email": null,
                "verified_name": null,
                "verified_phone": null
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
                "exp_month": 12,
                "exp_year": 2050,
                "address_line1_check": "unchecked",
                "address_zip_check": "unchecked",
                "brand": "Visa",
                "country": "US",
                "cvc_check": "unchecked",
                "funding": "credit",
                "last4": "4242",
                "three_d_secure": "optional"
            }
        }
        """.trimIndent()
    )

    val SOURCE_CARD = requireNotNull(PARSER.parse(SOURCE_CARD_JSON))

    val CARD = requireNotNull(
        PARSER.parse(
            JSONObject(
                """
        {
            "id": "card_1ELxrOCRMbs6FrXfdxOGjnaD",
            "object": "card",
            "address_city": null,
            "address_country": null,
            "address_line1": null,
            "address_line1_check": null,
            "address_line2": null,
            "address_state": null,
            "address_zip": null,
            "address_zip_check": null,
            "brand": "Visa",
            "country": "US",
            "customer": "cus_Epd7N0VR3cdjsr",
            "cvc_check": null,
            "dynamic_last4": null,
            "exp_month": 4,
            "exp_year": 2020,
            "funding": "credit",
            "last4": "4242",
            "metadata": {},
            "name": null,
            "tokenization_method": null
        }
                """.trimIndent()
            )
        )
    )

    val APPLE_PAY = JSONObject(
        """
        {
            "id": "card_189fi32eZvKYlo2CHK8NPRME",
            "object": "card",
            "address_city": "San Francisco",
            "address_country": "US",
            "address_line1": "123 Market St",
            "address_line1_check": "unavailable",
            "address_line2": "#345",
            "address_state": "CA",
            "address_zip": "94107",
            "address_zip_check": "unavailable",
            "brand": "Visa",
            "country": "US",
            "currency": "usd",
            "customer": "customer77",
            "cvc_check": "unavailable",
            "exp_month": 8,
            "exp_year": 2017,
            "funding": "credit",
            "fingerprint": "abc123",
            "last4": "4242",
            "name": "John Cardholder",
            "tokenization_method": "apple_pay"
        }
        """.trimIndent()
    )

    private val SOURCE_REDIRECT_JSON = JSONObject(
        """
        {
            "return_url": "https://google.com",
            "status": "succeeded",
            "url": "examplecompany://redirect-link"
        }
        """.trimIndent()
    )

    val REDIRECT = SourceJsonParser.RedirectJsonParser()
        .parse(SOURCE_REDIRECT_JSON)

    private val SOURCE_CODE_VERIFICATION_JSON = JSONObject(
        """
        {
            "attempts_remaining": 3,
            "status": "pending"
        }
        """.trimIndent()
    )

    val SOURCE_CODE_VERIFICATION =
        SourceJsonParser.CodeVerificationJsonParser().parse(SOURCE_CODE_VERIFICATION_JSON)

    private val SOURCE_RECEIVER_JSON = JSONObject(
        """
        {
            "address": "test_1MBhWS3uv4ynCfQXF3xQjJkzFPukr4K56N",
            "amount_charged": 10,
            "amount_received": 20,
            "amount_returned": 30
        }
        """.trimIndent()
    )

    val SOURCE_RECEIVER = SourceJsonParser.ReceiverJsonParser()
        .parse(SOURCE_RECEIVER_JSON)

    val SOURCE_OWNER_WITH_NULLS = JSONObject(
        """
        {
            "address": null,
            "email": "jenny.rosen@example.com",
            "name": "Jenny Rosen",
            "phone": "4158675309",
            "verified_address": null,
            "verified_email": null,
            "verified_name": null,
            "verified_phone": null
        }
        """.trimIndent()
    )

    val SOURCE_OWNER_WITHOUT_NULLS = JSONObject(
        """
        {
            "address": {
                "country": "US",
                "city": "San Francisco",
                "state": "CA",
                "postal_code": "94107",
                "line2": "#345",
                "line1": "123 Market St"
            },
            "email": "jenny.rosen@example.com",
            "name": "Jenny Rosen",
            "phone": "4158675309",
            "verified_address": {
                "country": "US",
                "city": "San Francisco",
                "state": "CA",
                "postal_code": "94107",
                "line2": "#345",
                "line1": "123 Market St"
            },
            "verified_email": "jenny.rosen@example.com",
            "verified_name": "Jenny Rosen",
            "verified_phone": "4158675309"
        }
        """.trimIndent()
    )

    internal val SOURCE_CARD_DATA_WITH_APPLE_PAY_JSON = JSONObject(
        """
            {
                "exp_month": 12,
                "exp_year": 2050,
                "address_line1_check": "unchecked",
                "address_zip_check": "unchecked",
                "brand": "Visa",
                "country": "US",
                "cvc_check": "unchecked",
                "funding": "credit",
                "last4": "4242",
                "three_d_secure": "optional",
                "tokenization_method": "apple_pay",
                "dynamic_last4": "4242"
            }
        """.trimIndent()
    )

    internal val SOURCE_WITH_SOURCE_ORDER = requireNotNull(
        PARSER.parse(
            JSONObject(
                """
        {
            "id": "src_1FfB6GKmrohBAXC",
            "object": "source",
            "amount": 1000,
            "created": 1573848540,
            "currency": "eur",
            "flow": "redirect",
            "livemode": false,
            "metadata": {},
            "source_order": $SOURCE_ORDER_JSON,
            "statement_descriptor": "WIDGET FACTORY",
            "status": "pending",
            "type": "klarna",
            "usage": "single_use"
        }
                """.trimIndent()
            )
        )
    )

    internal val CUSTOMER_SOURCE_CARD_JSON = JSONObject(
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

    internal const val DOGE_COIN = "dogecoin"

    internal val EXAMPLE_JSON_SOURCE_CUSTOM_TYPE = JSONObject(
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

    internal val DELETED_CARD_JSON = JSONObject(
        """
            {
                "id": "card_1ELdAlCRMbs6FrXfNbmZEOb7",
                "object": "card",
                "deleted": true
            }
        """.trimIndent()
    )

    internal val KLARNA = requireNotNull(
        PARSER.parse(
            JSONObject(
                """
        {
            "id": "src_1FfB6GKmrohBAXC",
            "object": "source",
            "amount": 1000,
            "created": 1573848540,
            "currency": "eur",
            "flow": "redirect",
            "livemode": false,
            "metadata": {},
            "source_order": $SOURCE_ORDER_JSON,
            "statement_descriptor": "WIDGET FACTORY",
            "status": "pending",
            "type": "klarna",
            "usage": "single_use",
            "klarna": {
                "first_name": "Arthur",
                "last_name": "Dent",
                "purchase_country": "UK",
                "client_token": "CLIENT_TOKEN",
                "pay_later_asset_urls_descriptive": "https:\/\/x.klarnacdn.net\/payment-method\/assets\/badges\/generic\/klarna.svg",
                "pay_later_asset_urls_standard": "https:\/\/x.klarnacdn.net\/payment-method\/assets\/badges\/generic\/klarna.svg",
                "pay_later_name": "Pay later in 14 days",
                "pay_later_redirect_url": "https:\/\/payment-eu.playground.klarna.com\/8b45xe2",
                "pay_over_time_asset_urls_descriptive": "https:\/\/x.klarnacdn.net\/payment-method\/assets\/badges\/generic\/klarna.svg",
                "pay_over_time_asset_urls_standard": "https:\/\/x.klarnacdn.net\/payment-method\/assets\/badges\/generic\/klarna.svg",
                "pay_over_time_name": "3 interest-free instalments",
                "pay_over_time_redirect_url": "https:\/\/payment-eu.playground.klarna.com\/8DA6imn",
                "payment_method_categories": "pay_later,pay_over_time"
            }
        }
                """.trimIndent()
            )
        )
    )
}
