package com.stripe.android.model;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(RobolectricTestRunner.class)
public class Stripe3ds2AuthResultTest {
    private static final String AUTH_RESULT_JSON = "{\n" +
            "\t\"id\": \"threeds2_1Ecwz3CRMbs6FrXfThtfogua\",\n" +
            "\t\"object\": \"three_d_secure_2\",\n" +
            "\t\"ares\": {\n" +
            "\t\t\"acsChallengeMandated\": \"Y\",\n" +
            "\t\t\"acsSignedContent\": \"eyJhbGciOiJFUzI1NiJ9.asdfasf.asdfasdfa\",\n" +
            "\t\t\"acsTransID\": \"dd23c757-211a-4c1b-add5-06a1450a642e\",\n" +
            "\t\t\"acsURL\": null,\n" +
            "\t\t\"authenticationType\": \"02\",\n" +
            "\t\t\"cardholderInfo\": null,\n" +
            "\t\t\"messageExtension\": null,\n" +
            "\t\t\"messageType\": \"ARes\",\n" +
            "\t\t\"messageVersion\": \"2.1.0\",\n" +
            "\t\t\"sdkTransID\": \"20158862-9d9d-4d71-83d4-9e65554ed92c\",\n" +
            "\t\t\"threeDSServerTransID\": \"e8ea0b42-0e74-42b2-92b4-1b27005f0596\"\n" +
            "\t},\n" +
            "\t\"created\": 1558541285,\n" +
            "\t\"error\": null,\n" +
            "\t\"livemode\": false,\n" +
            "\t\"source\": \"src_1Ecwz1CRMbs6FrXfUwt98lxf\",\n" +
            "\t\"state\": \"challenge_required\"\n" +
            "}";

    private static final String AUTH_RESULT_WITH_EXTENSIONS_JSON = "{\n" +
            "\t\"id\": \"threeds2_1Ecwz3CRMbs6FrXfThtfogua\",\n" +
            "\t\"object\": \"three_d_secure_2\",\n" +
            "\t\"ares\": {\n" +
            "\t\t\"acsChallengeMandated\": \"Y\",\n" +
            "\t\t\"acsSignedContent\": \"eyJhbGciOiJFUzI1NiJ9.asdfasf.asdfasdfa\",\n" +
            "\t\t\"acsTransID\": \"dd23c757-211a-4c1b-add5-06a1450a642e\",\n" +
            "\t\t\"acsURL\": null,\n" +
            "\t\t\"authenticationType\": \"02\",\n" +
            "\t\t\"cardholderInfo\": null,\n" +
            "\t\t\"messageExtension\": [\n" +
            "\t\t\t{\n" +
            "\t\t\t\t\"name\":\"extension1\",\n" +
            "\t\t\t\t\"id\":\"ID1\",\n" +
            "\t\t\t\t\"criticalityIndicator\":true,\n" +
            "\t\t\t\t\"data\":{\n" +
            "\t\t\t\t\t\"valueOne\":\"value\"\n" +
            "\t\t\t\t}\n" +
            "\t\t\t},\n" +
            "\t\t\t{\n" +
            "\t\t\t\t\"name\":\"extension2\",\n" +
            "\t\t\t\t\"id\":\"ID2\",\n" +
            "\t\t\t\t\"criticalityIndicator\":true,\n" +
            "\t\t\t\t\"data\":{\n" +
            "\t\t\t\t\t\"valueOne\":\"value1\",\n" +
            "\t\t\t\t\t\"valueTwo\":\"value2\"\n" +
            "\t\t\t\t}\n" +
            "\t\t\t},\n" +
            "\t\t\t{\n" +
            "\t\t\t\t\"name\":\"sharedData\",\n" +
            "\t\t\t\t\"id\":\"ID3\",\n" +
            "\t\t\t\t\"criticalityIndicator\":false,\n" +
            "\t\t\t\t\"data\":{\n" +
            "\t\t\t\t\t\"value3\":\"IkpTT05EYXRhIjogew0KImRhdGExIjogInNkYXRhIg0KfQ==\"\n" +
            "\t\t\t\t}\n" +
            "\t\t\t}\n" +
            "\t\t],\n" +
            "\t\t\"messageType\": \"ARes\",\n" +
            "\t\t\"messageVersion\": \"2.1.0\",\n" +
            "\t\t\"sdkTransID\": \"20158862-9d9d-4d71-83d4-9e65554ed92c\",\n" +
            "\t\t\"threeDSServerTransID\": \"e8ea0b42-0e74-42b2-92b4-1b27005f0596\"\n" +
            "\t},\n" +
            "\t\"created\": 1558541285,\n" +
            "\t\"error\": null,\n" +
            "\t\"livemode\": false,\n" +
            "\t\"source\": \"src_1Ecwz1CRMbs6FrXfUwt98lxf\",\n" +
            "\t\"state\": \"challenge_required\"\n" +
            "}";

