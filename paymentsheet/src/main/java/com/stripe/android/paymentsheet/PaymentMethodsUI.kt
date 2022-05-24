package com.stripe.android.paymentsheet

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
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
import com.stripe.android.ui.core.PaymentsTheme
import com.stripe.android.ui.core.elements.LpmRepository.SupportedPaymentMethod
import com.stripe.android.ui.core.elements.SectionCard
import com.stripe.android.ui.core.paymentsColors

internal const val ADD_PM_DEFAULT_PADDING = 12.0f
internal const val CARD_HORIZONTAL_PADDING = 6.0f
internal const val PM_LIST_PADDING = 17.0f

@VisibleForTesting
const val TEST_TAG_LIST = "PaymentMethodsUITestTag"

@Composable
internal fun PaymentMethodsUI(
    paymentMethods: List<SupportedPaymentMethod>,
    selectedIndex: Int,
    isEnabled: Boolean,
    onItemSelectedListener: (SupportedPaymentMethod) -> Unit
) {
    val scope = rememberCoroutineScope()
    val state = rememberLazyListState()

    LaunchedEffect(selectedIndex) {
        state.scrollToItem(selectedIndex, 0)
    }

    LaunchedEffect(isEnabled) {
        if (isEnabled) {
            state.reenableScrolling(scope)
        } else {
            state.disableScrolling(scope)
        }
    }
    PaymentsTheme {
        BoxWithConstraints(
            modifier = Modifier
                .testTag(TEST_TAG_LIST + "1")
        ) {
            val viewWidth = calculateViewWidth(
                this.maxWidth,
                paymentMethods.size
            )

            // TODO: userScrollEnabled will be available in compose version 1.2.0-alpha01+
            LazyRow(
                state = state,
                modifier = Modifier
                    .padding(start = PM_LIST_PADDING.dp)
                    .testTag(TEST_TAG_LIST)
            ) {
                itemsIndexed(items = paymentMethods, itemContent = { index, item ->
                    PaymentMethodUI(
                        modifier = Modifier.testTag(
                            TEST_TAG_LIST + stringResource(item.displayNameResource)
                        ),
                        viewWidth = viewWidth,
                        iconRes = item.iconResource,
                        title = stringResource(item.displayNameResource),
                        isSelected = index == selectedIndex,
                        isEnabled = isEnabled,
                        tintOnSelected = item.shouldTintOnSelection(),
                        itemIndex = index,
                        onItemSelectedListener = {
                            onItemSelectedListener(paymentMethods[it])
                        }
                    )
                })
            }
        }
    }
}

internal fun calculateViewWidth(
    maxWidth: Dp,
    numberOfPaymentMethods: Int
): Dp {
    val targetWidthDp = maxWidth - (PM_LIST_PADDING.dp * 2)
    val minItemWidthDp = (100 + (2 * CARD_HORIZONTAL_PADDING)).dp

    val viewWidth = if ((minItemWidthDp * numberOfPaymentMethods) < targetWidthDp) {
        targetWidthDp / numberOfPaymentMethods
    } else {
        val maxNumVisibleItemsAtMinimumSize = (targetWidthDp / minItemWidthDp).toInt()
        targetWidthDp / maxNumVisibleItemsAtMinimumSize
    }

    return viewWidth
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
    val color = if (isSelected) MaterialTheme.colors.primary
    else MaterialTheme.paymentsColors.onComponent

    val lpmTextSelector: @Composable () -> Unit = {
        LpmSelectorText(
            text = title,
            isEnabled = isEnabled,
            textColor = color,
            modifier = Modifier.padding(top = 6.dp, start = ADD_PM_DEFAULT_PADDING.dp)
        )
    }

    MeasureComposableWidth(composable = lpmTextSelector) { lpmSelectorTextWidth ->
        SectionCard(
            isSelected = isSelected,
            modifier = modifier
                .alpha(alpha = if (isEnabled) 1.0F else 0.6F)
                .height(60.dp)
                .width(
                    maxOf(
                        viewWidth,
                        lpmSelectorTextWidth +
                            (CARD_HORIZONTAL_PADDING.dp * 2) +
                            ADD_PM_DEFAULT_PADDING.dp
                    )
                )
                .padding(horizontal = CARD_HORIZONTAL_PADDING.dp)
                .selectable(
                    selected = isSelected,
                    enabled = isEnabled,
                    onClick = {
                        onItemSelectedListener(itemIndex)
                    }
                )
        ) {
            Column {
                val colorFilter = if (tintOnSelected) ColorFilter.tint(color) else null
                Image(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    colorFilter = colorFilter,
                    modifier = Modifier
                        .padding(top = ADD_PM_DEFAULT_PADDING.dp, start = ADD_PM_DEFAULT_PADDING.dp)
                )
                lpmTextSelector()
            }
        }
    }
}
