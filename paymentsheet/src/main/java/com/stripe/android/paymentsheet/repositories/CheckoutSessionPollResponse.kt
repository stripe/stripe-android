package com.stripe.android.paymentsheet.repositories

import com.stripe.android.core.model.StripeModel
import com.stripe.android.core.model.parsers.ModelJsonParser
import kotlinx.parcelize.Parcelize
import org.json.JSONObject

@Parcelize
internal data class CheckoutSessionPollResponse(
    val sessionId: String,
    val state: State,
    val paymentObjectStatus: String?,
) : StripeModel {
    enum class State(val code: String) {
        ACTIVE("active"),
        PROCESSING_SYNC_PAYMENT("processing_sync_payment"),
        PROCESSING_SUBSCRIPTION("processing_subscription"),
        SUCCEEDED("succeeded"),
        PROCESSING_ASYNC_PAYMENT("processing_async_payment"),
        PENDING_ASYNC_CUSTOMER_ACTION("pending_async_customer_action"),
        FAILED_ASYNC_PAYMENT("failed_async_payment"),
        INVALID("invalid"),
        EXPIRED("expired"),
    }
}

internal object CheckoutSessionPollResponseJsonParser : ModelJsonParser<CheckoutSessionPollResponse> {
    override fun parse(json: JSONObject): CheckoutSessionPollResponse? {
        val sessionId = (json.opt("session_id") as? String)?.takeIf { it.isNotEmpty() } ?: return null
        val state = CheckoutSessionPollResponse.State.entries.firstOrNull { it.code == json.opt("state") }
            ?: return null
        val paymentObjectStatus = if (json.isNull("payment_object_status")) {
            null
        } else {
            json.opt("payment_object_status") as? String ?: return null
        }
        return CheckoutSessionPollResponse(sessionId, state, paymentObjectStatus)
    }
}
