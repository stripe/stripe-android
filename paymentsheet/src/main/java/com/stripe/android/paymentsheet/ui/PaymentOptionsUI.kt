package com.stripe.android.paymentsheet.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.PaymentOptionUi
import com.stripe.android.paymentsheet.PaymentOptionsItem
import com.stripe.android.paymentsheet.PaymentOptionsState
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.toPaymentSelection
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.uicore.shouldUseDarkDynamicColor
import com.stripe.android.uicore.stripeColors
import com.stripe.android.R as StripeR

@Composable
internal fun PaymentOptions(
    viewModel: BaseSheetViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.paymentOptionsState.collectAsState()
    val isEditing by viewModel.editing.collectAsState()
    val isProcessing by viewModel.processing.collectAsState()

    PaymentOptions(
        state = state,
        isEditing = isEditing,
        isProcessing = isProcessing,
        onAddCardPressed = viewModel::transitionToAddPaymentScreen,
        onItemSelected = viewModel::handlePaymentMethodSelected,
        onItemRemoved = viewModel::removePaymentMethod,
        modifier = modifier,
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun PaymentOptions(
    state: PaymentOptionsState,
    isEditing: Boolean,
    isProcessing: Boolean,
    onAddCardPressed: () -> Unit,
    onItemSelected: (PaymentSelection?) -> Unit,
    onItemRemoved: (PaymentMethod) -> Unit,
    modifier: Modifier = Modifier,
    scrollState: LazyListState = rememberLazyListState(),
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val width = rememberItemWidth(maxWidth)

        LazyRow(
            state = scrollState,
            userScrollEnabled = !isProcessing,
            contentPadding = PaddingValues(horizontal = 17.dp),
        ) {
            items(state.items) { item ->
                val isEnabled = !isProcessing && (!isEditing || item.isEnabledDuringEditing)
                val isSelected = item == state.selectedItem && !isEditing

                PaymentOption(
                    item = item,
                    width = width,
                    isEditing = isEditing,
                    isEnabled = isEnabled,
                    isSelected = isSelected,
                    onAddCardPressed = onAddCardPressed,
                    onItemSelected = onItemSelected,
                    onItemRemoved = onItemRemoved,
                    modifier = Modifier
                        .semantics { testTagsAsResourceId = true }
                        .testTag(item.viewType.name)
                )
            }
        }
    }
}

@Composable
internal fun rememberItemWidth(maxWidth: Dp): Dp = remember(maxWidth) {
    val targetWidth = maxWidth - 17.dp * 2
    val minItemWidth = 100.dp + (6.dp * 2)
    // numVisibleItems is incremented in steps of 0.5 items (1, 1.5, 2, 2.5, 3, ...)
    val numVisibleItems = (targetWidth * 2 / minItemWidth).toInt() / 2f
    (targetWidth / numVisibleItems)
}

@Composable
private fun PaymentOption(
    item: PaymentOptionsItem,
    width: Dp,
    isEnabled: Boolean,
    isEditing: Boolean,
    isSelected: Boolean,
    onAddCardPressed: () -> Unit,
    onItemSelected: (PaymentSelection?) -> Unit,
    onItemRemoved: (PaymentMethod) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (item) {
        is PaymentOptionsItem.AddCard -> {
            AddCard(
                width = width,
                isEnabled = isEnabled,
                onAddCardPressed = onAddCardPressed,
                modifier = modifier,
            )
        }
        is PaymentOptionsItem.GooglePay -> {
            GooglePay(
                width = width,
                isEnabled = isEnabled,
                isSelected = isSelected,
                onItemSelected = onItemSelected,
                modifier = modifier,
            )
        }
        is PaymentOptionsItem.Link -> {
            Link(
                width = width,
                isEnabled = isEnabled,
                isSelected = isSelected,
                onItemSelected = onItemSelected,
                modifier = modifier,
            )
        }
        is PaymentOptionsItem.SavedPaymentMethod -> {
            SavedPaymentMethod(
                paymentMethod = item,
                width = width,
                isEnabled = isEnabled,
                isEditing = isEditing,
                isSelected = isSelected,
                onItemSelected = onItemSelected,
                onItemRemoved = onItemRemoved,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun AddCard(
    width: Dp,
    isEnabled: Boolean,
    onAddCardPressed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val iconRes = if (MaterialTheme.stripeColors.component.shouldUseDarkDynamicColor()) {
        R.drawable.stripe_ic_paymentsheet_add_dark
    } else {
        R.drawable.stripe_ic_paymentsheet_add_light
    }

    PaymentOptionUi(
        viewWidth = width,
        isEditing = false,
        isSelected = false,
        isEnabled = isEnabled,
        labelText = stringResource(R.string.stripe_paymentsheet_add_payment_method_button_label),
        iconRes = iconRes,
        onItemSelectedListener = onAddCardPressed,
        description = stringResource(R.string.stripe_add_new_payment_method),
        modifier = modifier,
    )
}

@Composable
private fun GooglePay(
    width: Dp,
    isEnabled: Boolean,
    isSelected: Boolean,
    onItemSelected: (PaymentSelection?) -> Unit,
    modifier: Modifier = Modifier,
) {
    PaymentOptionUi(
        viewWidth = width,
        isEditing = false,
        isSelected = isSelected,
        isEnabled = isEnabled,
        iconRes = R.drawable.stripe_google_pay_mark,
        labelText = stringResource(StripeR.string.stripe_google_pay),
        description = stringResource(StripeR.string.stripe_google_pay),
        onItemSelectedListener = { onItemSelected(PaymentSelection.GooglePay) },
        modifier = modifier,
    )
}

@Composable
private fun Link(
    width: Dp,
    isEnabled: Boolean,
    isSelected: Boolean,
    onItemSelected: (PaymentSelection?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val surfaceColor = MaterialTheme.stripeColors.component
    val linkLogoColor = remember(surfaceColor) {
        if (surfaceColor.shouldUseDarkDynamicColor()) {
            Color.Black
        } else {
            Color.White
        }
    }

    PaymentOptionUi(
        viewWidth = width,
        isEditing = false,
        isSelected = isSelected,
        isEnabled = isEnabled,
        iconRes = R.drawable.stripe_link_mark,
        iconTint = linkLogoColor,
        labelText = stringResource(StripeR.string.stripe_link),
        description = stringResource(StripeR.string.stripe_link),
        onItemSelectedListener = { onItemSelected(PaymentSelection.Link) },
        modifier = modifier,
    )
}

@Composable
private fun SavedPaymentMethod(
    paymentMethod: PaymentOptionsItem.SavedPaymentMethod,
    width: Dp,
    isEnabled: Boolean,
    isEditing: Boolean,
    isSelected: Boolean,
    onItemSelected: (PaymentSelection?) -> Unit,
    onItemRemoved: (PaymentMethod) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val labelIcon = paymentMethod.paymentMethod.getLabelIcon()
    val labelText = paymentMethod.paymentMethod.getLabel(context.resources) ?: return

    val removeTitle = stringResource(
        R.string.stripe_paymentsheet_remove_pm,
        paymentMethod.displayName,
    )

    PaymentOptionUi(
        viewWidth = width,
        isEditing = isEditing,
        isSelected = isSelected,
        isEnabled = isEnabled,
        iconRes = paymentMethod.paymentMethod.getSavedPaymentMethodIcon() ?: 0,
        labelIcon = labelIcon,
        labelText = labelText,
        removePmDialogTitle = removeTitle,
        description = paymentMethod.getDescription(context.resources),
        onRemoveListener = { onItemRemoved(paymentMethod.paymentMethod) },
        onRemoveAccessibilityDescription = paymentMethod.getRemoveDescription(context.resources),
        onItemSelectedListener = { onItemSelected(paymentMethod.toPaymentSelection()) },
        modifier = modifier,
    )
}
