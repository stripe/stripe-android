package com.stripe.android.view

import android.content.Context
import android.os.Parcelable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.stripe.android.R
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.databinding.StripeCardBrandViewBinding
import com.stripe.android.model.CardBrand
import com.stripe.android.model.CardBrand.Unknown
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.uicore.elements.SingleChoiceDropdown
import com.stripe.android.utils.AppCompatOrMdcTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.parcelize.Parcelize
import kotlin.properties.Delegates
import com.stripe.android.uicore.R as StripeUiCoreR

internal const val CardBrandDropdownTestTag = "CardBrandDropdownTestTag"

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

    @Parcelize
    internal data class State(
        val isCbcEligible: Boolean = false,
        val reserveSpaceForCbcDropdown: Boolean = true,
        val isLoading: Boolean = false,
        val brand: CardBrand = Unknown,
        val userSelectedBrand: CardBrand? = null,
        val possibleBrands: List<CardBrand> = emptyList(),
        val merchantPreferredNetworks: List<CardBrand> = emptyList(),
        val shouldShowCvc: Boolean = false,
        val shouldShowErrorIcon: Boolean = false,
        val tintColor: Int = 0,
    ) : Parcelable

    private val stateFlow = MutableStateFlow(State())

    private var state: State
        get() = stateFlow.value
        set(value) {
            stateFlow.value = value
        }

    var isCbcEligible: Boolean
        get() = state.isCbcEligible
        set(value) {
            stateFlow.update { it.copy(isCbcEligible = value) }
        }

    var isLoading: Boolean by Delegates.observable(
        false
    ) { _, wasLoading, isLoading ->
        stateFlow.update { it.copy(isLoading = isLoading) }

        if (wasLoading != isLoading) {
            if (isLoading) {
                progressView.show()
            } else {
                progressView.hide()
            }
        }
    }

    var brand: CardBrand
        get() = state.brand
        set(value) {
            stateFlow.update { it.copy(brand = value) }
        }

    var possibleBrands: List<CardBrand>
        get() = state.possibleBrands
        set(value) {
            stateFlow.update { it.copy(possibleBrands = value) }
        }

    var merchantPreferredNetworks: List<CardBrand>
        get() = state.merchantPreferredNetworks
        set(value) {
            stateFlow.update { it.copy(merchantPreferredNetworks = value) }
        }

    var shouldShowCvc: Boolean
        get() = state.shouldShowCvc
        set(value) {
            stateFlow.update { it.copy(shouldShowCvc = value) }
        }

    var shouldShowErrorIcon: Boolean
        get() = state.shouldShowErrorIcon
        set(value) {
            stateFlow.update { it.copy(shouldShowErrorIcon = value) }
        }

    internal var tintColorInt: Int
        get() = state.tintColor
        set(value) {
            stateFlow.update { it.copy(tintColor = value) }
        }

    internal var reserveSpaceForCbcDropdown: Boolean
        get() = state.reserveSpaceForCbcDropdown
        set(value) {
            stateFlow.update { it.copy(reserveSpaceForCbcDropdown = value) }
        }

    init {
        isClickable = false
        isFocusable = false

        iconView.setContent {
            AppCompatOrMdcTheme {
                val state by stateFlow.collectAsState()

                LaunchedEffect(state) {
                    determineCardBrandToDisplay(
                        userSelectedBrand = state.userSelectedBrand,
                        autoDeterminedBrand = state.brand,
                        possibleBrands = state.possibleBrands,
                        merchantPreferredBrands = state.merchantPreferredNetworks,
                    )
                }

                CardBrand(
                    isLoading = state.isLoading,
                    currentBrand = state.brand,
                    possibleBrands = state.possibleBrands,
                    shouldShowCvc = state.shouldShowCvc,
                    shouldShowErrorIcon = state.shouldShowErrorIcon,
                    tintColorInt = state.tintColor,
                    isCbcEligible = state.isCbcEligible,
                    reserveSpaceForCbcDropdown = state.reserveSpaceForCbcDropdown,
                    onBrandSelected = this::handleBrandSelected,
                )
            }
        }
    }

    fun createNetworksParam(): PaymentMethodCreateParams.Card.Networks? {
        return PaymentMethodCreateParams.Card.Networks(
            preferred = brand.takeIf { it != Unknown }?.code,
        ).takeIf {
            isCbcEligible && possibleBrands.size > 1
        }
    }

    private fun handleBrandSelected(brand: CardBrand?) {
        stateFlow.update { it.copy(userSelectedBrand = brand) }
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
        super.onRestoreInstanceState(savedState?.superState ?: state)
    }

    @Parcelize
    internal data class SavedState(
        val superSavedState: Parcelable?,
        val state: State,
    ) : BaseSavedState(superSavedState), Parcelable
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
    reserveSpaceForCbcDropdown: Boolean,
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

    val showDropdown = remember(possibleBrands, shouldShowCvc, shouldShowErrorIcon) {
        isCbcEligible && possibleBrands.size > 1 && !shouldShowCvc && !shouldShowErrorIcon
    }

    Box(modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable(enabled = showDropdown) { expanded = true }
                .testTag(CardBrandDropdownTestTag),
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

            Spacer(modifier = Modifier.requiredWidth(1.dp))

            AnimatedVisibility(visible = showDropdown || reserveSpaceForCbcDropdown) {
                Spacer(modifier = Modifier.requiredWidth(1.dp))

                Image(
                    painter = painterResource(StripeUiCoreR.drawable.stripe_ic_chevron_down),
                    contentDescription = null,
                    modifier = Modifier
                        .requiredSize(8.dp)
                        .graphicsLayer { alpha = dropdownIconAlpha },
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
    val choices = brands.map { it.toChoice() }

    SingleChoiceDropdown(
        title = resolvableString(id = R.string.stripe_card_brand_choice_selection_header),
        expanded = expanded,
        currentChoice = currentBrand.takeIf { it != Unknown }?.toChoice(),
        choices = choices,
        headerTextColor = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.medium),
        optionTextColor = MaterialTheme.colors.onSurface,
        onChoiceSelected = { choice ->
            val choiceIndex = choices.indexOf(choice)
            val brand = brands.getOrNull(choiceIndex)

            if (brand != null) {
                onBrandSelected(brand)
            }
        },
        onDismiss = onDismiss,
    )
}

private fun CardBrand.toChoice(): CardBrandChoice {
    return CardBrandChoice(
        label = resolvableString(displayName),
        icon = icon
    )
}
