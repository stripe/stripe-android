package com.stripe.android.view

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.annotation.ColorInt
import androidx.appcompat.widget.AppCompatSpinner
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.stripe.android.R
import com.stripe.android.databinding.StripeCardBrandSpinnerDropdownBinding
import com.stripe.android.databinding.StripeCardBrandSpinnerMainBinding
import com.stripe.android.model.CardBrand
import androidx.appcompat.R as AppCompatR

internal class CardBrandSpinner @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = AppCompatR.attr.spinnerStyle
) : AppCompatSpinner(context, attrs, defStyleAttr, MODE_DROPDOWN) {
    private val cardBrandsAdapter = Adapter(context)
    private var defaultBackground: Drawable? = null

    init {
        adapter = cardBrandsAdapter
        dropDownWidth = resources.getDimensionPixelSize(R.dimen.stripe_card_brand_spinner_dropdown_width)
    }

    val cardBrand: CardBrand?
        get() {
            return selectedItem as CardBrand?
        }

    override fun onFinishInflate() {
        super.onFinishInflate()

        defaultBackground = background

        setCardBrands(
            listOf(CardBrand.Unknown)
        )
    }

    fun setTintColor(@ColorInt tintColor: Int) {
        cardBrandsAdapter.tintColor = tintColor
    }

    @JvmSynthetic
    fun setCardBrands(cardBrands: List<CardBrand>) {
        cardBrandsAdapter.clear()
        cardBrandsAdapter.addAll(cardBrands)
        cardBrandsAdapter.notifyDataSetChanged()
        setSelection(0)

        // enable dropdown selector if there are multiple card brands, disable otherwise
        if (cardBrands.size > 1) {
            isClickable = true
            isEnabled = true
            background = defaultBackground
        } else {
            isClickable = false
            isEnabled = false
            setBackgroundColor(
                ContextCompat.getColor(context, android.R.color.transparent)
            )
        }
    }

    internal class Adapter(
        context: Context
    ) : ArrayAdapter<CardBrand>(
        context,
        0
    ) {
        private val layoutInflater = LayoutInflater.from(context)

        @ColorInt
        internal var tintColor: Int = 0

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val viewBinding = convertView?.let {
                StripeCardBrandSpinnerMainBinding.bind(it)
            } ?: StripeCardBrandSpinnerMainBinding.inflate(layoutInflater, parent, false)

            val cardBrand = requireNotNull(getItem(position))
            viewBinding.image.also {
                it.setImageDrawable(createCardBrandDrawable(cardBrand))
                it.contentDescription = cardBrand.displayName
            }

            return viewBinding.root
        }

        override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
            val viewBinding = convertView?.let {
                StripeCardBrandSpinnerDropdownBinding.bind(it)
            } ?: StripeCardBrandSpinnerDropdownBinding.inflate(layoutInflater, parent, false)

            val cardBrand = requireNotNull(getItem(position))
            viewBinding.textView.also {
                it.text = cardBrand.displayName
                it.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    createCardBrandDrawable(cardBrand),
                    null,
                    null,
                    null
                )
            }

            return viewBinding.root
        }

        private fun createCardBrandDrawable(cardBrand: CardBrand): Drawable {
            val icon = requireNotNull(
                ContextCompat.getDrawable(context, cardBrand.icon)
            )
            return if (cardBrand == CardBrand.Unknown) {
                val compatIcon = DrawableCompat.wrap(icon)
                DrawableCompat.setTint(compatIcon.mutate(), tintColor)
                DrawableCompat.unwrap(compatIcon)
            } else {
                icon
            }
        }
    }
}
