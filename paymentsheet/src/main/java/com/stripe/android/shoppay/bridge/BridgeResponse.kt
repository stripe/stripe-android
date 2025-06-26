package com.stripe.android.shoppay.bridge

import org.json.JSONObject

internal sealed interface BridgeResponse<T : JsonSerializer> : JsonSerializer {
    val type: String
    data class Data<T : JsonSerializer>(val data: T) : BridgeResponse<T> {
        override val type = "data"
        override fun toJson(): JSONObject {
            return JSONObject().apply {
                put("data", data.toJson())
                put("type", type)
            }
        }
    }

    data class Error<T : JsonSerializer>(val message: String) : BridgeResponse<T> {
        override val type: String = "error"

        override fun toJson(): JSONObject {
            return JSONObject().apply {
                put("message", message)
                put("type", type)
            }
        }
    }
}

internal interface JsonSerializer {
    fun toJson(): JSONObject
}
