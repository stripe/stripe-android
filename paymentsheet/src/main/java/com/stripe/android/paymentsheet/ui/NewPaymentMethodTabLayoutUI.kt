@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package com.stripe.android.paymentsheet.ui

import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.paymentsheet.model.PaymentMethodIncentive
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.getOuterFormInsets
import com.stripe.android.uicore.image.StripeImageLoader
import com.stripe.android.uicore.strings.resolve

private object PaymentMethodsUISpacing {
    val carouselInnerPadding = 12.dp
}

@VisibleForTesting
const val TEST_TAG_LIST = "PaymentMethodsUITestTag"

@Composable
internal fun NewPaymentMethodTabLayoutUI(
    paymentMethods: List<SupportedPaymentMethod>,
    selectedIndex: Int,
    isEnabled: Boolean,
    incentive: PaymentMethodIncentive?,
    onItemSelectedListener: (SupportedPaymentMethod) -> Unit,
    imageLoader: StripeImageLoader,
    modifier: Modifier = Modifier,
    updatePaymentMethodVisibility: (AddPaymentMethodInitialVisibilityTrackerData) -> Unit = {},
    state: LazyListState = rememberLazyListState(),
) {
    // This is to fix an issue in tests involving this composable
    // where the test would succeed when run in isolation, but would
    // fail when run as part of test suite.
    val inspectionMode = LocalInspectionMode.current
    LaunchedEffect(selectedIndex) {
        if (inspectionMode) {
            state.scrollToItem(selectedIndex)
        } else {
            state.animateScrollToItem(selectedIndex)
        }
    }
    val paymentMethodCodes = remember(paymentMethods) { paymentMethods.map { it.code } }
    BoxWithConstraints(
        modifier = modifier.testTag(TEST_TAG_LIST + "1")
    ) {
        val viewWidth = rememberViewWidth(maxWidth = this.maxWidth, numberOfPaymentMethods = paymentMethods.size)
        val screenWidth = LocalConfiguration.current.screenWidthDp.dp
        val innerPadding = PaymentMethodsUISpacing.carouselInnerPadding
        LaunchedEffect(paymentMethodCodes) {
            updatePaymentMethodVisibility(
                AddPaymentMethodInitialVisibilityTrackerData(
                    paymentMethodCodes = paymentMethodCodes,
                    tabWidth = viewWidth,
                    screenWidth = screenWidth,
                    innerPadding = innerPadding
                )
            )
        }

        LazyRow(
            state = state,
            contentPadding = StripeTheme.getOuterFormInsets(),
            horizontalArrangement = Arrangement.spacedBy(innerPadding),
            userScrollEnabled = isEnabled,
            modifier = Modifier.testTag(TEST_TAG_LIST)
        ) {
            itemsIndexed(items = paymentMethods) { index, item ->
                NewPaymentMethodTab(
                    modifier = Modifier.testTag(
                        TEST_TAG_LIST + item.code
                    ),
                    minViewWidth = viewWidth,
                    iconRes = item.icon(),
                    iconUrl = item.iconUrl(),
                    imageLoader = imageLoader,
                    title = item.displayName.resolve(),
                    isSelected = index == selectedIndex,
                    isEnabled = isEnabled,
                    iconRequiresTinting = item.iconRequiresTinting,
                    promoBadge = incentive?.takeIfMatches(item.code)?.displayText,
                    onItemSelectedListener = {
                        onItemSelectedListener(paymentMethods[index])
                    }
                )
            }
        }
    }
}

/**
 * Calculates the number of payment method tabs that are visible on screen.
 */
private fun calculateNumberOfVisibleItems(
    totalItems: Int,
    tabWidth: Dp,
    screenWidth: Dp,
    innerPadding: Dp,
): Int {
    if (totalItems <= 0) return 0
    if (totalItems == 1) return 1

    // Calculate how many items can fit with their spacing
    val itemWithPadding = tabWidth + innerPadding
    val maxItemsThatFit = (screenWidth / itemWithPadding).toInt()

    // Check if there's enough remaining space for a partially visible item
    val usedWidth = maxItemsThatFit * itemWithPadding
    val remainingWidth = screenWidth - usedWidth

    // Consider an item visible if at least 95% of it is shown
    @Suppress("MagicNumber")
    val visibilityThreshold = tabWidth * 0.95f
    val hasPartiallyVisibleItem = remainingWidth >= visibilityThreshold

    val totalVisibleItems = if (hasPartiallyVisibleItem) {
        maxItemsThatFit + 1
    } else {
        maxItemsThatFit
    }

    // Ensure we don't exceed the total number of items and always show at least 1
    return totalVisibleItems.coerceIn(1, totalItems)
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
    val targetWidth = maxWidth - (StripeTheme.formInsets.end + StripeTheme.formInsets.start).dp
    val minItemWidth = 90.dp

    val minimumCardsWidth = minItemWidth * numberOfPaymentMethods
    val spacingWidth = PaymentMethodsUISpacing.carouselInnerPadding * (numberOfPaymentMethods - 1)
    val minimumContentWidth = minimumCardsWidth + spacingWidth

    val viewWidth = if (minimumContentWidth <= targetWidth) {
        // Stretch cards to fill entire width
        (targetWidth - spacingWidth) / numberOfPaymentMethods
    } else {
        // Naively finds the minimum item width for the target peek amount
        listOf(.3f, .4f, .5f).minOf { lastCardPeekAmount ->
            computeItemWidthWhenExceedingMaxWidth(
                availableWidth = targetWidth,
                minItemWidth = minItemWidth,
                spacing = PaymentMethodsUISpacing.carouselInnerPadding,
                lastCardPeekAmount = lastCardPeekAmount,
            )
        }
    }
    return viewWidth
}

private fun computeItemWidthWhenExceedingMaxWidth(
    availableWidth: Dp,
    minItemWidth: Dp,
    spacing: Dp,
    lastCardPeekAmount: Float,
): Dp {
    val itemWithSpacing = minItemWidth + spacing

    val peekingCardWidth = (minItemWidth.value * lastCardPeekAmount).dp
    val remainingWidthAfterAddingFixedCards = availableWidth - minItemWidth - peekingCardWidth
    val numberOfAdditionalCards = (remainingWidthAfterAddingFixedCards / itemWithSpacing).toInt()

    val visibleCards = numberOfAdditionalCards + 1 + lastCardPeekAmount
    val overallSpacing = spacing * numberOfAdditionalCards

    return ((availableWidth - overallSpacing).value / visibleCards).dp
}
