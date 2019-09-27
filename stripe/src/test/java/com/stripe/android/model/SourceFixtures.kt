package com.stripe.android.model

import org.json.JSONObject

internal object SourceFixtures {

    @JvmField
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

    @JvmField
    val WECHAT = Source.fromJson(JSONObject(
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
    ))!!

    @JvmField
    val CARD = Source.fromJson(JSONObject(
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
    ))!!

    @JvmField
    val APPLE_PAY = JSONObject(
        """
        {
            "id": "card_189fi32eZvKYlo2CHK8NPRME",
            "object": "card",
            "address_city": "Des Moines",
            "address_country": "US",
            "address_line1": "123 Any Street",
            "address_line1_check": "unavailable",
            "address_line2": "456",
            "address_state": "IA",
            "address_zip": "50305",
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

    @JvmField
    val SOURCE_REDIRECT_JSON = JSONObject(
        """
        {
            "return_url": "https://google.com",
            "status": "succeeded",
            "url": "examplecompany://redirect-link"
        }
        """.trimIndent()
    )

    @JvmField
    val SOURCE_REDIRECT = SourceRedirect.fromJson(SOURCE_REDIRECT_JSON)!!

    @JvmField
    val SOURCE_CODE_VERIFICATION_JSON = JSONObject(
        """
        {
            "attempts_remaining": 3,
            "status": "pending"
        }
        """.trimIndent()
    )

    @JvmField
    val SOURCE_CODE_VERIFICATION = SourceCodeVerification.fromJson(
        SOURCE_CODE_VERIFICATION_JSON
    )!!

    @JvmField
    val SOURCE_RECEIVER_JSON = JSONObject(
        """
        {
            "address": "test_1MBhWS3uv4ynCfQXF3xQjJkzFPukr4K56N",
            "amount_charged": 10,
            "amount_received": 20,
            "amount_returned": 30
        }
        """.trimIndent()
    )

    @JvmField
    val SOURCE_RECEIVER = SourceReceiver.fromJson(SOURCE_RECEIVER_JSON)!!

    @JvmField
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

    @JvmField
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

    @JvmField
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
}
