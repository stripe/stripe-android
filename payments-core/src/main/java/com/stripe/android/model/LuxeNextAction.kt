package com.stripe.android.model

import android.net.Uri
import com.stripe.android.StripeIntentResult
import org.json.JSONObject

data class PiStatusSpec(
    val associatedStatuses: List<StripeIntent.Status>,
    @StripeIntentResult.Outcome val outcome: Int
)

data class RedirectNextActionSpec(
    val hostedPagePath: String,
    val returnToUrlPath: String,
)

data class LuxeNextAction(
    val handleNextActionSpec: Map<StripeIntent.Status, RedirectNextActionSpec>,
    val handlePiStatus: List<PiStatusSpec>,
)

enum class LpmNextActionData {
    Instance;

    fun getTerminalStatus(lpmCode: PaymentMethodCode?, status: StripeIntent.Status?): Int? {
        return codeToNextActionSpec[lpmCode]?.let { luxeNextAction ->
            luxeNextAction.handlePiStatus.firstOrNull {
                it.associatedStatuses.contains(status)
            }?.outcome
        }
    }

    fun getNextAction(stripeIntent: StripeIntent): StripeIntent.NextActionData? {
        return stripeIntent.paymentMethod?.type?.code?.let { lpmCode ->
            codeToNextActionSpec[lpmCode]?.let { luxeNextAction ->
                luxeNextAction.handleNextActionSpec[stripeIntent.status]?.let { redirectNextAction ->
                    stripeIntent.nextActionRawString?.let {
                        val jsonNextAction = JSONObject(it)
                        StripeIntent.NextActionData.RedirectToUrl(
                            returnUrl = getPath(
                                redirectNextAction.returnToUrlPath,
                                jsonNextAction
                            ).toString(),
                            url = Uri.parse(
                                getPath(
                                    redirectNextAction.hostedPagePath,
                                    jsonNextAction
                                ).toString()
                            )
                        )
                    }
                }
            }
        }
    }

    private fun getPath(path: String, json: JSONObject): JSONObject? {
        val pathArray = path.replace("next_action.", "").split(".")
        var jsonObject: JSONObject? = json
        for (key in pathArray) {
            if (jsonObject == null) {
                break
            }
            if (jsonObject.has(key)) {
                jsonObject = jsonObject.optJSONObject(key)
            }
        }
        return jsonObject
    }

    fun supported(code: String) = codeToNextActionSpec.contains(code)

    private val codeToNextActionSpec = mutableMapOf<String, LuxeNextAction>().apply {
        put(
            "konbini",
            LuxeNextAction(
                handleNextActionSpec = mapOf(
                    StripeIntent.Status.RequiresAction to
                        RedirectNextActionSpec(
                            hostedPagePath = "next_action.konbini_display_details.hosted_voucher_url",
                            returnToUrlPath = "next_action.redirect_to_url.return_url"
                        )
                ),
                handlePiStatus = listOf(
                    PiStatusSpec(
                        associatedStatuses = listOf(StripeIntent.Status.Succeeded),
                        outcome = StripeIntentResult.Outcome.SUCCEEDED
                    ),
                    PiStatusSpec(
                        associatedStatuses = listOf(StripeIntent.Status.RequiresAction),
                        outcome = StripeIntentResult.Outcome.CANCELED
                    )
                )
            )
        )
    }
}
