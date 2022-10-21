package com.stripe.android.paymentsheet

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import com.stripe.android.paymentsheet.ui.getLabel
import com.stripe.android.paymentsheet.ui.getLabelIcon
import com.stripe.android.paymentsheet.ui.getSavedPaymentMethodIcon
import com.stripe.android.ui.core.paymentsColors
import com.stripe.android.ui.core.shouldUseDarkDynamicColor

@Composable
internal fun PaymentOptions(
    state: PaymentOptionsState,
    isEnabled: Boolean,
    isEditing: Boolean,
    paymentMethodNameProvider: (String?) -> String?,
    onAddCard: () -> Unit,
    onRemove: (PaymentOptionsItem.SavedPaymentMethod) -> Unit,
    onItemSelected: (PaymentOptionsItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier) {
        val itemPadding = PaymentMethodsSpacing.carouselInnerPadding

        // We need to add the item padding here. Unlike in PaymentMethodsUI, we need to add the
        // padding within the row item instead of between them so that we can properly render the
        // delete badge when in edit mode.
        val itemWidth = rememberViewWidth(
            maxWidth = maxWidth,
            numberOfPaymentMethods = state.items.size,
        ) + itemPadding

        val contentPadding = remember {
            PaymentMethodsSpacing.carouselOuterPadding - itemPadding / 2
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = contentPadding),
            userScrollEnabled = isEnabled,
        ) {
            items(state.items) { item ->
                val isSelected = item == state.selectedItem
                PaymentOptionCard(
                    item = item,
                    width = itemWidth,
                    isSelected = isSelected,
                    isEnabled = isEnabled,
                    isEditing = isEditing,
                    paymentMethodNameProvider = paymentMethodNameProvider,
                    onAddCard = onAddCard,
                    onRemove = onRemove,
                    onItemSelected = onItemSelected,
                )
            }
        }
    }
}

@Composable
private fun PaymentOptionCard(
    item: PaymentOptionsItem,
    width: Dp,
    isSelected: Boolean,
    isEnabled: Boolean,
    isEditing: Boolean,
    paymentMethodNameProvider: (String?) -> String?,
    onAddCard: () -> Unit,
    onRemove: (PaymentOptionsItem.SavedPaymentMethod) -> Unit,
    onItemSelected: (PaymentOptionsItem) -> Unit,
) {
    when (item) {
        is PaymentOptionsItem.AddCard -> {
            AddCard(
                width = width,
                isEnabled = isEnabled,
                onAddCard = onAddCard,
            )
        }
        is PaymentOptionsItem.GooglePay -> {
            GooglePay(
                item = item,
                width = width,
                isSelected = isSelected,
                isEnabled = isEnabled,
                onItemSelected = onItemSelected,
            )
        }
        is PaymentOptionsItem.Link -> {
            Link(
                item = item,
                width = width,
                isSelected = isSelected,
                isEnabled = isEnabled,
                onItemSelected = onItemSelected,
            )
        }
        is PaymentOptionsItem.SavedPaymentMethod -> {
            SavedPaymentMethod(
                savedPaymentMethod = item,
                width = width,
                isSelected = isSelected,
                isEditing = isEditing,
                isEnabled = isEnabled,
                paymentMethodNameProvider = paymentMethodNameProvider,
                onRemove = onRemove,
                onItemSelected = onItemSelected,
            )
        }
    }
}

@Composable
private fun AddCard(
    width: Dp,
    isEnabled: Boolean,
    onAddCard: () -> Unit,
) {
    val iconRes = if (
        MaterialTheme.paymentsColors.component.shouldUseDarkDynamicColor()
    ) {
        R.drawable.stripe_ic_paymentsheet_add_dark
    } else {
        R.drawable.stripe_ic_paymentsheet_add_light
    }
    PaymentOptionUI(
        viewWidth = width,
        isEditing = false,
        isSelected = false,
        isEnabled = isEnabled,
        labelText = stringResource(R.string.stripe_paymentsheet_add_payment_method_button_label),
        iconRes = iconRes,
        onItemSelectedListener = onAddCard,
        description = stringResource(R.string.add_new_payment_method)
    )
}

@Composable
private fun GooglePay(
    item: PaymentOptionsItem,
    width: Dp,
    isSelected: Boolean,
    isEnabled: Boolean,
    onItemSelected: (PaymentOptionsItem) -> Unit,
) {
    val resources = LocalContext.current.resources

    PaymentOptionUI(
        viewWidth = width,
        isEditing = false,
        isSelected = isSelected,
        isEnabled = isEnabled,
        iconRes = R.drawable.stripe_google_pay_mark,
        labelText = resources.getString(R.string.google_pay),
        description = resources.getString(R.string.google_pay),
        onItemSelectedListener = { onItemSelected(item) }
    )
}

@Composable
private fun Link(
    item: PaymentOptionsItem,
    width: Dp,
    isSelected: Boolean,
    isEnabled: Boolean,
    onItemSelected: (PaymentOptionsItem) -> Unit,
) {
    val resources = LocalContext.current.resources

    PaymentOptionUI(
        viewWidth = width,
        isEditing = false,
        isSelected = isSelected,
        isEnabled = isEnabled,
        iconRes = R.drawable.stripe_link_mark,
        labelText = resources.getString(R.string.link),
        description = resources.getString(R.string.link),
        onItemSelectedListener = { onItemSelected(item) }
    )
}

@Composable
private fun SavedPaymentMethod(
    savedPaymentMethod: PaymentOptionsItem.SavedPaymentMethod,
    width: Dp,
    isSelected: Boolean,
    isEnabled: Boolean,
    isEditing: Boolean,
    paymentMethodNameProvider: (String?) -> String?,
    onRemove: (PaymentOptionsItem.SavedPaymentMethod) -> Unit,
    onItemSelected: (PaymentOptionsItem) -> Unit,
) {
    val resources = LocalContext.current.resources

    val labelIcon = savedPaymentMethod.paymentMethod.getLabelIcon()
    val labelText = savedPaymentMethod.paymentMethod.getLabel(resources) ?: return

    val paymentMethodName = paymentMethodNameProvider(savedPaymentMethod.paymentMethod.type?.code)
    val removeTitle = resources.getString(R.string.stripe_paymentsheet_remove_pm, paymentMethodName)

    PaymentOptionUI(
        viewWidth = width,
        isEditing = isEditing,
        isSelected = isSelected,
        isEnabled = isEnabled,
        iconRes = savedPaymentMethod.paymentMethod.getSavedPaymentMethodIcon() ?: 0,
        labelIcon = labelIcon,
        labelText = labelText,
        removePmDialogTitle = removeTitle,
        description = savedPaymentMethod.getDescription(resources),
        onRemoveListener = { onRemove(savedPaymentMethod) },
        onRemoveAccessibilityDescription = savedPaymentMethod.getRemoveDescription(resources),
        onItemSelectedListener = { onItemSelected(savedPaymentMethod) }
    )
}
