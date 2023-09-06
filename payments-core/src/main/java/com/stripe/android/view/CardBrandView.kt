package com.stripe.android.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.annotation.ColorInt
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.stripe.android.databinding.StripeCardBrandViewBinding
import com.stripe.android.model.CardBrand
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.properties.Delegates

internal class CardBrandView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    private val viewBinding: StripeCardBrandViewBinding = StripeCardBrandViewBinding.inflate(
        LayoutInflater.from(context),
        this
    )
    private val iconView = viewBinding.icon
    private val progressView = viewBinding.progress

    @ColorInt
    internal var tintColorInt: Int = 0

    private val isLoadingFlow = MutableStateFlow(false)
    private val brandFlow = MutableStateFlow(CardBrand.Unknown)
    private val possibleBrandsFlow = MutableStateFlow(emptyList<CardBrand>())
    private val shouldShowCvcFlow = MutableStateFlow(false)
    private val shouldShowErrorIconFlow = MutableStateFlow(false)

    var isLoading: Boolean by Delegates.observable(
        false
    ) { _, wasLoading, isLoading ->
        isLoadingFlow.value = isLoading

        if (wasLoading != isLoading) {
            if (isLoading) {
                progressView.show()
            } else {
                progressView.hide()
            }
        }
    }

    var brand: CardBrand
        get() = brandFlow.value
        set(value) {
            brandFlow.value = value
        }

    var possibleBrands: List<CardBrand>
        get() = possibleBrandsFlow.value
        set(value) {
            possibleBrandsFlow.value = value
        }

    var shouldShowCvc: Boolean by Delegates.observable(
        false
    ) { _, _, newValue ->
        shouldShowCvcFlow.value = newValue
    }

    var shouldShowErrorIcon: Boolean by Delegates.observable(
        false
    ) { _, _, newValue ->
        shouldShowErrorIconFlow.value = newValue
    }

    init {
        isClickable = false
        isFocusable = false

        iconView.setContent {
            val isLoading by isLoadingFlow.collectAsState()
            val currentBrand by brandFlow.collectAsState()
            val possibleBrands by possibleBrandsFlow.collectAsState()
            val shouldShowCvc by shouldShowCvcFlow.collectAsState()
            val shouldShowErrorIcon by shouldShowErrorIconFlow.collectAsState()

            CardBrandSelectorIcon(
                isLoading = isLoading,
                currentBrand = currentBrand,
                possibleBrands = possibleBrands,
                shouldShowCvc = shouldShowCvc,
                shouldShowErrorIcon = shouldShowErrorIcon,
                tintColorInt = tintColorInt,
                onBrandSelected = this::handleBrandSelected,
            )
        }
    }

    private fun handleBrandSelected(brand: CardBrand?) {
        brandFlow.value = brand ?: CardBrand.Unknown
    }
}
