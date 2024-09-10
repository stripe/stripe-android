package com.stripe.android.view

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.annotation.VisibleForTesting
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import com.stripe.android.R
import com.stripe.android.databinding.StripeMaskedCardViewBinding
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.payments.model.R as PaymentsModelR

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

    internal val viewBinding = StripeMaskedCardViewBinding.inflate(
        LayoutInflater.from(context),
        this
    )
    private val themeConfig = ThemeConfig(context)
    private val cardDisplayFactory = CardDisplayTextFactory(resources, themeConfig)

    init {
        applyTint(viewBinding.brandIcon)
        applyTint(viewBinding.checkIcon)
    }

    private fun applyTint(imageView: ImageView) {
        ImageViewCompat.setImageTintList(
            imageView,
            ColorStateList.valueOf(
                themeConfig.getTintColor(true)
            )
        )
    }

    override fun setSelected(selected: Boolean) {
        super.setSelected(selected)
        updateCheckMark()
        updateUi()
    }

    fun setPaymentMethod(paymentMethod: PaymentMethod) {
        val card = paymentMethod.card

        cardBrand = CardBrand.fromCode(card?.displayBrand).takeIf { brand ->
            brand != CardBrand.Unknown
        } ?: card?.brand ?: CardBrand.Unknown

        last4 = card?.last4

        updateUi()
    }

    private fun updateUi() {
        updateBrandIcon()
        viewBinding.details.text = cardDisplayFactory.createStyled(cardBrand, last4, isSelected)
    }

    private fun updateBrandIcon() {
        viewBinding.brandIcon.setImageDrawable(
            ContextCompat.getDrawable(
                context,
                when (cardBrand) {
                    CardBrand.AmericanExpress -> R.drawable.stripe_ic_amex_template_32
                    CardBrand.Discover -> R.drawable.stripe_ic_discover_template_32
                    CardBrand.JCB -> R.drawable.stripe_ic_jcb_template_32
                    CardBrand.DinersClub -> R.drawable.stripe_ic_diners_template_32
                    CardBrand.Visa -> R.drawable.stripe_ic_visa_template_32
                    CardBrand.MasterCard -> R.drawable.stripe_ic_mastercard_template_32
                    CardBrand.UnionPay -> R.drawable.stripe_ic_unionpay_template_32
                    CardBrand.CartesBancaires -> R.drawable.stripe_ic_cartebancaire_template_32
                    CardBrand.Unknown -> PaymentsModelR.drawable.stripe_ic_unknown
                }
            )
        )
    }

    private fun updateCheckMark() {
        viewBinding.checkIcon.visibility = if (isSelected) {
            View.VISIBLE
        } else {
            View.INVISIBLE
        }
    }
}
