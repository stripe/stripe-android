package com.stripe.android.model.parsers

import com.stripe.android.model.StripeModel
import org.json.JSONObject

internal interface ModelJsonParser<out ModelType : StripeModel?> {
    fun parse(json: JSONObject): ModelType
}
