package com.stripe.android.stripe3ds2.transaction

import com.google.common.truth.Truth.assertThat
import com.stripe.android.stripe3ds2.ChallengeMessageFixtures
import com.stripe.android.stripe3ds2.transactions.ErrorData
import kotlin.test.Test
import kotlin.test.assertEquals

class ProtocolErrorEventFactoryTest {

    @Test
    fun create_shouldReturnExpectedProtocolErrorEvent() {
        val protocolErrorEvent = ProtocolErrorEventFactory()
            .create(
                ErrorData(
                    errorMessageType = "",
                    acsTransId = ChallengeMessageFixtures.ACS_TRANS_ID,
                    sdkTransId = ChallengeMessageFixtures.SDK_TRANS_ID,
                    errorDetail = "Description of the failure.",
                    errorCode = "Data Decryption Failure",
                    errorDescription = "Data could not be decrypted.",
                    messageVersion = ChallengeMessageFixtures.MESSAGE_VERSION_210
                )
            )

        assertThat(protocolErrorEvent.sdkTransactionId)
            .isEqualTo(ChallengeMessageFixtures.SDK_TRANS_ID)

        val errorMessage = protocolErrorEvent.errorMessage
        assertEquals("Data Decryption Failure", errorMessage.errorCode)
        assertEquals("Data could not be decrypted.", errorMessage.errorDescription)
        assertEquals("Description of the failure.", errorMessage.errorDetails)
        assertThat(errorMessage.transactionId)
            .isEqualTo(ChallengeMessageFixtures.ACS_TRANS_ID)
    }
}
