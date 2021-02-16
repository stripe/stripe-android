package com.stripe.android.model

import org.json.JSONObject

internal object StripeFileFixtures {
    val DEFAULT = JSONObject(
        """
            {
                "id": "file_1FzRQ6CRMbs6FrXfgjerzyUQ",
                "object": "file",
                "created": 1578677834,
                "filename": "upload.png",
                "purpose": "business_icon",
                "size": 25722,
                "title": null,
                "type": "png",
                "url": "https://files.stripe.com/v1/files/file_1G1H0DBbvEc/contents"
            }
        """.trimIndent()
    )
}
