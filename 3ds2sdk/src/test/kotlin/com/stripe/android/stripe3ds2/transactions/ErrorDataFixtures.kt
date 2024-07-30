package com.stripe.android.stripe3ds2.transactions

import org.json.JSONObject
import java.util.UUID

internal object ErrorDataFixtures {
    private val ERROR_MESSAGE_JSON = JSONObject(
        """
        {
            "threeDSServerTransID": "${UUID.randomUUID()}",
            "acsTransID": "${UUID.randomUUID()}",
            "dsTransID": "${UUID.randomUUID()}",
            "errorCode": "Data Decryption Failure",
            "errorComponent": "D",
            "errorDescription": "Data could not be decrypted by the receiving system due to technical or other reason.",
            "errorDetail": "Description of the failure.",
            "errorMessageType": "CReq",
            "messageVersion": "2.2.0",
            "messageType": "Erro",
            "sdkTransID": "${UUID.randomUUID()}"
        }
        """.trimIndent()
    )

    internal val ERROR_DATA = ErrorData.fromJson(ERROR_MESSAGE_JSON)
}
