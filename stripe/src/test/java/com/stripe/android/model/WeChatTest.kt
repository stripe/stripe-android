package com.stripe.android.model

import kotlin.test.Test
import kotlin.test.assertEquals
import org.json.JSONException
import org.json.JSONObject

class WeChatTest {

    @Test
    @Throws(JSONException::class)
    fun fromJson_shouldReturnExpectedObject() {
        val actual = WeChat.fromJson(JSONObject(WE_CHAT_PAY_JSON))

        val expected = WeChat.Builder()
            .setStatementDescriptor("ORDER 123")
            .setAppId("wxa0dfnoie578ce")
            .setNonce("yFNjgfoni3kZEPYID")
            .setPackageValue("Sign=WXPay")
            .setPartnerId("2623457")
            .setPrepayId("wx070440552351e841913701900")
            .setSign("1A98A09EA74DCF12349B33DED3FF6BCED1C062C63B43AE773D8")
            .setTimestamp("1565134055")
            .build()

        assertEquals(expected, actual)
    }

    companion object {

        private val WE_CHAT_PAY_JSON = "{\n" +
            "\t\t\"statement_descriptor\": \"ORDER 123\",\n" +
            "\t\t\"android_appId\": \"wxa0dfnoie578ce\",\n" +
            "\t\t\"android_nonceStr\": \"yFNjgfoni3kZEPYID\",\n" +
            "\t\t\"android_package\": \"Sign=WXPay\",\n" +
            "\t\t\"android_partnerId\": \"2623457\",\n" +
            "\t\t\"android_prepayId\": \"wx070440552351e841913701900\",\n" +
            "\t\t\"android_sign\": \"1A98A09EA74DCF12349B33DED3FF6BCED1C062C63B43AE773D8\",\n" +
            "\t\t\"android_timeStamp\": \"1565134055\"\n" +
            "\t}"
    }
}
