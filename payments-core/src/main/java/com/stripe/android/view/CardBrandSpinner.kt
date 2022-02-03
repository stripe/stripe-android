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
import com.stripe.android.databinding.CardBrandSpinnerDropdownBinding
import com.stripe.android.databinding.CardBrandSpinnerMainBinding
import com.stripe.android.ui.core.elements.CardBrand

internal class CardBrandSpinner @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.appcompat.R.attr.spinnerStyle
) : AppCompatSpinner(context, attrs, defStyleAttr, MODE_DROPDOWN) {
    private val cardBrandsAdapter = Adapter(context)
    private var defaultBackground: Drawable? = null

    init {
        adapter = cardBrandsAdapter
        dropDownWidth = resources.getDimensionPixelSize(R.dimen.card_brand_spinner_dropdown_width)
    }

    val cardBrand: com.stripe.android.ui.core.elements.CardBrand?
        get() {
            return selectedItem as com.stripe.android.ui.core.elements.CardBrand?
        }

    override fun onFinishInflate() {
        super.onFinishInflate()

        defaultBackground = background

        setCardBrands(
            listOf(com.stripe.android.ui.core.elements.CardBrand.Unknown)
        )
    }

    fun setTintColor(@ColorInt tintColor: Int) {
        cardBrandsAdapter.tintColor = tintColor
    }

    @JvmSynthetic
    fun setCardBrands(cardBrands: List<com.stripe.android.ui.core.elements.CardBrand>) {
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
    ) : ArrayAdapter<com.stripe.android.ui.core.elements.CardBrand>(
        context,
        0
    ) {
        private val layoutInflater = LayoutInflater.from(context)

        @ColorInt
        internal var tintColor: Int = 0

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val viewBinding = convertView?.let {
                CardBrandSpinnerMainBinding.bind(it)
            } ?: CardBrandSpinnerMainBinding.inflate(layoutInflater, parent, false)

            val cardBrand = requireNotNull(getItem(position))
            viewBinding.image.also {
                it.setImageDrawable(createCardBrandDrawable(cardBrand))
                it.contentDescription = cardBrand.displayName
            }

            return viewBinding.root
        }

        override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
            val viewBinding = convertView?.let {
                CardBrandSpinnerDropdownBinding.bind(it)
            } ?: CardBrandSpinnerDropdownBinding.inflate(layoutInflater, parent, false)

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

        private fun createCardBrandDrawable(cardBrand: com.stripe.android.ui.core.elements.CardBrand): Drawable {
            val icon = requireNotNull(
                ContextCompat.getDrawable(context, cardBrand.icon)
            )
            return if (cardBrand == com.stripe.android.ui.core.elements.CardBrand.Unknown) {
                val compatIcon = DrawableCompat.wrap(icon)
                DrawableCompat.setTint(compatIcon.mutate(), tintColor)
                DrawableCompat.unwrap(compatIcon)
            } else {
                icon
            }
        }
    }
}
