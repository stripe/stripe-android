package com.stripe.android.view

import android.content.Context
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
import androidx.annotation.VisibleForTesting
import androidx.transition.TransitionManager
import com.stripe.android.R
import com.stripe.android.databinding.StripeCardBrandViewBinding
import com.stripe.android.model.CardBrand
import com.stripe.android.model.CardBrand.Unknown
import com.stripe.android.model.Networks
import com.stripe.android.model.PaymentMethodCreateParams
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.parcelize.Parcelize

internal class CardBrandView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    private val viewBinding: StripeCardBrandViewBinding =
        StripeCardBrandViewBinding.inflate(LayoutInflater.from(context), this)
    private val iconView = viewBinding.icon
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
    ) : Parcelable

    private var stateFlow = MutableStateFlow(State())

    private var state: State
        get() = stateFlow.value
        set(value) {
            stateFlow.value = value
        }

    var isCbcEligible: Boolean
        get() = state.isCbcEligible
        set(value) {
            stateFlow.update { it.copy(isCbcEligible = value) }
            updateBrandSpinner()
        }

    var brand: CardBrand
        get() = state.brand
        set(value) {
            stateFlow.update { it.copy(brand = value) }
            determineCardBrandToDisplay()
            updateBrandSpinner()
        }

    var possibleBrands: List<CardBrand>
        get() = state.possibleBrands
        set(value) {
            stateFlow.update { it.copy(possibleBrands = value) }
            determineCardBrandToDisplay()
            updateBrandSpinner()
        }

    var merchantPreferredNetworks: List<CardBrand>
        get() = state.merchantPreferredNetworks
        set(value) {
            stateFlow.update { it.copy(merchantPreferredNetworks = value) }
            determineCardBrandToDisplay()
        }

    var shouldShowCvc: Boolean
        get() = state.shouldShowCvc
        set(value) {
            stateFlow.update { it.copy(shouldShowCvc = value) }
            setCardBrandIconAndTint()
        }

    var shouldShowErrorIcon: Boolean
        get() = state.shouldShowErrorIcon
        set(value) {
            stateFlow.update { it.copy(shouldShowErrorIcon = value) }
            setCardBrandIconAndTint()
        }

    init {
        isClickable = false
        isFocusable = false

        determineCardBrandToDisplay()
        updateBrandSpinner()
    }

    fun paymentMethodCreateParamsNetworks(): PaymentMethodCreateParams.Card.Networks? {
        val defaultNetworkParam = brandPaymentMethodCreateParamsNetworks()
        if (defaultNetworkParam != null) return defaultNetworkParam
        return merchantPreferredNetworks.firstOrNull()?.code?.let {
            PaymentMethodCreateParams.Card.Networks(
                preferred = it,
            )
        }
    }

    private fun brandPaymentMethodCreateParamsNetworks(): PaymentMethodCreateParams.Card.Networks? {
        if (brand == Unknown) return null
        return PaymentMethodCreateParams.Card.Networks(
            preferred = brand.code,
        ).takeIf {
            isCbcEligible && possibleBrands.size > 1
        }
    }

    fun cardParamsNetworks(): Networks? {
        val brandPreferredNetwork = brandCardParamsNetworks()
        if (brandPreferredNetwork != null) return brandPreferredNetwork
        return merchantPreferredNetworks.firstOrNull()
            ?.takeIf { it != Unknown }?.code
            ?.let { network ->
                Networks(
                    preferred = network
                )
            }
    }

    private fun brandCardParamsNetworks(): Networks? {
        return brand
            .takeIf { it != Unknown }?.code
            ?.let { network ->
                Networks(
                    preferred = network
                )
            }.takeIf {
                isCbcEligible && possibleBrands.size > 1
            }
    }

    @VisibleForTesting
    internal fun handleBrandSelected(brand: CardBrand?) {
        brand?.let {
            stateFlow.update { it.copy(userSelectedBrand = brand) }
            determineCardBrandToDisplay()
        }
    }

    private fun setCardBrandIconAndTint() {
        iconView.setImageResource(
            when {
                shouldShowErrorIcon -> state.brand.errorIcon
                shouldShowCvc -> state.brand.cvcIcon
                else -> state.brand.icon
            }
        )
    }

    private fun determineCardBrandToDisplay() {
        val newBrand = if (state.possibleBrands.size > 1) {
            selectCardBrandToDisplay(state.userSelectedBrand, state.possibleBrands, state.merchantPreferredNetworks)
        } else {
            state.brand
        }
        if (brand != newBrand) brand = newBrand
        setCardBrandIconAndTint()
    }

    private fun updateBrandSpinner() {
        val showDropdown = isCbcEligible && possibleBrands.size > 1 && !shouldShowCvc && !shouldShowErrorIcon
        val parentViewGroup = parent as? ViewGroup

        if (showDropdown) {
            initListPopup()
            this.setOnClickListener {
                if (listPopup.isShowing) {
                    listPopup.dismiss()
                } else {
                    listPopup.show()
                }
            }

            parentViewGroup.animateNextChanges()
            chevron.visibility = View.VISIBLE
        } else {
            this.setOnClickListener(null)
            parentViewGroup.animateNextChanges()
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
        super.onRestoreInstanceState(savedState?.superState ?: state)
    }

    private fun ViewGroup?.animateNextChanges() {
        this?.let {
            TransitionManager.endTransitions(this)
            TransitionManager.beginDelayedTransition(this)
        }
    }

    @Parcelize
    internal data class SavedState(
        val superSavedState: Parcelable?,
        val state: State,
    ) : BaseSavedState(superSavedState), Parcelable
}

internal class BrandAdapter(
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

        if (position > 0) updateView(view, position)
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

    private fun updateView(view: View, position: Int) {
        brands.getOrNull(position - 1)?.let { brand ->
            val isSelected = brand == selectedBrand
            view.findViewById<ImageView>(R.id.brand_icon)?.setImageResource(brand.icon)
            view.findViewById<ImageView>(R.id.brand_check).apply {
                if (isSelected) {
                    visibility = View.VISIBLE
                    setColorFilter(colorUtils.colorPrimary)
                } else {
                    visibility = View.GONE
                }
            }
            view.findViewById<TextView>(R.id.brand_text)?.apply {
                text = brand.displayName
                if (isSelected) {
                    setTextColor(colorUtils.colorPrimary)
                    typeface = Typeface.DEFAULT_BOLD
                } else {
                    typeface = Typeface.DEFAULT
                }
            }
        }
    }
}
