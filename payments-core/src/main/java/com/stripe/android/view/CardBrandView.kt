package com.stripe.android.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material.ContentAlpha
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.google.accompanist.themeadapter.material.MdcTheme
import com.stripe.android.R
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.databinding.StripeCardBrandViewBinding
import com.stripe.android.model.CardBrand
import com.stripe.android.model.CardBrand.Unknown
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.uicore.elements.SingleChoiceDropdown
import com.stripe.android.utils.FeatureFlags
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

    private val isCbcEligibleFlow = MutableStateFlow(false)
    private val isLoadingFlow = MutableStateFlow(false)
    private val brandFlow = MutableStateFlow(Unknown)
    private val userSelectedBrandFlow = MutableStateFlow<CardBrand?>(null)
    private val possibleBrandsFlow = MutableStateFlow(emptyList<CardBrand>())
    private val merchantPreferredNetworksFlow = MutableStateFlow(emptyList<CardBrand>())
    private val shouldShowCvcFlow = MutableStateFlow(false)
    private val shouldShowErrorIconFlow = MutableStateFlow(false)
    private val tintColorFlow = MutableStateFlow(0)

    var isCbcEligible: Boolean
        get() = isCbcEligibleFlow.value
        set(value) {
            isCbcEligibleFlow.value = value
        }

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

    var merchantPreferredNetworks: List<CardBrand>
        get() = merchantPreferredNetworksFlow.value
        set(value) {
            merchantPreferredNetworksFlow.value = value
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

    internal var tintColorInt: Int
        get() = tintColorFlow.value
        set(value) {
            tintColorFlow.value = value
        }

    init {
        isClickable = false
        isFocusable = false

        iconView.setContent {
            MdcTheme {
                val isCbcEligible by isCbcEligibleFlow.collectAsState()
                val isLoading by isLoadingFlow.collectAsState()
                val currentBrand by brandFlow.collectAsState()
                val userSelectedBrand by userSelectedBrandFlow.collectAsState()
                val possibleBrands by possibleBrandsFlow.collectAsState()
                val merchantPreferredBrands by merchantPreferredNetworksFlow.collectAsState()
                val shouldShowCvc by shouldShowCvcFlow.collectAsState()
                val shouldShowErrorIcon by shouldShowErrorIconFlow.collectAsState()
                val tintColorInt by tintColorFlow.collectAsState()

                LaunchedEffect(userSelectedBrand, possibleBrands, merchantPreferredBrands) {
                    determineCardBrandToDisplay(userSelectedBrand, possibleBrands, merchantPreferredBrands)
                }

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

    fun createNetworksParam(): PaymentMethodCreateParams.Card.Networks? {
        return PaymentMethodCreateParams.Card.Networks(
            preferred = brand.takeIf { it != Unknown }?.code,
        ).takeIf {
            FeatureFlags.cardBrandChoice.isEnabled && isCbcEligible && possibleBrands.size > 1
        }
    }

    private fun handleBrandSelected(brand: CardBrand?) {
        userSelectedBrandFlow.value = brand ?: Unknown
    }

    private fun determineCardBrandToDisplay(
        currentBrand: CardBrand?,
        possibleBrands: List<CardBrand>,
        merchantPreferredBrands: List<CardBrand>,
    ) {
        brand = selectCardBrandToDisplay(currentBrand, possibleBrands, merchantPreferredBrands)
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
            isLoading -> Color(tintColorInt).takeIf { currentBrand == Unknown }
            shouldShowErrorIcon -> null
            shouldShowCvc -> Color(tintColorInt)
            else -> Color(tintColorInt).takeIf { currentBrand == Unknown }
        }
    }

    val showDropdown = remember(possibleBrands, shouldShowCvc) {
        isCbcEligible && possibleBrands.size > 1 && !shouldShowCvc
    }

    Box(modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable(enabled = showDropdown) {
                expanded = true
            },
        ) {
            val dropdownIconAlpha by animateFloatAsState(
                targetValue = if (showDropdown) ContentAlpha.medium else 0f,
                label = "CbcDropdownIconAlpha",
            )

            Image(
                painter = painterResource(icon),
                colorFilter = tint?.let { ColorFilter.tint(it) },
                contentDescription = null,
                modifier = Modifier.requiredSize(width = 32.dp, height = 21.dp),
            )

            Image(
                painter = painterResource(R.drawable.stripe_ic_arrow_down),
                contentDescription = null,
                modifier = Modifier
                    .requiredSize(8.dp)
                    .graphicsLayer { alpha = dropdownIconAlpha },
            )
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
        label = resolvableString(id = R.string.stripe_card_brand_choice_no_selection),
        icon = Unknown.icon
    )

    val allPossibleBrands = listOf(Unknown) + brands
    val choices = allPossibleBrands.map { brand ->
        brand.toChoice(noSelection)
    }

    SingleChoiceDropdown(
        title = resolvableString(id = R.string.stripe_card_brand_choice_selection_header),
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
    return if (this == Unknown) {
        noSelection
    } else {
        CardBrandChoice(
            label = resolvableString(displayName),
            icon = icon
        )
    }
}
