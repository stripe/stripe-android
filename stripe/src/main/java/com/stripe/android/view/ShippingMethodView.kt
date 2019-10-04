package com.stripe.android.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.RelativeLayout
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import com.stripe.android.R
import com.stripe.android.model.ShippingMethod
import kotlinx.android.synthetic.main.shipping_method_view.view.*

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

    init {
        View.inflate(context, R.layout.shipping_method_view, this)

        val rawSelectedColorInt = colorUtils.getThemeAccentColor().data
        val rawUnselectedTextColorPrimaryInt = colorUtils.getThemeTextColorPrimary().data
        val rawUnselectedTextColorSecondaryInt = colorUtils.getThemeTextColorSecondary().data

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
            shipping_method_name.setTextColor(selectedColorInt)
            shipping_method_description.setTextColor(selectedColorInt)
            shipping_method_price.setTextColor(selectedColorInt)
            selected_icon.visibility = View.VISIBLE
        } else {
            shipping_method_name.setTextColor(unselectedTextColorPrimaryInt)
            shipping_method_description.setTextColor(unselectedTextColorSecondaryInt)
            shipping_method_price.setTextColor(unselectedTextColorPrimaryInt)
            selected_icon.visibility = View.INVISIBLE
        }
    }

    fun setShippingMethod(shippingMethod: ShippingMethod) {
        shipping_method_name.text = shippingMethod.label
        shipping_method_description.text = shippingMethod.detail
        shipping_method_price.text = PaymentUtils.formatPriceStringUsingFree(
            shippingMethod.amount,
            shippingMethod.currency,
            context.getString(R.string.price_free)
        )
    }
}
