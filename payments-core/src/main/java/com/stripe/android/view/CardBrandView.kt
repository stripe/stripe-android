package com.stripe.android.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.annotation.ColorInt
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material.ContentAlpha
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.accompanist.themeadapter.material.MdcTheme
import com.stripe.android.R
import com.stripe.android.databinding.StripeCardBrandViewBinding
import com.stripe.android.model.CardBrand
import com.stripe.android.uicore.elements.SingleChoiceDropdown
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

    var isCbcEligible: Boolean = false

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

    var shouldShowCvc: Boolean
        get() = shouldShowCvcFlow.value
        set(value) {
            shouldShowCvcFlow.value = value
        }

    var shouldShowErrorIcon: Boolean
        get() = shouldShowErrorIconFlow.value
        set(value) {
            shouldShowErrorIconFlow.value = value
        }

    init {
        isClickable = false
        isFocusable = false

        iconView.setContent {
            MdcTheme {
                val isLoading by isLoadingFlow.collectAsState()
                val currentBrand by brandFlow.collectAsState()
                val possibleBrands by possibleBrandsFlow.collectAsState()
                val shouldShowCvc by shouldShowCvcFlow.collectAsState()
                val shouldShowErrorIcon by shouldShowErrorIconFlow.collectAsState()

                CardBrand(
                    isLoading = isLoading,
                    currentBrand = currentBrand,
                    possibleBrands = possibleBrands,
                    shouldShowCvc = shouldShowCvc,
                    shouldShowErrorIcon = shouldShowErrorIcon,
                    tintColorInt = tintColorInt,
                    isCbcEligible = isCbcEligible,
                    onBrandSelected = this::handleBrandSelected,
                )
            }
        }
    }

    private fun handleBrandSelected(brand: CardBrand?) {
        brandFlow.value = brand ?: CardBrand.Unknown
    }
}

@Composable
private fun CardBrand(
    isLoading: Boolean,
    currentBrand: CardBrand,
    possibleBrands: List<CardBrand>,
    shouldShowCvc: Boolean,
    shouldShowErrorIcon: Boolean,
    tintColorInt: Int,
    isCbcEligible: Boolean,
    modifier: Modifier = Modifier,
    onBrandSelected: (CardBrand?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    val icon = remember(isLoading, currentBrand, shouldShowCvc, shouldShowErrorIcon) {
        when {
            isLoading -> currentBrand.icon
            shouldShowErrorIcon -> currentBrand.errorIcon
            shouldShowCvc -> currentBrand.cvcIcon
            else -> currentBrand.icon
        }
    }

    val tint = remember(isLoading, currentBrand, shouldShowCvc, shouldShowErrorIcon) {
        when {
            isLoading -> Color(tintColorInt).takeIf { currentBrand == CardBrand.Unknown }
            shouldShowErrorIcon -> null
            shouldShowCvc -> Color(tintColorInt)
            else -> Color(tintColorInt).takeIf { currentBrand == CardBrand.Unknown }
        }
    }

    val showDropdown = remember(possibleBrands) { isCbcEligible && possibleBrands.size > 1 }

    Box(modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable(enabled = showDropdown) {
                expanded = true
            },
        ) {
            Image(
                painter = painterResource(icon),
                colorFilter = tint?.let { ColorFilter.tint(it) },
                contentDescription = null,
                modifier = Modifier.requiredSize(width = 32.dp, height = 21.dp),
            )

            if (showDropdown) {
                Image(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    alpha = ContentAlpha.disabled,
                    modifier = Modifier.padding(end = 4.dp),
                )
            }
        }

        if (showDropdown) {
            CardBrandChoiceDropdown(
                expanded = expanded,
                currentBrand = currentBrand,
                brands = possibleBrands,
                onBrandSelected = { brand ->
                    onBrandSelected(brand)
                    expanded = false
                },
                onDismiss = { expanded = false },
            )
        }
    }
}

@Composable
private fun CardBrandChoiceDropdown(
    expanded: Boolean,
    currentBrand: CardBrand,
    brands: List<CardBrand>,
    onBrandSelected: (CardBrand?) -> Unit,
    onDismiss: () -> Unit
) {
    val noSelection = CardBrandChoice(
        label = stringResource(id = R.string.stripe_card_brand_choice_no_selection),
        icon = CardBrand.Unknown.icon
    )

    val allPossibleBrands = listOf(CardBrand.Unknown) + brands
    val choices = allPossibleBrands.map { brand ->
        brand.toChoice(noSelection)
    }

    SingleChoiceDropdown(
        title = stringResource(id = R.string.stripe_card_brand_choice_selection_header),
        expanded = expanded,
        currentChoice = currentBrand.toChoice(noSelection),
        choices = choices,
        onChoiceSelected = { choice ->
            when (val choiceIndex = choices.indexOf(choice)) {
                -1 -> Unit
                0 -> onBrandSelected(null)
                else -> onBrandSelected(allPossibleBrands[choiceIndex])
            }
        },
        onDismiss = onDismiss,
    )
}

private fun CardBrand.toChoice(noSelection: CardBrandChoice): CardBrandChoice {
    return if (this == CardBrand.Unknown) {
        noSelection
    } else {
        CardBrandChoice(
            label = displayName,
            icon = icon
        )
    }
}
