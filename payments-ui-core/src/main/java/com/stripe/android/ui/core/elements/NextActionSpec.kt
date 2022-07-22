package com.stripe.android.ui.core.elements

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class PIStatusType {
    @SerialName("finished")
    Finished,

    @SerialName("canceled")
    Canceled,

    @SerialName("failed") // TODO: determine if still in spec.
    Failed
}

@Serializable
data class PiStatusSpec(
    @SerialName("associated_statuses")
    val associatedStatuses: List<String>,
    @SerialName("outcome")
    val outcome: PIStatusType
)

@Serializable
@SerialName("redirect_to_hosted_page")
data class RedirectNextActionSpec(
    @SerialName("associated_statuses")
    val associatedStatuses: List<String>, // ["requires_action"]
    @SerialName("hosted_page_path")
    val hostedPagePath: String, // next_action.konbini_display_details.hosted_voucher_url
    @SerialName("return_url_path")
    val returnUrlPath: String // next_action.konbini_display_details.hosted_voucher_url
)

@Serializable
@SerialName("next_action_spec")
data class NextActionSpec(
    @SerialName("handle_next_action_specs")
    val handleNextActionSpec: List<RedirectNextActionSpec>,

    @SerialName("handle_pi_status_specs")
    val handlePiStatus: List<PiStatusSpec>
)