    private static final String AUTH_RESULT_ERROR_JSON = "{\n" +
            "\t\"id\": \"threeds2_1Ecwz3CRMbs6FrXfThtfogua\",\n" +
            "\t\"object\": \"three_d_secure_2\",\n" +
            "\t\"ares\": {\n" +
            "\t\t\"acsChallengeMandated\": \"Y\",\n" +
            "\t\t\"acsSignedContent\": \"eyJhbGciOiJFUzI1NiJ9.asdfasf.asdfasdfa\",\n" +
            "\t\t\"acsTransID\": \"dd23c757-211a-4c1b-add5-06a1450a642e\",\n" +
            "\t\t\"acsURL\": null,\n" +
            "\t\t\"authenticationType\": \"02\",\n" +
            "\t\t\"cardholderInfo\": null,\n" +
            "\t\t\"messageExtension\": null,\n" +
            "\t\t\"messageType\": \"ARes\",\n" +
            "\t\t\"messageVersion\": \"2.1.0\",\n" +
            "\t\t\"sdkTransID\": \"20158862-9d9d-4d71-83d4-9e65554ed92c\",\n" +
            "\t\t\"threeDSServerTransID\": \"e8ea0b42-0e74-42b2-92b4-1b27005f0596\"\n" +
            "\t},\n" +
            "\t\"created\": 1558541285,\n" +
            "\t\"error\": {\n" +
            "\t\t\"threeDSServerTransID\": \"e8ea0b42-0e74-42b2-92b4-1b27005f0596\",\n" +
            "\t\t\"acsTransID\": \"dd23c757-211a-4c1b-add5-06a1450a642e\",\n" +
            "\t\t\"dsTransID\": \"ff23c757-211a-4c1b-add5-06a1450a642e\",\n" +
            "\t\t\"errorCode\": \"error code 1234\",\n" +
            "\t\t\"errorDescription\": \"error description\",\n" +
            "\t\t\"errorDetail\": \"error detail\",\n" +
            "\t\t\"errorComponent\": \"error component\",\n" +
            "\t\t\"errorMessageType\": \"error message type\",\n" +
            "\t\t\"messageType\": \"Error\",\n" +
            "\t\t\"messageVersion\": \"2.1.0\",\n" +
            "\t\t\"sdkTransID\": \"20158862-9d9d-4d71-83d4-9e65554ed92c\"\n" +
            "\t},\n" +
            "\t\"livemode\": false,\n" +
            "\t\"source\": \"src_1Ecwz1CRMbs6FrXfUwt98lxf\",\n" +
            "\t\"state\": \"challenge_required\"\n" +
            "}";

    @Test
    public void fromJSON_validData_createsObject() throws JSONException {
        final Stripe3ds2AuthResult jsonResult = Stripe3ds2AuthResult
                .fromJson(new JSONObject(AUTH_RESULT_JSON));

        final Stripe3ds2AuthResult expectedResult = new Stripe3ds2AuthResult.Builder()
                .setId("threeds2_1Ecwz3CRMbs6FrXfThtfogua")
                .setObjectType("three_d_secure_2")
                .setLiveMode(false)
                .setCreated(1558541285L)
                .setSource("src_1Ecwz1CRMbs6FrXfUwt98lxf")
                .setState("challenge_required")
                .setAres(new Stripe3ds2AuthResult.Ares.Builder()
                        .setAcsChallengeMandated("Y")
                        .setAcsTransId("dd23c757-211a-4c1b-add5-06a1450a642e")
                        .setAcsSignedContent("eyJhbGciOiJFUzI1NiJ9.asdfasf.asdfasdfa")
                        .setAuthenticationType("02")
                        .setMessageType("ARes")
                        .setMessageVersion("2.1.0")
                        .setSdkTransId("20158862-9d9d-4d71-83d4-9e65554ed92c")
                        .setThreeDSServerTransId("e8ea0b42-0e74-42b2-92b4-1b27005f0596")
                        .build())
                .build();

        assertEquals(expectedResult, jsonResult);
    }

