package com.stripe.android

import android.os.Parcel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import org.json.JSONException
import org.json.JSONObject
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EphemeralKeyTest {

    @Test
    @Throws(JSONException::class)
    fun fromJson_createsKeyWithExpectedValues() {
        assertEquals("ephkey_123", EPHEMERAL_KEY.id)
        assertEquals("ephemeral_key", EPHEMERAL_KEY.objectType)
        assertEquals("ek_test_123", EPHEMERAL_KEY.secret)
        assertFalse(EPHEMERAL_KEY.isLiveMode)
        assertEquals(1483575790L, EPHEMERAL_KEY.created)
        assertEquals(1483579790L, EPHEMERAL_KEY.expires)
        assertEquals("customer", EPHEMERAL_KEY.type)
        assertEquals("cus_123", EPHEMERAL_KEY.customerId)
    }

    @Test
    @Throws(JSONException::class)
    fun toParcel_fromParcel_createsExpectedObject() {
        val ephemeralKey = EPHEMERAL_KEY
        val parcel = Parcel.obtain()
        ephemeralKey.writeToParcel(parcel, 0)
        // We need to reset the data position of the parcel or else we'll continue reading
        // null values off the end.
        parcel.setDataPosition(0)

        val createdKey = CustomerEphemeralKey.CREATOR.createFromParcel(parcel)

        assertEquals(ephemeralKey.id, createdKey.id)
        assertEquals(ephemeralKey.created, createdKey.created)
        assertEquals(ephemeralKey.expires, createdKey.expires)
        assertEquals(ephemeralKey.customerId, createdKey.customerId)
        assertEquals(ephemeralKey.type, createdKey.type)
        assertEquals(ephemeralKey.secret, createdKey.secret)
        assertEquals(ephemeralKey.isLiveMode, createdKey.isLiveMode)
        assertEquals(ephemeralKey.objectType, createdKey.objectType)
    }

    companion object {
        private val EPHEMERAL_KEY = CustomerEphemeralKey.fromJson(JSONObject(
            """
            {
                "id": "ephkey_123",
                "object": "ephemeral_key",
                "secret": "ek_test_123",
                "created": 1483575790,
                "livemode": false,
                "expires": 1483579790,
                "associated_objects": [{
                    "type": "customer",
                    "id": "cus_123"
                }]
            }
            """.trimIndent()
        ))
    }
}
