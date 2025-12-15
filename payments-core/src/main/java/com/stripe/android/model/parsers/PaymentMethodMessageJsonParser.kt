package com.stripe.android.model.parsers

import com.stripe.android.core.model.parsers.ModelJsonParser
import com.stripe.android.core.model.parsers.ModelJsonParser.Companion.jsonArrayToList
import com.stripe.android.model.PaymentMethodMessage
import com.stripe.android.model.PaymentMethodMessageImage
import com.stripe.android.model.PaymentMethodMessageLearnMore
import com.stripe.android.model.PaymentMethodMessageLegalDisclosure
import org.json.JSONArray
import org.json.JSONObject

internal class PaymentMethodMessageJsonParser : ModelJsonParser<PaymentMethodMessage> {
    override fun parse(json: JSONObject): PaymentMethodMessage? {
        val paymentMethods = jsonArrayToList(json.optJSONArray(FIELD_PAYMENT_METHODS))
        val singlePartner = maybeGetSinglePartner(json, paymentMethods)
        val multiPartner = maybeGetMultiPartner(json, paymentMethods)
        val unexpectedError = maybeGetUnexpectedError(json)

        return unexpectedError ?: singlePartner ?: multiPartner ?: PaymentMethodMessage.NoContent(paymentMethods)
    }

    private fun maybeGetMultiPartner(
        json: JSONObject,
        paymentMethods: List<String>
    ): PaymentMethodMessage.MultiPartner? {
        val content = json.optJSONObject(FIELD_CONTENT) ?: return null
        val imagesMap = getImages(content.optJSONArray(FIELD_IMAGES))
        val learnMore = getLearnMore(content) ?: return null
        val promotion = getPromotion(content) ?: return null
        val legalDisclosure = getLegalDisclosure(content)
        return PaymentMethodMessage.MultiPartner(
            promotion = promotion,
            lightImages = imagesMap[FIELD_LIGHT_THEME_PNG] ?: emptyList(),
            darkImages = imagesMap[FIELD_DARK_THEME_PNG] ?: emptyList(),
            flatImages = imagesMap[FIELD_FLAT_THEME_PNG] ?: emptyList(),
            learnMore = learnMore,
            legalDisclosure = legalDisclosure,
            paymentMethods = paymentMethods
        )
    }

    private fun maybeGetSinglePartner(
        json: JSONObject,
        paymentMethods: List<String>
    ): PaymentMethodMessage.SinglePartner? {
        val paymentPlanGroups = json.optJSONArray(FIELD_PAYMENT_PLAN_GROUPS)
        // Only use inline_partner_promotion if one payment plan group is present
        if (paymentPlanGroups?.length() != 1) return null
        val paymentPlanGroup = paymentPlanGroups.get(0) as? JSONObject ?: return null
        val content = paymentPlanGroup.optJSONObject(FIELD_CONTENT) ?: return null
        val inlinePartnerPromotion = getInlinePartnerPromotion(content) ?: return null
        val learnMore = getLearnMore(content) ?: return null

        val imagesMap = getImages(content.optJSONArray(FIELD_IMAGES))
        // Because the single partner image will be displayed inline, if any of the images are not present
        // return null so we'll fallback to multi-partner
        val lightImage = getImage(imagesMap[FIELD_LIGHT_THEME_PNG]) ?: return null
        val darkImage = getImage(imagesMap[FIELD_DARK_THEME_PNG]) ?: return null
        val flatImage = getImage(imagesMap[FIELD_FLAT_THEME_PNG]) ?: return null
        val legalDisclosure = getLegalDisclosure(content)

        return PaymentMethodMessage.SinglePartner(
            inlinePartnerPromotion = inlinePartnerPromotion,
            lightImage = lightImage,
            darkImage = darkImage,
            flatImage = flatImage,
            learnMore = learnMore,
            legalDisclosure = legalDisclosure,
            paymentMethods = paymentMethods
        )
    }

