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
        if (last4.isNullOrBlank()) {
            val displayString = SpannableString(brandText)
            setSpan(
                displayString,
                TypefaceSpan("sans-serif-medium"),
                0,
                brandLength
            )
            return displayString
        }

        val cardEndingIn = resources.getString(R.string.stripe_card_ending_in, brandText, last4)
        val totalLength = cardEndingIn.length
        val last4Start = cardEndingIn.indexOf(last4)
        val last4End = last4Start + last4.length
        val brandStart = cardEndingIn.indexOf(brandText)
        val brandEnd = brandStart + brandText.length

        @ColorInt val textColor = themeConfig.getTextColor(isSelected)

        @ColorInt val lightTextColor = themeConfig.getTextAlphaColor(isSelected)

        val displayString = SpannableString(cardEndingIn)

        // style full string
        setSpan(
            displayString,
            ForegroundColorSpan(lightTextColor),
            0,
            totalLength
        )

        // override brand style
        setSpan(
            displayString,
            TypefaceSpan("sans-serif-medium"),
            brandStart,
            brandEnd
        )
        setSpan(
            displayString,
            ForegroundColorSpan(textColor),
            brandStart,
            brandEnd
        )

        // override last 4 style
        setSpan(
            displayString,
            TypefaceSpan("sans-serif-medium"),
            last4Start,
            last4End
        )
        setSpan(
            displayString,
            ForegroundColorSpan(textColor),
            last4Start,
            last4End
        )

        return displayString
    }

    @JvmSynthetic
    internal fun createUnstyled(card: PaymentMethod.Card): String {
        return resources.getString(
            R.string.stripe_card_ending_in,
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
