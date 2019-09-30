package com.stripe.android.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.json.JSONObject
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class Stripe3ds2AuthResultTest {

    @Test
    fun fromJSON_validData_createsObject() {
        val result = Stripe3ds2AuthResult.fromJson(AUTH_RESULT_JSON)
        assertTrue(result.ares?.isChallenge == true)

        val expectedResult = Stripe3ds2AuthResult.Builder()
            .setId("threeds2_1Ecwz3CRMbs6FrXfThtfogua")
            .setObjectType("three_d_secure_2")
            .setLiveMode(false)
            .setCreated(1558541285L)
            .setSource("src_1Ecwz1CRMbs6FrXfUwt98lxf")
            .setState("challenge_required")
            .setAres(Stripe3ds2AuthResult.Ares.Builder()
                .setAcsChallengeMandated("Y")
                .setAcsTransId("dd23c757-211a-4c1b-add5-06a1450a642e")
                .setAcsSignedContent("eyJhbGciOiJFUzI1NiJ9.asdfasf.asdfasdfa")
                .setAuthenticationType("02")
                .setMessageType("ARes")
                .setMessageVersion("2.1.0")
                .setSdkTransId("20158862-9d9d-4d71-83d4-9e65554ed92c")
                .setThreeDSServerTransId("e8ea0b42-0e74-42b2-92b4-1b27005f0596")
                .setTransStatus("C")
                .build())
            .build()

        assertEquals(expectedResult, result)
    }

    @Test
    fun fromJSON_dataWithMessageExtensions_createsObject() {
        val jsonResult = Stripe3ds2AuthResult.fromJson(AUTH_RESULT_WITH_EXTENSIONS_JSON)

        val extensions = listOf(
            Stripe3ds2AuthResult.MessageExtension.Builder()
                .setName("extension1")
                .setId("ID1")
                .setCriticalityIndicator(true)
                .setData(mapOf("key1" to "value1"))
                .build(),
            Stripe3ds2AuthResult.MessageExtension.Builder()
                .setName("extension2")
                .setId("ID2")
                .setCriticalityIndicator(true)
                .setData(mapOf(
                    "key1" to "value1",
                    "key2" to "value2"
                ))
                .build(),
            Stripe3ds2AuthResult.MessageExtension.Builder()
                .setName("sharedData")
                .setId("ID3")
                .setCriticalityIndicator(false)
                .setData(mapOf("key" to "IkpTT05EYXRhIjogew0KImRhdGExIjogInNkYXRhIg0KfQ=="))
                .build()
        )

        val expectedResult = Stripe3ds2AuthResult.Builder()
            .setId("threeds2_1Ecwz3CRMbs6FrXfThtfogua")
            .setObjectType("three_d_secure_2")
            .setLiveMode(false)
            .setCreated(1558541285L)
            .setSource("src_1Ecwz1CRMbs6FrXfUwt98lxf")
            .setState("challenge_required")
            .setAres(Stripe3ds2AuthResult.Ares.Builder()
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
            .build()

        assertEquals(expectedResult, jsonResult)
    }

    @Test
    fun fromJSON_errorData_createsObjectWithError() {
        val jsonResult = Stripe3ds2AuthResult.fromJson(AUTH_RESULT_ERROR_JSON)

        val expectedResult = Stripe3ds2AuthResult.Builder()
            .setId("threeds2_1Ecwz3CRMbs6FrXfThtfogua")
            .setObjectType("three_d_secure_2")
            .setLiveMode(false)
            .setCreated(1558541285L)
            .setSource("src_1Ecwz1CRMbs6FrXfUwt98lxf")
            .setState("challenge_required")
            .setAres(Stripe3ds2AuthResult.Ares.Builder()
                .setAcsChallengeMandated("Y")
                .setAcsTransId("dd23c757-211a-4c1b-add5-06a1450a642e")
                .setAcsSignedContent("eyJhbGciOiJFUzI1NiJ9.asdfasf.asdfasdfa")
                .setAuthenticationType("02")
                .setMessageType("ARes")
                .setMessageVersion("2.1.0")
                .setSdkTransId("20158862-9d9d-4d71-83d4-9e65554ed92c")
                .setThreeDSServerTransId("e8ea0b42-0e74-42b2-92b4-1b27005f0596")
                .build())
            .setError(Stripe3ds2AuthResult.ThreeDS2Error.Builder()
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
            .build()

        assertEquals(expectedResult, jsonResult)
    }

    @Test
    fun fromJson_invalidElementFormatJson_shouldPopulateErrorField() {
        val result = Stripe3ds2AuthResult.fromJson(AUTH_RESULT_ERROR_INVALID_ELEMENT_FORMAT_JSON)
        assertNull(result.ares)
        assertNull(result.fallbackRedirectUrl)
        val error = result.error!!
        assertEquals("sdkMaxTimeout", error.errorDetail)
        assertEquals(
            "Format or value of one or more Data Elements is Invalid according to the Specification",
            error.errorDescription)
    }

    @Test
    fun fromJson_fallbackRedirectUrl_shouldReturnValidRedirectData() {
        val result = Stripe3ds2AuthResult.fromJson(AUTH_RESULT_FALLBACK_REDIRECT_URL_JSON)
        assertNull(result.ares)
        assertNull(result.error)

        assertEquals(
            "https://hooks.stripe.com/3d_secure_2_eap/begin_test/src_1Ecve7CRMbs6FrXfm8AxXMIh/src_client_secret_F79yszOBAiuaZTuIhbn3LPUW",
            result.fallbackRedirectUrl
        )
    }

    companion object {
        private val AUTH_RESULT_JSON = JSONObject(
            """
            {
                "id": "threeds2_1Ecwz3CRMbs6FrXfThtfogua",
                "object": "three_d_secure_2",
                "ares": {
                    "acsChallengeMandated": "Y",
                    "acsSignedContent": "eyJhbGciOiJFUzI1NiJ9.asdfasf.asdfasdfa",
                    "acsTransID": "dd23c757-211a-4c1b-add5-06a1450a642e",
                    "acsURL": null,
                    "authenticationType": "02",
                    "cardholderInfo": null,
                    "messageExtension": null,
                    "messageType": "ARes",
                    "messageVersion": "2.1.0",
                    "sdkTransID": "20158862-9d9d-4d71-83d4-9e65554ed92c",
                    "threeDSServerTransID": "e8ea0b42-0e74-42b2-92b4-1b27005f0596",
                    "transStatus": "C"
                },
                "created": 1558541285,
                "error": null,
                "livemode": false,
                "source": "src_1Ecwz1CRMbs6FrXfUwt98lxf",
                "state": "challenge_required"
            }
            """.trimIndent()
        )

        private val AUTH_RESULT_WITH_EXTENSIONS_JSON = JSONObject(
            """
            {
                "id": "threeds2_1Ecwz3CRMbs6FrXfThtfogua",
                "object": "three_d_secure_2",
                "ares": {
                    "acsChallengeMandated": "Y",
                    "acsSignedContent": "eyJhbGciOiJFUzI1NiJ9.asdfasf.asdfasdfa",
                    "acsTransID": "dd23c757-211a-4c1b-add5-06a1450a642e",
                    "acsURL": null,
                    "authenticationType": "02",
                    "cardholderInfo": null,
                    "messageExtension": [{
                            "name": "extension1",
                            "id": "ID1",
                            "criticalityIndicator": true,
                            "data": {
                                "key1": "value1"
                            }
                        },
                        {
                            "name": "extension2",
                            "id": "ID2",
                            "criticalityIndicator": true,
                            "data": {
                                "key1": "value1",
                                "key2": "value2"
                            }
                        },
                        {
                            "name": "sharedData",
                            "id": "ID3",
                            "criticalityIndicator": false,
                            "data": {
                                "key": "IkpTT05EYXRhIjogew0KImRhdGExIjogInNkYXRhIg0KfQ=="
                            }
                        }
                    ],
                    "messageType": "ARes",
                    "messageVersion": "2.1.0",
                    "sdkTransID": "20158862-9d9d-4d71-83d4-9e65554ed92c",
                    "threeDSServerTransID": "e8ea0b42-0e74-42b2-92b4-1b27005f0596"
                },
                "created": 1558541285,
                "error": null,
                "livemode": false,
                "source": "src_1Ecwz1CRMbs6FrXfUwt98lxf",
                "state": "challenge_required"
            }
            """.trimIndent()
        )

        private val AUTH_RESULT_ERROR_JSON = JSONObject(
            """
            {
                "id": "threeds2_1Ecwz3CRMbs6FrXfThtfogua",
                "object": "three_d_secure_2",
                "ares": {
                    "acsChallengeMandated": "Y",
                    "acsSignedContent": "eyJhbGciOiJFUzI1NiJ9.asdfasf.asdfasdfa",
                    "acsTransID": "dd23c757-211a-4c1b-add5-06a1450a642e",
                    "acsURL": null,
                    "authenticationType": "02",
                    "cardholderInfo": null,
                    "messageExtension": null,
                    "messageType": "ARes",
                    "messageVersion": "2.1.0",
                    "sdkTransID": "20158862-9d9d-4d71-83d4-9e65554ed92c",
                    "threeDSServerTransID": "e8ea0b42-0e74-42b2-92b4-1b27005f0596"
                },
                "created": 1558541285,
                "error": {
                    "threeDSServerTransID": "e8ea0b42-0e74-42b2-92b4-1b27005f0596",
                    "acsTransID": "dd23c757-211a-4c1b-add5-06a1450a642e",
                    "dsTransID": "ff23c757-211a-4c1b-add5-06a1450a642e",
                    "errorCode": "error code 1234",
                    "errorDescription": "error description",
                    "errorDetail": "error detail",
                    "errorComponent": "error component",
                    "errorMessageType": "error message type",
                    "messageType": "Error",
                    "messageVersion": "2.1.0",
                    "sdkTransID": "20158862-9d9d-4d71-83d4-9e65554ed92c"
                },
                "livemode": false,
                "source": "src_1Ecwz1CRMbs6FrXfUwt98lxf",
                "state": "challenge_required"
            }
            """.trimIndent()
        )

        private val AUTH_RESULT_ERROR_INVALID_ELEMENT_FORMAT_JSON = JSONObject(
            """
            {
                "ares": null,
                "livemode": true,
                "created": 1562711486,
                "id": "threeds2_1EuRqMAWhjPjYwPi83sPpdVY",
                "source": "src_1EuRqGAWhjPjYwPid0T5ZrrF",
                "state": "failed",
                "error": {
                    "errorComponent": "D",
                    "acsTransID": null,
                    "errorDescription": "Format or value of one or more Data Elements is Invalid according to the Specification",
                    "messageType": "Erro",
                    "dsTransID": "3fa2e398-4146-42f0-b905-a52c06b5caa2",
                    "errorCode": "203",
                    "errorDetail": "sdkMaxTimeout",
                    "errorMessageType": "AReq",
                    "messageVersion": "2.1.0",
                    "sdkTransID": "a9e7db5d-e95c-4cc6-a8b7-df1cee092879",
                    "threeDSServerTransID": "161d5143-340c-4e40-8ee1-a272be64aecc"
                },
                "object": "three_d_secure_2"
            }
            """.trimIndent()
        )

        private val AUTH_RESULT_FALLBACK_REDIRECT_URL_JSON = JSONObject(
            """
            {
                "ares": null,
                "livemode": true,
                "created": 1562711486,
                "id": "threeds2_1EuRqMAWhjPjYwPi83sPpdVY",
                "source": "src_1EuRqGAWhjPjYwPid0T5ZrrF",
                "state": "failed",
                "fallback_redirect_url": "https://hooks.stripe.com/3d_secure_2_eap/begin_test/src_1Ecve7CRMbs6FrXfm8AxXMIh/src_client_secret_F79yszOBAiuaZTuIhbn3LPUW",
                "object": "three_d_secure_2"
            }
            """.trimIndent()
        )
    }
}
