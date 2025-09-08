package com.stripe.android.model.parsers

import com.stripe.android.core.model.StripeJsonUtils
import com.stripe.android.core.model.parsers.ModelJsonParser
import com.stripe.android.model.MandateData
import org.json.JSONObject

internal class MandateDataJsonParser : ModelJsonParser<MandateData> {
    override fun parse(json: JSONObject): MandateData? {
        json.optJSONObject(
            FIELD_CUSTOMER_ACCEPTANCE
        )?.let { customerAcceptanceJson ->
            val type = StripeJsonUtils.optString(customerAcceptanceJson, FIELD_TYPE)
            val online = customerAcceptanceJson.optJSONObject(FIELD_ONLINE)?.let {
                MandateData.CustomerAcceptance.Online(
                    ipAddress = StripeJsonUtils.optString(it, FIELD_IP_ADDRESS),
                    userAgent = StripeJsonUtils.optString(it, FIELD_USER_AGENT)
                )
            }
            if (type != null) {
                return MandateData(
                    MandateData.CustomerAcceptance(
                        online = online,
                        type = type
                    )
                )
            }
        }
        return null
    }

    private companion object {
        const val FIELD_CUSTOMER_ACCEPTANCE = "customer_acceptance"
        const val FIELD_TYPE = "type"
        const val FIELD_ONLINE = "online"
        const val FIELD_IP_ADDRESS = "ip_address"
        const val FIELD_USER_AGENT = "user_agent"
    }

}