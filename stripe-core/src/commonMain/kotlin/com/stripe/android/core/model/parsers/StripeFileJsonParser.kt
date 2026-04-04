package com.stripe.android.core.model.parsers

import androidx.annotation.RestrictTo
import com.stripe.android.core.model.StripeFile
import com.stripe.android.core.model.StripeFilePurpose
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class StripeFileJsonParser(
    private val json: Json = Json
) : StripeModelParser<StripeFile> {
    override fun parse(jsonString: String): StripeFile {
        val jsonObject = json.parseToJsonElement(jsonString).jsonObject

        return StripeFile(
            id = jsonObject.string(FIELD_ID),
            created = jsonObject.long(FIELD_CREATED),
            filename = jsonObject.string(FIELD_FILENAME),
            purpose = StripeFilePurpose.fromCode(jsonObject.string(FIELD_PURPOSE)),
            size = jsonObject.int(FIELD_SIZE),
            title = jsonObject.string(FIELD_TITLE),
            type = jsonObject.string(FIELD_TYPE),
            url = jsonObject.string(FIELD_URL)
        )
    }

    private companion object {
        private const val FIELD_ID = "id"
        private const val FIELD_CREATED = "created"
        private const val FIELD_FILENAME = "filename"
        private const val FIELD_PURPOSE = "purpose"
        private const val FIELD_SIZE = "size"
        private const val FIELD_TITLE = "title"
        private const val FIELD_TYPE = "type"
        private const val FIELD_URL = "url"
    }
}

private fun JsonObject.string(key: String): String? {
    return this[key]?.jsonPrimitive?.contentOrNull
}

private fun JsonObject.long(key: String): Long? {
    return this[key]?.jsonPrimitive?.longOrNull
}

private fun JsonObject.int(key: String): Int? {
    return this[key]?.jsonPrimitive?.intOrNull
}
