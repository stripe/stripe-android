package com.stripe.android.core.model.parsers

import androidx.annotation.RestrictTo
import com.stripe.android.core.model.StripeModel
import org.json.JSONArray
import org.json.JSONObject

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface ModelJsonParser<out ModelType : StripeModel> {
    fun parse(json: JSONObject): ModelType?

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {
        fun jsonArrayToList(jsonArray: JSONArray?): List<String> {
            return jsonArray?.let {
                (0 until jsonArray.length()).map { jsonArray.getString(it) }
            } ?: emptyList()
        }
    }
}
