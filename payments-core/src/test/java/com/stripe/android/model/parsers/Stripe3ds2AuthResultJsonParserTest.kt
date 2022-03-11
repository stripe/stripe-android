package com.stripe.android.model.parsers

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.Stripe3ds2AuthResult
import com.stripe.android.model.Stripe3ds2AuthResultFixtures
import org.json.JSONObject
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@Suppress("MaxLineLength")
@RunWith(RobolectricTestRunner::class)
class Stripe3ds2AuthResultJsonParserTest {

    @Test
    fun parse_validData_createsObject() {
        val result = parse(AUTH_RESULT_JSON)
        assertThat(result.ares?.isChallenge)
            .isTrue()

        assertThat(result)
            .isEqualTo(
                Stripe3ds2AuthResult(
                    id = "threeds2_1Ecwz3CRMbs6FrXfThtfogua",
                    liveMode = false,
                    created = 1558541285L,
                    source = "src_1Ecwz1CRMbs6FrXfUwt98lxf",
                    state = "challenge_required",
                    ares = Stripe3ds2AuthResult.Ares(
                        acsChallengeMandated = "Y",
                        acsTransId = "dd23c757-211a-4c1b-add5-06a1450a642e",
                        acsSignedContent = "eyJhbGciOiJFUzI1NiJ9.asdfasf.asdfasdfa",
                        authenticationType = "02",
                        messageType = "ARes",
                        messageVersion = "2.1.0",
                        sdkTransId = "20158862-9d9d-4d71-83d4-9e65554ed92c",
                        threeDSServerTransId = "e8ea0b42-0e74-42b2-92b4-1b27005f0596",
                        transStatus = "C"
                    )
                )
            )
    }

    @Test
    fun parse_dataWithMessageExtensions_createsObject() {
        assertThat(parse(AUTH_RESULT_WITH_EXTENSIONS_JSON))
            .isEqualTo(
                Stripe3ds2AuthResult(
                    id = "threeds2_1Ecwz3CRMbs6FrXfThtfogua",
                    liveMode = false,
                    created = 1558541285L,
                    source = "src_1Ecwz1CRMbs6FrXfUwt98lxf",
                    state = "challenge_required",
                    ares = Stripe3ds2AuthResult.Ares(
                        acsChallengeMandated = "Y",
                        acsTransId = "dd23c757-211a-4c1b-add5-06a1450a642e",
                        acsSignedContent = "eyJhbGciOiJFUzI1NiJ9.asdfasf.asdfasdfa",
                        authenticationType = "02",
                        messageType = "ARes",
                        messageVersion = "2.1.0",
                        messageExtension = listOf(
                            Stripe3ds2AuthResult.MessageExtension(
                                name = "extension1",
                                id = "ID1",
                                criticalityIndicator = true,
                                data = mapOf("key1" to "value1")
                            ),
                            Stripe3ds2AuthResult.MessageExtension(
                                name = "extension2",
                                id = "ID2",
                                criticalityIndicator = true,
                                data = mapOf(
                                    "key1" to "value1",
                                    "key2" to "value2"
                                )
                            ),
                            Stripe3ds2AuthResult.MessageExtension(
                                name = "sharedData",
                                id = "ID3",
                                criticalityIndicator = false,
                                data = mapOf("key" to "IkpTT05EYXRhIjogew0KImRhdGExIjogInNkYXRhIg0KfQ==")
                            )
                        ),
                        sdkTransId = "20158862-9d9d-4d71-83d4-9e65554ed92c",
                        threeDSServerTransId = "e8ea0b42-0e74-42b2-92b4-1b27005f0596"
                    )
                )
            )
    }