    @Test
    public void fromJSON_dataWithMessageExtensions_createsObject() throws JSONException {
        final Stripe3ds2AuthResult jsonResult = Stripe3ds2AuthResult
                .fromJson(new JSONObject(AUTH_RESULT_WITH_EXTENSIONS_JSON));

        final AbstractMap<String, String> extension1Data = new HashMap<>(1);
        extension1Data.put("valueOne", "value");

        final AbstractMap<String, String> extension2Data = new HashMap<>(2);
        extension2Data.put("valueOne", "value1");
        extension2Data.put("valueTwo", "value2");

        final AbstractMap<String, String> extension3Data = new HashMap<>(1);
        extension3Data.put("value3", "IkpTT05EYXRhIjogew0KImRhdGExIjogInNkYXRhIg0KfQ==");

        final List<Stripe3ds2AuthResult.MessageExtension> extensions = Arrays.asList(
                new Stripe3ds2AuthResult.MessageExtension.Builder()
                        .setName("extension1")
                        .setId("ID1")
                        .setCriticalityIndicator(true)
                        .setData(extension1Data)
                        .build(),
                new Stripe3ds2AuthResult.MessageExtension.Builder()
                        .setName("extension2")
                        .setId("ID2")
                        .setCriticalityIndicator(true)
                        .setData(extension2Data)
                        .build(),
                new Stripe3ds2AuthResult.MessageExtension.Builder()
                        .setName("sharedData")
                        .setId("ID3")
                        .setCriticalityIndicator(false)
                        .setData(extension3Data)
                        .build()
        );

        final Stripe3ds2AuthResult expectedResult = new Stripe3ds2AuthResult.Builder()
                .setId("threeds2_1Ecwz3CRMbs6FrXfThtfogua")
                .setObjectType("three_d_secure_2")
                .setLiveMode(false)
                .setCreated(1558541285L)
                .setSource("src_1Ecwz1CRMbs6FrXfUwt98lxf")
                .setState("challenge_required")
                .setAres(new Stripe3ds2AuthResult.Ares.Builder()
                        .setAcsChallengeMandated("Y")
                        .setAcsTransId("dd23c757-211a-4c1b-add5-06a1450a642e")
                        .setAcsSignedContent("eyJhbGciOiJFUzI1NiJ9.asdfasf.asdfasdfa")
                        .setAuthenticationType("02")
                        .setMessageType("ARes")
                        .setMessageVersion("2.1.0")
                        .setMessageExtension(extensions)
                        .setSdkTransId("20158862-9d9d-4d71-83d4-9e65554ed92c")
                        .setThreeDSServerTransId("e8ea0b42-0e74-42b2-92b4-1b27005f0596")
                        .build())
                .build();

        assertEquals(expectedResult, jsonResult);
    }

    @Test
    public void fromJSON_errorData_createsObjectWithError() throws JSONException {
        final Stripe3ds2AuthResult jsonResult = Stripe3ds2AuthResult
                .fromJson(new JSONObject(AUTH_RESULT_ERROR_JSON));

        final Stripe3ds2AuthResult expectedResult = new Stripe3ds2AuthResult.Builder()
                .setId("threeds2_1Ecwz3CRMbs6FrXfThtfogua")
                .setObjectType("three_d_secure_2")
                .setLiveMode(false)
                .setCreated(1558541285L)
                .setSource("src_1Ecwz1CRMbs6FrXfUwt98lxf")
                .setState("challenge_required")
                .setAres(new Stripe3ds2AuthResult.Ares.Builder()
                        .setAcsChallengeMandated("Y")
                        .setAcsTransId("dd23c757-211a-4c1b-add5-06a1450a642e")
                        .setAcsSignedContent("eyJhbGciOiJFUzI1NiJ9.asdfasf.asdfasdfa")
                        .setAuthenticationType("02")
                        .setMessageType("ARes")
                        .setMessageVersion("2.1.0")
                        .setSdkTransId("20158862-9d9d-4d71-83d4-9e65554ed92c")
                        .setThreeDSServerTransId("e8ea0b42-0e74-42b2-92b4-1b27005f0596")
                        .build())
                .setError(new Stripe3ds2AuthResult.ThreeDS2Error.Builder()
                        .setThreeDSServerTransId("e8ea0b42-0e74-42b2-92b4-1b27005f0596")
                        .setAcsTransId("dd23c757-211a-4c1b-add5-06a1450a642e")
                        .setDsTransId("ff23c757-211a-4c1b-add5-06a1450a642e")
                        .setErrorCode("error code 1234")
                        .setErrorComponent("error component")
                        .setErrorDetail("error detail")
                        .setErrorDescription("error description")
                        .setErrorMessageType("error message type")
                        .setMessageType("Error")
                        .setMessageVersion("2.1.0")
                        .setSdkTransId("20158862-9d9d-4d71-83d4-9e65554ed92c")
                        .build())
                .build();

        assertEquals(expectedResult, jsonResult);
    }
}
