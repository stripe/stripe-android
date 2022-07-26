package com.stripe.android.model

import android.net.Uri
import androidx.annotation.RestrictTo
import org.json.JSONObject

data class LuxeActionCreatorForStatus(
    val status: StripeIntent.Status,
    val actionCreator: ActionCreator
) {
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    sealed class ActionCreator {
        fun create(stripeIntentJsonString: String) =
            create(JSONObject(stripeIntentJsonString))

        internal abstract fun create(stripeIntentJson: JSONObject): LuxeNextActionRepository.Result

        /**
         * The returnToUrlPath cannot be null because otherwise it will trigger the web flow
         * which we do not want.
         */
        data class RedirectActionCreator(
            val redirectPagePath: String,
            val returnToUrlPath: String?
        ) : ActionCreator() {
            override fun create(stripeIntentJson: JSONObject): LuxeNextActionRepository.Result {
                val returnUrl = getPath(returnToUrlPath, stripeIntentJson)
                val url = getPath(redirectPagePath, stripeIntentJson)
                return if (url != null) {
                    LuxeNextActionRepository.Result.Action(
                        StripeIntent.NextActionData.RedirectToUrl(
                            returnUrl = returnUrl ?: "stripesdk://payment_return_url/com.stripe.android.paymentsheet.example",
                            url = Uri.parse(url)
                        )
                    )
                } else {
                    LuxeNextActionRepository.Result.NotSupported
                }
            }
        }

        object NoActionCreator : ActionCreator() {
            override fun create(stripeIntentJson: JSONObject) =
                LuxeNextActionRepository.Result.NoAction
        }
    }

    internal companion object {
        /**
         * This function will take a path string like: next_action\[redirect]\[url] and
         * find that key path in the json object.
         */
        internal fun getPath(path: String?, json: JSONObject): String? {
            if (path == null) {
                return null
            }
            val pathArray = ("[*" + "([A-Za-z_0-9]+)" + "]*").toRegex().findAll(path)
                .map { it.value }
                .distinct()
                .filterNot { it.isEmpty() }
                .toList()
            var jsonObject: JSONObject? = json
            var pathIndex = 0
            while (pathIndex < pathArray.size &&
                jsonObject != null &&
                jsonObject.opt(pathArray[pathIndex]) !is String
            ) {
                val key = pathArray[pathIndex]
                if (jsonObject.has(key)) {
                    val tempJsonObject = jsonObject.optJSONObject(key)

                    if (tempJsonObject != null) {
                        jsonObject = tempJsonObject
                    }
                }
                pathIndex++
            }
            return jsonObject?.opt(pathArray[pathArray.size - 1]) as? String
        }
    }
}