    @Test
    fun parse_errorData_createsObjectWithError() {
        assertThat(parse(AUTH_RESULT_ERROR_JSON))
            .isEqualTo(
                Stripe3ds2AuthResult(
                    id = "threeds2_1Ecwz3CRMbs6FrXfThtfogua",
                    liveMode = false,
                    created = 1558541285L,
                    source = "src_1Ecwz1CRMbs6FrXfUwt98lxf",
                    state = "challenge_required",
                    ares = Stripe3ds2AuthResult.Ares(
                        acsChallengeMandated = "Y",
                        acsTransId = "dd23c757-211a-4c1b-add5-06a1450a642e",
                        acsSignedContent = "eyJhbGciOiJFUzI1NiJ9.asdfasf.asdfasdfa",
                        authenticationType = "02",
                        messageType = "ARes",
                        messageVersion = "2.1.0",
                        sdkTransId = "20158862-9d9d-4d71-83d4-9e65554ed92c",
                        threeDSServerTransId = "e8ea0b42-0e74-42b2-92b4-1b27005f0596"
                    ),
                    error = Stripe3ds2AuthResult.ThreeDS2Error(
                        threeDSServerTransId = "e8ea0b42-0e74-42b2-92b4-1b27005f0596",
                        acsTransId = "dd23c757-211a-4c1b-add5-06a1450a642e",
                        dsTransId = "ff23c757-211a-4c1b-add5-06a1450a642e",
                        errorCode = "error code 1234",
                        errorComponent = "error component",
                        errorDetail = "error detail",
                        errorDescription = "error description",
                        errorMessageType = "error message type",
                        messageType = "Error",
                        messageVersion = "2.1.0",
                        sdkTransId = "20158862-9d9d-4d71-83d4-9e65554ed92c"
                    )
                )
            )
    }

    @Test
    fun parse_invalidElementFormatJson_shouldPopulateErrorField() {
        val result = parse(AUTH_RESULT_ERROR_INVALID_ELEMENT_FORMAT_JSON)
        assertThat(result.ares)
            .isNull()
        assertThat(result.fallbackRedirectUrl)
            .isNull()

        val error = result.error
        assertThat(error?.errorDetail)
            .isEqualTo("sdkMaxTimeout")
        assertThat(error?.errorDescription)
            .isEqualTo(
                "Format or value of one or more Data Elements is Invalid according to the Specification"
            )
    }

    @Test
    fun parse_fallbackRedirectUrl_shouldReturnValidRedirectData() {
        val result = parse(AUTH_RESULT_FALLBACK_REDIRECT_URL_JSON)
        assertThat(result.ares)
            .isNull()
        assertThat(result.error)
            .isNull()

        assertThat(result.fallbackRedirectUrl)
            .isEqualTo(
                "https://hooks.stripe.com/3d_secure_2_eap/begin_test/" +
                    "src_1Ecve7CRMbs6FrXfm8AxXMIh/src_client_secret_F79yszOBAiuaZTuIhbn3LPUW"
            )
    }

    @Test
    fun `parse with creq field should return expected value`() {
        assertThat(
            parse(Stripe3ds2AuthResultFixtures.CHALLENGE_COMPLETION_JSON).creq
        ).isEqualTo(
            "eyJ0aHJlZURTU2VydmVyVHJhbnNJRCI6IjA4NDAwMTRiLWY1YTgtNDA5My04MzdkLTgxOTA3Ym" +
                "JmMmU2MiIsImFjc1RyYW5zSUQiOiIwZjdkMTU0MC1mYTM2LTQ2OWYtYmQ5ZC03ZjIxYzkzMmU2MjEiLCJ" +
                "jaGFsbGVuZ2VXaW5kb3dTaXplIjoiMDUiLCJtZXNzYWdlVHlwZSI6IkNSZXEiLCJtZXNzYWdlVmVyc2lvbiI6IjIuMS4wIn0="
        )
    }

    private companion object {
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

        private fun parse(jsonObject: JSONObject): Stripe3ds2AuthResult {
            return Stripe3ds2AuthResultJsonParser().parse(jsonObject)
        }
    }
}
