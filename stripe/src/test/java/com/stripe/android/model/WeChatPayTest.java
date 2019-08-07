package com.stripe.android.model;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class WeChatPayTest {

    private static final String WE_CHAT_PAY_JSON = "{\n" +
            "\t\t\"statement_descriptor\": \"ORDER 123\",\n" +
            "\t\t\"android_appid\": \"wxa0dfnoie578ce\",\n" +
            "\t\t\"android_noncestr\": \"yFNjgfoni3kZEPYID\",\n" +
            "\t\t\"android_package\": \"Sign=WXPay\",\n" +
            "\t\t\"android_partnerid\": \"2623457\",\n" +
            "\t\t\"android_prepayid\": \"wx070440552351e841913701900\",\n" +
            "\t\t\"android_sign\": \"1A98A09EA74DCF12349B33DED3FF6BCED1C062C63B43AE773D8\",\n" +
            "\t\t\"android_timestamp\": \"1565134055\"\n" +
            "\t}";

    @Test
    public void fromJson_shouldReturnExpectedObject() throws JSONException {
        final WeChatPay actual = WeChatPay.fromJson(new JSONObject(WE_CHAT_PAY_JSON));

        final WeChatPay expected = new WeChatPay.Builder()
                .setStatementDescriptor("ORDER 123")
                .setAppId("wxa0dfnoie578ce")
                .setNonce("yFNjgfoni3kZEPYID")
                .setPackageValue("Sign=WXPay")
                .setPartnerId("2623457")
                .setPrepayId("wx070440552351e841913701900")
                .setSign("1A98A09EA74DCF12349B33DED3FF6BCED1C062C63B43AE773D8")
                .setTimestamp("1565134055")
                .build();

        assertEquals(expected, actual);
    }
}
