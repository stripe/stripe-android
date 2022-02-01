package com.stripe.android.stripe3ds2.transactions

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.HashMap

/**
 * See "EMV 3DS Protocol and Core Functions Specification - Table A.7: Message Extension Attributes"
 */
@Parcelize
data class MessageExtension internal constructor(
    // The name of the extension data set as defined by the extension owner.
    val name: String,

    // A unique identifier for the extension.
    // Note: Payment System Registered Application Provider Identifier (RID) is required as prefix
    // of the ID.
    internal val id: String,

    // A boolean value indicating whether the recipient must understand the contents of the
    // extension to interpret the entire message.
    val criticalityIndicator: Boolean,

    // The data carried in the extension.
    private val data: Map<String, String?> = emptyMap()
) : Parcelable {

    val isProcessable: Boolean
        get() {
            return SUPPORTED_MESSAGE_EXTENSIONS.contains(name)
        }

    @Throws(JSONException::class)
    internal fun toJson(): JSONObject {
        return JSONObject()
            .put(FIELD_NAME, name)
            .put(FIELD_ID, id)
            .put(FIELD_CRITICALITY_INDICATOR, criticalityIndicator)
            .put(FIELD_DATA, JSONObject(data))
    }

    companion object {
        internal const val FIELD_NAME = "name"
        internal const val FIELD_ID = "id"
        internal const val FIELD_CRITICALITY_INDICATOR = "criticalityIndicator"
        internal const val FIELD_DATA = "data"

        private const val ID_MAX_LENGTH = 64
        private const val NAME_MAX_LENGTH = 64
        private const val DATA_VALUE_MAX_LENGTH = 8059
        private const val MESSAGE_EXTENSIONS_MAX_COUNT = 10

        private val SUPPORTED_MESSAGE_EXTENSIONS: List<String> = emptyList()

        @JvmStatic
        @Throws(JSONException::class)
        fun toJsonArray(messageExtensions: List<MessageExtension>?): JSONArray? {
            if (messageExtensions == null) {
                return null
            }

            val messageExtensionJsonArray = JSONArray()
            messageExtensions.forEach {
                messageExtensionJsonArray.put(it.toJson())
            }
            return messageExtensionJsonArray
        }

        @JvmStatic
        @Throws(ChallengeResponseParseException::class)
        fun fromJson(messageExtensionsJson: JSONArray?): List<MessageExtension>? {
            if (messageExtensionsJson == null) {
                return null
            }

            val messageExtensions =
                (0 until messageExtensionsJson.length()).mapNotNull { idx ->
                    messageExtensionsJson.optJSONObject(idx)
                }.map { messageExtensionJson ->
                    fromJson(messageExtensionJson)
                }

            if (messageExtensions.size > MESSAGE_EXTENSIONS_MAX_COUNT) {
                throw ChallengeResponseParseException
                    .createInvalidDataElementFormat("messageExtensions")
            }

            return messageExtensions
        }

        @Throws(ChallengeResponseParseException::class)
        private fun fromJson(messageExtensionJson: JSONObject): MessageExtension {
            val name = messageExtensionJson.optString(FIELD_NAME)
            if (name.length > NAME_MAX_LENGTH) {
                throw ChallengeResponseParseException
                    .createInvalidDataElementFormat("messageExtension.name")
            }

            val id = messageExtensionJson.optString(FIELD_ID)
            if (id.length > ID_MAX_LENGTH) {
                throw ChallengeResponseParseException
                    .createInvalidDataElementFormat("messageExtension.id")
            }

            val data = HashMap<String, String>()
            val dataJson = messageExtensionJson.optJSONObject(FIELD_DATA)
            if (dataJson != null) {
                val keys = dataJson.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val value = dataJson.optString(key)
                    if (value.length > DATA_VALUE_MAX_LENGTH) {
                        throw ChallengeResponseParseException
                            .createInvalidDataElementFormat("messageExtension.data.value")
                    } else {
                        data[key] = value
                    }
                }
            }

            return MessageExtension(
                name = name,
                id = id,
                criticalityIndicator = messageExtensionJson.optBoolean(FIELD_CRITICALITY_INDICATOR),
                data = data
            )
        }
    }
}
