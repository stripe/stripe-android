package com.stripe.android.view

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import com.stripe.android.R
import com.stripe.android.model.ShippingMethod

/**
 * Renders the information related to a shipping method.
 */
internal class ShippingMethodView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr) {
    private val label: TextView
    private val detail: TextView
    private val amount: TextView
    private val checkmark: ImageView
    private val colorUtils = StripeColorUtils(context)

    @ColorInt
    private val selectedColorInt: Int
    @ColorInt
    private val unselectedTextColorSecondaryInt: Int
    @ColorInt
    private val unselectedTextColorPrimaryInt: Int

    init {
        View.inflate(context, R.layout.shipping_method_view, this)
        label = findViewById(R.id.tv_label_smv)
        detail = findViewById(R.id.tv_detail_smv)
        amount = findViewById(R.id.tv_amount_smv)
        checkmark = findViewById(R.id.iv_selected_icon)

        val rawSelectedColorInt = colorUtils.getThemeAccentColor().data
        val rawUnselectedTextColorPrimaryInt = colorUtils.getThemeTextColorPrimary().data
        val rawUnselectedTextColorSecondaryInt = colorUtils.getThemeTextColorSecondary().data

        selectedColorInt =
            if (StripeColorUtils.isColorTransparent(rawSelectedColorInt)) {
                ContextCompat.getColor(context, R.color.accent_color_default)
            } else {
                rawSelectedColorInt
            }

        unselectedTextColorPrimaryInt =
            if (StripeColorUtils.isColorTransparent(rawUnselectedTextColorPrimaryInt)) {
                ContextCompat.getColor(context, R.color.color_text_unselected_primary_default)
            } else {
                rawUnselectedTextColorPrimaryInt
            }

        unselectedTextColorSecondaryInt =
            if (StripeColorUtils.isColorTransparent(rawUnselectedTextColorSecondaryInt)) {
                ContextCompat.getColor(context, R.color.color_text_unselected_secondary_default)
            } else {
                rawUnselectedTextColorSecondaryInt
            }

        val params = LayoutParams(LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        params.addRule(Gravity.CENTER_VERTICAL)
        params.height = ViewUtils.getPxFromDp(
            context,
            resources.getDimensionPixelSize(R.dimen.shipping_method_view_height)
        )
        layoutParams = params
    }

    override fun setSelected(selected: Boolean) {
        if (selected) {
            label.setTextColor(selectedColorInt)
            detail.setTextColor(selectedColorInt)
            amount.setTextColor(selectedColorInt)
            checkmark.visibility = View.VISIBLE
        } else {
            label.setTextColor(unselectedTextColorPrimaryInt)
            detail.setTextColor(unselectedTextColorSecondaryInt)
            amount.setTextColor(unselectedTextColorPrimaryInt)
            checkmark.visibility = View.INVISIBLE
        }
    }

    fun setShippingMethod(shippingMethod: ShippingMethod) {
        label.text = shippingMethod.label
        detail.text = shippingMethod.detail
        amount.text = PaymentUtils.formatPriceStringUsingFree(
            shippingMethod.amount,
            shippingMethod.currency,
            context.getString(R.string.price_free)
        )
    }
}
