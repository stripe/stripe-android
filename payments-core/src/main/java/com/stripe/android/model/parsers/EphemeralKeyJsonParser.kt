package com.stripe.android.model.parsers

import com.stripe.android.EphemeralKey
import com.stripe.android.core.model.parsers.ModelJsonParser
import org.json.JSONObject

internal class EphemeralKeyJsonParser : ModelJsonParser<EphemeralKey> {
    override fun parse(json: JSONObject): EphemeralKey {
        val created = json.getLong(FIELD_CREATED)
        val expires = json.getLong(FIELD_EXPIRES)
        val id = json.getString(FIELD_ID)
        val liveMode = json.getBoolean(FIELD_LIVEMODE)
        val objectType = json.getString(FIELD_OBJECT)
        val secret = json.getString(FIELD_SECRET)

        // Get the values from the associated objects array first element
        val associatedObjectArray = json.getJSONArray(FIELD_ASSOCIATED_OBJECTS)
        val typeObject = associatedObjectArray.getJSONObject(0)
        val type = typeObject.getString(FIELD_TYPE)
        val objectId = typeObject.getString(FIELD_ID)

        return EphemeralKey(
            objectId = objectId,
            created = created,
            expires = expires,
            id = id,
            isLiveMode = liveMode,
            objectType = objectType,
            secret = secret,
            type = type
        )
    }

    private companion object {
        private const val FIELD_CREATED = "created"
        private const val FIELD_EXPIRES = "expires"
        private const val FIELD_SECRET = "secret"
        private const val FIELD_LIVEMODE = "livemode"
        private const val FIELD_OBJECT = "object"
        private const val FIELD_ID = "id"
        private const val FIELD_ASSOCIATED_OBJECTS = "associated_objects"
        private const val FIELD_TYPE = "type"
    }
}
