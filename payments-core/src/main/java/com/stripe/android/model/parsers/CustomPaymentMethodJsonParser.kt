package com.stripe.android.model.parsers

import com.stripe.android.core.model.parsers.ModelJsonParser
import com.stripe.android.model.ElementsSession
import org.json.JSONObject

internal class CustomPaymentMethodJsonParser : ModelJsonParser<ElementsSession.CustomPaymentMethod> {
    override fun parse(json: JSONObject): ElementsSession.CustomPaymentMethod? {
        val type = json.optString(FIELD_TYPE) ?: return null
        val displayName = json.optString(FIELD_DISPLAY_NAME) ?: return null
        val logoUrl = json.optString(FIELD_LOGO_URL) ?: return null

        return ElementsSession.CustomPaymentMethod(
            id = type,
            displayName = displayName,
            logoUrl = logoUrl,
        )
    }

    private companion object {
        const val FIELD_TYPE = "type"
        const val FIELD_DISPLAY_NAME = "display_name"
        const val FIELD_LOGO_URL = "logo_url"
    }
}
