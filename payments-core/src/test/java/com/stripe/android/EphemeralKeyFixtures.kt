package com.stripe.android

import com.stripe.android.model.parsers.EphemeralKeyJsonParser
import org.json.JSONObject

internal object EphemeralKeyFixtures {
    val FIRST_JSON = JSONObject(
        """
        {
            "id": "ephkey_123",
            "object": "ephemeral_key",
            "secret": "ek_test_123",
            "created": 1501179335,
            "livemode": false,
            "expires": 1501199335,
            "associated_objects": [{
                "type": "customer",
                "id": "cus_AQsHpvKfKwJDrF"
            }]
        }
        """.trimIndent()
    )

    val SECOND_JSON = JSONObject(
        """
        {
            "id": "ephkey_ABC",
            "object": "ephemeral_key",
            "secret": "ek_test_456",
            "created": 1601189335,
            "livemode": false,
            "expires": 1601199335,
            "associated_objects": [{
                "type": "customer",
                "id": "cus_abc123"
            }]
        }
        """.trimIndent()
    )

    val FIRST = EphemeralKeyJsonParser().parse(FIRST_JSON)
    val SECOND = EphemeralKeyJsonParser().parse(SECOND_JSON)

    fun create(
        expires: Long
    ): EphemeralKey {
        return FIRST.copy(
            expires = expires
        )
    }
}

internal fun EphemeralKey.copy(
    objectId: String = this.objectId,
    created: Long = this.created,
    expires: Long = this.expires,
    id: String = this.id,
    isLiveMode: Boolean = this.isLiveMode,
    objectType: String = this.objectType,
    secret: String = this.secret,
    type: String = this.type
): EphemeralKey {
    return EphemeralKey(
        objectId = objectId,
        created = created,
        expires = expires,
        id = id,
        isLiveMode = isLiveMode,
        objectType = objectType,
        secret = secret,
        type = type
    )
}
