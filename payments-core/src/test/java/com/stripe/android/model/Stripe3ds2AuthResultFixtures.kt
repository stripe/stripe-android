package com.stripe.android.model

import com.stripe.android.model.parsers.Stripe3ds2AuthResultJsonParser
import org.json.JSONObject

@Suppress("MaxLineLength")
internal object Stripe3ds2AuthResultFixtures {

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
