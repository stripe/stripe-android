package com.stripe.android.view

import android.content.Context
import android.text.SpannableString
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
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

    private val themeConfig = ThemeConfig(context)
    private val cardDisplayFactory = CardDisplayTextFactory(resources, themeConfig)

    val textColorValues: IntArray
        @VisibleForTesting
        get() = themeConfig.textColorValues

    init {
        View.inflate(getContext(), R.layout.masked_card_view, this)
        cardIconImageView = findViewById(R.id.masked_icon_view)
        cardInformationTextView = findViewById(R.id.masked_card_info_view)
        checkMarkImageView = findViewById(R.id.masked_check_icon)

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
        val drawable = ContextCompat.getDrawable(context, resourceId) ?: return
        val icon = DrawableCompat.wrap(drawable)
        DrawableCompat.setTint(
            icon.mutate(),
            themeConfig.getTintColor(isSelected || isCheckMark)
        )
        imageView.setImageDrawable(icon)
    }

    private fun createDisplayString(): SpannableString {
        return cardDisplayFactory.createStyled(cardBrand, last4, isSelected)
    }

    private fun updateCheckMark() {
        if (isSelected) {
            checkMarkImageView.visibility = View.VISIBLE
        } else {
            checkMarkImageView.visibility = View.INVISIBLE
        }
    }

    private companion object {
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
    }
}
