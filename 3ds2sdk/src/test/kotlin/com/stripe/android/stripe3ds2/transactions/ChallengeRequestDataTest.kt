package com.stripe.android.stripe3ds2.transactions

import com.google.common.truth.Truth.assertThat
import com.stripe.android.stripe3ds2.ChallengeMessageFixtures
import com.stripe.android.stripe3ds2.transaction.SdkTransactionId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ChallengeRequestDataTest {

    @Test
    fun toJsonObject_returnsExpectedObject() {
        val messageExtension = MessageExtensionTest.FIXTURE
        val creqData = ChallengeRequestData(
            acsTransId = ChallengeMessageFixtures.ACS_TRANS_ID,
            threeDsServerTransId = ChallengeMessageFixtures.SERVER_TRANS_ID,
            sdkTransId = ChallengeMessageFixtures.SDK_TRANS_ID,
            messageVersion = ChallengeMessageFixtures.MESSAGE_VERSION_210,
            cancelReason = ChallengeRequestData.CancelReason.UserSelected,
            messageExtensions = listOf(messageExtension, messageExtension),
            shouldResendChallenge = false,
            threeDSRequestorAppURL = ChallengeMessageFixtures.THREE_DS_APP_URL
        )

        val creqJson = creqData.toJson()

        assertEquals(
            ChallengeMessageFixtures.SERVER_TRANS_ID,
            creqJson.getString(ChallengeRequestData.FIELD_3DS_SERVER_TRANS_ID)
        )
        assertEquals(
            ChallengeMessageFixtures.ACS_TRANS_ID,
            creqJson.getString(ChallengeRequestData.FIELD_ACS_TRANS_ID)
        )
        assertEquals(
            "01",
            creqJson.getString(ChallengeRequestData.FIELD_CHALLENGE_CANCEL)
        )

        assertThat(
            SdkTransactionId(creqJson.getString(ChallengeRequestData.FIELD_SDK_TRANS_ID))
        ).isEqualTo(ChallengeMessageFixtures.SDK_TRANS_ID)

        assertEquals(
            ChallengeRequestData.MESSAGE_TYPE,
            creqJson.getString(ChallengeRequestData.FIELD_MESSAGE_TYPE)
        )
        assertEquals(
            RESEND_CHALLENGE,
            creqJson.getString(ChallengeRequestData.FIELD_RESEND_CHALLENGE)
        )
        assertFalse(creqJson.has(ChallengeRequestData.FIELD_CHALLENGE_DATA_ENTRY))
        assertFalse(creqJson.has(ChallengeRequestData.FIELD_CHALLENGE_NO_ENTRY))
        assertFalse(creqJson.has(ChallengeRequestData.FIELD_CHALLENGE_HTML_DATA_ENTRY))

        assertEquals(
            ChallengeMessageFixtures.MESSAGE_VERSION_210,
            creqJson.getString(ChallengeRequestData.FIELD_MESSAGE_VERSION)
        )
        assertFalse(creqJson.has(ChallengeRequestData.FIELD_OOB_CONTINUE))

        val extensions = creqJson
            .getJSONArray(ChallengeRequestData.FIELD_MESSAGE_EXTENSION)
        assertEquals(
            MessageExtensionTest.FIXTURE.toJson().toString(),
            extensions.getJSONObject(0).toString()
        )
    }

    private companion object {
        private const val RESEND_CHALLENGE = "N"
    }
}