    /**
     * Check if expected keys exist. These keys can be mapped to null or empty objects but they should always be
     * present in the response.
     */
    private fun maybeGetUnexpectedError(json: JSONObject): PaymentMethodMessage.UnexpectedError? {
        val paymentPlanGroups = json.optJSONArray(FIELD_PAYMENT_PLAN_GROUPS)
        if (paymentPlanGroups?.length() == 1) {
            val paymentPlanGroup = paymentPlanGroups.get(0) as? JSONObject
            val contentExists = paymentPlanGroup?.has(FIELD_CONTENT) == true
            if (!contentExists) {
                return PaymentMethodMessage.UnexpectedError(
                    "payment_plan_groups.content not found"
                )
            }
            val content = paymentPlanGroup.get(FIELD_CONTENT) as? JSONObject
            val inlinePartnerPromotionExists = content?.has(FIELD_INLINE_PARTNER_PROMOTION) == true
            if (!inlinePartnerPromotionExists) {
                return PaymentMethodMessage.UnexpectedError(
                    "payment_plan_groups.content.inline_partner_promotion not found"
                )
            }
        } else {
            val contentExists = json.has(FIELD_CONTENT)
            if (!contentExists) {
                return PaymentMethodMessage.UnexpectedError(
                    "content not found"
                )
            }
            val content = json.optJSONObject(FIELD_CONTENT)
            val promotionExists = content?.has(FIELD_PROMOTION) == true
            if (!promotionExists) {
                return PaymentMethodMessage.UnexpectedError(
                    "content.promotion not found"
                )
            }
        }
        return null
    }

    private fun getPromotion(json: JSONObject): String? {
        val promotion = json.optJSONObject(FIELD_PROMOTION)
        return promotion?.optString(FIELD_MESSAGE).takeIf { !it.isNullOrBlank() }
    }

    private fun getInlinePartnerPromotion(json: JSONObject): String? {
        val inlinePartnerPromotion = json.optJSONObject(FIELD_INLINE_PARTNER_PROMOTION)
        return inlinePartnerPromotion?.optString(FIELD_MESSAGE).takeIf { !it.isNullOrBlank() }
    }

    private fun getLearnMore(json: JSONObject): PaymentMethodMessageLearnMore? {
        val learnMore = json.optJSONObject(FIELD_LEARN_MORE) ?: return null
        val url = learnMore.optString(FIELD_URL)
        val message = learnMore.optString(FIELD_MESSAGE)
        if (url.isNullOrBlank()) return null
        return PaymentMethodMessageLearnMore(
            url = url,
            message = message
        )
    }

    private fun getLegalDisclosure(json: JSONObject): PaymentMethodMessageLegalDisclosure? {
        val legalDisclosure = json.optJSONObject(FIELD_LEGAL_DISCLOSURE)
        val message = legalDisclosure?.optString(FIELD_MESSAGE)
        return if (!message.isNullOrBlank()) {
            PaymentMethodMessageLegalDisclosure(
                message = message
            )
        } else {
            null
        }
    }

    /**
     * Images are sent as an array of objects, each object containing dark, flat, and light PNG and SVG URLs
     * for all payment methods (affirm, klarna, afterpay_clearpay) and image types (logo, icon).
     */
    private fun getImages(json: JSONArray?): Map<String, List<PaymentMethodMessageImage>> {
        if (json == null) return emptyMap()
        val images = mutableMapOf<String, MutableList<PaymentMethodMessageImage>>(
            FIELD_LIGHT_THEME_PNG to mutableListOf(),
            FIELD_DARK_THEME_PNG to mutableListOf(),
            FIELD_FLAT_THEME_PNG to mutableListOf()
        )

        for (i in 0 until json.length()) {
            val obj = json.optJSONObject(i)
            val paymentMethodType = obj.optString(FIELD_PAYMENT_METHOD_TYPE)
            val role = obj.optString(FIELD_ROLE)
            val text = obj.optString(FIELD_TEXT)

            if (role != IMAGE_TYPE_LOGO) continue

            listOf(FIELD_LIGHT_THEME_PNG, FIELD_DARK_THEME_PNG, FIELD_FLAT_THEME_PNG).forEach { key ->
                val url = obj.optJSONObject(key)?.optString(FIELD_URL)
                if (!url.isNullOrEmpty()) {
                    images[key]?.add(PaymentMethodMessageImage(role, url, paymentMethodType, text))
                }
            }
        }

        return images
    }

    private fun getImage(imageList: List<PaymentMethodMessageImage>?): PaymentMethodMessageImage? =
        if (imageList.isNullOrEmpty()) null else imageList[0]

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
        const val FIELD_LEGAL_DISCLOSURE = "legal_disclosure"
    }
}
