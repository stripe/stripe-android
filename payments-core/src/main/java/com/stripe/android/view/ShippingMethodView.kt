package com.stripe.android.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.RelativeLayout
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import com.stripe.android.R
import com.stripe.android.databinding.StripeShippingMethodViewBinding
import com.stripe.android.model.ShippingMethod

/**
 * Renders the information related to a shipping method.
 */
internal class ShippingMethodView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr) {
    private val colorUtils = StripeColorUtils(context)

    @ColorInt
    private val selectedColorInt: Int

    @ColorInt
    private val unselectedTextColorSecondaryInt: Int

    @ColorInt
    private val unselectedTextColorPrimaryInt: Int

    private val viewBinding = StripeShippingMethodViewBinding.inflate(
        LayoutInflater.from(context),
        this
    )

    init {
        val rawSelectedColorInt = colorUtils.colorAccent
        val rawUnselectedTextColorPrimaryInt = colorUtils.textColorPrimary
        val rawUnselectedTextColorSecondaryInt = colorUtils.textColorSecondary

        selectedColorInt =
            if (StripeColorUtils.isColorTransparent(rawSelectedColorInt)) {
                ContextCompat.getColor(context, R.color.stripe_accent_color_default)
            } else {
                rawSelectedColorInt
            }

        unselectedTextColorPrimaryInt =
            if (StripeColorUtils.isColorTransparent(rawUnselectedTextColorPrimaryInt)) {
                ContextCompat.getColor(context, R.color.stripe_color_text_unselected_primary_default)
            } else {
                rawUnselectedTextColorPrimaryInt
            }

        unselectedTextColorSecondaryInt =
            if (StripeColorUtils.isColorTransparent(rawUnselectedTextColorSecondaryInt)) {
                ContextCompat.getColor(context, R.color.stripe_color_text_unselected_secondary_default)
            } else {
                rawUnselectedTextColorSecondaryInt
            }
    }

    override fun setSelected(selected: Boolean) {
        if (selected) {
            viewBinding.name.setTextColor(selectedColorInt)
            viewBinding.description.setTextColor(selectedColorInt)
            viewBinding.price.setTextColor(selectedColorInt)
            viewBinding.selectedIcon.visibility = View.VISIBLE
        } else {
            viewBinding.name.setTextColor(unselectedTextColorPrimaryInt)
            viewBinding.description.setTextColor(unselectedTextColorSecondaryInt)
            viewBinding.price.setTextColor(unselectedTextColorPrimaryInt)
            viewBinding.selectedIcon.visibility = View.INVISIBLE
        }
    }

    fun setShippingMethod(shippingMethod: ShippingMethod) {
        viewBinding.name.text = shippingMethod.label
        viewBinding.description.text = shippingMethod.detail
        viewBinding.price.text = PaymentUtils.formatPriceStringUsingFree(
            shippingMethod.amount,
            shippingMethod.currency,
            context.getString(R.string.stripe_price_free)
        )
    }
}
