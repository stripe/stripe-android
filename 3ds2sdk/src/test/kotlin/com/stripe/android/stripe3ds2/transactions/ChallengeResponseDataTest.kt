package com.stripe.android.stripe3ds2.transactions

import android.util.DisplayMetrics
import com.stripe.android.stripe3ds2.ChallengeMessageFixtures
import com.stripe.android.stripe3ds2.transaction.TransactionStatus
import com.stripe.android.stripe3ds2.utils.ParcelUtils
import org.json.JSONObject
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class ChallengeResponseDataTest {

    @Test
    fun checkMessageType_withMissingMessageVersion_shouldThrowException() {
        assertFailsWith<ChallengeResponseParseException> {
            ChallengeResponseData.checkMessageType(JSONObject())
        }
    }

    @Test
    fun getYesNoValue_withInvalidChallengeCompletionInd_shouldThrowExceptionWith203Code() {
        val json = JSONObject().put("challengeCompletionInd", "55")
        val exception: ChallengeResponseParseException = assertFailsWith {
            ChallengeResponseData.getYesNoValue(json, "challengeCompletionInd", true)
        }
        assertEquals(203, exception.code.toLong())
    }

    @Test
    fun getYesNoValue_withMissingChallengeCompletionInd_shouldThrowExceptionWith201Code() {
        val json = JSONObject().put("challengeCompletionInd", "")
        val exception: ChallengeResponseParseException = assertFailsWith {
            ChallengeResponseData.getYesNoValue(json, "challengeCompletionInd", true)
        }
        assertEquals(201, exception.code.toLong())
    }

    @Test
    fun getResendInformationLabel_withInvalidResendInformationLabel_shouldThrowException() {
        assertFailsWith<ChallengeResponseParseException> {
            ChallengeResponseData.getResendInformationLabel(
                CRES_JSON_WITH_INVALID_RESEND_INFORMATION_LABEL
            )
        }
    }

    @Test
    fun getYesNoValue_withInvalidChallengeInfoTextIndicator_shouldThrowException() {
        assertFailsWith<ChallengeResponseParseException> {
            ChallengeResponseData.getYesNoValue(
                CRES_JSON_WITH_INVALID_CHALLENGE_INFO_TEXT_INDICATOR,
                "challengeInfoTextIndicator",
                false
            )
        }
    }

    @Test
    fun getChallengeSelectInfoArray_withInvalidChallengeSelectInfo_shouldThrowException() {
        assertFailsWith<ChallengeResponseParseException> {
            ChallengeResponseData.getChallengeSelectInfoArray(
                CRES_JSON_WITH_INVALID_CHALLENGE_SELECT_INFO
            )
        }
    }

    @Test
    fun getTransactionId_withInvalidAcsTransId_shouldThrowException() {
        assertFailsWith<ChallengeResponseParseException> {
            ChallengeResponseData.getTransactionId(
                CRES_JSON_WITH_INVALID_ACS_TRANS_ID,
                "acsTransID"
            )
        }
    }

    @Test
    fun fromJson_withInvalidUiType_shouldThrowException() {
        assertFailsWith<ChallengeResponseParseException> {
            ChallengeResponseData.fromJson(CRES_JSON_WITH_INVALID_UI_TYPE)
        }
    }

    @Test
    fun getMessageVersion_withInvalidMessageVersion_shouldThrowException() {
        assertFailsWith<ChallengeResponseParseException> {
            ChallengeResponseData.getMessageVersion(JSONObject())
        }
    }

    @Test
    fun getSubmitAuthenticationLabel_withMissingLabel_shouldThrowException() {
        assertFailsWith<ChallengeResponseParseException> {
            ChallengeResponseData.getSubmitAuthenticationLabel(
                JSONObject(),
                UiType.Text
            )
        }
    }

    @Test
    fun getDecodedHtml_withMissingHtml_shouldThrowException() {
        assertFailsWith<ChallengeResponseParseException> {
            ChallengeResponseData.getDecodedAcsHtml(
                JSONObject(),
                UiType.Html
            )
        }
    }

    @Test
    fun getOobContinueLabel_withMissingOobContinueLabel_shouldThrowException() {
        assertFailsWith<ChallengeResponseParseException> {
            ChallengeResponseData.getOobContinueLabel(
                JSONObject(),
                UiType.OutOfBand
            )
        }
    }

    @Test
    fun fromJson_withUnrecognizedCriticalMessageExtensions_shouldThrowException() {
        val messageExtensions = listOf(
            MessageExtension(
                name = "unrecognized_name",
                id = "",
                criticalityIndicator = true
            )
        )
        val cresData = ChallengeMessageFixtures.CRES.copy(
            messageExtensions = messageExtensions
        )
        val cresJson = cresData.toJson()
        val exception: ChallengeResponseParseException = assertFailsWith {
            ChallengeResponseData.getMessageExtensions(cresJson)
        }
        assertEquals(202, exception.code.toLong())
    }

    @Test
    fun getTransStatus_withMissingField_shouldThrowException() {
        val exception: ChallengeResponseParseException = assertFailsWith {
            ChallengeResponseData.getTransStatus(JSONObject())
        }
        assertEquals(201, exception.code.toLong())
    }

    @Test
    fun getTransStatus_withInvalidValue_shouldThrowException() {
        val json = JSONObject().put("transStatus", "ABC")
        val exception: ChallengeResponseParseException = assertFailsWith {
            ChallengeResponseData.getTransStatus(json)
        }
        assertEquals(203, exception.code.toLong())
    }

    @Test
    fun fromJson_withValidFinalCres_shouldReturnObject() {
        val cresData = ChallengeResponseData.fromJson(FINAL_CRES_JSON)
        assertTrue(cresData.isChallengeCompleted)
        assertEquals(TransactionStatus.ChallengeAdditionalAuth.code, cresData.transStatus)
    }

    @Test
    fun fromJson_withValidTextInputJson_shouldReturnObject() {
        val cresData = ChallengeResponseData.fromJson(TEXT_INPUT_CRES_JSON)

        assertNotNull(UUID.fromString(cresData.acsTransId))
        assertNull(cresData.acsHtml)

        assertEquals(UiType.Text, cresData.uiType)
        assertFalse(cresData.isChallengeCompleted)

        assertEquals("Header", cresData.challengeInfoHeader)
        assertEquals("Label", cresData.challengeInfoLabel)
        assertEquals("Text", cresData.challengeInfoText)
        assertEquals("Tap continue to proceed.", cresData.challengeAdditionalInfoText)
        assertFalse(cresData.shouldShowChallengeInfoTextIndicator)

        assertEquals("Expand Label", cresData.expandInfoLabel)
        assertEquals("Expand Text", cresData.expandInfoText)

        assertNotNull(cresData.issuerImage)
        assertEquals(
            "http://acs.com/medium_image.png",
            cresData.issuerImage?.mediumUrl
        )
        assertEquals(
            "http://acs.com/high_image.png",
            cresData.issuerImage?.highUrl
        )
        assertEquals(
            "http://acs.com/extraHigh_image.png",
            cresData.issuerImage?.extraHighUrl
        )

        assertNotNull(cresData.paymentSystemImage)
        assertEquals(
            "http://ds.com/medium_image.png",
            cresData.paymentSystemImage?.mediumUrl
        )
        assertEquals(
            "http://ds.com/high_image.png",
            cresData.paymentSystemImage?.highUrl
        )
        assertEquals(
            "http://ds.com/extraHigh_image.png",
            cresData.paymentSystemImage?.extraHighUrl
        )

        assertEquals("Click here to open Your Bank App", cresData.oobAppLabel)
        assertEquals("bank://deeplink", cresData.oobAppUrl)
        assertEquals("Continue", cresData.oobContinueLabel)

        assertNull(cresData.messageExtensions)
        assertEquals("Resend Information?", cresData.resendInformationLabel)
        assertEquals("Submit", cresData.submitAuthenticationLabel)
        assertEquals(
            "Would you like to add this Merchant to your whitelist?",
            cresData.whitelistingInfoText
        )
        assertEquals("Why Info Label", cresData.whyInfoLabel)
        assertEquals("Why Info Text", cresData.whyInfoText)

        val challengeSelectOptions =
            cresData.challengeSelectOptions
        assertNotNull(challengeSelectOptions)
        assertEquals(2, challengeSelectOptions.size)
        val firstOption = challengeSelectOptions[0]
        assertEquals("phone", firstOption.name)
        assertEquals("Mobile **** **** 321", firstOption.text)
        val secondOption = challengeSelectOptions[1]
        assertEquals("mail", secondOption.name)
        assertEquals("Email a*******g**@g***.com", secondOption.text)

        assertNotNull(cresData.sdkTransId)

        assertEquals("", cresData.transStatus)
    }

    @Test
    fun imageGetUrlForDensity_shouldReturnCorrectDensityImageUrl() {
        val cresData = ChallengeResponseData.fromJson(TEXT_INPUT_CRES_JSON)

        assertNotNull(cresData.paymentSystemImage)
        assertEquals(
            "http://ds.com/medium_image.png",
            cresData.paymentSystemImage?.getUrlForDensity(DisplayMetrics.DENSITY_LOW)
        )
        assertEquals(
            "http://ds.com/medium_image.png",
            cresData.paymentSystemImage?.getUrlForDensity(DisplayMetrics.DENSITY_MEDIUM)
        )

        assertEquals(
            "http://ds.com/high_image.png",
            cresData.paymentSystemImage?.getUrlForDensity(DisplayMetrics.DENSITY_MEDIUM + 1)
        )
        assertEquals(
            "http://ds.com/high_image.png",
            cresData.paymentSystemImage?.getUrlForDensity(DisplayMetrics.DENSITY_HIGH)
        )
        assertEquals(
            "http://ds.com/high_image.png",
            cresData.paymentSystemImage?.getUrlForDensity(DisplayMetrics.DENSITY_XHIGH - 1)
        )

        assertEquals(
            "http://ds.com/extraHigh_image.png",
            cresData.paymentSystemImage?.getUrlForDensity(DisplayMetrics.DENSITY_XHIGH)
        )
        assertEquals(
            "http://ds.com/extraHigh_image.png",
            cresData.paymentSystemImage?.getUrlForDensity(DisplayMetrics.DENSITY_XXHIGH)
        )
    }

    @Test
    fun getHighestFidelityImageUrl_highestFidelityImageURLReturned() {
        val extraHighimage = ChallengeResponseData.Image(
            "medium",
            "high",
            "extraHigh"
        )
        assertEquals("extraHigh", extraHighimage.highestFidelityImageUrl)

        val highImage = ChallengeResponseData.Image(
            "medium",
            "high",
            null
        )
        assertEquals("high", highImage.highestFidelityImageUrl)

        val mediumImage = ChallengeResponseData.Image(
            "medium",
            "",
            null
        )
        assertEquals("medium", mediumImage.highestFidelityImageUrl)

        val noImages = ChallengeResponseData.Image(
            "",
            "",
            null
        )
        assertNull(noImages.highestFidelityImageUrl)
    }

    @Test
    fun testHtmlDecoding() {
        val cresData = ChallengeResponseData.fromJson(HTML_CRES_JSON)

        assertNotNull(cresData.acsHtml)
        assertTrue(
            true == cresData.acsHtml?.contains(
                "<title>3DS - One-Time Passcode - PA</title>"
            )
        )

        assertNotNull(cresData.acsHtmlRefresh)
        assertTrue(
            true == cresData.acsHtmlRefresh?.contains(
                "<p>Tap continue once you have verified this payment.</p>"
            )
        )
    }

    @Test
    fun isValidForUi_withNoUi_IsTrue() {
        val cresData = ChallengeMessageFixtures.CRES.copy(
            isChallengeCompleted = true
        )
        assertTrue(cresData.isValidForUi)
    }

    @Test
    fun isValidForUi_withHtmlUiAndNoHtmlField_IsFalse() {
        val cresData = ChallengeMessageFixtures.CRES.copy(
            isChallengeCompleted = false,
            uiType = UiType.Html
        )
        assertFalse(cresData.isValidForUi)
    }

    @Test
    fun isValidForUi_withOobUiAndMissingFields_IsFalse() {
        val cresData = ChallengeMessageFixtures.CRES.copy(
            isChallengeCompleted = false,
            uiType = UiType.OutOfBand,
            oobContinueLabel = "Continue"
        )
        assertFalse(cresData.isValidForUi)
    }

    @Test
    fun isValidForUi_withSingleSelectUiAndMissingFields_IsFalse() {
        val cresData = ChallengeMessageFixtures.CRES.copy(
            isChallengeCompleted = false,
            uiType = UiType.SingleSelect,
            challengeSelectOptions = listOf(
                ChallengeResponseData.ChallengeSelectOption("new_york", "New York"),
                ChallengeResponseData.ChallengeSelectOption("paris", "Paris")
            )
        )
        assertFalse(cresData.isValidForUi)
    }

    @Test
    fun testParcelable() {
        val cresData = ChallengeResponseData.fromJson(TEXT_INPUT_CRES_JSON)
        assertEquals(cresData, ParcelUtils.get(cresData))
    }

    @Suppress("ktlint:standard:max-line-length")
    companion object {

        private val TEXT_INPUT_CRES_JSON = JSONObject(
            """
            {
                "messageType": "CRes",
                "acsTransID": "${UUID.randomUUID()}",
                "threeDSServerTransID": "${UUID.randomUUID()}",
                "acsUiType": "01",
                "challengeCompletionInd": "N",
                "challengeInfoHeader": "Header",
                "challengeInfoLabel": "Label",
                "challengeInfoText": "Text",
                "challengeAddInfo": "Tap continue to proceed.",
                "shouldShowChallengeInfoTextIndicator": "N",
                "challengeSelectInfo": [{
                    "phone": "Mobile **** **** 321"
                }, {
                    "mail": "Email a*******g**@g***.com"
                }],
                "expandInfoLabel": "Expand Label",
                "expandInfoText": "Expand Text",
                "issuerImage": {
                    "medium": "http:\/\/acs.com\/medium_image.png",
                    "high": "http:\/\/acs.com\/high_image.png",
                    "extraHigh": "http:\/\/acs.com\/extraHigh_image.png"
                },
                "messageVersion": "2.1.0",
                "oobAppURL": "bank:\/\/deeplink",
                "oobAppLabel": "Click here to open Your Bank App",
                "oobContinueLabel": "Continue",
                "psImage": {
                    "medium": "http:\/\/ds.com\/medium_image.png",
                    "high": "http:\/\/ds.com\/high_image.png",
                    "extraHigh": "http:\/\/ds.com\/extraHigh_image.png"
                },
                "resendInformationLabel": "Resend Information?",
                "sdkTransID": "${UUID.randomUUID()}",
                "submitAuthenticationLabel": "Submit",
                "whitelistingInfoText": "Would you like to add this Merchant to your whitelist?",
                "whyInfoLabel": "Why Info Label",
                "whyInfoText": "Why Info Text"
            }
            """.trimIndent()
        )

        private val FINAL_CRES_JSON = JSONObject(
            """
            {
                "messageType": "CRes",
                "acsTransID": "${UUID.randomUUID()}",
                "threeDSServerTransID": "${UUID.randomUUID()}",
                "challengeCompletionInd": "Y",
                "messageVersion": "2.1.0",
                "sdkTransID": "${UUID.randomUUID()}",
                "transStatus": "C"
            }
            """.trimIndent()
        )

        private const val ENCODED_HTML = "PCFET0NUWVBFIEhUTUw-Cgo8aHRtbD4KPGhlYWQ-CiAgICA8dGl0bGU-M0RTIC0gT25lLVRpbWUgUGFzc2NvZGUgLSBQQTwvdGl0bGU-CiAgICA8bWV0YSBjaGFyc2V0PSJ1dGYtOCIvPgogICAgPG1ldGEgbmFtZT0idmlld3BvcnQiIGNvbnRlbnQ9IndpZHRoPWRldmljZS13aWR0aCwgaW5pdGlhbC1zY2FsZT0xIi8-CgogICAgPHN0eWxlPgoJCQkvKiBHZW5lcmFsIFN0eWxpbmcqLwoJCQlib2R5IHsgICAgCgkJCQlmb250LWZhbWlseTogJ09wZW4gU2FucycsIHNhbnMtc2VyaWY7CgkJCQlmb250LXNpemU6MTFwdDsKCQkJfQoJCQkKCQkJb2wsIHVsIHsKCQkJCWxpc3Qtc3R5bGU6IG5vbmU7CgkJCQlwYWRkaW5nLWxlZnQ6MHB4OwoJCQl9CgkJCQoJCQoJCQlzdW1tYXJ5Ojotd2Via2l0LWRldGFpbHMtbWFya2VyIHsKCQkJICBkaXNwbGF5OiBub25lCgkJCX0KCgkJCXN1bW1hcnkgewoJCQkJYmFja2dyb3VuZC1jb2xvcjogI2VlZWVlZTsKCQkJCWNvbG9yOiMzMzM7CgkJCQlwYWRkaW5nOiA3cHggMTBweCA3cHggMTBweDsKCQkJCW1hcmdpbi1ib3R0b206IDEzcHg7CgkJCX0KCQkJCgkJCWRpdi5yZWxhdGVkLWNvbnRhaW5lciB7IAoJCQkJbWFyZ2luLXRvcDo1MHB4OwoJCQl9CgkJCQoJCQlwLnN1bW1hcnktcCB7CgkJCQlwYWRkaW5nOiA3cHggMTBweCA3cHggMTBweDsKCQkJCWJhY2tncm91bmQ6ICNmNGY0ZjQ7CgkJCQltYXJnaW4tdG9wOi0yMnB4OwoJCQkJbWFyZ2luLWJvdHRvbTowcHg7CgkJCX0KCQkJCgkJCS5idXR0b24gewoJCQkJYmFja2dyb3VuZC1jb2xvcjogIzAwNzk4NzsKCQkJCWJvcmRlci1yYWRpdXM6IDVweDsKCQkJCWJvcmRlcjogMDsKCQkJCWNvbG9yOiAjZmZmZmZmOwoJCQkJY3Vyc29yOiBwb2ludGVyOwoJCQkJZGlzcGxheTogaW5saW5lLWJsb2NrOwoJCQkJZm9udC13ZWlnaHQ6IDQwMDsKCQkJCWhlaWdodDogMi41ZW07CgkJCQlsaW5lLWhlaWdodDogMi41ZW07CgkJCQl0ZXh0LWFsaWduOiBjZW50ZXI7CgkJCQl0ZXh0LWRlY29yYXRpb246IG5vbmU7CgkJCQl3aGl0ZS1zcGFjZTogbm93cmFwOwoJCQkJZGlzcGxheTogYmxvY2s7CgkJCQl3aWR0aDogMTAwJTsKCQkJCW1hcmdpbi1ib3R0b206MTVweDsKCQkJfQoJCQkJLmJ1dHRvbi5wcmltYXJ5IHsKCQkJCWJhY2tncm91bmQtY29sb3I6ICNlZjgyMDA7CgkJCQl9CgoJCQkJLmJ1dHRvbjpob3ZlciB7CgkJCQkJYmFja2dyb3VuZC1jb2xvcjogIzczNzM3MzsKCQkJCX0KCgkJCQkuYnV0dG9uOmFjdGl2ZSB7CgkJCQkJYmFja2dyb3VuZC1jb2xvcjogIzU5NTk1OTsKCQkJCX0KCQkJCQoJCQkJLmNhbnZhcyB7CgkJCQkJcGFkZGluZzogMTVweDsgYm9yZGVyOiAxcHggc29saWQgI2VlZTsKCQkJCQltYXJnaW4tYm90dG9tOjE1cHg7CgkJCQkJfQoJCQkJCQoJCQkJI2NvbnRlbnQgaDIgewoJCQkJCW1hcmdpbi10b3A6MHB4OwoJCQkJfQoJCQkJLmlucHV0LWZpZWxkIHsKCQkJCQliYWNrZ3JvdW5kOiAjZmZmOwoJCQkJCWJvcmRlcjogc29saWQgMXB4ICNlMGUwZTA7CgkJCQkJYm9yZGVyLXJhZGl1czogNXB4OwoJCQkJCWNvbG9yOiBpbmhlcml0OwoJCQkJCWRpc3BsYXk6IGJsb2NrOwoJCQkJCW91dGxpbmU6IDA7CgkJCQkJcGFkZGluZzogN3B4IDBweCA3cHggMHB4OwoJCQkJCXRleHQtZGVjb3JhdGlvbjogbm9uZTsKCQkJCQl3aWR0aDogMTAwJTsKCQkJCQltYXJnaW4tYm90dG9tOjE1cHg7CgkJCQl9CgkJCQkKCQkJCQoJCQkJLypJbWFnZXMqLwoJCQkJCS5sb2dvLWJhbmsgewoJCQkJCWJhY2tncm91bmQtcG9zaXRpb246IGNlbnRlcjsKCQkJCQlkaXNwbGF5OmlubGluZS1ibG9jazsKCQkJCQl3aWR0aDogMTUwcHg7CgkJCQkJaGVpZ2h0OiAzNXB4OwoJCQkJCWJhY2tncm91bmQtc2l6ZTogYXV0byAgMzNweDsKCQkJCQliYWNrZ3JvdW5kLXJlcGVhdDogbm8tcmVwZWF0OwoJCQkJCWJhY2tncm91bmQtaW1hZ2U6IHVybChkYXRhOmltYWdlL3BuZztiYXNlNjQsaVZCT1J3MEtHZ29BQUFBTlNVaEVVZ0FBQUwwQUFBQTBDQVlBQUFEZmFEa2dBQUFBQVhOU1IwSUFyczRjNlFBQUFBUm5RVTFCQUFDeGp3djhZUVVBQUFBSmNFaFpjd0FBRHNNQUFBN0RBY2R2cUdRQUFBWFpTVVJCVkhoZTdaa3hVaXc3REVYLzBvaFpBREVMSUg0NU9URTVNVGs1T1N0Z0I2eGlmaDBLVjkybmtpMjcyejJQR2V0VXFXREdibGt0WDZ2dG52OU9TYklZS2Zwa09WTDB5WEtrNkpQbFNORW55NUdpVDVZalJaOHNSNG8rV1k0VWZiSWMzYUsvdWJrWnNvZUhoOVBUMDlQcDQrUGp4OFBsOHY3Ky90ZTluUU1kai9HVGVSd21lclhIeDhmVDE5ZlhqNmZMSTBWL1haeEY5QmlWLzFKSjBWOFhtMFFmVFFKYm1yZTN0MitoNjNVdkx5OC9QUzZMRlAxMWNZam9GUlgrM2QzZHo3ZVhSWXIrdWpoYzlGUjl2ZllTRDdZcCt1dmljTkhEbm10L0F5bjY2K0xzb3UrcDlKd0hlT056ZjMvLzE3VzN0N2ZmMnlYT0J0SGJJTDJ1OFByNmV2cno1OCszbjlMR0dNL1B6MDEvdmFMSGg4Yk0vMXZmV3VsNHMwWC8rZm41L1RyWm5ybUlsKzlwNzRYN1l6N1VGL2xsL29vZnpSLzlJbWJHNTNHNDZBbXdYRWN5V3JBZ1ZEUXR3eGNpcnFGOW1SaWJRR3Y0WTdGNTlJaCtwdUJCeDVzcGVrU2p2bXRHdndqaTBnTGlHWE0wSXZxWjhkVTRYUFJVMW5JZEZiVUdnbzhTNkZrdEZ1MHpzcEM4S3FLVGhsbG1DeDUwdkpGOHQ2RDZxdC9JNkYvRG50VmFwa0p1aVg1bWZDME9FVDNDWVlWYkliVFFTb3o0V0NCMks4UzROakVzS2cvdFU0eHIxU2VWM1M0SXI0SzBSSCtFNEVISG15RjY4cWsrZVpQR0hKVlkrY3RudnRkK1hxR2lyOWV2Rkl6aXl5dGlOZEhQakM5aWsraEhEU0dWNEQxczFZZ21XUk5FWWozVUgwYkNQT3dFZW91ekpYcGRoTE1FRHpyZVh0SHJGaE5yeGNuM3VvZ3grL1N6QXJYRnFlRDU4a1EvTzc2SXM0aWVHMjFObkNheDlmZ3IyQ1I1YUh2a2s0T1k5cmZVUkgrVTRFSEgyeXQ2dlQrS1JCUW43VnFsN1krS1dpU2lTbXNMbWpjWHMrT0xPSXZvaTQwRzEwTDllbWg3Tkc2cmtvUFhmcVRnUWNmYkszcUVWbnoxSGdCciszQmJjSHJ1Vzg5MW51aG54dGZESVh0NkVrRWZBdE1WaWRYZWtMUW8vaEF2WXJQN09nOXRqK0tsWGZ0YmJMc0tIaHROZWcvcVA0by9Zb3V2V2s2WXYvSWRpNzJINkVsZTJyQzk4ZlZ3aU9nVkhtOHFmQVRiZ2txQ3VLa09kdTlXTXc5dGorS05FbWpiUFp2NUZBUDFQWkp2ankyK2Fqa1ozWXFDTHBRbFJBOElJcnFlYW00cmFLOTVhSHNVYjVSQTI0NnhrSmxBL1R4Nm9HcWhZNDNtMjdMRlZ5MG5XMFN2dnBZUnZUM00yTU1QZ205VmRaNE9KSXZ0RW05aFJnK3lVYnhSQW0wN0F1ZWVpRnVmWXIwaTZFSEhHODIzWll1dldrNnkwZytnMTF2UmF5SXhLajZKcWxWT3hLYjlQYlE5aWpkS29HM1hWM1FzUW0zYmNtYnhVSjliOHEyb3I5NzRWS2hZUWUvM2lEMzkzdmg2K0NlVjN1NS9SMTZCUVNSUzBQWW8zc2hmMU01RWxqWXFQNHR5THpyZWFMNHRHdC9ldHlOMkxudnVkWW0zTnhaYnliVlNncmIxK05Za1loN2FIdm1NUkIyMTIrMVc3VmZpRWRUZmFMNHRlcWJxV1pSMjIyYUZPRktrYkc0OGdjNk9MK0p3MFNOd0RkQjdlNk8rYTcrY0ZyekRycmNOMHZhalJROTJZZThWNmt4ZlZuaXQzeFg0M3A2dmJINzFYcGxiVzhRS25pOVA5TFBqaXpoTTlQUmhCYXJnTVUvVWVoUDBKNmw2SS96UGRWcGgxTHg0b25hRmR1MXZpZG9MR2gvLzF5YXVCeDB2aXI4SHV5aUpqNXlXR1BucjVkaXJvdlMxL1hUT2FyNncybFprWm53Um0wUy8xYWpTSHZaUU1tcmU0VWZienlWNjIyL0xoQlRVenhaRFJCYTdMWXlzdFZlMmUvdGVhL21jR1YrTHM0bmVtd1NGVmV4ZFo2MVVBRzY0Zk9lSlM2ODVsK2pCYnIraXNXdW9qeTFXeXplNTh2cGI2MW13M0p0OWtsdmpLUjRkWkpXWjhkVTRUUFRsWmpta2xFZFVCSTlIUkdQM2JId3VyekVMdWtoWUNCYTkvcHlpNTE1VkNNUytCUjF2aTdXS0RIbEdORGJQQ0pMdmRXc1p3ZjB5eDFxRWlpL21DSFRyMG5QSW54bWZSN2ZvazJRckt2cm9pWDhPVXZUSk1GcDllN1p2K2hRbzFmOWZrcUpQaHRIdFc3UmRzV2UxdlZ1VEdhVG9rMkhzWVIzaDI0clBaOXVQejcrQkZIMHlETlU2ZW10ampVTnA3d3VObzBuUko1dmdQYjMzNDVOblBBbCtpK0FoUlovc2dqMDdvcllMZ01yTzY4V2VnKzY1U2RFbnk1R2lUNVlqUlo4c1I0bytXWTRVZmJJY0tmcGtPVkwweVhLazZKUGxTTkVueTVHaVR4YmpkUG9mUTRHSEpwSUc5cThBQUFBQVNVVk9SSzVDWUlJPSk7CgkJCQkJfQkJCQoJCQkJCgkJCQkKCQkJCQoJCQkJLmxvZ28tdWx7CgkJCQkJZGlzcGxheTppbmxpbmUtYmxvY2s7CgkJCQkJd2lkdGg6IDk2cHg7CgkJCQkJaGVpZ2h0OiA0N3B4OwkKCQkJCQlmbG9hdDpyaWdodDsKCQkJCQliYWNrZ3JvdW5kLXNpemU6IGF1dG8gIDQ3cHg7CgkJCQkJYmFja2dyb3VuZC1yZXBlYXQ6IG5vLXJlcGVhdDsKCQkJCQliYWNrZ3JvdW5kLWltYWdlOiB1cmwoZGF0YTppbWFnZS9wbmc7YmFzZTY0LGlWQk9SdzBLR2dvQUFBQU5TVWhFVWdBQUFTQUFBQUNNQ0FZQUFBRGNLblNoQUFBQUFYTlNSMElBcnM0YzZRQUFBQVJuUVUxQkFBQ3hqd3Y4WVFVQUFBQUpjRWhaY3dBQURzTUFBQTdEQWNkdnFHUUFBQ0J0U1VSQlZIaGU3WjBMa0Y5VmZjZXRZd3MrR3V0VXBrV25VaWtVYXlDQUJDVnFHQlNKWlFKTloyQVNHTlNKanhHVDRaRWdGQk9jRHBLR3B3Um5iS3ltQ0RvVkdHeVZpbWhyc25uc0pvRnMwandXNHU1bTg5Zzh5TWE4TndtYjdHNTIvNmZuYy9iZTdNblplLy8zM1Avci92Ly8vRDR6djBsMi8vZWNlODd2N1BuK3ovdThUUW1DSUdTRUNKQWdDSmtoQWlRSVFtYUlBQW1Da0JraVFJSWdaSVlJa0NBSW1TRUNKQWhDWm9nQUNZS1FHWFV0UUxtVEE2cTNhNS9xYm01UmUxNzRqZG82OTRlcWRkcDMxUHBKMDFYenVDbHF4VVhYcThaeng2c2w3NzFTTGZtejA2M3hBK1BONTgzamJqSFB0MDUvMElRbkh1THIzYlBQeEM4SVF1SFVsUUFOOXZhcFl4czNxOTAvZlVtMTNURkhyYm5taTZycHc5ZXF4VnBRRnAwOVJpMzZrMHZVb3JNdVVRM3Z2RlF0ZnZmbGF2R2Zma3d0R1RWMmhQZ1llKzlZOHpuUDhUemhUSGdkRC9FUjc1cHJ2cVJhNzNqSXZPL1k3emViOXd1QzRFL05DOURKN3FQcXdNS1ZhdE45ajV2V3l0Snp4bW14MEdLakJXUHh1eTdUSW5LRkVaTklrU25VakRoZFllSnZRSmowKzNndjc5OTA3K01tUGFSTEVJVDgxS1FBRGZiMXEwTk5hMVRiakxscTVlZ2JUQ3VGMWdtQ0VOdWlLYmZwOS9KKzB0R2cwME82Mm1ZOGJOSkplZ1ZCR0VsTkNWRHZudjFxeC96bjFPcXJiMU9MUjEweEpEcnZ1VHhhRURJMjBtWFNwOVBack5PN1kvN3pKdjJDSUF4VEV3TFVzMldINnBqOWxGcCt3WVNoRmdZdG5WSjNxOHBsT3Aya2wzU1QvbzdaODB4K0JFR29jZ0U2M3ZtbWF2L21ZNnJ4ZzFmckNueXhibFY4TExxUzE0aVJmdkt4VE9lSGZKRS9RVGlUcVVvQjZqOTRXRzJkOHdQVitLSFBEQWtQQThrUkZicFdqZnlRcjhielBtUHlTWDRGNFV5a3FnUW9OekNnOXJ6NFc3WHkwa21teTJLbXlTTXFjTDBZK1NPZjVKZDg1d1lHQTA4SXdwbEIxUWhRVDBlbjJqQmxwaGt2WVZZcnFzS1d4Qmc3R25XRjZRNlpOVDc2ZmF6emFUaDd6TER4YzVBTzArM1R6NWR6ekdueHUzVWE5UHRhZFA1N09yWUhIaEdFK2lkN0FSb2NWTHVmL2FYcGpqVG8xa0RVcXVTQ2pmVTZXa0FhenRhQ291Tm12YzVpL2J0bDU0NVhLejV5dlZwMTVjMXF6YlZUMWJvYnA2bVd5VE8wQUdqVC8vSXp2K2R6bnVONXdqWG84TVJqRmpJaVRLVVVKWjF2NHNZUCtBTy9DRUs5azZrQTllMDlvRForWmZad2hZNnFtR2xOdDFiTXltVWpabU9OZ0xDVll2TURUNm11bi8xS0hWNjVUdlZzM2FuNkQzYWJsY3U1bUlyTzcvbWM4Um1lSnh6aGlZZjRpSmY0emF3YzZhZVZGSldlbEdZRVU4ZUhYL3IySGd4U0l3ajFTV1lDMUwyNlJiMDI5aVl6R0Z0MFMyS1VicDBnT21kZFltYVkxdDc0RGRYNTVEUHE4S3ZyVmYraDd1Q05wWVY0aVovM3JMM3hkdlBlY0p0SDBma3h3bmF4OFE5K0VvUjZKUk1CWWtNbjNScFRXYU1xb0tmUldxQUZzdlQ5VjZsMUUyOVh1eGI4WEIzZnRrczNYM0xCbXlxRWZoL3Y1ZjJrWTRsT2p4bEVMN0pWaDMvd0UvNFNoSHFrc2dLa0srcTJSeGVZMlo5aUtxZFpaYXhiRzhzdm5LQTIzZjlkZGJTbHZXckdUT2k2a1I3U1Jmck1uclFpVm12akp3eS9WVnhZQmFITVZFeUFCdnY2VlBzOWo1amQ1R1pXS2FLeUpWblk0bG54MFlscSs3eWZtREdrYW9iMGtjNlZPcjFGdFlnWTF6cHJqR3FmK1lqeG95RFVDeFVSb01FVHZXcmoxNzV0eGpVS0doOEpCbnVienYrYzZueml4NnIvUUcwdDNDTzluVTg4YmRKUFBncjN3Y1hHajRNblJJU0UrcURzQWpRc1BvVk5zWWZqUksxM3psRW5kbllGc2RZbXBKOTgyUGxLWmRwL1F5TDBnUEdySU5RNlpSVWdqcUU0SlQ1UkZTcWZqUnI2eG0vKzFLM3EwTExWUVl6MUFma2hYOFl2QlJ3ZlFqalRFcEpqUG9RYXAzd0NsTXVwdHBtUEdCRkoyK1V3SzVCMXhleVlOVStkUFBwV0VHRjlRYjdZR2M4Q3g5UXJ2NE11S2Y2VmdXbWhsaW1iQURGcll3YWNVNG9QV3lHWVBkcjM2NlZCVFBVTitTUy81RHZLSDdHR0NPa3dablpNRUdxVXNnaFExd3V2cUFaejNuSzYyUzYrMWRkOGJxcnEyWHhtN1lmcTJieEQ1L3ZMUTEyeUNML0Vtdll2U3hyd3R5RFVJaVVYb081VkxXclpCOGFubTNJT3VoUnZUUDNXR1h1V012bCtZK3FzSVJGSzBXckV6eXhXN0Y2MUlZaXBlam02cnRWc1pWazc0YXVxNmErdU9TMGYvSzd0cm45UkI1ZXNDcDZ1TDhoZm1GZDhJQXhSVWdGaTNRdmJCMUxOOE9qS1J2ZURRK1Z6L1dmMm9PcWd6ajkrTU4yeEZDS0V2L0Y3dGE2TFFsVHNDcGhrUEl0WTFSTWlRTkdVVG9CeU9iT0IwZ3c2VzM5TWVRM3gwWlZueTBQelpUQTFSUHNCZnhnUlR5RkNabnBlKzcvYS9ManRrUVdSNlUweVdraGRQLzN2SUpiYVJ3UW9tcElKMEp2UC9DSmRwZEhQTVlocXhFY1lBWDVKTllnZmlEbmxVQzNRcFhMVFNVWGMrYS9QbmRiQzZYM3pEMFpzTm41NTFvam42NlZMSmdJVVRVa0U2SzMyYmFyeHZNK21HdmZoN0J1Nkc5THlpVUg3QmYrd2x5ektmMUdHLzV0ME9WQWVXWU9nMkdtalJiUDNwWWJnMDNnSVo0OFBzWTBGZ2FwMVJJQ2lLVjZBZEVVeEp4bW1tTUVaR25DZXBYTDlKNE5JUm5KazdVYTE3K1VsYXQ5dmxzWGJLMHZWL3Y5dFVnTTl4NE5RSStFNGkzMi9Ubzdud01JVmF2QjRkYTB1eGo4TXpLZVpIZU5aVGxiTVV0Z1JERnRFK0grYWxvd3JYdlZRWVVXQW9pbGFnRGdxZ3VORWZic0tETEF5MVo0MDI5Vnk2emZWd2ovNnU2RWpVK09NTHNyN3g2bWVMVHVEVUNOWk4ybTZXdmoyL1BIUTFXRW02Y1NPNnR2cWdaODRuZEY3blJCZE1aMm5MSS93b0lMWmFmSnArYmpZRlpaV1VLMGpBaFJOVVFMRUpzdFh4MHd5WnhxSHpzMW5yUGhsMFIzblB5ZngraGYreWV3QWo0cm5sSTI2d2dqSDhhM3hBclQrcGpzVEt5KzNWSEFEeDRtZGU0SlExUVhuUkMrLzhQUGVLNllwRHc2NnoyclRMb0lScG9XS1Z3aUlGdUdiUHpIWmpDWGw2NGJ4R2VOS0czUloyKy9HK0puZjgzayt3dmRob1VBd2dPN21KV3BnblBFczBraGF3MmNKUnp4aHVrV0FvaWxLZ0xoU3hydDdNR3FzMlhhdzkrWEZRZWo4VkZ5QXpxdGVBUUs2by9qUGQrOFk1VUw1VkJxNlduWTZraXArUG56R2Z0d3hvM3lHS01SMUJWMEJ3dXl3b2JuNWlYc3V0TEQ3S1FJVVRjRUNSS1duMHZwZW5VT0ZZRytYTHlKQUkrbVk5YVMzNEZNdTVPbjQxbDFCNk1yZ1RydVhjejBQWW1DL3k4Y1FoQ2hoc3dXSUZwTWRKalEzYk5Rc1g1UVJ6bTRkaVFBTlU3QUF0ZC96cUs0TWZtdCttQjVtOS9mSkk4ZUMwTW1JQUkwRS8rRkhzOXdoSWgrdVVUNlVVeVZ4SzJXNVFBanNsZyt0R3dRcGFucmZibjFnaUtTTExVQ2hJVVJoZlB4cmQ3L2M1MGtMN3c4RmluLzUyVTVqYUNKQXd4UWtRSXhKY0YweUZkZDE3Z2lqeS9DK2o2dURTOU90NXhBQmlnWS80aytmcmhqNTRyRDhTdDQxWmxkMi9sOHU3SmFXMnpLSndrNFh3dUxpQ2dxQ2xnODdQcDZOZXorL3Qxcy9tQWpRTUFVSkVGMHAzNjRBejdYZE1TY0k2WThJVUR5dDJwOXAvSittNjFzc2xSSWcrejArRmRydXJrV2x5eFdncUZaU0NLMGgrMW5DNXNPTld3Um9tTlFDMU51MVR5Mi9ZSUxYb2tPZVdYNytkZXI0OXQxQmFIOUVnT0xCbnh6djZsMEd1cndvdDBwUUtRRktpeTBDUGdLVWI5MlMzZnFpZGVPRGpBRkZrMXFBZHN4L3pudDFMdCsrblk4L0hZUk1od2hRZnZDcmR5dElseGZsVmdtcVJZQm9wU0FxVkhhNlhQWllqSThBNWNQZU1zS1lsdytrSXd3akFqUk1LZ0hLOWZXcjV2RzNtWVZ1b1RQampLdG91TDJpYi8raElIUTZSSUR5MDdmdm9QR3Z6NVUvbE5kcVhXNlVYN214S3hwV0NXaXQ4RjZFaGZFWSsvMVJWcXdBMlNMckt5WjJGMUFFYUpoVUFuU29jZlhRRmNRZXE1NzUxdTJjOTJ3UU1qMGlRTWxzZi9KWnY5YW9MaS9LamZJck4zWkZ3K3hacWJRUU50L2dzcnUreHRleUVDQTdmaEdnWVZJSlVOdmRjNzJhL1diYzRjSUpxbmRQNGVNT0lrREptUEU0N1dlZnNTREtyZTN1aDRPUTVjTWRvTTAzbUp0RU9LWFArSWtiRCtJVE5jVWRHbUVRQ2lvNzArZFkrSmtJVVBYZ0xVQW5EeDlSSzBmZjROWGs1NDk5MC8xUEJDRUxRd1RJRDI1ZzlmdFN1TnlVSCtWWWJ1d0JWOTlCMmloc2dYSEhXbXdSNERrK3A1TEh0WmhLS1VEMkdCRC85d0hSQ2NPSUFBM2pMVURzRm05Z0wxTFMzVjZqeHBxNzJvK3Vid3RDRm9ZSWtCOUgxN2NhZnlldUM5TGxoZ2hSanVYRzdZYlpDL2g4c1dlYU1Mc3I1N2F5RUk4a2JBRW9Wb0RzdENXdEZ3cXhWMWVMQUEzakxVQ2I3bjNNNjhnTlZ1bXVuWGg3RUtwd1JJQTh5ZW5Xd01TdmU2Mk9OaTFUWFk3bGhsYUlQUmhNQ3lYZnRMYUwyNzF5QlNPTldJQ2JubUlGeUJYQUpJRjFueGNCR3NaTGdMZ0FyM25jTFdxeHgreFh3MW1YcUYwL2VqRUlXVGdpUVA3c1d2Q2kxMkEwNWRjOGJrcEZMalMwdXp3WWd1TFRVdUVaVzN6NHY5MzZBVmNzM005dG9sWWlSN1ZhMG9xYTNhTEpKN0JSN3hjQkdzWkxnSTV0M0t5V25qTXVzWm5QVEF0Yk5QSUpnaThpUVA2UWY3WmNtQm5LaVB5ZE1yckh1aHlQYmV3SVFwYVhxQ05XYVgzUVJiTkZnLzhqV0h6bVBzK3pMbFJxK3hrRWhlZjRmUWlDUUVXM3hjdzJsN1FDUkpwZG9lUjlZYjVJQzJteVcxNmhpUUFONHlWQXUvVWZ4NklrTWREV2NQYWxhdDBOeFhlL29Ob0VhTnRqLzY3Vy84TjB0ZUhtdStQdHBydDB1dS9MNUhhS2RUZDh3L2cvS24rMjRWUEtzMUpFaVpDdlJZbFBpTHZwTmEzWllnVnBCUWhJbngwbXpoQW4ydzhpUU1ONENaRFplK1RSeEdlTW9mTzd6d1NoaXFQYUJHakR6WGVwaFcrN3lNUVZaK1owZ0QvL2hEcmUrV1lRcW5KczEzNzNtUTJqSENuUFNrTHJKcW9sRUdlMGhIekdqSHpGamZoY2dYSEhiUW9SSUNDZXVGWVd4bWNJRmFJVC9rNEVhSmhFQWNvTkRLbzExM3d4ZWZVemk5MjBIWDUxWFJDeU9LcE5nRGdpTmlrZWs1NFA2UFFVc1BldFdQQzdPYkFzWVpFbzVVaDU1Z1lHZ3BDVmc4cEt5eVdxcThYdndxbjBOUEE4NHpHdXdJWHgyVUptQ3hiL3R5bFVnSURXRkROamRyNUlEKzhJdTJRaVFORWtDaENMM1pvK2ZLMnBwS0VEbzR6RmNDcytjcjNxUDlnZGhDd09FYUIwNEhmOG43UW9rYnhTbnNVc0VoV0VVcEVvUU4zTkxXb3hmN3hKMzZ6dnZOUWNBRjhxUklEU3MxNzdQM0U2M3JSVXJ6VGxLZ2haa3loQTV0YUxwSXFuamZHSGpoSTJMVVdBMHJQNWdlLzVyZFhTK2NqeTFneEJDRWtVb0sxei84MXpjSE9NNnZyWnkwR280aEVCU2cvK1QvU1pOc3B6Njl3ZkJxRUVJVHNTQmFoMTJvUEpNMkNtV1Q5V0hWNVptZ0ZvRUFGS0QvNzNHWWcyTTJHNlhBVWhheElGeUd0Y0lSQ0NuanhDa0JZUm9QVDBiTm1oL2ZIcHhBV0psQ2ZsS2doWmt5aEFMTjFQdWhCdmVBYXNkQmZoaVFDbEIvOTd6WVRwOG1ScmpTQmtUYUlBcmJoSS8wRW4zUDNGSC9SclY5NnNCbnY3Z2xERkl3S1VIdnkvU3BkRDRoZUdMay9LVlJDeUpsR0FHbldGOGxyY2R1MVVYUU1HZzFERkl3SlVBTnIvNWg1NWowV2psS3NnWkUyaUFFWCtBVHRtMWdEZE9DMElVUnBFZ0FxRGNrZ2Nzd3RNRUxLbU5BS2tLMmJMNUpsQmlOSWdBbFFZbEVOaU9nTVRoS3dwblFCTkVRR3FDZ0hTNVNBQ0pOUUswZ0xTSmkwZ1FjaUdPaGtEMmhXRUdvbU1BY1diSUdSTm9nQmxOUXYyeHBmdVR4UWdGdHdaQWRwV0FnSDZFQUxVRllRYVNVMElrTXlDQ1RWR29nQmx0UTZvN1U2UFE5QjBSVEpiUUZhc0RVS2Rqam5MK3BPM0pwNWxUZjY0UDUzYlJ1T29CUUdTZFVCQ3JaRW9RRm10aE9aa1JkOU5zRzEzUnQvUGZYQnBzMXJ5dm84bnR1QVd2L3N5dGVxcXlWcXc0Z1cwRmdUSSswd2dYWjZ5RWxxb0JoSUZLS3U5WVB0L3Qzem9IcktvOTlrV2lNdk9Ienl2Y2llSFQvazc5dm9tOWRwWVdnTitOM204TWZWYlFjaG9ha0dBOEQvbFFEb2kweGNZNVZuT3ZXRHV5WUR1K2NzKzJPbE5lMHBpc1pCZW55Tmg2eG43QkVmS3Mxd2tDbEJXdStINzloOVN5Ly8yODRuZjVzWjBoZVBTdmRWWGYwSDkvdlovTmdmRU04YlI0Q0UrR1BuYi9aT1hnamRIVXdzQzVMc2JIc0Z0blY2KzNmQzJBR0VjalpvV08zd2xCWWlqVmNNYkxzNWtxa2FBc2pvUENOcnU0aTc2aXlQZk44SzQrZk5kbHhreE1VS1IwQW9JRFlGYmZzRjFxbmZQL3VDdDBkU0NBSFg5eDh1bUhDTFRabG01endOeUJRaExLeUxGaEMwVXViMTBtS29Sb0hRbkluNHZDRlVhdUk5czJWOStPbkVRdkJoYjlNY1hxNjF6ZmhDOE1aNWFFS0NPMlU5NWZWbVFqM0tlaUJnbFFHbTdZbmJZU2dtUW5XNFJvQ29Sb0t6T2hBN3BmUHpwb1ZaUTBwMzBCUmdWY2RVbmIxSDloNDhFYjR1bkZnUUkveWVPMTVrdVdublBoSTRTSUN4TlY4d09Kd0pVZWFwR2dMSzZGU01rMTM5U2JmenFBN3FsTWpwUkJOTVlGWFg1MzF5bmptNW9DOTZVbjJvWG9HcTZGU05PZ0RCZk1Ta2tUTEdJQUExVE5RTEUvVkdyTTdnWHpHYWc1NFJxbmZZZExRQ1hKaTRKU0RTZFJycGRLMGZmb0xwWHZ4NjhJWmxxRjZCcXVoZk1yc2kwZXV5TCszeTdZdUh6bUFoUTVha2FBWUxXT3g1S25nblR4dmhEcVc1R2Rja05EcW8zbi9tRlduN2g1NDJBR0NGS3FHeTI4YzNQN0ErcnA5K1lPa3VkMkJXLzdTS0thaGNnLzNWVDViOFoxYTNJWEVob3A4R25LMlkvN3lOQVBNTkZnRkVYRkRLekZTZDZoTE9manpMeVFCemh6KzZsaGk1MjVjWHlwZC8yVGZNbkpnZS9IVW1ZUDU2eDQwN0tYNGlkejFCY0NXZjdpN2pDRzJOOUJZamxDdllYVEpKdlhMd0VLSXU3NGVQbzdkcHJIUGZhMkp2TU56NlZEbUZwZU9kbFpoWU1ZVEttLzA5NnpPZGFPQnExazE2LzdUNTFzSEdOeXVWeVFXeitWTHNBcmRWK1R4ei8wY2IybG5MZkRlOEtFTmd6VEZpK1NnbSt6MUx4N1BmRldYaEZzb3RkTWVPTVBGRFJ3cCtKS3g5dWVrSWZSRUdGRFovajc5cWwyUHlGdUFKa0M0eHRZUnoyNTNFQ1ZLejRnSmNBTVJ1MTlKeHh1b0xsYjNIUXVtajg0TlY1ZDZlWGlvR2U0N3Jic1Y1MXpudldqQkg5bjNiU3FxdW1xRmN2KzBlekFISDErTnZNSC8ybSs3K3IvdkNmLzVOM241Y1AxU3hBK0h1WjludlNZZlNVSCtWNGJHTkhFTEk4UkFrUUZjbitZMDNxaW9YUFlYRUNSSGkzUlpCa2JpWDFGU0N3V3d0eEN4VkpreDBXaTZ2QVlQdkVqYk1VK1F1eDgrbCtHWVJHV3NJeVNSS2dVb2dQZUFtUTJWTTE3cGJFUFZVWXJaRmRQM294Q0ZsWmNqcWRBOGQ2MU9EeEUxcWhTcmN4RnFwWmdIWXRlTkdyaTB6NXNiV0c4aXduVVFJRWFicGk5bk54QW1SWFRpb0Q4ZG5QVXBsNHA1MmVmUEhGcFR1RStQTjlEbTRlTWRJV2hkMnFRdHhjM0hUemM5aEZBdTZkSngyMkVHQlIrYk1GS0RTRUtMeTdubi90dVBNSlVLbkVCN3dFQ0RiZCs1amZyWnU2RzdCMm91NkdwZS9sVkRWVkswQzZPN2wyNHRlOXVsOTBSeW5IY3BPdkl2dDJ4WktlNFpzKy9KektFTmNpQ2JHN09sR1ZIWklFeUs3RWNlTTF0a2dsdFpqc1N1NktzWjAvTEY4bGR3VWhLbit1QU1YNUlDUk9nTnhXV1RIaUE5NENkR0RoaXFHOVdVbnJjZWltdmY4cWRYUzkzL1IyclZDdEFuUjBmYXRhcXYyZDFEMDJLOFhmYzdrcHgzS1RyeUx6QjV4VVdTRDhISXNTSUxzUzJOL2MrVWdTaENRQkFqdnRZWGZGSmt3WDc3SkZMMnA4eDg2RG0wZGJxRzBCaU1NVkdEZC83dWRSNmJHSkVxQlNpdzk0QzlESncwZk0xRFYveEdFQzRzeDgwOTcvUkJDeVBxaFdBV0tNeTJmMmkzS2ovQ2pIY3BOVWtkMXY5NmhuN00vZHlrbDN3ZjdjRjd0U1JiM1RSNEJzVVhISFc2aWc0V2M4WitmVHJheDJIaEExbC9BenpGZGdiWEZ3MDU4a1VDNjJyL0JMT2NRSHZBVUkydTVtYjViUEgvdkgxUElMSjVoRmpQVkNOUW9RL3NYUFBodDJLYmUydXg4T1FwWVhuNHBzUDRPNUZjTCt6QlVndHpJVllyUXdYSHpTYlkveHVKWFEvZ3p4SVUvaHoyNUxMNTg0dWZuenhSVU5tN1J4Mm5FaFBMYjRZSEgrU1VzcUFUclV1SHBvcHNWai9RMkRvc3hRMVF0cEJPakVqc29JMFBZbm4vVWFmRGFMUkhYYUtMOUs0Rk9SYVFIWTNSbit3RzNDMzJPdUFORjlzRDh2eE53S0NqN3B0bHM1cnFqWTR6L2g0SzZkeC9CM1lIZXgzSlpVdFFsUW5DVzFvbnhJSlVETU1qV1B2eTE1VmJRMm12d3JkQUZ4ckVZOTRDZEFZOVd5di9pVTJ2dGZ2MVBkcjI1UWg1ZXZUYlJEald2TW5lNXA0ZlJHS29EUHluREtpMlVKbEY4bDhLbkk0QXFKL2F6OWUxZUFmQ3BIa2hVcVFHQ0xoMTBKdzFhQ0xhWjJuTGJRNUJ0THFsWUJJbC8yT0pyN3BWRUlxUVFJZHN4L3p1OWJWeHZOZmphVDFnTmVBaFFZclExV1h2dll3bmVNVnUwekh3bmU0cy9RSmwzUGN0RGxSYmxWQ3QrS0RQYXpXRmloN2QvbEV5QzNvaFdEYjdydDdsTTRtR3VQNmRnelduWmF3OS9iWWhEVkZheEdBVUpzRUVvM25ueCs4aUcxQUpseGh3djh4aDNNV05ENTExVjhYVXc1U0NOQXBvdnFhV3dyYVovNWFQQVdQL0JuMC9tZjh5OERYVjZWSEk5TEkwQnhYYkh3Wjh3Vm9FSXJhQksrNmJiRkpxem90aWpaZzhaMldzTzgyVjIxcU5rb04zOXVDeWtPTzk1U0NoQ3RIanNOakZtRm4xRjJkdGN5TGFrRkNEcG16ZlAvOW1Yd3M4eDdqeXBCS2dGS1lmaW4vWjUwQW9RLzAvaS9ZL2E4SUdSbFNDTkFFTlVWczM5MkJjZ1dnS2pQQ3lWTnVoR1Q4RmtxcDEwcFhjRUlmeDkrWm5kajRpcXZIYVljczJCSjJHWGdpaGw1c0w4MDNNL1RVSkFBOVhSc0gxcjZyN3NRWVNKaWpmVXA3L3U0T3JpMCtBR3JMS2tXQWNLUDVxRDlwSFUvMmlnZnRzYjBiTjRlaEs0TWFRVUk3REN1UlFtTVhZbDlLMEJTdXRLazI2NmdDRVNZbnJDVlkyUEhhN2VVb3A0TktYWWRrQ3RhcFJRZ3NQT0JSYlhrZkNoSWdJQks0M3RjS3F0MG16OTFxenA1NUZnUXV2YW9CZ0hDZi9qUlo5VXpSdm1rYlYyVmdqUVZPY1R0aXRrV0pVQjJCY0dTMXFXNHJheW9sa2VhZE50VDdMWlkyT00vSVhiWHlNNWp2bmU0RlR4Zi9raUxIUzlpNkZKcUFRTGJYN3pmYmZuNVVMQUFjUnNwbC9uNUhwZHF1Z0s2NjFhclZJTUFwZW42VWk3YzlwcnYxdGh5VVlnQWdTc1NvVVVKRUgvc2RwY0Q0NzE4ODlzVmdaL3Q5R0JSSWdIMmMvdy9xVUxacmJEUW9ycEwvTTU5RGt1YXhuYlRIZVl2QkJIRnY2NXdSNldoSEFMa2RvV2pCdFNUS0ZpQWdMT1VmU3NFWFFhT3o5ajc4dUlnZEcyUnRRRHRlM25KMElGakhsMHZqSGg5enJvdUIzYkZTU05BNEZZNkxFcUF3UDNtOXpGRUswNVk3SmFLYlhFVksrcjVxTGo1bmZ0Y1ZDdkZoWEJSSXBmUDR2eGREZ0VDK3prc1N2enlVWlFBOVI4NHJGYU9tYVI4N3Q3Q1dMUENnV0k5SFoxQkRMVkRsZ0xFbUJ0KzgxbnpnMUVlS3krZFpNb25DNG9Sb0tpdVdKd0FBYzlIaVZhVUlSajVXalZ4Z2haWEFkMldUYjZLNnJiVzRscGhMcVRYSjMra20yNWJIT1VTSUxCRmtuVGs4N0ZMVVFJRWUxNTRaV2hoSXQvT1FTTHlHWldZKzh0UGRoOE5ZcWdOV2liUFZJdmVNZHBNYTVmU0ZyNzlvNnA5UnZ3V0NmeGs3bnYzRlQ5ZERwUkhPVys5U01LdU1Ha0ZDTnl1V0Q0QkN1RVp4a25jaXM3UFZQYWs3azRJejlsak9tRWNjZGlDbFMrdmJtc3BiVXNoeko5YjJmRTEva3FxOU9VVUlEZHVYM0dGb2dXSTR5QTJUSm5wZFZSSGFIenJjeXdxQjg3WENyK2Y5cUJxK3V2UHFoVVgvWDFKcmVsRG4xRmJIdngrOEpiVHdULzR5YnVicTQxbktRL0tSUkNxbmVJRlNQTlcremJWZU41bnpUZDZWS1dJTWlyS3B2c2VyNW1LY3ZMb1cyWmJTWi91MXBUVTloOVVKOS9xQ2Q1aW9mMkNmM3hYbldQNHYwbVhBK1VoQ0xWQVNRUUlPRERlVEE5N2RzWE1LbURkcmRqeTBQd2dCc0VHditDZk5QN0UvNVNESU5RS0pSTWd2ckUzZm1XMmJ0bDRYcVdNQlpYR2lKQjBHWWJRZnNBZnFjUmNHMzdILytKSG9aWW9uUUJwK3ZZZU1MZFYrQzZVTTRZSTZXOTZ1aHU1L3NyczFxNVdCblgrOFlNWmNFNGhQdmdiditOL1FhZ2xTaXBBMEwycXhaeUprMlk4aU1vV0RrelgydXhZcVNEZnB3YWNVNGdQZmw1MjduanQ5dzFCVElKUU81UmNnS0RyaFZlR1ZraHplRmxFcFlrektoOVR6cFhldTVRMTVKZDhHL0dKOEV1c2NleUhGcUN1NTE4SlloS0UycUlzQWdUYkhsMlFiaEExTUxvZkhETzY3OWRMZzVqcUcvSkpmcjNYK1lSR3ExR0h3YytDVUt1VVRZQVlERzJiK1lnWkhFMHJRdVptMDFGanpkNG5wci9yRWZMRk1SbHNyL0JkNFh6S0VCL2RXc0svTXVnczFETGxFeUFORitCdC9OcTMwM2N0TUMxQWlCZTd2dzh0cTh4WnhwV0MvSkF2NHhmUHZWMjJFUTYvbHZ1Q1FVRW9OMlVWSUJnODBUc3NRa2wzaWtWWU9LUFdldWVjb3E5WHpoclNUejdzZktVeTdUOHozZjYxQjR4ZkJhSFdLYnNBd2VraWxQNGJmNmcxZElrNWhyVHppUjludHNteVVFaHY1eE5QbS9RWDdBUFQ3VUo4ZE11bnR5K0lXUkJxbTRvSUVBejI5YW4yZXg0WkdwaE9PVHNXR2pNK1ZHQnUyOWcrN3lkVnYrNkY5SkZPMGt1NlV5MU5zRTM3cStHc01lYndldndvQ1BWQ3hRVElrTXVaV1J1bTZBdXVqTnE0OG9jOVVzd2VjVFBvMFpaMmxSc2NERjZTTGFTRDlKQXUwa2M2Zlc2VGpUUDhoSm5aTGhsd0Z1cU15Z3BRQUVkRnNIaXVvSEVReThJV0VYZlJyNXQ0dTlxMTRPZnErTFpkbGErbytuMjhsL2VURHRKVFZJc25NUHlEbjdJOFdrTVF5a2ttQWdUZHpTMW0rMEFoMC9RamJOVFFuakphR3h5V3YvYkdiNmpPSjU5UmgxOWRyL29QZFFkdkxDMzloNDZZK0hrUDcrTzl2TitJYWdFelc2ZFpNTjZEZjdwWHR3UnZGSVQ2SXpNQmdyNjlCODBHU2lwdHNhMkZVOFo0Q1dJVURQYXUrTWoxYXYyazZhcGo5bE9xNjJlL1VvZFhybE05VzNlcS9vUGRaakEzcnV2RzcvbWM1M3EyN0RUaENNOUJUY1JIdkVOQ01TUTY1c3JxcVBTa05QeEFmUGdGL3doQ1BaT3BBQmwwUmQvOTdDL05BZXJtVUxNQ3B1cGpUUXRFV0tFUmlrVm5qVEVMLytqV0lDQ3JycnpaYklGWWQrTTAxVEo1aG1xWm9rMy95OC84bnM5NWp1Y0p4MEF3YVR3bG1QcDNrZTh0eEpoaTF5MG8vSUEvOElzZzFEdlpDMUFBNTBTYmt4WGZkVm42bGNGcERORUk5bER4SHQ2SG9MQVY0cFR4YzVBT0l6UzBia29wTm81eGhqUHZhOUg1cjhYenNnV2hVS3BHZ0NBM01LRDJ2UGhiYzZDNkdjVDF2UEtuVm8zOGtVL3lTNzV6QTlMcUVjNHNxa3FBUXZvUEhqWlh5bkR2R0lPeFhqZXcxcENSSC9MVnBMdGI1SlA4Q3NLWlNGVUtVTWp4empkVit6Y2ZNOWNMR3lHaU94UlJvV3ZGU0QvNVlNYU1mSkUvUVRpVHFXb0JDdW5ac3NQTVlpMi9ZSUxwc2pCZVVzNHhtWklhZzljNnZhU2I5TE1EbnZ3SWdsQWpBaFRTdTJlLzJqSC9PZFY4OVcxbTJ0dU1FeFd4eXJpY1psWnJrejZkVHRLN1kvN3pKdjJDSUF4VFV3SVV3akVVaDVyV3FMWVpjOVhLMFRlWTJTcFQyV2taRmJzSXNGRFQ3K1g5cG9XbTAwTzYybVk4Yk5JcHgyWUlRalExS1VBMm5LVjhZT0ZLYzVoNzg3aGIxTkp6eHBuMVBtWVBsaFlFTTRCZDZ1NmFqbzk0aWI5QnY0ZjM4VjdldituZXgwMTZ6dFN6clFVaERUVXZRRGFzWEQ2MmNiUGEvZE9YVk5zZGM5U2FhNzZvbWo1OHJWcXNSWU5kK0xST3d1MFNabzJQT2JjNlJweU15QVJyaFZqSWlOQVFYc2REZk1TNzVwb3ZxZFk3SGpMdjQ3MXlUSVlncEtPdUJNZ2xkM0pBOVhidE0vdk8yTkM1ZGU0UFZldTA3NWl0Rk0zanBxZ1ZGMTJ2R3M4ZHI4Vm1wQUR4ZXo2blZjUHpyZE1mTk9HSmgvaDY5K3d6OFF1Q1VEaDFMVUNDSUZRM0lrQ0NJR1NHQ0pBZ0NKa2hBaVFJUW1hSUFBbUNrQmtpUUlJZ1pJWUlrQ0FJbVNFQ0pBaENab2dBQ1lLUUdTSkFnaUJraGdpUUlBaVpJUUlrQ0VKbWlBQUpncEFSU3YwL0JtcUNFVER3MG1FQUFBQUFTVVZPUks1Q1lJST0pOwoJCQkJfQoKICAgIDwvc3R5bGU-CjwvaGVhZD4KPGJvZHkgY2xhc3M9IiI-CjxkaXYgaWQ9InBhZ2Utd3JhcHBlciI-CiAgICA8IS0tIEhlYWRlciBTZWN0aW9uIC0tPgogICAgPGhlYWRlciBpZD0iaGVhZGVyIiBjbGFzcz0iY2FudmFzIj4KICAgICAgICA8c3BhbiBjbGFzcz0ibG9nby11bCI-PC9zcGFuPgogICAgICAgIDxzcGFuIGNsYXNzPSJsb2dvLWJhbmsiPjwvc3Bhbj4KICAgIDwvaGVhZGVyPgoKICAgIDwhLS0gTWFpbiBDb250ZW50IFNlY3Rpb24gLS0-CiAgICA8c2VjdGlvbiBpZD0iY29udGVudCIgY2xhc3M9ImNhbnZhcyI-CiAgICAgICAgPGRpdiBjbGFzcz0icm93Ij4KICAgICAgICAgICAgPGgyPlB1cmNoYXNlIEF1dGhlbnRpY2F0aW9uPC9oMj4KICAgICAgICAgICAgPHA-V2UgaGF2ZSBzZW5kIHlvdSBhIHRleHQgbWVzc2FnZSB3aXRoIGEgY29kZSB0byB5b3VyIHJlZ2lzdGVyZWQgbW9iaWxlIG51bWJlciBlbmRpbmcgaW4gKioqLjwvcD4KICAgICAgICAgICAgPHA-WW91IGFyZSBwYXlpbmcgTWVyY2hhbnQgQUJDIHRoZSBhbW91bnQgb2YgJHh4eC54eCBvbiBtbS9kZC95eS48L3A-CiAgICAgICAgPC9kaXY-CgogICAgICAgIDxkaXYgY2xhc3M9InJvdyI-CiAgICAgICAgICAgIDxwPjxzdHJvbmc-RW50ZXIgeW91ciBjb2RlIGJlbG93Ojwvc3Ryb25nPjwvcD4KICAgICAgICAgICAgPGZvcm0gYWN0aW9uPSJIVFRQUzovL0VNVjNEUy9jaGFsbGVuZ2UiIG1ldGhvZD0icG9zdCIgbmFtZT0iY2FyZGhvbGRlcklucHV0Ij4KICAgICAgICAgICAgICAgIDxpbnB1dCB0eXBlPSJ0ZXh0IiBjbGFzcz0iaW5wdXQtZmllbGQiIG5hbWU9ImNvZGUiIHZhbHVlPSIgRW50ZXIgQ29kZSBIZXJlIj4KICAgICAgICAgICAgICAgIDxpbnB1dCB0eXBlPSJzdWJtaXQiIGNsYXNzPSJidXR0b24gcHJpbWFyeSIgdmFsdWU9IlNVQk1JVCI-CiAgICAgICAgICAgIDwvZm9ybT4KICAgICAgICAgICAgPGZvcm0gYWN0aW9uPSJIVFRQUzovL0VNVjNEUy9jaGFsbGVuZ2UiIG1ldGhvZD0icG9zdCIgbmFtZT0icmVzZW5kQ2hhbGxlbmdlRGF0YSI-CiAgICAgICAgICAgICAgICA8aW5wdXQgdHlwZT0iaGlkZGVuIiBuYW1lPSJyZXNlbmQiIHZhbHVlPSJ0cnVlIj4KICAgICAgICAgICAgICAgIDxpbnB1dCB0eXBlPSJzdWJtaXQiIGNsYXNzPSJidXR0b24iIHZhbHVlPSJSRVNFTkQgQ09ERSI-CiAgICAgICAgICAgIDwvZm9ybT4KICAgICAgICA8L2Rpdj4KICAgIDwvc2VjdGlvbj4KCiAgICA8IS0tIEhlbHAgU2VjdGlvbiAtLT4KICAgIDxzZWN0aW9uIGlkPSJoZWxwIiBjbGFzcz0iY29udGFpbmVyIj4KICAgICAgICA8ZGl2IGNsYXNzPSJyb3ciPgogICAgICAgICAgICA8ZGV0YWlscz4KICAgICAgICAgICAgICAgIMKgwqDCoMKgwqDCoMKgwqDCoMKgwqDCoAogICAgICAgICAgICAgICAgPHN1bW1hcnk-TmVlZCBzb21lIGhlbHA_PC9zdW1tYXJ5PgogICAgICAgICAgICAgICAgPHAgY2xhc3M9InN1bW1hcnktcCI-SGVscCBjb250ZW50IHdpbGwgYmUgZGlzcGxheWVkIGhlcmUuPC9wPgogICAgICAgICAgICAgICAgwqDCoMKgwqDCoMKgwqAgwqAKICAgICAgICAgICAgPC9kZXRhaWxzPgogICAgICAgIDwvZGl2PgogICAgICAgIDxkaXYgY2xhc3M9InJvdyBkZXRhaWxlZCI-CiAgICAgICAgICAgIDxkZXRhaWxzPgogICAgICAgICAgICAgICAgwqDCoMKgwqDCoMKgwqDCoMKgwqDCoMKgCiAgICAgICAgICAgICAgICA8c3VtbWFyeT5MZWFybiBtb3JlIGFib3V0IGF1dGhlbnRpY2F0aW9uPC9zdW1tYXJ5PgogICAgICAgICAgICAgICAgPHAgY2xhc3M9InN1bW1hcnktcCI-QXV0aGVudGljYXRpb24gaW5mb3JtYXRpb24gd2lsbCBiZSBkaXNwbGF5ZWQgaGVyZS48L3A-CiAgICAgICAgICAgICAgICDCoMKgwqDCoMKgwqDCoCDCoAogICAgICAgICAgICA8L2RldGFpbHM-CiAgICAgICAgPC9kaXY-CiAgICA8L3NlY3Rpb24-CgogICAgPCEtLSBGb290ZXIgLS0-CiAgICA8Zm9vdGVyIGlkPSJmb290ZXIiPgogICAgICAgIDx1bCBjbGFzcz0iY29weXJpZ2h0Ij4KICAgICAgICAgICAgPGxpPiZjb3B5OyBVTCAuIEFsbCByaWdodHMgcmVzZXJ2ZWQuPC9saT4KICAgICAgICAgICAgPC9saT4KICAgICAgICA8L3VsPgogICAgPC9mb290ZXI-CjwvZGl2CjwvYm9keT4KPC9odG1sPg"

        private const val ENCODED_HTML_REFRESH = "PCFET0NUWVBFIEhUTUw-Cgo8aHRtbD4KPGhlYWQ-CiAgICA8dGl0bGU-M0RTIC0gT09CPC90aXRsZT4KICAgIDxtZXRhIGNoYXJzZXQ9InV0Zi04Ii8-CiAgICA8bWV0YSBuYW1lPSJ2aWV3cG9ydCIgY29udGVudD0id2lkdGg9ZGV2aWNlLXdpZHRoLCBpbml0aWFsLXNjYWxlPTEiLz4KCiAgICA8c3R5bGU-CgkJCS8qIEdlbmVyYWwgU3R5bGluZyovCgkJCWJvZHkgeyAgICAKCQkJCWZvbnQtZmFtaWx5OiAnT3BlbiBTYW5zJywgc2Fucy1zZXJpZjsKCQkJCWZvbnQtc2l6ZToxMXB0OwoJCQl9CgkJCQoJCQlvbCwgdWwgewoJCQkJbGlzdC1zdHlsZTogbm9uZTsKCQkJCXBhZGRpbmctbGVmdDowcHg7CgkJCX0KCQkJCgkJCgkJCXN1bW1hcnk6Oi13ZWJraXQtZGV0YWlscy1tYXJrZXIgewoJCQkgIGRpc3BsYXk6IG5vbmUKCQkJfQoKCQkJc3VtbWFyeSB7CgkJCQliYWNrZ3JvdW5kLWNvbG9yOiAjZWVlZWVlOwoJCQkJY29sb3I6IzMzMzsKCQkJCXBhZGRpbmc6IDdweCAxMHB4IDdweCAxMHB4OwoJCQkJbWFyZ2luLWJvdHRvbTogMTNweDsKCQkJfQoJCQkKCQkJZGl2LnJlbGF0ZWQtY29udGFpbmVyIHsgCgkJCQltYXJnaW4tdG9wOjUwcHg7CgkJCX0KCQkJCgkJCXAuc3VtbWFyeS1wIHsKCQkJCXBhZGRpbmc6IDdweCAxMHB4IDdweCAxMHB4OwoJCQkJYmFja2dyb3VuZDogI2Y0ZjRmNDsKCQkJCW1hcmdpbi10b3A6LTIycHg7CgkJCQltYXJnaW4tYm90dG9tOjBweDsKCQkJfQoJCQkKCQkJLmJ1dHRvbiB7CgkJCQliYWNrZ3JvdW5kLWNvbG9yOiAjMDA3OTg3OwoJCQkJYm9yZGVyLXJhZGl1czogN3B4OwoJCQkJYm9yZGVyOiAwOwoJCQkJY29sb3I6ICNmZmZmZmY7CgkJCQljdXJzb3I6IHBvaW50ZXI7CgkJCQlkaXNwbGF5OiBpbmxpbmUtYmxvY2s7CgkJCQlmb250LXdlaWdodDogNDAwOwoJCQkJaGVpZ2h0OiAyLjVlbTsKCQkJCWxpbmUtaGVpZ2h0OiAyLjVlbTsKCQkJCXRleHQtYWxpZ246IGNlbnRlcjsKCQkJCXRleHQtZGVjb3JhdGlvbjogbm9uZTsKCQkJCXdoaXRlLXNwYWNlOiBub3dyYXA7CgkJCQlkaXNwbGF5OiBibG9jazsKCQkJCXdpZHRoOiAxMDAlOwoJCQkJbWFyZ2luLWJvdHRvbToxNXB4OwoJCQl9CgkJCQkuYnV0dG9uLnByaW1hcnkgewoJCQkJYmFja2dyb3VuZC1jb2xvcjogI2VmODIwMDsKCQkJCX0KCgkJCQkuYnV0dG9uOmhvdmVyIHsKCQkJCQliYWNrZ3JvdW5kLWNvbG9yOiAjNzM3MzczOwoJCQkJfQoKCQkJCS5idXR0b246YWN0aXZlIHsKCQkJCQliYWNrZ3JvdW5kLWNvbG9yOiAjNTk1OTU5OwoJCQkJfQoJCQkJCgkJCQkuY2FudmFzIHsKCQkJCQlwYWRkaW5nOiAxNXB4OyBib3JkZXI6IDFweCBzb2xpZCAjZWVlOwoJCQkJCW1hcmdpbi1ib3R0b206MTVweDsKCQkJCQl9CgkJCQkJCgkJCQkjY29udGVudCBoMiB7CgkJCQkJbWFyZ2luLXRvcDowcHg7CgkJCQl9CgkJCQkuaW5wdXQtZmllbGQgewoJCQkJCWJhY2tncm91bmQ6ICNmZmY7CgkJCQkJYm9yZGVyOiBzb2xpZCAxcHggI2UwZTBlMDsKCQkJCQlib3JkZXItcmFkaXVzOiA1cHg7CgkJCQkJY29sb3I6IGluaGVyaXQ7CgkJCQkJZGlzcGxheTogYmxvY2s7CgkJCQkJb3V0bGluZTogMDsKCQkJCQlwYWRkaW5nOiA3cHggMHB4IDdweCAwcHg7CgkJCQkJdGV4dC1kZWNvcmF0aW9uOiBub25lOwoJCQkJCXdpZHRoOiAxMDAlOwoJCQkJCW1hcmdpbi1ib3R0b206MTVweDsKCQkJCX0KCQkJCQoJCQkJCgkJCQkvKkltYWdlcyovCgkJCQkJLmxvZ28tYmFuayB7CgkJCQkJYmFja2dyb3VuZC1wb3NpdGlvbjogY2VudGVyOwoJCQkJCWRpc3BsYXk6aW5saW5lLWJsb2NrOwoJCQkJCXdpZHRoOiAxNTBweDsKCQkJCQloZWlnaHQ6IDM1cHg7CgkJCQkJYmFja2dyb3VuZC1zaXplOiBhdXRvICAzM3B4OwoJCQkJCWJhY2tncm91bmQtcmVwZWF0OiBuby1yZXBlYXQ7CgkJCQkJYmFja2dyb3VuZC1pbWFnZTogdXJsKGRhdGE6aW1hZ2UvcG5nO2Jhc2U2NCxpVkJPUncwS0dnb0FBQUFOU1VoRVVnQUFBTDBBQUFBMENBWUFBQURmYURrZ0FBQUFBWE5TUjBJQXJzNGM2UUFBQUFSblFVMUJBQUN4and2OFlRVUFBQUFKY0VoWmN3QUFEc01BQUE3REFjZHZxR1FBQUFYWlNVUkJWSGhlN1preFVpdzdERVgvMG9oWkFERUxJSDQ1T1RFNU1UazVPU3RnQjZ4aWZoMEtWOTJua2kyNzJ6MlBHZXRVcVdER2Jsa3RYNnZ0bnY5T1NiSVlLZnBrT1ZMMHlYS2s2SlBsU05Fbnk1R2lUNVlqUlo4c1I0bytXWTRVZmJJYzNhSy91Ymtac29lSGg5UFQwOVBwNCtQang4UGw4djcrL3RlOW5RTWRqL0dUZVJ3bWVyWEh4OGZUMTlmWGo2ZkxJMFYvWFp4RjlCaVYvMUpKMFY4WG0wUWZUUUpibXJlM3QyK2g2M1V2THk4L1BTNkxGUDExY1lqb0ZSWCszZDNkejdlWFJZcit1amhjOUZSOXZmWVNEN1lwK3V2aWNOSERubXQvQXluNjYrTHNvdStwOUp3SGVPTnpmMy8vMTdXM3Q3ZmYyeVhPQnRIYklMMnU4UHI2ZXZyejU4KzNuOUxHR00vUHowMS92YUxIaDhiTS8xdmZXdWw0czBYLytmbjUvVHJabnJtSWwrOXA3NFg3WXo3VUYvbGwvb29melIvOUltYkc1M0c0NkFtd1hFY3lXckFnVkRRdHd4Y2lycUY5bVJpYlFHdjRZN0Y1OUloK3B1QkJ4NXNwZWtTanZtdEd2d2ppMGdMaUdYTTBJdnFaOGRVNFhQUlUxbklkRmJVR2dvOFM2Rmt0RnUwenNwQzhLcUtUaGxsbUN4NTB2SkY4dDZENnF0L0k2Ri9EbnRWYXBrSnVpWDVtZkMwT0VUM0NZWVZiSWJUUVNvejRXQ0IySzhTNE5qRXNLZy90VTR4cjFTZVYzUzRJcjRLMFJIK0U0RUhIbXlGNjhxaytlWlBHSEpWWStjdG52dGQrWHFHaXI5ZXZGSXppeXl0aU5kSFBqQzlpaytoSERTR1Y0RDFzMVlnbVdSTkVZajNVSDBiQ1BPd0Vlb3V6SlhwZGhMTUVEenJlWHRIckZoTnJ4Y24zdW9neCsvU3pBclhGcWVENThrUS9PNzZJczRpZUcyMU5uQ2F4OWZncjJDUjVhSHZrazRPWTlyZlVSSCtVNEVISDJ5dDZ2VCtLUkJRbjdWcWw3WStLV2lTaVNtc0xtamNYcytPTE9Jdm9pNDBHMTBMOWVtaDdORzZya29QWGZxVGdRY2ZiSzNxRVZuejFIZ0JyKzNCYmNIcnVXODkxbnVobnh0ZkRJWHQ2RWtFZkF0TVZpZFhla0xRby9oQXZZclA3T2c5dGorS2xYZnRiYkxzS0hodE5lZy9xUDRvL1lvdXZXazZZdi9JZGk3Mkg2RWxlMnJDOThmVndpT2dWSG04cWZBVGJna3FDdUtrT2R1OVdNdzl0aitLTkVtamJQWnY1RkFQMVBaSnZqeTIrYWprWjNZcUNMcFFsUkE4SUlycWVhbTRyYUs5NWFIc1ViNVJBMjQ2eGtKbEEvVHg2b0dxaFk0M20yN0xGVnkwblcwU3Z2cFlSdlQzTTJNTVBnbTlWZFo0T0pJdnRFbTloUmcreVVieFJBbTA3QXVlZWlGdWZZcjBpNkVISEc4MjNaWXV2V2s2eTBnK2cxMXZSYXlJeEtqNkpxbFZPeEtiOVBiUTlpamRLb0czWFYzUXNRbTNiY21ieFVKOWI4cTJvcjk3NFZLaFlRZS8zaUQzOTN2aDYrQ2VWM3U1L1IxNkJRU1JTMFBZbzNzaGYxTTVFbGpZcVA0dHlMenJlYUw0dEd0L2V0eU4yTG52dWRZbTNOeFpieWJWU2dyYjErTllrWWg3YUh2bU1SQjIxMisxVzdWZmlFZFRmYUw0dGVxYnFXWlIyMjJhRk9GS2tiRzQ4Z2M2T0wrSncwU053RGRCN2U2TythNytjRnJ6RHJyY04wdmFqUlE5MlllOFY2a3hmVm5pdDN4WDQzcDZ2Ykg3MVhwbGJXOFFLbmk5UDlMUGppemhNOVBSaEJhcmdNVS9VZWhQMEo2bDZJL3pQZFZwaDFMeDRvbmFGZHUxdmlkb0xHaC8vMXlhdUJ4MHZpcjhIdXlpSmo1eVdHUG5yNWRpcm92UzEvWFRPYXI2dzJsWmtabndSbTBTLzFhalNIdlpRTW1yZTRVZmJ6eVY2MjIvTGhCVFV6eFpEUkJhN0xZeXN0VmUyZS90ZWEvbWNHVitMczRuZW13U0ZWZXhkWjYxVUFHNjRmT2VKUzY4NWwrakJicitpc1d1b2p5MVd5emU1OHZwYjYxbXczSnQ5a2x2aktSNGRaSldaOGRVNFRQVGxaam1rbEVkVUJJOUhSR1AzYkh3dXJ6RUx1a2hZQ0JhOS9weWk1MTVWQ01TK0JSMXZpN1dLREhsR05EYlBDSkx2ZFdzWndmMHl4MXFFaWkvbUNIVHIwblBJbnhtZlI3Zm9rMlFyS3Zyb2lYOE9VdlRKTUZwOWU3WnYraFFvMWY5ZmtxSlBodEh0VzdSZHNXZTF2VnVUR2FUb2sySHNZUjNoMjRyUFo5dVB6NytCRkgweUROVTZlbXRqalVOcDd3dU5vMG5SSjV2Z1BiMzM0NU5uUEFsK2krQWhSWi9zZ2owN29yWUxnTXJPNjhXZWcrNjVTZEVueTVHaVQ1WWpSWjhzUjRvK1dZNFVmYkljS2Zwa09WTDB5WEtrNkpQbFNORW55NUdpVHhiamRQb2ZRNEdISnBJRzlxOEFBQUFBU1VWT1JLNUNZSUk9KTsKCQkJCQl9CQkJCgkJCQkKCQkJCQoJCQkJCgkJCQkubG9nby11bHsKCQkJCQlkaXNwbGF5OmlubGluZS1ibG9jazsKCQkJCQl3aWR0aDogOTZweDsKCQkJCQloZWlnaHQ6IDQ3cHg7CQoJCQkJCWZsb2F0OnJpZ2h0OwoJCQkJCWJhY2tncm91bmQtc2l6ZTogYXV0byAgNDdweDsKCQkJCQliYWNrZ3JvdW5kLXJlcGVhdDogbm8tcmVwZWF0OwoJCQkJCWJhY2tncm91bmQtaW1hZ2U6IHVybChkYXRhOmltYWdlL3BuZztiYXNlNjQsaVZCT1J3MEtHZ29BQUFBTlNVaEVVZ0FBQVNBQUFBQ01DQVlBQUFEY0tuU2hBQUFBQVhOU1IwSUFyczRjNlFBQUFBUm5RVTFCQUFDeGp3djhZUVVBQUFBSmNFaFpjd0FBRHNNQUFBN0RBY2R2cUdRQUFDQnRTVVJCVkhoZTdaMExrRjlWZmNldFl3cytHdXRVcGtXblVpa1VheUNBQkNWcUdCU0paUUpOWjJBU0dOU0pqeEdUNFpFZ0ZCT2NEcEtHcHdSbmJLeW1DRG9WR0d5VmltaHJzbm5zSm9GczBqd1c0dTVtODlnOHlNYThOd21iN0c1Mi82Zm5jL2JlN01uWmUvLzMzUC9yL3YvLy9ENHp2MGwyLy9lY2U4N3Y3UG4rei91OFRRbUNJR1NFQ0pBZ0NKa2hBaVFJUW1hSUFBbUNrQmtpUUlJZ1pJWUlrQ0FJbVNFQ0pBaENab2dBQ1lLUUdYVXRRTG1UQTZxM2E1L3FibTVSZTE3NGpkbzY5NGVxZGRwMzFQcEowMVh6dUNscXhVWFhxOFp6eDZzbDc3MVNMZm16MDYzeEErUE41ODNqYmpIUHQwNS8wSVFuSHVMcjNiUFB4QzhJUXVIVWxRQU45dmFwWXhzM3E5MC9mVW0xM1RGSHJibm1pNnJwdzllcXhWcFFGcDA5UmkzNmswdlVvck11VVEzdnZGUXRmdmZsYXZHZmZrd3RHVFYyaFBnWWUrOVk4em5QOFR6aFRIZ2REL0VSNzVwcnZxUmE3M2pJdk8vWTd6ZWI5d3VDNEUvTkM5REo3cVBxd01LVmF0TjlqNXZXeXRKenhtbXgwR0tqQldQeHV5N1RJbktGRVpOSWtTblVqRGhkWWVKdlFKajArM2d2Nzk5MDcrTW1QYVJMRUlUODFLUUFEZmIxcTBOTmExVGJqTGxxNWVnYlRDdUYxZ21DRU51aUtiZnA5L0orMHRHZzAwTzYybVk4Yk5KSmVnVkJHRWxOQ1ZEdm52MXF4L3puMU9xcmIxT0xSMTB4SkRydnVUeGFFREkyMG1YU3A5UFpyTk83WS83ekp2MkNJQXhURXdMVXMyV0g2cGo5bEZwK3dZU2hGZ1l0blZKM3E4cGxPcDJrbDNTVC9vN1o4MHgrQkVHb2NnRTYzdm1tYXYvbVk2cnhnMWZyQ255eGJsVjhMTHFTMTRpUmZ2S3hUT2VIZkpFL1FUaVRxVW9CNmo5NFdHMmQ4d1BWK0tIUERBa1BBOGtSRmJwV2pmeVFyOGJ6UG1QeVNYNEY0VXlrcWdRb056Q2c5cno0VzdYeTBrbW15MktteVNNcWNMMFkrU09mNUpkODV3WUdBMDhJd3BsQjFRaFFUMGVuMmpCbHBoa3ZZVllycXNLV3hCZzdHbldGNlE2Wk5UNzZmYXp6YVRoN3pMRHhjNUFPMCszVHo1ZHp6R254dTNVYTlQdGFkUDU3T3JZSEhoR0UraWQ3QVJvY1ZMdWYvYVhwampUbzFrRFVxdVNDamZVNldrQWF6dGFDb3VObXZjNWkvYnRsNTQ1WEt6NXl2VnAxNWMxcXpiVlQxYm9icDZtV3lUTzBBR2pULy9JenYrZHpudU41d2pYbzhNUmpGaklpVEtVVUpaMXY0c1lQK0FPL0NFSzlrNmtBOWUwOW9EWitaZlp3aFk2cW1HbE50MWJNeW1ValptT05nTENWWXZNRFQ2bXVuLzFLSFY2NVR2VnMzYW42RDNhYmxjdTVtSXJPNy9tYzhSbWVKeHpoaVlmNGlKZjR6YXdjNmFlVkZKV2VsR1lFVThlSFgvcjJIZ3hTSXdqMVNXWUMxTDI2UmIwMjlpWXpHRnQwUzJLVWJwMGdPbWRkWW1hWTF0NzREZFg1NURQcThLdnJWZitoN3VDTnBZVjRpWi8zckwzeGR2UGVjSnRIMGZreHduYXg4UTkrRW9SNkpSTUJZa01uM1JwVFdhTXFvS2ZSV3FBRnN2VDlWNmwxRTI5WHV4YjhYQjNmdGtzM1gzTEJteXFFZmgvdjVmMmtZNGxPanhsRUw3SlZoMy93RS80U2hIcWtzZ0trSytxMlJ4ZVkyWjlpS3FkWlpheGJHOHN2bktBMjNmOWRkYlNsdldyR1RPaTZrUjdTUmZyTW5yUWlWbXZqSnd5L1ZWeFlCYUhNVkV5QUJ2djZWUHM5ajVqZDVHWldLYUt5SlZuWTRsbngwWWxxKzd5Zm1ER2thb2Iwa2M2Vk9yMUZ0WWdZMXpwcmpHcWYrWWp4b3lEVUN4VVJvTUVUdldyajE3NXR4alVLR2g4SkJudWJ6ditjNm56aXg2ci9RRzB0M0NPOW5VODhiZEpQUGdyM3djWEdqNE1uUklTRStxRHNBalFzUG9WTnNZZmpSSzEzemxFbmRuWUZzZFltcEo5ODJQbEtaZHAvUXlMMGdQR3JJTlE2WlJVZ2pxRTRKVDVSRlNxZmpScjZ4bS8rMUszcTBMTFZRWXoxQWZraFg4WXZCUndmUWpqVEVwSmpQb1FhcDN3Q2xNdXB0cG1QR0JGSjIrVXdLNUIxeGV5WU5VK2RQUHBXRUdGOVFiN1lHYzhDeDlRcnY0TXVLZjZWZ1dtaGxpbWJBREZyWXdhY1U0b1BXeUdZUGRyMzY2VkJUUFVOK1NTLzVEdktIN0dHQ09rd1puWk1FR3FVc2doUTF3dXZxQVp6M25LNjJTNisxZGQ4YnFycTJYeG03WWZxMmJ4RDUvdkxRMTJ5Q0wvRW12WXZTeHJ3dHlEVUlpVVhvTzVWTFdyWkI4YW5tM0lPdWhSdlRQM1dHWHVXTXZsK1krcXNJUkZLMFdyRXp5eFc3RjYxSVlpcGVqbTZydFZzWlZrNzRhdXE2YSt1T1MwZi9LN3RybjlSQjVlc0NwNnVMOGhmbUZkOElBeFJVZ0ZpM1F2YkIxTE44T2pLUnZlRFErVnovV2Yyb09xZ3pqOStNTjJ4RkNLRXYvRjd0YTZMUWxUc0NwaGtQSXRZMVJNaVFOR1VUb0J5T2JPQjBndzZXMzlNZVEzeDBaVm55MFB6WlRBMVJQc0JmeGdSVHlGQ1pucGUrNy9hL0xqdGtRV1I2VTB5V2toZFAvM3ZJSmJhUndRb21wSUowSnZQL0NKZHBkSFBNWWhxeEVjWUFYNUpOWWdmaURubFVDM1FwWExUU1VYYythL1BuZGJDNlgzekQwWnNObjU1MW9qbjY2VkxKZ0lVVFVrRTZLMzJiYXJ4dk0rbUd2Zmg3QnU2RzlMeWlVSDdCZit3bHl6S2YxR0cvNXQwT1ZBZVdZT2cyR21qUmJQM3BZYmcwM2dJWjQ4UHNZMEZnYXAxUklDaUtWNkFkRVV4SnhtbW1NRVpHbkNlcFhMOUo0TklSbkprN1VhMTcrVWxhdDl2bHNYYkswdlYvdjl0VWdNOXg0TlFJK0U0aTMyL1RvN253TUlWYXZCNGRhMHV4ajhNektlWkhlTlpUbGJNVXRnUkRGdEUrSCthbG93clh2VlFZVVdBb2lsYWdEZ3FndU5FZmJzS0RMQXkxWjQwMjlWeTZ6ZlZ3ai82dTZFalUrT01Mc3I3eDZtZUxUdURVQ05aTjJtNld2ajIvUEhRMVdFbTZjU082dHZxZ1o4NG5kRjduUkJkTVoybkxJL3dvSUxaYWZKcCtiallGWlpXVUswakFoUk5VUUxFSnN0WHgwd3laeHFIenMxbnJQaGwwUjNuUHlmeCtoZit5ZXdBajRybmxJMjZ3Z2pIOGEzeEFyVCtwanNUS3krM1ZIQUR4NG1kZTRKUTFRWG5SQysvOFBQZUs2WXBEdzY2ejJyVExvSVJwb1dLVndpSUZ1R2JQekhaakNYbDY0YnhHZU5LRzNSWjIrL0crSm5mODNrK3d2ZGhvVUF3Z083bUpXcGduUEVzMGtoYXcyY0pSenhodWtXQW9pbEtnTGhTeHJ0N01HcXMyWGF3OStYRlFlajhWRnlBenF0ZUFRSzZvL2pQZCs4WTVVTDVWQnE2V25ZNmtpcCtQbnpHZnR3eG8zeUdLTVIxQlYwQnd1eXdvYm41aVhzdXRMRDdLUUlVVGNFQ1JLV24wdnBlblVPRllHK1hMeUpBSSttWTlhUzM0Rk11NU9uNDFsMUI2TXJnVHJ1WGN6MFBZbUMveThjUWhDaGhzd1dJRnBNZEpqUTNiTlFzWDVRUnptNGRpUUFOVTdBQXRkL3pxSzRNZm10K21CNW05L2ZKSThlQzBNbUlBSTBFLytGSHM5d2hJaCt1VVQ2VVV5VnhLMlc1UUFqc2xnK3RHd1FwYW5yZmJuMWdpS1NMTFVDaElVUmhmUHhyZDcvYzUwa0w3dzhGaW4vNTJVNWphQ0pBd3hRa1FJeEpjRjB5RmRkMTdnaWp5L0MrajZ1RFM5T3Q1eEFCaWdZLzRrK2ZyaGo1NHJEOFN0NDFabGQyL2w4dTdKYVcyektKd2s0WHd1TGlDZ3FDbGc4N1BwNk5leisvdDFzL21BalFNQVVKRUYwcDM2NEF6N1hkTVNjSTZZOElVRHl0MnA5cC9KK202MXNzbFJJZyt6MCtGZHJ1cmtXbHl4V2dxRlpTQ0swaCsxbkM1c09OV3dSb21OUUMxTnUxVHkyL1lJTFhva09lV1g3K2RlcjQ5dDFCYUg5RWdPTEJueHp2NmwwR3Vyd290MHBRS1FGS2l5MENQZ0tVYjkyUzNmcWlkZU9EakFGRmsxcUFkc3gvem50MUx0KytuWTgvSFlSTWh3aFFmdkNyZHl0SWx4ZmxWZ21xUllCb3BTQXFWSGE2WFBaWWpJOEE1Y1BlTXNLWWx3K2tJd3dqQWpSTUtnSEs5ZldyNXZHM21ZVnVvVFBqakt0b3VMMmliLytoSUhRNlJJRHkwN2Z2b1BHdno1VS9sTmRxWFc2VVg3bXhLeHBXQ1dpdDhGNkVoZkVZKy8xUlZxd0EyU0xyS3laMkYxQUVhSmhVQW5Tb2NmWFFGY1FlcTU3NTF1MmM5MndRTWowaVFNbHNmL0padjlhb0xpL0tqZklyTjNaRncreFpxYlFRTnQvZ3NydSt4dGV5RUNBN2ZoR2dZVklKVU52ZGM3MmEvV2JjNGNJSnFuZFA0ZU1PSWtESm1QRTQ3V2Vmc1NES3JlM3VoNE9RNWNNZG9NMDNtSnRFT0tYUCtJa2JEK0lUTmNVZEdtRVFDaW83MCtkWStKa0lVUFhnTFVBbkR4OVJLMGZmNE5YazU0OTkwLzFQQkNFTFF3VElEMjVnOWZ0U3VOeVVIK1ZZYnV3QlY5OUIyaWhzZ1hISFdtd1I0RGsrcDVMSHRaaEtLVUQyR0JELzl3SFJDY09JQUEzakxVRHNGbTlnTDFMUzNWNmp4cHE3Mm8rdWJ3dENGb1lJa0I5SDE3Y2FmeWV1QzlMbGhnaFJqdVhHN1liWkMvaDhzV2VhTUxzcjU3YXlFSThrYkFFb1ZvRHN0Q1d0RndxeFYxZUxBQTNqTFVDYjduM002OGdOVnVtdW5YaDdFS3B3UklBOHllbld3TVN2ZTYyT05pMVRYWTdsaGxhSVBSaE1DeVhmdExhTDI3MXlCU09OV0lDYm5tSUZ5QlhBSklGMW54Y0JHc1pMZ0xnQXIzbmNMV3F4eCt4WHcxbVhxRjAvZWpFSVdUZ2lRUDdzV3ZDaTEyQTA1ZGM4YmtwRkxqUzB1endZZ3VMVFV1RVpXM3o0djkzNkFWY3MzTTl0b2xZaVI3VmEwb3FhM2FMSko3QlI3eGNCR3NaTGdJNXQzS3lXbmpNdXNablBUQXRiTlBJSmdpOGlRUDZRZjdaY21CbktpUHlkTXJySHVoeVBiZXdJUXBhWHFDTldhWDNRUmJORmcvOGpXSHptUHMrekxsUnEreGtFaGVmNGZRaUNRRVczeGN3Mmw3UUNSSnBkb2VSOVliNUlDMm15VzE2aGlRQU40eVZBdS9VZng2SWtNZERXY1BhbGF0ME54WGUvb05vRWFOdGovNjdXLzhOMHRlSG11K1B0cHJ0MHV1L0w1SGFLZFRkOHcvZy9LbisyNFZQS3MxSkVpWkN2UllsUGlMdnBOYTNaWWdWcEJRaElueDBtemhBbjJ3OGlRTU40Q1pEWmUrVFJ4R2VNb2ZPN3p3U2hpcVBhQkdqRHpYZXBoVys3eU1RVlorWjBnRC8vaERyZStXWVFxbkpzMTM3M21RMmpIQ25QU2tMckpxb2xFR2UwaEh6R2pIekZqZmhjZ1hISGJRb1JJQ0NldUZZV3htY0lGYUlUL2s0RWFKaEVBY29OREtvMTEzd3hlZlV6aTkyMEhYNTFYUkN5T0twTmdEZ2lOaWtlazU0UDZQUVVzUGV0V1BDN09iQXNZWkVvNVVoNTVnWUdncENWZzhwS3l5V3FxOFh2d3FuME5QQTg0ekd1d0lYeDJVSm1DeGIvdHlsVWdJRFdGRE5qZHI1SUQrOEl1MlFpUU5Fa0NoQ0wzWm8rZksycHBLRURvNHpGY0NzK2NyM3FQOWdkaEN3T0VhQjA0SGY4bjdRb2tieFNuc1VzRWhXRVVwRW9RTjNOTFdveGY3eEozNnp2dk5RY0FGOHFSSURTczE3N1AzRTYzclJVcnpUbEtnaFpreWhBNXRhTHBJcW5qZkdIamhJMkxVV0EwclA1Z2UvNXJkWFMrY2p5MWd4QkNFa1VvSzF6LzgxemNIT002dnJaeTBHbzRoRUJTZy8rVC9TWk5zcHo2OXdmQnFFRUlUc1NCYWgxMm9QSk0yQ21XVDlXSFY1Wm1nRm9FQUZLRC83M0dZZzJNMkc2WEFVaGF4SUZ5R3RjSVJDQ25qeENrQllSb1BUMGJObWgvZkhweEFXSmxDZmxLZ2haa3loQUxOMVB1aEJ2ZUFhc2RCZmhpUUNsQi85N3pZVHA4bVJyalNCa1RhSUFyYmhJLzBFbjNQM0ZIL1JyVjk2c0JudjdnbERGSXdLVUh2eS9TcGRENGhlR0xrL0tWUkN5SmxHQUduV0Y4bHJjZHUxVVhRTUdnMURGSXdKVUFOci81aDU1ajBXamxLc2daRTJpQUVYK0FUdG0xZ0RkT0MwSVVScEVnQXFEY2tnY3N3dE1FTEttTkFLa0syYkw1SmxCaU5JZ0FsUVlsRU5pT2dNVGhLd3BuUUJORVFHcUNnSFM1U0FDSk5RSzBnTFNKaTBnUWNpR09oa0QyaFdFR29tTUFjV2JJR1JOb2dCbE5RdjJ4cGZ1VHhRZ0Z0d1pBZHBXQWdINkVBTFVGWVFhU1UwSWtNeUNDVFZHb2dCbHRRNm83VTZQUTlCMFJUSmJRRmFzRFVLZGpqbkwrcE8zSnA1bFRmNjRQNTNiUnVPb0JRR1NkVUJDclpFb1FGbXRoT1prUmQ5TnNHMTNSdC9QZlhCcHMxcnl2bzhudHVBV3Yvc3l0ZXFxeVZxdzRnVzBGZ1RJKzB3Z1haNnlFbHFvQmhJRktLdTlZUHQvdDN6b0hyS285OWtXaU12T0h6eXZjaWVIVC9rNzl2b205ZHBZV2dOK04zbThNZlZiUWNob2FrR0E4RC9sUURvaTB4Y1k1Vm5PdldEdXlZRHUrY3MrMk9sTmUwcGlzWkJlbnlOaDZ4bjdCRWZLczF3a0NsQld1K0g3OWg5U3kvLzI4NG5mNXNaMGhlUFN2ZFZYZjBIOS92Wi9OZ2ZFTThiUjRDRStHUG5iL1pPWGdqZEhVd3NDNUxzYkhzRnRuVjYrM2ZDMkFHRWNqWm9XTzN3bEJZaWpWY01iTHM1a3FrYUFzam9QQ05ydTRpNzZpeVBmTjhLNCtmTmRseGt4TVVLUjBBb0lEWUZiZnNGMXFuZlAvdUN0MGRTQ0FIWDl4OHVtSENMVFpsbTV6d055QlFoTEt5TEZoQzBVdWIxMG1Lb1JvSFFuSW40dkNGVWF1STlzMlY5K09uRVF2QmhiOU1jWHE2MXpmaEM4TVo1YUVLQ08yVTk1ZlZtUWozS2VpQmdsUUdtN1luYllTZ21Rblc0Um9Db1JvS3pPaEE3cGZQenBvVlpRMHAzMEJSZ1ZjZFVuYjFIOWg0OEViNHVuRmdRSS95ZU8xNWt1V25uUGhJNFNJQ3hOVjh3T0p3SlVlYXBHZ0xLNkZTTWsxMzlTYmZ6cUE3cWxNanBSQk5NWUZYWDUzMXluam01b0M5NlVuMm9Yb0dxNkZTTk9nREJmTVNra1RMR0lBQTFUTlFMRS9WR3JNN2dYekdhZzU0UnFuZllkTFFDWEppNEpTRFNkUnJwZEswZmZvTHBYdng2OElabHFGNkJxdWhmTXJzaTBldXlMKzN5N1l1SHptQWhRNWFrYUFZTFdPeDVLbmduVHh2aERxVzVHZGNrTkRxbzNuL21GV243aDU0MkFHQ0ZLcUd5MjhjM1A3QStycDkrWU9rdWQyQlcvN1NLS2FoY2cvM1ZUNWI4WjFhM0lYRWhvcDhHbksyWS83eU5BUE1ORmdGRVhGREt6RlNkNmhMT2Zqekx5UUJ6aHorNmxoaTUyNWNYeXBkLzJUZk1uSmdlL0hVbVlQNTZ4NDA3S1g0aWR6MUJjQ1dmN2k3akNHMk45QllqbEN2WVhUSkp2WEx3RUtJdTc0ZVBvN2RwckhQZmEySnZNTno2VkRtRnBlT2RsWmhZTVlUS20vMDk2ek9kYU9CcTFrMTYvN1Q1MXNIR055dVZ5UVd6K1ZMc0FyZFYrVHh6LzBjYjJsbkxmRGU4S0VOZ3pURmkrU2dtK3oxTHg3UGZGV1hoRnNvdGRNZU9NUEZEUndwK0pLeDl1ZWtJZlJFR0ZEWi9qNzlxbDJQeUZ1QUprQzR4dFlSejI1M0VDVkt6NGdKY0FNUnUxOUp4eHVvTGxiM0hRdW1qODROVjVkNmVYaW9HZTQ3cmJzVjUxem52V2pCSDluM2JTcXF1bXFGY3YrMGV6QUhIMStOdk1ILzJtKzcrci92Q2YvNU4zbjVjUDFTeEErSHVaOW52U1lmU1VIK1Y0YkdOSEVMSThSQWtRRmNuK1kwM3Fpb1hQWVhFQ1JIaTNSWkJrYmlYMUZTQ3dXd3R4Q3hWSmt4MFdpNnZBWVB2RWpiTVUrUXV4OCtsK0dZUkdXc0l5U1JLZ1VvZ1BlQW1RMlZNMTdwYkVQVlVZclpGZFAzb3hDRmxaY2pxZEE4ZDYxT0R4RTFxaFNyY3hGcXBaZ0hZdGVOR3JpMHo1c2JXRzhpd25VUUlFYWJwaTluTnhBbVJYVGlvRDhkblBVcGw0cDUyZWZQSEZwVHVFK1BOOURtNGVNZElXaGQycVF0eGMzSFR6YzloRkF1NmRKeDIyRUdCUitiTUZLRFNFS0x5N25uL3R1UE1KVUtuRUI3d0VDRGJkKzVqZnJadTZHN0Iyb3U2R3BlL2xWRFZWSzBDNk83bDI0dGU5dWw5MFJ5bkhjcE92SXZ0MnhaS2U0WnMrL0p6S0VOY2lDYkc3T2xHVkhaSUV5SzdFY2VNMXRrZ2x0WmpzU3U2S3NaMC9MRjhsZHdVaEtuK3VBTVg1SUNST2dOeFdXVEhpQTk0Q2RHRGhpcUc5V1VucmNlaW12ZjhxZFhTOTMvUjJyVkN0QW5SMGZhdGFxdjJkMUQwMks4WGZjN2tweDNLVHJ5THpCNXhVV1NEOEhJc1NJTHNTMk4vYytVZ1NoQ1FCQWp2dFlYZkZKa3dYNzdKRkwycDh4ODZEbTBkYnFHMEJpTU1WR0RkLzd1ZFI2YkdKRXFCU2l3OTRDOURKdzBmTTFEVi94R0VDNHN4ODA5Ny9SQkN5UHFoV0FXS015MmYyaTNLai9DakhjcE5Va2Qxdjk2aG43TS9keWtsM3dmN2NGN3RTUmIzVFI0QnNVWEhIVzZpZzRXYzhaK2ZUcmF4MkhoQTFsL0F6ekZkZ2JYRncwNThrVUM2MnIvQkxPY1FIdkFVSTJ1NW1iNWJQSC92SDFQSUxKNWhGalBWQ05Rb1Evc1hQUGh0MktiZTJ1eDhPUXBZWG40cHNQNE81RmNMK3pCVWd0eklWWXJRd1hIelNiWS94dUpYUS9nenhJVS9oejI1TEw1ODR1Zm56eFJVTm03UngybkVoUExiNFlISCtTVXNxQVRyVXVIcG9wc1ZqL1EyRG9zeFExUXRwQk9qRWpzb0kwUFlubi9VYWZEYUxSSFhhS0w5SzRGT1JhUUhZM1JuK3dHM0MzMk91QU5GOXNEOHZ4TndLQ2o3cHRsczVycWpZNHovaDRLNmR4L0IzWUhleDNKWlV0UWxRbkNXMW9ueElKVURNTWpXUHZ5MTVWYlEybXZ3cmRBRnhyRVk5NENkQVk5V3l2L2lVMnZ0ZnYxUGRyMjVRaDVldlRiUkRqV3ZNbmU1cDRmUkdLb0RQeW5ES2kyVUpsRjhsOEtuSTRBcUovYXo5ZTFlQWZDcEhraFVxUUdDTGgxMEp3MWFDTGFaMm5MYlE1QnRMcWxZQklsLzJPSnI3cFZFSXFRUUlkc3gvenU5YlZ4dk5mamFUMWdOZUFoUVlyUTFXWHZ2WXduZU1WdTB6SHduZTRzL1FKbDNQY3REbFJibFZDdCtLRFBheldGaWg3ZC9sRXlDM29oV0RiN3J0N2xNNG1HdVA2ZGd6V25aYXc5L2JZaERWRmF4R0FVSnNFRW8zbm54KzhpRzFBSmx4aHd2OHhoM01XTkQ1MTFWOFhVdzVTQ05BcG92cWFXd3JhWi81YVBBV1AvQm4wL21mOHk4RFhWNlZISTlMSTBCeFhiSHdaOHdWb0VJcmFCSys2YmJGSnF6b3RpalpnOFoyV3NPODJWMjFxTmtvTjM5dUN5a09POTVTQ2hDdEhqc05qRm1GbjFGMmR0Y3lMYWtGQ0RwbXpmUC85bVh3czh4N2p5cEJLZ0ZLWWZpbi9aNTBBb1EvMC9pL1kvYThJR1JsU0NOQUVOVVZzMzkyQmNnV2dLalBDeVZOdWhHVDhGa3FwMTBwWGNFSWZ4OStabmRqNGlxdkhhWWNzMkJKMkdYZ2lobDVzTDgwM00vVFVKQUE5WFJzSDFyNnI3c1FZU0ppamZVcDcvdTRPcmkwK0FHckxLa1dBY0tQNXFEOXBIVS8yaWdmdHNiMGJONGVoSzRNYVFVSTdEQ3VSUW1NWFlsOUswQlN1dEtrMjY2Z0NFU1luckNWWTJQSGE3ZVVvcDROS1hZZGtDdGFwUlFnc1BPQlJiWGtmQ2hJZ0lCSzQzdGNLcXQwbXo5MXF6cDU1RmdRdXZhb0JnSENmL2pSWjlVelJ2bWtiVjJWZ2pRVk9jVHRpdGtXSlVCMkJjR1MxcVc0cmF5b2xrZWFkTnRUN0xaWTJPTS9JWGJYeU01anZuZTRGVHhmL2tpTEhTOWk2RkpxQVFMYlg3emZiZm41VUxBQWNSc3BsL241SHBkcXVnSzY2MWFyVklNQXBlbjZVaTdjOXBydjF0aHlVWWdBZ1NzU29VVUpFSC9zZHBjRDQ3MTg4OXNWZ1ovdDlHQlJJZ0gyYy93L3FVTFpyYkRRb3JwTC9NNTlEa3VheG5iVEhlWXZCQkhGdjY1d1I2V2hIQUxrZG9XakJ0U1RLRmlBZ0xPVWZTc0VYUWFPejlqNzh1SWdkRzJSdFFEdGUzbkowSUZqSGwwdmpIaDl6cm91QjNiRlNTTkE0Rlk2TEVxQXdQM205ekZFSzA1WTdKYUtiWEVWSytyNXFMajVuZnRjVkN2RmhYQlJJcGZQNHZ4ZERnRUMremtzU3Z6eVVaUUE5Ujg0ckZhT21hUjg3dDdDV0xQQ2dXSTlIWjFCRExWRGxnTEVtQnQrODFuemcxRWVLeStkWk1vbkM0b1JvS2l1V0p3QUFjOUhpVmFVSVJqNVdqVnhnaFpYQWQyV1RiNks2cmJXNGxwaExxVFhKMytrbTI1YkhPVVNJTEJGa25Uazg3RkxVUUlFZTE1NFpXaGhJdC9PUVNMeUdaV1krOHRQZGg4TllxZ05XaWJQVkl2ZU1kcE1hNWZTRnI3OW82cDlSdndXQ2Z4azdudjNGVDlkRHBSSE9XKzlTTUt1TUdrRkNOeXVXRDRCQ3VFWnhrbmNpczdQVlBhazdrNEl6OWxqT21FY2NkaUNsUyt2Ym1zcGJVc2h6SjliMmZFMS9rcXE5T1VVSURkdVgzR0ZvZ1dJNHlBMlRKbnBkVlJIYUh6cmN5d3FCODdYQ3IrZjlxQnErdXZQcWhVWC9YMUpyZWxEbjFGYkh2eCs4SmJUd1QvNHlidWJxNDFuS1EvS1JSQ3FuZUlGU1BOVyt6YlZlTjVuelRkNlZLV0lNaXJLcHZzZXI1bUtjdkxvVzJaYlNaL3UxcFRVOWg5VUo5L3FDZDVpb2YyQ2YzeFhuV1A0djBtWEErVWhDTFZBU1FRSU9ERGVUQTk3ZHNYTUttRGRyZGp5MFB3Z0JzRUd2K0NmTlA3RS81U0RJTlFLSlJNZ3ZyRTNmbVcyYnRsNFhxV01CWlhHaUpCMEdZYlFmc0FmcWNSY0czN0gvK0pIb1pZb25RQnArdlllTUxkVitDNlVNNFlJNlc5NnVodTUvc3JzMXE1V0JuWCs4WU1aY0U0aFB2Z2J2K04vUWFnbFNpcEEwTDJxeFp5SmsyWThpTW9XRGt6WDJ1eFlxU0RmcHdhY1U0Z1BmbDUyN25qdDl3MUJUSUpRTzVSY2dLRHJoVmVHVmtoemVGbEVwWWt6S2g5VHpwWGV1NVExNUpkOEcvR0o4RXVzY2V5SEZxQ3U1MThKWWhLRTJxSXNBZ1RiSGwyUWJoQTFNTG9mSERPNjc5ZExnNWpxRy9KSmZyM1grWVJHcTFHSHdjK0NVS3VVVFlBWURHMmIrWWdaSEUwclF1Wm0wMUZqemQ0bnByL3JFZkxGTVJsc3IvQmQ0WHpLRUIvZFdzSy9NdWdzMURMbEV5QU5GK0J0L05xMzAzY3RNQzFBaUJlN3Z3OHRxOHhaeHBXQy9KQXY0eGZQdlYyMkVRNi9sdnVDUVVFb04yVVZJQmc4MFRzc1FrbDNpa1ZZT0tQV2V1ZWNvcTlYemhyU1R6N3NmS1V5N1Q4ejNmNjFCNHhmQmFIV0tic0F3ZWtpbFA0YmY2ZzFkSWs1aHJUemlSOW50c215VUVodjV4TlBtL1FYN0FQVDdVSjhkTXVudHkrSVdSQnFtNG9JRUF6MjlhbjJleDRaR3BoT09Uc1dHak0rVkdCdTI5Zys3eWRWdis2RjlKRk8wa3U2VXkxTnNFMzdxK0dzTWVid2V2d29DUFZDeFFUSWtNdVpXUnVtNkF1dWpOcTQ4b2M5VXN3ZWNUUG8wWloybFJzY0RGNlNMYVNEOUpBdTBrYzZmVzZUalRQOGhKblpMaGx3RnVxTXlncFFBRWRGc0hpdW9IRVF5OElXRVhmUnI1dDR1OXExNE9mcStMWmRsYStvK24yOGwvZVREdEpUVklzbk1QeURuN0k4V2tNUXlra21BZ1RkelMxbSswQWgwL1FqYk5UUW5qSmFHeHlXdi9iR2I2ak9KNTlSaDE5ZHIvb1BkUWR2TEMzOWg0NlkrSGtQNytPOXZOK0lhZ0V6VzZkWk1ONkRmN3BYdHdSdkZJVDZJek1CZ3I2OUI4MEdTaXB0c2EyRlU4WjRDV0lVRFBhdStNajFhdjJrNmFwajlsT3E2MmUvVW9kWHJsTTlXM2VxL29QZFpqQTNydXZHNy9tYzUzcTI3RFRoQ005QlRjUkh2RU5DTVNRNjVzcnFxUFNrTlB4QWZQZ0Yvd2hDUFpPcEFCbDBSZC85N0MvTkFlcm1VTE1DcHVwalRRdEVXS0VSaWtWbmpURUwvK2pXSUNDcnJyelpiSUZZZCtNMDFUSjVobXFab2szL3k4Lzhuczk1anVjSngwQXdhVHdsbVBwM2tlOHR4SmhpMXkwby9JQS84SXNnMUR2WkMxQUE1MFNia3hYZmRWbjZsY0ZwRE5FSTlsRHhIdDZIb0xBVjRwVHhjNUFPSXpTMGJrb3BObzV4aGpQdmE5SDVyOFh6c2dXaFVLcEdnQ0EzTUtEMnZQaGJjNkM2R2NUMXZQS25WbzM4a1UveVM3NXpBOUxxRWM0c3FrcUFRdm9QSGpaWHluRHZHSU94WGpldzFwQ1JIL0xWcEx0YjVKUDhDc0taU0ZVS1VNanh6amRWK3pjZk05Y0xHeUdpT3hSUm9XdkZTRC81WU1hTWZKRS9RVGlUcVdvQkN1blpzc1BNWWkyL1lJTHBzakJlVXM0eG1aSWFnOWM2dmFTYjlMTURudndJZ2xBakFoVFN1MmUvMmpIL09kVjg5VzFtMnR1TUV4V3h5cmljWmxacmt6NmRUdEs3WS83ekp2MkNJQXhUVXdJVXdqRVVoNXJXcUxZWmM5WEswVGVZMlNwVDJXa1pGYnNJc0ZEVDcrWDlwb1dtMDBPNjJtWThiTklweDJZSVFqUTFLVUEybktWOFlPRktjNWg3ODdoYjFOSnp4cG4xUG1ZUGxoWUVNNEJkNnU2YWpvOTRpYjlCdjRmMzhWN2V2K25leDAxNnp0U3pyUVVoRFRVdlFEYXNYRDYyY2JQYS9kT1hWTnNkYzlTYWE3Nm9tajU4clZxc1JZTmQrTFJPd3UwU1pvMlBPYmM2UnB5TXlBUnJoVmpJaU5BUVhzZERmTVM3NXBvdnFkWTdIakx2NDcxeVRJWWdwS091Qk1nbGQzSkE5WGJ0TS92TzJOQzVkZTRQVmV1MDc1aXRGTTNqcHFnVkYxMnZHczhkcjhWbXBBRHhlejZuVmNQenJkTWZOT0dKaC9oNjkrd3o4UXVDVURoMUxVQ0NJRlEzSWtDQ0lHU0dDSkFnQ0praEFpUUlRbWFJQUFtQ2tCa2lRSUlnWklZSWtDQUltU0VDSkFoQ1pvZ0FDWUtRR1NKQWdpQmtoZ2lRSUFpWklRSWtDRUptaUFBSmdwQVJTdjAvQm1xQ0VURHcwbUVBQUFBQVNVVk9SSzVDWUlJPSk7CgkJCQl9CgogICAgPC9zdHlsZT4KPC9oZWFkPgo8Ym9keSBjbGFzcz0iIj4KPGRpdiBpZD0icGFnZS13cmFwcGVyIj4KICAgIDwhLS0gSGVhZGVyIFNlY3Rpb24gLS0-CiAgICA8aGVhZGVyIGlkPSJoZWFkZXIiIGNsYXNzPSJjYW52YXMiPgogICAgICAgIDxzcGFuIGNsYXNzPSJsb2dvLXVsIj48L3NwYW4-CiAgICAgICAgPHNwYW4gY2xhc3M9ImxvZ28tYmFuayI-PC9zcGFuPgogICAgPC9oZWFkZXI-CgogICAgPCEtLSBNYWluIENvbnRlbnQgU2VjdGlvbiAtLT4KICAgIDxzZWN0aW9uIGlkPSJjb250ZW50IiBjbGFzcz0iY2FudmFzIj4KICAgICAgICA8ZGl2IGNsYXNzPSJyb3ciPgogICAgICAgICAgICA8aDI-UGF5bWVudCBTZWN1cml0eTwvaDI-CgogICAgICAgICAgICA8cD5UYXAgY29udGludWUgb25jZSB5b3UgaGF2ZSB2ZXJpZmllZCB0aGlzIHBheW1lbnQuPC9wPgogICAgICAgIDwvZGl2PgoKICAgICAgICA8ZGl2IGNsYXNzPSJyb3ciPgogICAgICAgICAgICA8Zm9ybSBhY3Rpb249IkhUVFBTOi8vRU1WM0RTL2NoYWxsZW5nZSIgbWV0aG9kPSJwb3N0IiBuYW1lPSJjYXJkaG9sZGVySW5wdXQiPgogICAgICAgICAgICAgICAgPGlucHV0IHR5cGU9InN1Ym1pdCIgY2xhc3M9ImJ1dHRvbiBwcmltYXJ5IiB2YWx1ZT0iQ09OVElOVUUiPgogICAgICAgICAgICA8L2Zvcm0-CiAgICAgICAgPC9kaXY-CiAgICA8L3NlY3Rpb24-CgogICAgPCEtLSBIZWxwIFNlY3Rpb24gLS0-CiAgICA8c2VjdGlvbiBpZD0iaGVscCIgY2xhc3M9ImNvbnRhaW5lciI-CiAgICAgICAgPGRpdiBjbGFzcz0icm93Ij4KICAgICAgICAgICAgPGRldGFpbHM-CiAgICAgICAgICAgICAgICDCoMKgwqDCoMKgwqDCoMKgwqDCoMKgwqAKICAgICAgICAgICAgICAgIDxzdW1tYXJ5PkxlYXJuIG1vcmUgYWJvdXQgYXV0aGVudGljYXRpb248L3N1bW1hcnk-CiAgICAgICAgICAgICAgICA8cCBjbGFzcz0ic3VtbWFyeS1wIj5BdXRoZW50aWNhdGlvbiBpbmZvcm1hdGlvbiB3aWxsIGJlIGRpc3BsYXllZCBoZXJlLjwvcD4KICAgICAgICAgICAgICAgIMKgwqDCoMKgwqDCoMKgIMKgCiAgICAgICAgICAgIDwvZGV0YWlscz4KICAgICAgICA8L2Rpdj4KICAgICAgICA8ZGl2IGNsYXNzPSJyb3cgZGV0YWlsZWQiPgogICAgICAgICAgICA8ZGV0YWlscz4KICAgICAgICAgICAgICAgIMKgwqDCoMKgwqDCoMKgwqDCoMKgwqDCoAogICAgICAgICAgICAgICAgPHN1bW1hcnk-TmVlZCBzb21lIGhlbHA_PC9zdW1tYXJ5PgogICAgICAgICAgICAgICAgPHAgY2xhc3M9InN1bW1hcnktcCI-SGVscCBjb250ZW50IHdpbGwgYmUgZGlzcGxheWVkIGhlcmUuPC9wPgogICAgICAgICAgICAgICAgwqDCoMKgwqDCoMKgwqAgwqAKICAgICAgICAgICAgPC9kZXRhaWxzPgogICAgICAgIDwvZGl2PgogICAgPC9zZWN0aW9uPgoKICAgIDwhLS0gRm9vdGVyIC0tPgogICAgPGZvb3RlciBpZD0iZm9vdGVyIj4KICAgICAgICA8dWwgY2xhc3M9ImNvcHlyaWdodCI-CiAgICAgICAgICAgIDxsaT4mY29weTsgVUwgLiBBbGwgcmlnaHRzIHJlc2VydmVkLjwvbGk-CiAgICAgICAgICAgIDwvbGk-CiAgICAgICAgPC91bD4KICAgIDwvZm9vdGVyPgo8L2Rpdgo8L2JvZHk-CjwvaHRtbD4"

        private val HTML_CRES_JSON = JSONObject(
            """
            {
                "messageType": "CRes",
                "acsTransID": "${UUID.randomUUID()}",
                "threeDSServerTransID": "${UUID.randomUUID()}",
                "acsHTML": "$ENCODED_HTML",
                "acsHTMLRefresh": "$ENCODED_HTML_REFRESH",
                "acsUiType": "05",
                "challengeCompletionInd": "N",
                "challengeInfoHeader": "Header",
                "challengeInfoLabel": "Label",
                "challengeInfoText": "Text",
                "shouldShowChallengeInfoTextIndicator": "N",
                "challengeSelectInfo": [{
                    "phone": "Mobile **** **** 321"
                }, {
                    "mail": "Email a*******g**@g***.com"
                }],
                "expandInfoLabel": "Expand Label",
                "expandInfoText": "Expand Text",
                "issuerImage": {
                    "medium": "http:\/\/acs.com\/medium_image.png",
                    "high": "http:\/\/acs.com\/high_image.png",
                    "extraHigh": "http:\/\/acs.com\/extraHigh_image.png"
                },
                "messageVersion": "2.1.0",
                "oobAppURL": "bank:\/\/deeplink",
                "oobAppLabel": "Click here to open Your Bank App",
                "oobContinueLabel": "Continue",
                "psImage": {
                    "medium": "http:\/\/ds.com\/medium_image.png",
                    "high": "http:\/\/ds.com\/high_image.png",
                    "extraHigh": "http:\/\/ds.com\/extraHigh_image.png"
                },
                "resendInformationLabel": "Resend Information?",
                "sdkTransID": "${UUID.randomUUID()}",
                "submitAuthenticationLabel": "Submit",
                "whitelistingInfoText": "Would you like to add this Merchant to your whitelist?",
                "whyInfoLabel": "Why Info Label",
                "whyInfoText": "Why Info Text"
            }
            """.trimIndent()
        )

        private val CRES_JSON_WITH_INVALID_CHALLENGE_COMPLETION_IND = JSONObject(
            """
            {
                "challengeCompletionInd": "55"
            }
            """.trimIndent()
        )

        private val CRES_JSON_WITH_INVALID_RESEND_INFORMATION_LABEL = JSONObject(
            """
            {
                "resendInformationLabel": ""
            }
            """.trimIndent()
        )

        private val CRES_JSON_WITH_INVALID_CHALLENGE_INFO_TEXT_INDICATOR = JSONObject(
            """
            {
                "challengeInfoTextIndicator": ""
            }
            """.trimIndent()
        )

        private val CRES_JSON_WITH_INVALID_CHALLENGE_SELECT_INFO = JSONObject(
            """
            {
                "challengeSelectInfo": "12345"
            }
            """.trimIndent()
        )

        private val CRES_JSON_WITH_INVALID_ACS_TRANS_ID = JSONObject(
            """
            {
                "acsTransID": "1-2-3-4"
            }
            """.trimIndent()
        )

        private val CRES_JSON_WITH_INVALID_UI_TYPE = JSONObject(
            """
            {
                "acsUiType": "1597"
            }
            """.trimIndent()
        )
    }
}
