package com.stripe.android.model.parsers

import com.stripe.android.core.model.parsers.ModelJsonParser
import com.stripe.android.core.model.parsers.ModelJsonParser.Companion.jsonArrayToList
import com.stripe.android.model.MessagingImageType
import com.stripe.android.model.MessagingImage
import com.stripe.android.model.PaymentMethodMessage
import org.json.JSONArray
import org.json.JSONObject

internal class PaymentMethodMessageJsonParser : ModelJsonParser<PaymentMethodMessage> {
    override fun parse(json: JSONObject): PaymentMethodMessage? {
        val paymentMethods = jsonArrayToList(json.optJSONArray(FIELD_PAYMENT_METHODS))
        val content = json.optJSONObject(FIELD_CONTENT) ?: return null
        val imagesMap = getImages(content.optJSONArray(FIELD_IMAGES))
        val learnMore = getLearnMoreUrl(content)
        val promotion = getPromotion(content)
        val inlinePartnerPromo = maybeGetInlinePartnerPromotion(json, paymentMethods)

        return PaymentMethodMessage(
            paymentMethods = paymentMethods,
            inlinePartnerPromotion = inlinePartnerPromo,
            promotion = promotion,
            lightImages = imagesMap[FIELD_LIGHT_THEME_PNG] ?: emptyList(),
            darkImages = imagesMap[FIELD_DARK_THEME_PNG] ?: emptyList(),
            flatImages = imagesMap[FIELD_FLAT_THEME_PNG] ?: emptyList(),
            learnMoreUrl = learnMore
        )
    }

    private fun maybeGetInlinePartnerPromotion(json: JSONObject, paymentMethods: List<String>): String? {
        // Only use inline_partner_promotion if only one payment method is available
        if (paymentMethods.size != 1) return null
        val paymentPlanGroups = json.optJSONArray(FIELD_PAYMENT_PLAN_GROUPS)
        val paymentPlanGroup = paymentPlanGroups?.get(0) as? JSONObject ?: return null
        val content = paymentPlanGroup.optJSONObject(FIELD_CONTENT)
        val inlinePartnerPromotion = content?.optJSONObject(FIELD_INLINE_PARTNER_PROMOTION)
        return inlinePartnerPromotion?.optString(FIELD_MESSAGE).takeIf { !it.isNullOrBlank() }
    }

    private fun getPromotion(json: JSONObject): String? {
        val promotion = json.optJSONObject(FIELD_PROMOTION)
        return promotion?.optString(FIELD_MESSAGE).takeIf { !it.isNullOrBlank() }
    }

    private fun getLearnMoreUrl(json: JSONObject): String? {
        val learnMore = json.optJSONObject(FIELD_LEARN_MORE)
        return learnMore?.optString(FIELD_URL).takeIf { !it.isNullOrBlank() }
    }

    private fun getImages(json: JSONArray?): Map<String, List<MessagingImage>> {
        if (json == null) return emptyMap()
        val images = mutableMapOf<String, MutableList<MessagingImage>>(
            FIELD_LIGHT_THEME_PNG to mutableListOf(),
            FIELD_DARK_THEME_PNG to mutableListOf(),
            FIELD_FLAT_THEME_PNG to mutableListOf()
        )

        for (i in 0 until json.length()) {
            val obj = json.optJSONObject(i) ?: continue
            val paymentMethodType = obj.optString(FIELD_PAYMENT_METHOD_TYPE)
            val role = obj.optString(FIELD_ROLE)
            val text = obj.optString(FIELD_TEXT)

            listOf(FIELD_LIGHT_THEME_PNG, FIELD_DARK_THEME_PNG, FIELD_FLAT_THEME_PNG).forEach { key ->
                val url = obj.optJSONObject(key)?.optString(FIELD_URL)
                if (!url.isNullOrEmpty() && role == IMAGE_TYPE_LOGO) {
                    images[key]?.add(MessagingImage(role, url, paymentMethodType, text))
                }
            }
        }

        return images
    }

    private companion object {
        const val FIELD_IMAGES = "images"
        const val FIELD_DARK_THEME_PNG = "dark_theme_png"
        const val FIELD_FLAT_THEME_PNG = "flat_theme_png"
        const val FIELD_LIGHT_THEME_PNG = "light_theme_png"
        const val FIELD_PAYMENT_METHOD_TYPE = "payment_method_type"
        const val FIELD_PAYMENT_METHODS = "payment_methods"
        const val FIELD_ROLE = "role"
        const val FIELD_TEXT = "text"
        const val FIELD_INLINE_PARTNER_PROMOTION = "inline_partner_promotion"
        const val FIELD_PAYMENT_PLAN_GROUPS = "payment_plan_groups"
        const val FIELD_LEARN_MORE = "learn_more"
        const val FIELD_MESSAGE = "message"
        const val FIELD_PROMOTION = "promotion"
        const val FIELD_URL = "url"
        const val FIELD_CONTENT = "content"
        const val IMAGE_TYPE_LOGO = "logo"
    }
}
