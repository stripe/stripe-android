package com.stripe.android.paymentsheet.repositories

import com.stripe.android.core.model.StripeModel
import com.stripe.android.core.model.parsers.ModelJsonParser
import kotlinx.parcelize.Parcelize
import org.json.JSONObject

@Parcelize
internal data class CheckoutClientPollResponse(
    val id: String,
    val confirmationStatus: ConfirmationStatus,
    val status: CheckoutSessionResponse.Status,
    val paymentStatus: CheckoutSessionResponse.PaymentStatus,
) : StripeModel {
    enum class ConfirmationStatus { NOT_STARTED, PENDING, SUCCEEDED, FAILED }
}

internal object CheckoutClientPollResponseJsonParser : ModelJsonParser<CheckoutClientPollResponse> {
    override fun parse(json: JSONObject): CheckoutClientPollResponse? = runCatching {
        CheckoutClientPollResponse(
            id = json.getString("id"),
            confirmationStatus = when (json.getString("confirmation_status")) {
                "not_started" -> CheckoutClientPollResponse.ConfirmationStatus.NOT_STARTED
                "pending" -> CheckoutClientPollResponse.ConfirmationStatus.PENDING
                "succeeded" -> CheckoutClientPollResponse.ConfirmationStatus.SUCCEEDED
                "failed" -> CheckoutClientPollResponse.ConfirmationStatus.FAILED
                else -> error("Unsupported confirmation_status")
            },
            status = parseStatus(json.getString("status")),
            paymentStatus = parsePaymentStatus(json.getString("payment_status")),
        )
    }.getOrNull()
}
