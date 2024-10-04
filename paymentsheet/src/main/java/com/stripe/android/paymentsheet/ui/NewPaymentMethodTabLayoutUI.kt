@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package com.stripe.android.paymentsheet.ui

import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.lpmfoundations.paymentmethod.definitions.CardDefinition
import com.stripe.android.lpmfoundations.paymentmethod.definitions.InstantDebitsDefinition
import com.stripe.android.ui.core.R
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.image.StripeImageLoader
import com.stripe.android.uicore.strings.resolve

private object PaymentMethodsUISpacing {
    val carouselOuterPadding = 20.dp
    val carouselInnerPadding = 12.dp
}

@VisibleForTesting
const val TEST_TAG_LIST = "PaymentMethodsUITestTag"

@Preview
@Composable
private fun NewPaymentMethodTabLayoutUIPreview() {
    StripeTheme {
        NewPaymentMethodTabLayoutUI(
            paymentMethods = listOf(
                SupportedPaymentMethod(
                    paymentMethodDefinition = CardDefinition,
                    displayNameResource = R.string.stripe_paymentsheet_payment_method_card,
                    iconResource = R.drawable.stripe_ic_paymentsheet_pm_card,
                    iconRequiresTinting = true,
                ),
                SupportedPaymentMethod(
                    code = InstantDebitsDefinition.type.code,
                    displayNameResource = R.string.stripe_paymentsheet_payment_method_instant_debits,
                    iconResource = R.drawable.stripe_ic_paymentsheet_pm_bank,
                    iconRequiresTinting = true,
                    lightThemeIconUrl = null,
                    darkThemeIconUrl = null,
                    incentive = null,
                )
            ),
            selectedIndex = 0,
            isEnabled = true,
            onItemSelectedListener = {},
            imageLoader = StripeImageLoader(LocalContext.current),
        )
    }
}

@Composable
internal fun NewPaymentMethodTabLayoutUI(
    paymentMethods: List<SupportedPaymentMethod>,
    selectedIndex: Int,
    isEnabled: Boolean,
    onItemSelectedListener: (SupportedPaymentMethod) -> Unit,
    imageLoader: StripeImageLoader,
    modifier: Modifier = Modifier,
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

    BoxWithConstraints(
        modifier = modifier.testTag(TEST_TAG_LIST + "1")
    ) {
        val viewWidth = rememberViewWidth(
            this.maxWidth,
            paymentMethods.size
        )

        LazyRow(
            state = state,
            contentPadding = PaddingValues(horizontal = PaymentMethodsUISpacing.carouselOuterPadding),
            horizontalArrangement = Arrangement.spacedBy(PaymentMethodsUISpacing.carouselInnerPadding),
            userScrollEnabled = isEnabled,
            modifier = Modifier.testTag(TEST_TAG_LIST)
        ) {
            itemsIndexed(items = paymentMethods) { index, item ->
                val iconUrl = if (isSystemInDarkTheme() && item.darkThemeIconUrl != null) {
                    item.darkThemeIconUrl
                } else {
                    item.lightThemeIconUrl
                }
                NewPaymentMethodTab(
                    modifier = Modifier
                        .testTag(TEST_TAG_LIST + item.code)
                        .fillMaxWidth(),
                    minWidth = viewWidth,
                    iconRes = item.iconResource,
                    iconUrl = iconUrl,
                    imageLoader = imageLoader,
                    title = item.displayName.resolve(),
                    isSelected = index == selectedIndex,
                    isEnabled = isEnabled,
                    iconRequiresTinting = item.iconRequiresTinting,
                    incentive = item.incentive,
                    onItemSelectedListener = {
                        onItemSelectedListener(paymentMethods[index])
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
    val targetWidth = maxWidth - (PaymentMethodsUISpacing.carouselOuterPadding * 2)
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
