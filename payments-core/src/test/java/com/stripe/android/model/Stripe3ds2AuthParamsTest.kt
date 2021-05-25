package com.stripe.android.model

import com.google.common.truth.Truth.assertThat
import org.json.JSONObject
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class Stripe3ds2AuthParamsTest {

    @Test
    fun toParamMap_shouldReturnCorrectObject() {
        val params = createParams().toParamMap()

        assertThat(params[Stripe3ds2AuthParams.FIELD_SOURCE])
            .isEqualTo(SOURCE_ID)

        assertThat(params[Stripe3ds2AuthParams.FIELD_APP])
            .isEqualTo(
                JSONObject(
                    """
                    {
                        "sdkAppID": "1.0.0",
                        "sdkTransID": "26a3ef80-f09c-4954-94f4-66c7fe9409ba",
                        "sdkEncData": "$DEVICE_DATA",
                        "sdkEphemPubKey": $SDK_EPHEMERAL_PUBLIC_KEY,
                        "sdkMaxTimeout": "05",
                        "sdkReferenceNumber": "3DS_LOA_SDK_STIN_12345",
                        "messageVersion": "2.1.0",
                        "deviceRenderOptions": {
                            "sdkInterface": "03",
                            "sdkUiType": ["01", "02", "03", "04", "05"]
                        }
                    }
                    """.trimIndent()
                ).toString()
            )

        assertThat(params[Stripe3ds2AuthParams.FIELD_FALLBACK_RETURN_URL])
            .isEqualTo(RETURN_URL)
    }

    @Test
    fun appParams_correctlyFormatsSdkTimeout() {
        listOf(
            5 to "05",
            10 to "10",
            19 to "19",
            99 to "99"
        ).forEach { (first, second) ->
            assertThat(
                createParams(timeout = first)
                    .appParams
                    .getString("sdkMaxTimeout")
            ).isEqualTo(second)
        }
    }

    private fun createParams(timeout: Int = 5): Stripe3ds2AuthParams {
        val appId = "1.0.0"
        val sdkReferenceNumber = "3DS_LOA_SDK_STIN_12345"
        val sdkTransactionId = "26a3ef80-f09c-4954-94f4-66c7fe9409ba"
        val messageVersion = "2.1.0"

        return Stripe3ds2AuthParams(
            SOURCE_ID,
            appId,
            sdkReferenceNumber,
            sdkTransactionId,
            DEVICE_DATA,
            SDK_EPHEMERAL_PUBLIC_KEY,
            messageVersion,
            timeout,
            RETURN_URL
        )
    }

    private companion object {
        private const val SOURCE_ID = "src_12345"
        private const val RETURN_URL = "stripe://payment-auth-return"
        private const val DEVICE_DATA = "eyJlbmMiOiJBMTI4Q0JDLUhTMjU2IiwiYWxnIjoiUlNBLU9BRVAtMjU2In0.nid2Q-Ii21cSPHBaszR5KSXz866yX9I7AthLKpfWZoc7RIfz11UJ1EHuvIRDIyqqJ8txNUKKoL4keqMTqK5Yc5TqsxMn0nML8pZaPn40nXsJm_HFv3zMeOtRR7UTewsDWIgf5J-A6bhowIOmvKPCJRxspn_Cmja-YpgFWTp08uoJvqgntgg1lHmI1kh1UV6DuseYFUfuQlICTqC3TspAzah2CALWZORF_QtSeHc_RuqK02wOQMs-7079jRuSdBXvI6dQnL5ESH25wHHosfjHMZ9vtdUFNJo9J35UI1sdWFDzzj8k7bt0BupZhyeU0PSM9EHP-yv01-MQ9eslPTVNbFJ9YOHtq8WamvlKDr1sKxz6Ac_gUM8NgEcPP9SafPVxDd4H1Fwb5-4NYu2AD4xoAgMWE-YtzvfIFXZcU46NDoi6Xum3cHJqTH0UaOhBoqJJft9XZXYW80fjts-v28TkA76-QPF7CTDM6KbupvBkSoRq218eJLEywySXgCwf-Q95fsBtnnyhKcvfRaByq5kT7PH3DYD1rCQLexJ76A79kurre9pDjTKAv85G9DNkOFuVUYnNB3QGFReCcF9wzkGnZXdfkgN2BkB6n94bbkEyjbRb5r37XH6oRagx2fWLVj7kC5baeIwUPVb5kV_x4Kle7C-FPY1Obz4U7s6SVRnLGXY.IP9OcQx5uZxBRluOpn1m6Q.w-Ko5Qg6r-KCmKnprXEbKA7wV-SdLNDAKqjtuku6hda_0crOPRCPU4nn26Yxj7EG.p01pl8CKukuXzjLeY3a_Ew"

        private val SDK_EPHEMERAL_PUBLIC_KEY = JSONObject(
            """
                {
                	"kty": "EC",
                	"use": "sig",
                	"crv": "P-256",
                	"kid": "b23da28b-d611-46a8-93af-44ad57ce9c9d",
                	"x": "hSwyaaAp3ppSGkpt7d9G8wnp3aIXelsZVo05EPpqetg",
                	"y": "OUVOv9xPh5RYWapla0oz3vCJWRRXlDmppy5BGNeSl-A"
                }
            """.trimIndent()
        ).toString()
    }
}
