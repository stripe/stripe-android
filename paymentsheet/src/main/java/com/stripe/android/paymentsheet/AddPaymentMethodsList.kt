package com.stripe.android.paymentsheet

import android.view.View
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.stripe.android.paymentsheet.model.SupportedPaymentMethod
import com.stripe.android.paymentsheet.ui.LpmSelectorText

internal const val ADD_PM_DEFAULT_PADDING = 12.0f
internal const val CARD_HORIZONTAL_PADDING = 6.0f
internal fun calculateViewWidth(parent: View, numberOfPaymentMethods: Int): Dp {
    val targetWidth = parent.measuredWidth - parent.paddingStart - parent.paddingEnd
    val screenDensity = parent.context.resources.displayMetrics.density
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

@Composable
internal fun PaymentMethodsUI(
    lpms: List<SupportedPaymentMethod>,
    initialSelectedIndex: Int,
    isEnabled: Boolean,
    viewWidth: Dp,
    onItemSelectedListener: (SupportedPaymentMethod) -> Unit
) {
    // TODO: Would prefer to make the selectedIndex observable
    val selectedIndex = remember { mutableStateOf(initialSelectedIndex) }
    val scope = rememberCoroutineScope()
    val state = rememberLazyListState()

    LaunchedEffect(isEnabled) {
        if (isEnabled) {
            state.reenableScrolling(scope)
        } else {
            state.disableScrolling(scope)
        }
    }

    // TODO: userScrollEnabled will be available in compose version 1.2.0-alpha01+
    LazyRow(state = state) {
        itemsIndexed(items = lpms, itemContent = { index, item ->
            PaymentMethodUI(
                viewWidth = viewWidth,
                iconRes = item.iconResource,
                title = stringResource(item.displayNameResource),
                isSelected = index == selectedIndex.value,
                isEnabled = isEnabled,
                itemIndex = index,
                onItemSelectedListener = {
                    selectedIndex.value = it
                    onItemSelectedListener(lpms.get(it))
                }
            )
        }
        )
    }
}

@Composable
internal fun PaymentMethodUI(
    viewWidth: Dp,
    iconRes: Int,
    title: String,
    isSelected: Boolean,
    isEnabled: Boolean,
    itemIndex: Int,
    onItemSelectedListener: (Int) -> Unit
) {
    val strokeColor = colorResource(
        if (isSelected) {
            R.color.stripe_paymentsheet_add_pm_card_selected_stroke
        } else {
            R.color.stripe_paymentsheet_add_pm_card_unselected_stroke
        }
    )

    val cardBackgroundColor = if (isEnabled) {
        colorResource(R.color.stripe_paymentsheet_elements_background_default)
    } else {
        colorResource(R.color.stripe_paymentsheet_elements_background_disabled)
    }

    Card(
        border = BorderStroke(if (isSelected) 2.dp else 1.5.dp, strokeColor),
        shape = RoundedCornerShape(6.dp),
        elevation = if (isSelected) 1.5.dp else 0.dp,
        backgroundColor = cardBackgroundColor,
        modifier = Modifier
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
                modifier = Modifier.padding(top = 6.dp, start = ADD_PM_DEFAULT_PADDING.dp)
            )
        }
    }
}
