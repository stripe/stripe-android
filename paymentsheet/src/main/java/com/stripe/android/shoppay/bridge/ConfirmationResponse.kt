package com.stripe.android.shoppay.bridge

import org.json.JSONObject

internal data class ConfirmationResponse(
    val status: String,
    val requiresAction: Boolean
) : JsonSerializer {
    override fun toJson(): JSONObject {
        return JSONObject().apply {
            put("status", status)
            put("requiresAction", requiresAction)
        }
    }
}
