package com.stripe.android.model.parsers

import com.stripe.android.model.StripeModel
import org.json.JSONArray
import org.json.JSONObject

internal interface ModelJsonParser<out ModelType : StripeModel?> {
    fun parse(json: JSONObject): ModelType

    companion object {
        internal fun jsonArrayToList(jsonArray: JSONArray?): List<String> {
            return jsonArray?.let {
                (0 until jsonArray.length()).map { jsonArray.getString(it) }
            } ?: emptyList()
        }
    }
}
