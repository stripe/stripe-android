package com.stripe.android.paymentsheet

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.stripe.android.paymentsheet.ui.LpmSelectorText
import com.stripe.android.ui.core.MeasureComposableWidth
import com.stripe.android.ui.core.forms.resources.LpmRepository.SupportedPaymentMethod
import com.stripe.android.ui.core.getBorderStroke
import com.stripe.android.ui.core.paymentsColors

private object Spacing {
    val cardLeadingInnerPadding = 12.dp
    val carouselOuterPadding = 20.dp
    val carouselInnerPadding = 12.dp
}

@VisibleForTesting
const val TEST_TAG_LIST = "PaymentMethodsUITestTag"

@Composable
internal fun PaymentMethodsUI(
    paymentMethods: List<SupportedPaymentMethod>,
    selectedIndex: Int,
    isEnabled: Boolean,
    onItemSelectedListener: (SupportedPaymentMethod) -> Unit,
    modifier: Modifier = Modifier
) {
    val state = rememberLazyListState()

    LaunchedEffect(selectedIndex) {
        state.animateScrollToItem(selectedIndex)
    }

    BoxWithConstraints(
        modifier = modifier.testTag(TEST_TAG_LIST + "1")
    ) {
        val viewWidth = rememberViewWidth(
            this.maxWidth,
            paymentMethods.size
        )

        LazyRow(
            state = state,
            contentPadding = PaddingValues(horizontal = Spacing.carouselOuterPadding),
            horizontalArrangement = Arrangement.spacedBy(Spacing.carouselInnerPadding),
            userScrollEnabled = isEnabled,
            modifier = Modifier.testTag(TEST_TAG_LIST)
        ) {
            itemsIndexed(items = paymentMethods) { index, item ->
                PaymentMethodUI(
                    modifier = Modifier.testTag(
                        TEST_TAG_LIST + stringResource(item.displayNameResource)
                    ),
                    viewWidth = viewWidth,
                    iconRes = item.iconResource,
                    title = stringResource(item.displayNameResource),
                    isSelected = index == selectedIndex,
                    isEnabled = isEnabled,
                    tintOnSelected = item.tintIconOnSelection,
                    itemIndex = index,
                    onItemSelectedListener = {
                        onItemSelectedListener(paymentMethods[it])
                    }
                )
            }
        }
    }
}

@Composable
private fun rememberViewWidth(
    maxWidth: Dp,
    numberOfPaymentMethods: Int
) = remember(maxWidth, numberOfPaymentMethods) {
    calculateViewWidth(maxWidth, numberOfPaymentMethods)
}

internal fun calculateViewWidth(
    maxWidth: Dp,
    numberOfPaymentMethods: Int
): Dp {
    val targetWidthDp = maxWidth - (Spacing.carouselOuterPadding * 2)
    val minItemWidthDp = 100.dp

    val minimumCardsWidth = minItemWidthDp * numberOfPaymentMethods
    val minimumSpacingWidth = Spacing.carouselInnerPadding * (numberOfPaymentMethods - 1)
    val minimumContentWidth = minimumCardsWidth + minimumSpacingWidth

    val viewWidth = if (minimumContentWidth < targetWidthDp) {
        // Stretch cards to fill entire width
        (targetWidthDp - minimumSpacingWidth) / numberOfPaymentMethods
    } else {
        computeMaxWidthOfItem(targetWidthDp, minItemWidthDp, Spacing.carouselInnerPadding)
    }
    return viewWidth
}

private fun computeMaxWidthOfItem(
    maxWidth: Dp,
    minItemWidth: Dp,
    spacing: Dp,
): Dp {
    var widthOfCards = minItemWidth
    var visibleCards = 1

    while (true) {
        val widthAfterAddingCard = widthOfCards + (spacing + minItemWidth)
        if (widthAfterAddingCard <= maxWidth) {
            widthOfCards = widthAfterAddingCard
            visibleCards += 1
        } else {
            break
        }
    }

    val overallSpacing = spacing * (visibleCards - 1)
    return (maxWidth - overallSpacing) / visibleCards
}

@Composable
internal fun PaymentMethodUI(
    viewWidth: Dp,
    iconRes: Int,
    title: String,
    isSelected: Boolean,
    isEnabled: Boolean,
    tintOnSelected: Boolean,
    itemIndex: Int,
    modifier: Modifier = Modifier,
    onItemSelectedListener: (Int) -> Unit
) {
    val color = if (isSelected) {
        MaterialTheme.colors.primary
    } else {
        MaterialTheme.paymentsColors.onComponent
    }

    val lpmTextSelector: @Composable () -> Unit = {
        LpmSelectorText(
            text = title,
            isEnabled = isEnabled,
            textColor = color,
            modifier = Modifier.padding(top = 6.dp, start = Spacing.cardLeadingInnerPadding)
        )
    }

    MeasureComposableWidth(composable = lpmTextSelector) { lpmSelectorTextWidth ->
        Card(
            modifier = modifier
                .alpha(alpha = if (isEnabled) 1.0F else 0.6F)
                .height(60.dp)
                .width(
                    maxOf(
                        viewWidth,
                        lpmSelectorTextWidth + Spacing.cardLeadingInnerPadding
                    )
                ),
            shape = MaterialTheme.shapes.medium,
            backgroundColor = MaterialTheme.paymentsColors.component,
            border = MaterialTheme.getBorderStroke(isSelected),
            elevation = if (isSelected) 1.5.dp else 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .selectable(
                        selected = isSelected,
                        enabled = isEnabled,
                        onClick = {
                            onItemSelectedListener(itemIndex)
                        }
                    )
            ) {
                val colorFilter = if (tintOnSelected) ColorFilter.tint(color) else null
                Image(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    colorFilter = colorFilter,
                    modifier = Modifier.padding(
                        top = Spacing.cardLeadingInnerPadding,
                        start = Spacing.cardLeadingInnerPadding,
                    )
                )
                lpmTextSelector()
            }
        }
    }
}
