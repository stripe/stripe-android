package com.stripe.android.view

import android.content.Context
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Typeface
import android.os.Parcelable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ListPopupWindow
import android.widget.TextView
import com.stripe.android.R
import com.stripe.android.databinding.StripeCardBrandViewRebuildBinding
import com.stripe.android.model.CardBrand
import com.stripe.android.model.CardBrand.Unknown
import com.stripe.android.model.PaymentMethodCreateParams
import kotlinx.parcelize.Parcelize
import kotlin.properties.Delegates

internal class CardBrandViewRebuild @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    private val viewBinding: StripeCardBrandViewRebuildBinding =
        StripeCardBrandViewRebuildBinding.inflate(LayoutInflater.from(context), this)
    private val iconView = viewBinding.icon
    private val progressView = viewBinding.progress
    private val chevron = viewBinding.chevron
    private val listPopup = ListPopupWindow(context)

    @Parcelize
    internal data class State(
        val isCbcEligible: Boolean = false,
        val isLoading: Boolean = false,
        val brand: CardBrand = Unknown,
        val userSelectedBrand: CardBrand? = null,
        val possibleBrands: List<CardBrand> = emptyList(),
        val merchantPreferredNetworks: List<CardBrand> = emptyList(),
        val shouldShowCvc: Boolean = false,
        val shouldShowErrorIcon: Boolean = false,
        val tintColor: Int = 0,
    ) : Parcelable

    private var state: State = State()

    var isCbcEligible: Boolean
        get() = state.isCbcEligible
        set(value) {
            state = state.copy(isCbcEligible = value)
            updateBrandSpinner()
        }

    var isLoading: Boolean by Delegates.observable(
        false
    ) { _, wasLoading, isLoading ->
        state = state.copy(isLoading = isLoading)
        setCardBrandIconAndTint()
        if (wasLoading != isLoading) {
            if (isLoading) {
                progressView.visibility = VISIBLE
            } else {
                progressView.visibility = GONE
            }
        }
    }

    var brand: CardBrand
        get() = state.brand
        set(value) {
            state = state.copy(brand = value)
            updateBrandSpinner()
        }

    var possibleBrands: List<CardBrand>
        get() = state.possibleBrands
        set(value) {
            state = state.copy(possibleBrands = value)
            determineCardBrandToDisplay()
            updateBrandSpinner()
        }

    var merchantPreferredNetworks: List<CardBrand>
        get() = state.merchantPreferredNetworks
        set(value) {
            state = state.copy(merchantPreferredNetworks = value)
            determineCardBrandToDisplay()
        }

    var shouldShowCvc: Boolean
        get() = state.shouldShowCvc
        set(value) {
            setCardBrandIconAndTint()
            state = state.copy(shouldShowCvc = value)
        }

    var shouldShowErrorIcon: Boolean
        get() = state.shouldShowErrorIcon
        set(value) {
            setCardBrandIconAndTint()
            state = state.copy(shouldShowErrorIcon = value)
        }

    internal var tintColorInt: Int
        get() = state.tintColor
        set(value) {
            state = state.copy(tintColor = value)
        }

    init {
        isClickable = false
        isFocusable = false

        determineCardBrandToDisplay()
        updateBrandSpinner()
    }

    fun createNetworksParam(): PaymentMethodCreateParams.Card.Networks? {
        return PaymentMethodCreateParams.Card.Networks(
            preferred = brand.takeIf { it != Unknown }?.code,
        ).takeIf {
            isCbcEligible && possibleBrands.size > 1
        }
    }

    private fun handleBrandSelected(brand: CardBrand?) {
        brand?.let {
            state = state.copy(userSelectedBrand = brand)
            determineCardBrandToDisplay()
        }
    }

    private fun setCardBrandIconAndTint() {
        iconView.setBackgroundResource(
            when {
                isLoading -> state.brand.icon
                shouldShowErrorIcon -> state.brand.errorIcon
                shouldShowCvc -> state.brand.cvcIcon
                else -> state.brand.icon
            }
        )

        val tint = when {
            isLoading -> tintColorInt.takeIf { state.brand == Unknown }
            shouldShowErrorIcon -> null
            shouldShowCvc -> tintColorInt
            else -> tintColorInt.takeIf { state.brand == Unknown }
        }

        iconView.colorFilter = tint?.let { PorterDuffColorFilter(it, PorterDuff.Mode.LIGHTEN) }
    }

    private fun determineCardBrandToDisplay() {
        brand = if (state.possibleBrands.size > 1) {
            selectCardBrandToDisplay(state.userSelectedBrand, state.possibleBrands, state.merchantPreferredNetworks)
        } else {
            state.brand
        }
        setCardBrandIconAndTint()
    }

    private fun updateBrandSpinner() {
        val showDropdown = isCbcEligible && possibleBrands.size > 1 && !shouldShowCvc && !shouldShowErrorIcon
        if (showDropdown) {
            initListPopup()
            this.setOnClickListener {
                if (listPopup.isShowing) {
                    listPopup.dismiss()
                } else {
                    listPopup.show()
                }
            }
            chevron.visibility = View.VISIBLE
        } else {
            this.setOnClickListener(null)
            chevron.visibility = View.GONE
        }
    }

    private fun initListPopup() {
        val adapter = BrandAdapter(context, possibleBrands, brand)
        listPopup.setAdapter(adapter)
        listPopup.isModal = true
        listPopup.width = measureContentWidth(adapter)
        listPopup.setOnItemClickListener { _, _, position, _ ->
            possibleBrands.getOrNull(position - 1)?.let {
                handleBrandSelected(it)
            }
            listPopup.dismiss()
        }
        listPopup.anchorView = iconView
    }

    private fun measureContentWidth(adapter: BrandAdapter): Int {
        val widthMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        val heightMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        val count = adapter.count
        var maxWidth = 0

        for (i in 0..<count) {
            val itemView = adapter.getView(i, null, this)
            itemView.measure(widthMeasureSpec, heightMeasureSpec)
            maxWidth = maxWidth.coerceAtLeast(itemView.measuredWidth)
        }

        return maxWidth
    }

    override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()
        return SavedState(superState, state)
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        val savedState = state as? SavedState
        this.state = savedState?.state ?: State()
        determineCardBrandToDisplay()
        updateBrandSpinner()
        super.onRestoreInstanceState(savedState?.superState)
    }

    @Parcelize
    internal data class SavedState(
        val superSavedState: Parcelable?,
        val state: State,
    ) : BaseSavedState(superSavedState), Parcelable
}

