package com.stripe.android.view

import android.content.Context
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.TypefaceSpan
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.VisibleForTesting
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.stripe.android.R
import com.stripe.android.model.PaymentMethod

/**
 * View that displays card information without revealing the entire number, usually for
 * selection in a list. The view can be toggled to "selected" state. Colors for the selected
 * and unselected states are taken from the host Activity theme's
 * "colorAccent" and "colorControlNormal" states.
 */
internal class MaskedCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : LinearLayout(context, attrs, defStyle) {

    @PaymentMethod.Card.Brand
    @get:PaymentMethod.Card.Brand
    @get:VisibleForTesting
    var cardBrand: String? = null
        private set

    @get:VisibleForTesting
    var last4: String? = null
        private set

    private var isSelected: Boolean = false

    private val cardIconImageView: AppCompatImageView
    private val cardInformationTextView: AppCompatTextView
    private val checkMarkImageView: AppCompatImageView

    private val themeConfig: ThemeConfig

    val textColorValues: IntArray
        @VisibleForTesting
        get() = themeConfig.textColorValues

    init {
        View.inflate(getContext(), R.layout.masked_card_view, this)
        cardIconImageView = findViewById(R.id.masked_icon_view)
        cardInformationTextView = findViewById(R.id.masked_card_info_view)
        checkMarkImageView = findViewById(R.id.masked_check_icon)

        themeConfig = ThemeConfig(context)

        initializeCheckMark()
        updateCheckMark()
    }

    override fun isSelected(): Boolean {
        return isSelected
    }

    override fun setSelected(selected: Boolean) {
        isSelected = selected
        updateCheckMark()
        updateUi()
    }

    fun setPaymentMethod(paymentMethod: PaymentMethod) {
        cardBrand = if (paymentMethod.card != null)
            paymentMethod.card.brand
        else
            PaymentMethod.Card.Brand.UNKNOWN
        last4 = if (paymentMethod.card != null) paymentMethod.card.last4 else ""
        updateUi()
    }

    private fun updateUi() {
        updateBrandIcon()
        cardInformationTextView.text = createDisplayString()
    }

    private fun initializeCheckMark() {
        updateDrawable(R.drawable.ic_checkmark, checkMarkImageView, true)
    }

    private fun updateBrandIcon() {
        @DrawableRes val brandIconResId = ICON_RESOURCE_MAP[cardBrand]
        if (brandIconResId != null) {
            updateDrawable(brandIconResId, cardIconImageView, false)
        }
    }

    private fun updateDrawable(
        @DrawableRes resourceId: Int,
        imageView: ImageView,
        isCheckMark: Boolean
    ) {
        val icon = DrawableCompat.wrap(
            ContextCompat.getDrawable(context, resourceId)!!
        )
        DrawableCompat.setTint(
            icon.mutate(),
            themeConfig.getTintColor(isSelected || isCheckMark)
        )
        imageView.setImageDrawable(icon)
    }

    private fun createDisplayString(): SpannableString {
        val brandText: String = if (BRAND_RESOURCE_MAP.containsKey(cardBrand)) {
            resources.getString(BRAND_RESOURCE_MAP[cardBrand]!!)
        } else {
            resources.getString(R.string.unknown)
        }
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

    private fun updateCheckMark() {
        if (isSelected) {
            checkMarkImageView.visibility = View.VISIBLE
        } else {
            checkMarkImageView.visibility = View.INVISIBLE
        }
    }

    companion object {
        private val ICON_RESOURCE_MAP = mapOf(
            PaymentMethod.Card.Brand.AMERICAN_EXPRESS to R.drawable.ic_amex_template_32,
            PaymentMethod.Card.Brand.DINERS_CLUB to R.drawable.ic_diners_template_32,
            PaymentMethod.Card.Brand.DISCOVER to R.drawable.ic_discover_template_32,
            PaymentMethod.Card.Brand.JCB to R.drawable.ic_jcb_template_32,
            PaymentMethod.Card.Brand.MASTERCARD to R.drawable.ic_mastercard_template_32,
            PaymentMethod.Card.Brand.VISA to R.drawable.ic_visa_template_32,
            PaymentMethod.Card.Brand.UNIONPAY to R.drawable.ic_unionpay_template_32,
            PaymentMethod.Card.Brand.UNKNOWN to R.drawable.ic_unknown
        )
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
    }
}
