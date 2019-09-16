package com.stripe.android.view

import android.content.Context
import android.content.res.Resources
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.TypefaceSpan
import androidx.annotation.ColorInt
import com.stripe.android.R
import com.stripe.android.model.PaymentMethod

internal class CardDisplayTextFactory(
    private val resources: Resources,
    private val themeConfig: ThemeConfig
) {
    fun createStyled(brand: String?, last4: String?, isSelected: Boolean): SpannableString {
        val brandText: String = resources.getString(BRAND_RESOURCE_MAP[brand] ?: R.string.unknown)
        val cardEndingIn = resources.getString(R.string.ending_in, brandText, last4)
        val totalLength = cardEndingIn.length
        val brandLength = brandText.length
        val last4length = last4!!.length
        val last4Start = totalLength - last4length
        @ColorInt val textColor = themeConfig.getTextColor(isSelected)
        @ColorInt val lightTextColor = themeConfig.getTextAlphaColor(isSelected)

        val displayString = SpannableString(cardEndingIn)

        // style brand
        displayString.setSpan(
            TypefaceSpan("sans-serif-medium"),
            0,
            brandLength,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        displayString.setSpan(
            ForegroundColorSpan(textColor),
            0,
            brandLength,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        // style "ending in"
        displayString.setSpan(
            ForegroundColorSpan(lightTextColor),
            brandLength,
            last4Start,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        // style last 4
        displayString.setSpan(
            TypefaceSpan("sans-serif-medium"),
            last4Start,
            totalLength,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        displayString.setSpan(
            ForegroundColorSpan(textColor),
            last4Start,
            totalLength,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        return displayString
    }

    fun createUnstyled(card: PaymentMethod.Card): String {
        return resources.getString(
            R.string.ending_in,
            resources.getString(BRAND_RESOURCE_MAP[card.brand] ?: R.string.unknown),
            card.last4
        )
    }

    companion object {
        private val BRAND_RESOURCE_MAP = mapOf(
            PaymentMethod.Card.Brand.AMERICAN_EXPRESS to R.string.amex_short,
            PaymentMethod.Card.Brand.DINERS_CLUB to R.string.diners_club,
            PaymentMethod.Card.Brand.DISCOVER to R.string.discover,
            PaymentMethod.Card.Brand.JCB to R.string.jcb,
            PaymentMethod.Card.Brand.MASTERCARD to R.string.mastercard,
            PaymentMethod.Card.Brand.VISA to R.string.visa,
            PaymentMethod.Card.Brand.UNIONPAY to R.string.unionpay,
            PaymentMethod.Card.Brand.UNKNOWN to R.string.unknown
        )

        @JvmStatic
        fun create(context: Context) =
            CardDisplayTextFactory(context.resources, ThemeConfig(context))
    }
}
