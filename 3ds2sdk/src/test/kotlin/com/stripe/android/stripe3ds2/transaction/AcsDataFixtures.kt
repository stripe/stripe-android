package com.stripe.android.stripe3ds2.transaction

import org.json.JSONObject

internal object AcsDataFixtures {
    val DEFAULT_JSON = JSONObject(
        """
        {
            "acsURL": "https://testmode-acs.stripe.com/3d_secure_2_test/acct_123/threeds2_1HPOihJnhvQu/app_challenge/GXnh5fGIdB_oL4EeEc1BBrOYk=",
            "acsEphemPubKey": {
                "kty": "EC",
                "crv": "P-256",
                "x": "ogqvTyWKqjH5n8aruACjG7S1w8ZcFWMnOurkfZxGAVA",
                "y": "slB08MxGMZSQyMOJNUsClBr6mO9c0MLIYGAwaVnP4e0"
            },
            "sdkEphemPubKey": {
                "kty": "EC",
                "use": "sig",
                "crv": "P-256",
                "x": "Iuv7ln5IydYYj_3wWdmDB9ZMunVdg-XDg9wQej3v1eU",
                "y": "VZr-bvPcg_LR3D20DORSmqFSdSzcE3_39n5uxS9LayY"
            }
        }
        """.trimIndent()
    )

    fun create(acsUrl: String): JSONObject {
        return JSONObject(
            """
            {
                "acsURL": "$acsUrl",
                "acsEphemPubKey": {
                    "kty": "EC",
                    "crv": "P-256",
                    "x": "ogqvTyWKqjH5n8aruACjG7S1w8ZcFWMnOurkfZxGAVA",
                    "y": "slB08MxGMZSQyMOJNUsClBr6mO9c0MLIYGAwaVnP4e0"
                },
                "sdkEphemPubKey": {
                    "kty": "EC",
                    "use": "sig",
                    "crv": "P-256",
                    "x": "Iuv7ln5IydYYj_3wWdmDB9ZMunVdg-XDg9wQej3v1eU",
                    "y": "VZr-bvPcg_LR3D20DORSmqFSdSzcE3_39n5uxS9LayY"
                }
            }
            """.trimIndent()
        )
    }
}
