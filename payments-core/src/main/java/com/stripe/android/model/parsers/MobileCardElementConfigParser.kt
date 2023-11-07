package com.stripe.android.model.parsers

import com.stripe.android.core.model.parsers.ModelJsonParser
import com.stripe.android.model.MobileCardElementConfig
import org.json.JSONObject

internal class MobileCardElementConfigParser : ModelJsonParser<MobileCardElementConfig> {

    override fun parse(json: JSONObject): MobileCardElementConfig {
        return MobileCardElementConfig(
            cardBrandChoice = MobileCardElementConfig.CardBrandChoice(
                eligible = json.getJSONObject(FIELD_CARD_BRAND_CHOICE).getBoolean(FIELD_ELIGIBLE),
            )
        )
    }

    private companion object {
        const val FIELD_CARD_BRAND_CHOICE = "card_brand_choice"
        const val FIELD_ELIGIBLE = "eligible"
    }
}
