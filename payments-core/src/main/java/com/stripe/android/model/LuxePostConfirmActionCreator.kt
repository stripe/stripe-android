package com.stripe.android.model

import android.net.Uri
import androidx.annotation.RestrictTo
import org.json.JSONObject

sealed class LuxePostConfirmActionCreator {
    internal abstract fun create(stripeIntentJson: JSONObject): LuxePostConfirmActionRepository.Result

    data class RedirectActionCreator
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    constructor(
        val redirectPagePath: String,
        val returnToUrlPath: String
    ) : LuxePostConfirmActionCreator() {
        override fun create(stripeIntentJson: JSONObject): LuxePostConfirmActionRepository.Result {
            val returnUrl = getPath(returnToUrlPath, stripeIntentJson)
            val url = getPath(redirectPagePath, stripeIntentJson)
            return if ((returnUrl != null) && (url != null)) {
                LuxePostConfirmActionRepository.Result.Action(
                    StripeIntent.NextActionData.RedirectToUrl(
                        returnUrl = returnUrl,
                        url = Uri.parse(url)
                    )
                )
            } else {
                LuxePostConfirmActionRepository.Result.NotSupported
            }
        }
    }

    object NoActionCreator : LuxePostConfirmActionCreator() {
        override fun create(stripeIntentJson: JSONObject) =
            LuxePostConfirmActionRepository.Result.NoAction
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
