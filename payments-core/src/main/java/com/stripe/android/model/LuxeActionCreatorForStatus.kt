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
        abstract fun create(stripeIntentJsonString: String): LuxeNextActionRepository.Result
        data class RedirectActionCreator(
            val hostedPagePath: String,
            val returnToUrlPath: String?
        ) : ActionCreator() {
            override fun create(stripeIntentJsonString: String): LuxeNextActionRepository.Result {
                val stripeIntentJson = JSONObject(stripeIntentJsonString)
                val returnUrl = getPath(returnToUrlPath, stripeIntentJson)
                val url = getPath(hostedPagePath, stripeIntentJson)
                return if ((returnToUrlPath == null || returnUrl != null)
                    && (url != null)
                ) {
                    LuxeNextActionRepository.Result.Action(
                        StripeIntent.NextActionData.RedirectToUrl(
                            returnUrl = returnUrl,
                            url = Uri.parse(url)
                        )
                    )
                } else {
                    LuxeNextActionRepository.Result.NotSupported
                }
            }
        }

        /**
         * This function will take a path string like: next_action\[redirect]\[url] and
         * find that key path in the json object.
         */
        internal fun getPath(path: String?, json: JSONObject): String? {
            if (path != null) {
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
            }
            return null
        }

        object NoActionCreator : ActionCreator() {
            override fun create(stripeIntentJsonString: String) =
                LuxeNextActionRepository.Result.NoAction
        }
    }
}

