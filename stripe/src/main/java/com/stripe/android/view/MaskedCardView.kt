package com.stripe.android.view

import android.content.Context
import android.text.SpannableString
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.VisibleForTesting
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.stripe.android.R
import com.stripe.android.model.CardBrand
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

    var cardBrand: CardBrand = CardBrand.Unknown
        private set

    @get:VisibleForTesting
    var last4: String? = null
        private set

    private val cardIconImageView: ImageView
    private val cardInformationTextView: TextView
    private val checkMarkImageView: ImageView

    private val themeConfig = ThemeConfig(context)
    private val cardDisplayFactory = CardDisplayTextFactory(resources, themeConfig)

    val textColorValues: IntArray = themeConfig.textColorValues

    init {
        View.inflate(getContext(), R.layout.masked_card_view, this)
        cardIconImageView = findViewById(R.id.masked_icon_view)
        cardInformationTextView = findViewById(R.id.masked_card_info_view)
        checkMarkImageView = findViewById(R.id.masked_check_icon)

        initializeCheckMark()
        updateCheckMark()
    }

    override fun setSelected(selected: Boolean) {
        super.setSelected(selected)
        updateCheckMark()
        updateUi()
    }

    fun setPaymentMethod(paymentMethod: PaymentMethod) {
        cardBrand = paymentMethod.card?.let {
            CardBrand.fromCode(it.brand)
        } ?: CardBrand.Unknown
        last4 = paymentMethod.card?.last4
        updateUi()
    }

    private fun updateUi() {
        updateBrandIcon()
        cardInformationTextView.text = createDisplayString()
    }

    private fun initializeCheckMark() {
        updateImageViewDrawable(R.drawable.stripe_ic_checkmark, checkMarkImageView, true)
    }

    private fun updateBrandIcon() {
        updateImageViewDrawable(
            when (cardBrand) {
                CardBrand.AmericanExpress -> R.drawable.stripe_ic_amex_template_32
                CardBrand.Discover -> R.drawable.stripe_ic_discover_template_32
                CardBrand.JCB -> R.drawable.stripe_ic_jcb_template_32
                CardBrand.DinersClub -> R.drawable.stripe_ic_diners_template_32
                CardBrand.Visa -> R.drawable.stripe_ic_visa_template_32
                CardBrand.MasterCard -> R.drawable.stripe_ic_mastercard_template_32
                CardBrand.UnionPay -> R.drawable.stripe_ic_unionpay_template_32
                CardBrand.Unknown -> R.drawable.stripe_ic_unknown
            },
            cardIconImageView,
            false
        )
    }

    private fun updateImageViewDrawable(
        @DrawableRes resourceId: Int,
        imageView: ImageView,
        isCheckMark: Boolean
    ) {
        ContextCompat.getDrawable(context, resourceId)?.let {
            val icon = DrawableCompat.wrap(it)
            DrawableCompat.setTint(
                icon.mutate(),
                themeConfig.getTintColor(isSelected || isCheckMark)
            )
            imageView.setImageDrawable(icon)
        }
    }

    private fun createDisplayString(): SpannableString {
        return cardDisplayFactory.createStyled(cardBrand, last4, isSelected)
    }

    private fun updateCheckMark() {
        checkMarkImageView.visibility = if (isSelected) {
            View.VISIBLE
        } else {
            View.INVISIBLE
        }
    }
}
