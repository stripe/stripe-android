package com.stripe.android.view

import android.content.Context
import android.content.res.Resources
import android.text.ParcelableSpan
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.TypefaceSpan
import androidx.annotation.ColorInt
import com.stripe.android.R
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod

internal class CardDisplayTextFactory internal constructor(
    private val resources: Resources,
    private val themeConfig: ThemeConfig
) {
    internal constructor(context: Context) : this(context.resources, ThemeConfig(context))

    @JvmSynthetic
    internal fun createStyled(
        brand: CardBrand,
        last4: String?,
        isSelected: Boolean
    ): SpannableString {
        val brandText: String = brand.displayName
        val brandLength = brandText.length
        if (last4 == null) {
            val displayString = SpannableString(brandText)
            setSpan(
                displayString,
                TypefaceSpan("sans-serif-medium"),
                0,
                brandLength
            )
            return displayString
        }

        val cardEndingIn = resources.getString(R.string.card_ending_in, brandText, last4)
        val totalLength = cardEndingIn.length
        val last4length = last4.length
        val last4Start = totalLength - last4length
        @ColorInt val textColor = themeConfig.getTextColor(isSelected)
        @ColorInt val lightTextColor = themeConfig.getTextAlphaColor(isSelected)

        val displayString = SpannableString(cardEndingIn)

        // style brand
        setSpan(
            displayString,
            TypefaceSpan("sans-serif-medium"),
            0,
            brandLength
        )
        setSpan(
            displayString,
            ForegroundColorSpan(textColor),
            0,
            brandLength
        )

        // style "ending in"
        setSpan(
            displayString,
            ForegroundColorSpan(lightTextColor),
            brandLength,
            last4Start
        )

        // style last 4
        setSpan(
            displayString,
            TypefaceSpan("sans-serif-medium"),
            last4Start,
            totalLength
        )
        setSpan(
            displayString,
            ForegroundColorSpan(textColor),
            last4Start,
            totalLength
        )

        return displayString
    }

    @JvmSynthetic
    internal fun createUnstyled(card: PaymentMethod.Card): String {
        return resources.getString(
            R.string.card_ending_in,
            card.brand.displayName,
            card.last4
        )
    }

    private fun setSpan(
        displayString: SpannableString,
        span: ParcelableSpan,
        start: Int,
        end: Int
    ) {
        displayString.setSpan(
            span,
            start,
            end,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }
}