class BrandAdapter(
    context: Context,
    private val brands: List<CardBrand?>,
    private val selectedBrand: CardBrand?
) : ArrayAdapter<CardBrand?>(context, 0, brands) {

    private val inflater = LayoutInflater.from(context)
    private val colorUtils = StripeColorUtils(context)

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = if (position == 0) {
            inflater.inflate(R.layout.stripe_select_card_brand_view, parent, false)
        } else {
            inflater.inflate(R.layout.stripe_card_brand_choice_list_view, parent, false)
        }

        if (position > 0) {
            brands.getOrNull(position - 1)?.let {
                val isSelected = it == selectedBrand
                view.findViewById<ImageView>(R.id.brand_icon)?.setBackgroundResource(it.icon)
                view.findViewById<ImageView>(R.id.brand_check).apply {
                    if (isSelected) {
                        visibility = View.VISIBLE
                        setColorFilter(colorUtils.colorPrimary)
                    } else {
                        visibility = View.GONE
                    }
                }
                view.findViewById<TextView>(R.id.brand_text)?.apply {
                    text = it.displayName
                    if (isSelected) {
                        setTextColor(colorUtils.colorPrimary)
                        typeface = Typeface.DEFAULT_BOLD
                    } else {
                        setTextColor(colorUtils.textColorPrimary)
                        typeface = Typeface.DEFAULT
                    }
                }
            }
        }
        return view
    }

    override fun getItem(position: Int): CardBrand? {
        return if (position == 0) null else super.getItem(position - 1)
    }

    override fun getCount(): Int {
        return if (brands.isEmpty()) 0 else brands.size + 1
    }

    override fun areAllItemsEnabled(): Boolean {
        return false
    }

    override fun isEnabled(position: Int): Boolean {
        return position != 0
    }
}
