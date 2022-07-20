package com.stripe.android.model

import android.net.Uri
import com.stripe.android.StripeIntentResult
import org.json.JSONObject

class LuxeNextActionRepository {
    private val codeToNextActionSpec = mutableMapOf<String, LuxeNextAction>()

    fun update(additionalData: Map<String, LuxeNextAction>) {
        codeToNextActionSpec.putAll(additionalData)
    }

    fun getTerminalStatus(lpmCode: PaymentMethodCode?, status: StripeIntent.Status?) =
        codeToNextActionSpec[lpmCode]?.let { luxeNextAction ->
            luxeNextAction.handlePiStatus.firstOrNull {
                it.associatedStatuses.contains(status)
            }?.outcome
        }

    fun requiresAction(stripeIntent: StripeIntent) = stripeIntent.jsonString?.let {
        JSONObject(it)
            .optJSONObject("payment_method")
            ?.optString("type")
    }?.let { lpmCode ->
        codeToNextActionSpec[lpmCode]?.handleNextActionSpec?.containsKey(stripeIntent.status)
    } ?: false

    /**
     * Null returned from this function indicates there is definitively no next action.
     */
    fun getNextAction(stripeIntent: StripeIntent) = stripeIntent.jsonString?.let {
        JSONObject(it)
            .optJSONObject("payment_method")
            ?.optString("type")
    }?.let { lpmCode ->
        codeToNextActionSpec[lpmCode]?.let { luxeNextAction ->
            luxeNextAction.handleNextActionSpec[stripeIntent.status]?.let { redirectNextAction ->
                stripeIntent.jsonString?.let {
                    StripeIntent.NextActionData.RedirectToUrl(
                        returnUrl = getPath(
                            redirectNextAction.returnToUrlPath,
                            JSONObject(it)
                        ).toString(),
                        url = Uri.Builder().path(
                            getPath(
                                redirectNextAction.hostedPagePath,
                                JSONObject(it)
                            ).toString()
                        ).build()
                    )
                }
            }
        }
    }

    private fun getPath(path: String, json: JSONObject): String? {
        val pathArray = ("[*" + "([A-Za-z_0-9]+)" + "]*").toRegex().findAll(path)
            .map { it.value }
            .distinct()
            .filterNot { it.isEmpty() }
            .toList()
        var jsonObject: JSONObject? = json
        for (key in pathArray) {
            if (jsonObject == null) {
                break
            }
            if (jsonObject.has(key)) {
                val tempJsonObject = jsonObject.optJSONObject(key)
                val tempJsonString = jsonObject.get(key)

                if (tempJsonObject != null) {
                    jsonObject = tempJsonObject
                } else if ((tempJsonString as? String) != null) {
                    return tempJsonString
                }
            }
        }
        return null
    }

    companion object {

        val DEFAULT_DATA = mapOf(
            "afterpay_clearpay" to
                LuxeNextAction(
                    handleNextActionSpec = mapOf(
                        StripeIntent.Status.RequiresAction to
                            RedirectNextActionSpec(
                                hostedPagePath = "next_action[redirect_to_url][url]",
                                returnToUrlPath = "next_action[redirect_to_url][return_url]"
                            )
                    ),
                    handlePiStatus = listOf(
                        PiStatusSpec(
                            associatedStatuses = listOf(StripeIntent.Status.Succeeded),
                            outcome = StripeIntentResult.Outcome.SUCCEEDED
                        ),
                        PiStatusSpec(
                            associatedStatuses = listOf(StripeIntent.Status.RequiresPaymentMethod),
                            outcome = StripeIntentResult.Outcome.FAILED
                        ),
                        PiStatusSpec(
                            associatedStatuses = listOf(StripeIntent.Status.RequiresAction),
                            outcome = StripeIntentResult.Outcome.CANCELED
                        )
                    )
                ),
            "konbini" to
                LuxeNextAction(
                    handleNextActionSpec = mapOf(
                        StripeIntent.Status.RequiresAction to
                            RedirectNextActionSpec(
                                hostedPagePath = "next_action[konbini_display_details][hosted_voucher_url]",
                                returnToUrlPath = "next_action[konbini_display_details][return_url]"
                            )
                    ),
                    handlePiStatus = listOf(
                        PiStatusSpec(
                            associatedStatuses = listOf(StripeIntent.Status.RequiresAction),
                            outcome = StripeIntentResult.Outcome.SUCCEEDED
                        )
                    )
                ),

            "sepa_debit" to
                LuxeNextAction(
                    handleNextActionSpec = mapOf(
                        StripeIntent.Status.Processing to null
                    ),
                    handlePiStatus = listOf(
                        PiStatusSpec(
                            associatedStatuses = listOf(StripeIntent.Status.Processing),
                            outcome = StripeIntentResult.Outcome.SUCCEEDED
                        )
                    )
                )
        )

        val Instance: LuxeNextActionRepository = LuxeNextActionRepository()
//            .apply { update(DEFAULT_DATA) }
    }

    data class PiStatusSpec(
        val associatedStatuses: List<StripeIntent.Status>,
        @StripeIntentResult.Outcome val outcome: Int
    )

    data class RedirectNextActionSpec(
        val hostedPagePath: String,
        val returnToUrlPath: String,
    )

    data class LuxeNextAction(
        val handleNextActionSpec: Map<StripeIntent.Status, RedirectNextActionSpec?>,
        val handlePiStatus: List<PiStatusSpec>,
    )
}
