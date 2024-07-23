package com.stripe.android.view

import android.content.Context
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
        }

    var isLoading: Boolean by Delegates.observable(
        false
    ) { _, wasLoading, isLoading ->
        state = state.copy(isLoading = isLoading)

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
            updateBrandSpinner()
        }

    var merchantPreferredNetworks: List<CardBrand>
        get() = state.merchantPreferredNetworks
        set(value) {
            state = state.copy(merchantPreferredNetworks = value)
        }

    var shouldShowCvc: Boolean
        get() = state.shouldShowCvc
        set(value) {
            state = state.copy(shouldShowCvc = value)
        }

    var shouldShowErrorIcon: Boolean
        get() = state.shouldShowErrorIcon
        set(value) {
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
        iconView.setBackgroundResource(com.stripe.payments.model.R.drawable.stripe_ic_unknown)

        determineCardBrandToDisplay(
            state.userSelectedBrand,
            state.brand,
            state.possibleBrands,
            state.merchantPreferredNetworks
        )
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
        }
    }

    private fun determineCardBrandToDisplay(
        userSelectedBrand: CardBrand?,
        autoDeterminedBrand: CardBrand,
        possibleBrands: List<CardBrand>,
        merchantPreferredBrands: List<CardBrand>,
    ) {
        brand = if (possibleBrands.size > 1) {
            selectCardBrandToDisplay(userSelectedBrand, possibleBrands, merchantPreferredBrands)
        } else {
            autoDeterminedBrand
        }
    }

    override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()
        return SavedState(superState, state)
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        val savedState = state as? SavedState
        this.state = savedState?.state ?: State()
        determineCardBrandToDisplay(
            this.state.userSelectedBrand,
            this.state.brand,
            this.state.possibleBrands,
            this.state.merchantPreferredNetworks
        )
        updateBrandSpinner()
        super.onRestoreInstanceState(savedState?.superState)
    }

    @Parcelize
    internal data class SavedState(
        val superSavedState: Parcelable?,
        val state: State,
    ) : BaseSavedState(superSavedState), Parcelable

    private fun updateBrandSpinner() {
        chevron.visibility = if (possibleBrands.size > 1)  View.VISIBLE else View.GONE
        if (possibleBrands.size > 1) {
            val adapter = BrandAdapter(context, possibleBrands)
            this.setOnClickListener { listPopup.show() }
            listPopup.setAdapter(adapter)

            listPopup.setOnItemClickListener { parent, view, position, id ->
                possibleBrands.getOrNull(position - 1)?.let {
                    iconView.setBackgroundResource(it.icon)
                    handleBrandSelected(it)
                }
                listPopup.dismiss()
            }
            listPopup.anchorView = iconView
            listPopup.width = measureContentWidth(adapter)
            listPopup.setSelection(possibleBrands.indexOf(brand))
        } else {
            this.setOnClickListener(null)
        }
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
}

class BrandAdapter(
    context: Context,
    private val brands: List<CardBrand?>,
) : ArrayAdapter<CardBrand?>(context, 0, brands) {

    private val inflater = LayoutInflater.from(context)

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = if (position == 0) {
            inflater.inflate(R.layout.stripe_select_card_brand_view, parent, false)
        } else {
            inflater.inflate(R.layout.stripe_card_brand_spinner_view, parent, false)
        }

        if (position > 0) {
            brands.getOrNull(position - 1)?.let {
                view.findViewById<ImageView>(R.id.brand_icon)?.setBackgroundResource(it.icon)
                view.findViewById<TextView>(R.id.brand_text)?.text = it.displayName
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
