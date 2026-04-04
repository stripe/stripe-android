package com.stripe.android.core.model.parsers

import androidx.annotation.RestrictTo
import com.stripe.android.core.model.StripeModel
import org.json.JSONObject

/**
 * Android bridge for legacy parsers that still consume `JSONObject`.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class ModelJsonParserAdapter<out ModelType : StripeModel>(
    private val legacyParser: ModelJsonParser<ModelType>
) : StripeModelParser<ModelType> {
    override fun parse(jsonString: String): ModelType? {
        return legacyParser.parse(JSONObject(jsonString))
    }
}
