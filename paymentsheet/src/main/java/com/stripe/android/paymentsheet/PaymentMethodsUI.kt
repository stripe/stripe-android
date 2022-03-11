package com.stripe.android.paymentsheet

import android.util.DisplayMetrics
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.stripe.android.paymentsheet.model.SupportedPaymentMethod
import com.stripe.android.paymentsheet.ui.LpmSelectorText
import com.stripe.android.ui.core.PaymentsTheme

internal const val ADD_PM_DEFAULT_PADDING = 12.0f
internal const val CARD_HORIZONTAL_PADDING = 6.0f
internal const val PM_LIST_PADDING = 14.0f

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

    LaunchedEffect(isEnabled) {
        if (isEnabled) {
            state.reenableScrolling(scope)
        } else {
            state.disableScrolling(scope)
        }
    }

    BoxWithConstraints {
        val resources = LocalContext.current.resources
        val viewWidth = calculateViewWidth(
            dpToPx(resources.displayMetrics, this.maxWidth),
            resources.displayMetrics,
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
                    itemIndex = index,
                    onItemSelectedListener = {
                        onItemSelectedListener(paymentMethods[it])
                    }
                )
            })
        }
    }
}

internal fun calculateViewWidth(
    maxWidth: Int,
    displayMetrics: DisplayMetrics,
    numberOfPaymentMethods: Int
): Dp {
    val screenDensity = displayMetrics.density
    val targetWidth = maxWidth - dpToPx(displayMetrics, PM_LIST_PADDING.dp * 2)
    val minItemWidth = 100 * screenDensity + (2 * CARD_HORIZONTAL_PADDING)

    // if all items fit at min width, then span them across the sheet evenly filling it.
    // otherwise the number of items visible should be a multiple of .5
    val viewWidth =
        if (minItemWidth * numberOfPaymentMethods < targetWidth) {
            targetWidth / numberOfPaymentMethods
        } else {
            // numVisibleItems is incremented in steps of 0.5 items
            // (1, 1.5, 2, 2.5, 3, ...)
            val numVisibleItems = (targetWidth * 2 / minItemWidth).toInt() / 2f
            targetWidth / numVisibleItems
        }

    return (viewWidth.toInt() / screenDensity).dp
}

internal fun dpToPx(displayMetrics: DisplayMetrics, dp: Dp): Int {
    return (dp.value * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT)).toInt()
}

@Composable
internal fun PaymentMethodUI(
    viewWidth: Dp,
    iconRes: Int,
    title: String,
    isSelected: Boolean,
    isEnabled: Boolean,
    itemIndex: Int,
    modifier: Modifier = Modifier,
    onItemSelectedListener: (Int) -> Unit
) {
    val strokeColor =
        if (isSelected) {
            PaymentsTheme.colors.material.primary
        } else {
            PaymentsTheme.colors.colorComponentBorder
        }

    Card(
        border = BorderStroke(if (isSelected) 2.dp else 1.5.dp, strokeColor),
        shape = RoundedCornerShape(6.dp),
        elevation = if (isSelected) 1.5.dp else 0.dp,
        backgroundColor = PaymentsTheme.colors.colorComponentBackground,
        modifier = modifier
            .alpha(alpha = if (isEnabled) 1.0F else 0.6F)
            .height(60.dp)
            .width(viewWidth)
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
            Image(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier
                    .padding(top = ADD_PM_DEFAULT_PADDING.dp, start = ADD_PM_DEFAULT_PADDING.dp)
            )
            LpmSelectorText(
                text = title,
                isEnabled = isEnabled,
                textColor = PaymentsTheme.colors.material.onBackground,
                modifier = Modifier.padding(top = 6.dp, start = ADD_PM_DEFAULT_PADDING.dp)
            )
        }
    }
}
