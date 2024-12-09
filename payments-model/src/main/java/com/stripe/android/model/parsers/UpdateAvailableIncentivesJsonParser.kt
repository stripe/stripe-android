package com.stripe.android.model.parsers

import androidx.annotation.RestrictTo
import com.stripe.android.core.model.parsers.ModelJsonParser
import com.stripe.android.model.UpdateAvailableIncentives
import org.json.JSONObject

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object UpdateAvailableIncentivesJsonParser : ModelJsonParser<UpdateAvailableIncentives> {

    override fun parse(json: JSONObject): UpdateAvailableIncentives? {
        val incentives = json.optJSONArray("data")?.let { data ->
            List(data.length()) { index ->
                LinkConsumerIncentiveJsonParser.parse(data.getJSONObject(index))
            }
        }

        return incentives?.let {
            UpdateAvailableIncentives(data = it.filterNotNull())
        }
    }
}
