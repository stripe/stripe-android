package com.stripe.android.model

import com.stripe.android.model.parsers.Stripe3ds2AuthResultJsonParser
import org.json.JSONObject
import java.util.UUID

internal object Stripe3ds2AuthResultFixtures {
    internal val ARES_CHALLENGE_FLOW = Stripe3ds2AuthResult(
        id = "threeds2_1FDy0vCRMbs6",
        source = "src_1FDy0uCRMb",
        created = 1567363381,
        ares = Stripe3ds2AuthResult.Ares(
            acsChallengeMandated = "Y",
            acsTransId = UUID.randomUUID().toString(),
            sdkTransId = UUID.randomUUID().toString(),
            threeDSServerTransId = UUID.randomUUID().toString(),
            transStatus = Stripe3ds2AuthResult.Ares.VALUE_CHALLENGE,
            messageType = "ARes",
            messageVersion = "2.1.0"
        )
    )

    internal val ARES_FRICTIONLESS_FLOW = Stripe3ds2AuthResult(
        id = "threeds2_1Ecwz3CRMbs6FrXfThtfogua",
        liveMode = false,
        created = 1558541285L,
        source = "src_1Ecwz1CRMbs6FrXfUwt98lxf",
        ares = Stripe3ds2AuthResult.Ares(
            acsChallengeMandated = "N",
            acsTransId = UUID.randomUUID().toString(),
            sdkTransId = UUID.randomUUID().toString(),
            threeDSServerTransId = UUID.randomUUID().toString(),
            messageVersion = "2.1.0",
            messageType = "ARes"
        )
    )

    internal val ERROR = Stripe3ds2AuthResult(
        id = "threeds2_1FDy0vCRMbs6",
        source = "src_1FDy0uCRMb",
        created = 1567363381,
        error = Stripe3ds2AuthResult.ThreeDS2Error(
            acsTransId = UUID.randomUUID().toString(),
            dsTransId = UUID.randomUUID().toString(),
            sdkTransId = UUID.randomUUID().toString(),
            threeDSServerTransId = UUID.randomUUID().toString(),
            messageVersion = "2.1.0",
            messageType = "Erro",
            errorComponent = "D",
            errorCode = "302",
            errorDescription = "Data could not be decrypted by the receiving system due to technical or other reason.",
            errorMessageType = "AReq"
        )
    )

    internal val FALLBACK_REDIRECT_URL = Stripe3ds2AuthResult(
        id = "threeds2_1FDy0vCRMbs6",
        source = "src_1FDy0uCRMb",
        created = 1567363381,
        fallbackRedirectUrl = "https://hooks.stripe.com/3d_secure_2_eap/begin_test/src_1Ecve7CRMbs6FrXfm8AxXMIh/src_client_secret_F79yszOBAiuaZTuIhbn3LPUW"
    )

    internal val CHALLENGE_COMPLETION_JSON = JSONObject(
        """
        {
            "id": "threeds2_1HlyNpCRMbs6FrXfLz2jiYRV",
            "object": "three_d_secure_2",
            "ares": {
                "acsChallengeMandated": "Y",
                "acsSignedContent": "eyJhbGciOiJFUzI1NiJ9.eyJhY3NFcGhlbVB1YktleSI6eyJjcnYiOiJQLTI1NiIsImt0eSI6IkVDIiwieCI6ImxPd3pvUldfamxybUdndEtvZmR1a0pMejNDeENTQi1WenA4cTBWbHEzSGMiLCJ5IjoiNmlrcDFobHY1RjY2SXhENF9DOW1jbm9kMjNaTFNDempKczNRaWZfRDN3TSJ9LCJzZGtFcGhlbVB1YktleSI6eyJrdHkiOiJFQyIsInVzZSI6InNpZyIsImNydiI6IlAtMjU2IiwieCI6ImNpbVJZeTlVV3oySFJBY2p6NHpoY2dieFZLdl9uVWRGVThqMkxWR2d1NU0iLCJ5IjoiRUNySW9CQ2VXVVJSelRmZndhcTV0YUQ2cXJrb1BkT09sWUpFOGVFTkR5WSJ9LCJhY3NVUkwiOiJodHRwczovL3Rlc3Rtb2RlLWFjcy5zdHJpcGUuY29tLzNkX3NlY3VyZV8yX3Rlc3QvYWNjdF8xRFhCcDlDUk1iczZGclhmL3RocmVlZHMyXzFIbHlOcENSTWJzNkZyWGZMejJqaVlSVi9hcHBfY2hhbGxlbmdlL0NSOUI4c1lBZU9Nd0cxWmhyNDFUdnBjOVVLSTgtVk1reGk4eW5iZmJGdzg9In0.C_hwyoW5bynvMxK-oLdGSLEH6jXyB48YGnpOBB2oCDPAV8scOnXR7TKhtuLE7em8U0AdQzZAVg4oBFentiIGdA",
                "acsTransID": "0f7d1540-fa36-469f-bd9d-7f21c932e621",
                "acsURL": null,
                "authenticationType": "02",
                "cardholderInfo": null,
                "messageExtension": null,
                "messageType": "ARes",
                "messageVersion": "2.1.0",
                "sdkTransID": "4d6a2307-29db-4156-a6a0-8ce68dc5fc8b",
                "threeDSServerTransID": "0840014b-f5a8-4093-837d-81907bbf2e62",
                "transStatus": "C"
            },
            "created": 1605020625,
            "creq": "eyJ0aHJlZURTU2VydmVyVHJhbnNJRCI6IjA4NDAwMTRiLWY1YTgtNDA5My04MzdkLTgxOTA3YmJmMmU2MiIsImFjc1RyYW5zSUQiOiIwZjdkMTU0MC1mYTM2LTQ2OWYtYmQ5ZC03ZjIxYzkzMmU2MjEiLCJjaGFsbGVuZ2VXaW5kb3dTaXplIjoiMDUiLCJtZXNzYWdlVHlwZSI6IkNSZXEiLCJtZXNzYWdlVmVyc2lvbiI6IjIuMS4wIn0=",
            "error": null,
            "fallback_redirect_url": null,
            "livemode": false,
            "source": "src_1HlyNnCRMbs6FrXftBSOL675",
            "state": "failed"
        }
        """.trimIndent()
    )

    internal val CHALLENGE_COMPLETION = requireNotNull(
        Stripe3ds2AuthResultJsonParser().parse(CHALLENGE_COMPLETION_JSON)
    )
}
