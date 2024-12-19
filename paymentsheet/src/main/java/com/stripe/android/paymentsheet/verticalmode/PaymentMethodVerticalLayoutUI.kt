package com.stripe.android.paymentsheet.verticalmode

import androidx.annotation.RestrictTo
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.PaymentSheet.Appearance.Embedded
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.analytics.code
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.isSaved
import com.stripe.android.uicore.image.StripeImageLoader
import com.stripe.android.uicore.stripeColors
import com.stripe.android.uicore.utils.collectAsState
import org.jetbrains.annotations.VisibleForTesting

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
const val TEST_TAG_PAYMENT_METHOD_VERTICAL_LAYOUT = "TEST_TAG_PAYMENT_METHOD_VERTICAL_LAYOUT"
internal const val TEST_TAG_VIEW_MORE = "TEST_TAG_VIEW_MORE"
internal const val TEST_TAG_EDIT_SAVED_CARD = "TEST_TAG_VERTICAL_MODE_SAVED_PM_EDIT"
internal const val TEST_TAG_SAVED_TEXT = "TEST_TAG_SAVED_TEXT"

@OptIn(ExperimentalEmbeddedPaymentElementApi::class)
@Composable
internal fun PaymentMethodVerticalLayoutUI(
    interactor: PaymentMethodVerticalLayoutInteractor,
    modifier: Modifier = Modifier,
    isEmbedded: Boolean = false
) {
    val context = LocalContext.current
    val imageLoader = remember {
        StripeImageLoader(context.applicationContext)
    }

    val state by interactor.state.collectAsState()

    PaymentMethodVerticalLayoutUI(
        paymentMethods = state.displayablePaymentMethods,
        displayedSavedPaymentMethod = state.displayedSavedPaymentMethod,
        savedPaymentMethodAction = state.availableSavedPaymentMethodAction,
        selection = state.selection,
        isEnabled = !state.isProcessing,
        onViewMorePaymentMethods = {
            interactor.handleViewAction(
                PaymentMethodVerticalLayoutInteractor.ViewAction.TransitionToManageSavedPaymentMethods
            )
        },
        onEditPaymentMethod = {
            interactor.handleViewAction(
                PaymentMethodVerticalLayoutInteractor.ViewAction.EditPaymentMethod(it)
            )
        },
        onSelectSavedPaymentMethod = {
            interactor.handleViewAction(
                PaymentMethodVerticalLayoutInteractor.ViewAction.SavedPaymentMethodSelected(it.paymentMethod)
            )
        },
        onManageOneSavedPaymentMethod = {
            interactor.handleViewAction(
                PaymentMethodVerticalLayoutInteractor.ViewAction.OnManageOneSavedPaymentMethod(it)
            )
        },
        imageLoader = imageLoader,
        modifier = modifier
            .testTag(TEST_TAG_PAYMENT_METHOD_VERTICAL_LAYOUT),
        rowStyle = state.rowType,
        isEmbedded = isEmbedded
    )
}

@OptIn(ExperimentalEmbeddedPaymentElementApi::class)
@VisibleForTesting
@Composable
internal fun PaymentMethodVerticalLayoutUI(
    paymentMethods: List<DisplayablePaymentMethod>,
    displayedSavedPaymentMethod: DisplayableSavedPaymentMethod?,
    savedPaymentMethodAction: PaymentMethodVerticalLayoutInteractor.SavedPaymentMethodAction,
    selection: PaymentSelection?,
    isEnabled: Boolean,
    onViewMorePaymentMethods: () -> Unit,
    onManageOneSavedPaymentMethod: (DisplayableSavedPaymentMethod) -> Unit,
    onEditPaymentMethod: (DisplayableSavedPaymentMethod) -> Unit,
    onSelectSavedPaymentMethod: (DisplayableSavedPaymentMethod) -> Unit,
    imageLoader: StripeImageLoader,
    rowStyle: Embedded.RowStyle = Embedded.RowStyle.FloatingButton.default,
    isEmbedded: Boolean = false,
    modifier: Modifier = Modifier,
) {
    if (isEmbedded) {
        EmbeddedContent(
            paymentMethods = paymentMethods,
            displayedSavedPaymentMethod = displayedSavedPaymentMethod,
            savedPaymentMethodAction = savedPaymentMethodAction,
            selection = selection,
            isEnabled = isEnabled,
            onViewMorePaymentMethods = onViewMorePaymentMethods,
            onManageOneSavedPaymentMethod = onManageOneSavedPaymentMethod,
            onEditPaymentMethod = onEditPaymentMethod,
            onSelectSavedPaymentMethod = onSelectSavedPaymentMethod,
            imageLoader = imageLoader,
            modifier = modifier,
            rowStyle = rowStyle
        )
    } else {
        VerticalModeContent(
            paymentMethods = paymentMethods,
            displayedSavedPaymentMethod = displayedSavedPaymentMethod,
            savedPaymentMethodAction = savedPaymentMethodAction,
            selection = selection,
            isEnabled = isEnabled,
            onViewMorePaymentMethods = onViewMorePaymentMethods,
            onManageOneSavedPaymentMethod = onManageOneSavedPaymentMethod,
            onEditPaymentMethod = onEditPaymentMethod,
            onSelectSavedPaymentMethod = onSelectSavedPaymentMethod,
            imageLoader = imageLoader,
            modifier = modifier,
        )
    }
}

@OptIn(ExperimentalEmbeddedPaymentElementApi::class)
@Composable
private fun VerticalModeContent(
    paymentMethods: List<DisplayablePaymentMethod>,
    displayedSavedPaymentMethod: DisplayableSavedPaymentMethod?,
    savedPaymentMethodAction: PaymentMethodVerticalLayoutInteractor.SavedPaymentMethodAction,
    selection: PaymentSelection?,
    isEnabled: Boolean,
    onViewMorePaymentMethods: () -> Unit,
    onManageOneSavedPaymentMethod: (DisplayableSavedPaymentMethod) -> Unit,
    onEditPaymentMethod: (DisplayableSavedPaymentMethod) -> Unit,
    onSelectSavedPaymentMethod: (DisplayableSavedPaymentMethod) -> Unit,
    imageLoader: StripeImageLoader,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val textStyle = MaterialTheme.typography.subtitle1
        val textColor = MaterialTheme.stripeColors.onComponent

        if (displayedSavedPaymentMethod != null) {
            Text(
                text = stringResource(id = R.string.stripe_paymentsheet_saved),
                style = textStyle,
                color = textColor,
                modifier = Modifier.testTag(TEST_TAG_SAVED_TEXT),
            )
            SavedPaymentMethodRowButton(
                displayableSavedPaymentMethod = displayedSavedPaymentMethod,
                isEnabled = isEnabled,
                isSelected = selection?.isSaved == true,
                trailingContent = {
                    SavedPaymentMethodTrailingContent(
                        displayedSavedPaymentMethod = displayedSavedPaymentMethod,
                        savedPaymentMethodAction = savedPaymentMethodAction,
                        onViewMorePaymentMethods = onViewMorePaymentMethods,
                        onEditPaymentMethod = onEditPaymentMethod,
                        onManageOneSavedPaymentMethod = { onManageOneSavedPaymentMethod(displayedSavedPaymentMethod) },
                    )
                },
                onClick = { onSelectSavedPaymentMethod(displayedSavedPaymentMethod) },
            )
            Text(stringResource(id = R.string.stripe_paymentsheet_new_pm), style = textStyle, color = textColor)
        }

        val selectedIndex = remember(selection, paymentMethods) {
            if (selection == null || selection.isSaved) {
                -1
            } else {
                val code = selection.code()
                paymentMethods.indexOfFirst { it.code == code }
            }
        }

        NewPaymentMethodVerticalLayoutUI(
            paymentMethods = paymentMethods,
            selectedIndex = selectedIndex,
            isEnabled = isEnabled,
            imageLoader = imageLoader,
        )
    }
}

@OptIn(ExperimentalEmbeddedPaymentElementApi::class)
@Composable
private fun EmbeddedContent(
    paymentMethods: List<DisplayablePaymentMethod>,
    displayedSavedPaymentMethod: DisplayableSavedPaymentMethod?,
    savedPaymentMethodAction: PaymentMethodVerticalLayoutInteractor.SavedPaymentMethodAction,
    selection: PaymentSelection?,
    isEnabled: Boolean,
    onViewMorePaymentMethods: () -> Unit,
    onManageOneSavedPaymentMethod: (DisplayableSavedPaymentMethod) -> Unit,
    onEditPaymentMethod: (DisplayableSavedPaymentMethod) -> Unit,
    onSelectSavedPaymentMethod: (DisplayableSavedPaymentMethod) -> Unit,
    imageLoader: StripeImageLoader,
    rowStyle: Embedded.RowStyle,
    modifier: Modifier = Modifier,
) {
    val arrangement = if (rowStyle is Embedded.RowStyle.FloatingButton) {
        Arrangement.spacedBy(rowStyle.spacingDp.dp)
    } else {
        Arrangement.Top
    }
    Column(modifier = modifier, verticalArrangement = arrangement) {
        if (rowStyle.topSeparatorEnabled()) OptionalEmbeddedDivider(rowStyle)
        if (displayedSavedPaymentMethod != null) {
            SavedPaymentMethodRowButton(
                displayableSavedPaymentMethod = displayedSavedPaymentMethod,
                isEnabled = isEnabled,
                isSelected = selection?.isSaved == true,
                trailingContent = {
                    SavedPaymentMethodTrailingContent(
                        displayedSavedPaymentMethod = displayedSavedPaymentMethod,
                        savedPaymentMethodAction = savedPaymentMethodAction,
                        onViewMorePaymentMethods = onViewMorePaymentMethods,
                        onEditPaymentMethod = onEditPaymentMethod,
                        onManageOneSavedPaymentMethod = { onManageOneSavedPaymentMethod(displayedSavedPaymentMethod) },
                    )
                },
                onClick = { onSelectSavedPaymentMethod(displayedSavedPaymentMethod) },
                rowStyle = rowStyle
            )
        }

        OptionalEmbeddedDivider(rowStyle)

        val selectedIndex = remember(selection, paymentMethods) {
            if (selection == null || selection.isSaved) {
                -1
            } else {
                val code = selection.code()
                paymentMethods.indexOfFirst { it.code == code }
            }
        }

        paymentMethods.forEachIndexed { index, item ->
            NewPaymentMethodRowButton(
                isEnabled = isEnabled,
                isSelected = index == selectedIndex,
                displayablePaymentMethod = item,
                imageLoader = imageLoader,
                rowStyle = rowStyle
            )

            if (index != paymentMethods.lastIndex) {
                OptionalEmbeddedDivider(rowStyle)
            } else if (rowStyle.bottomSeparatorEnabled()) {
                OptionalEmbeddedDivider(rowStyle)
            }
        }
    }
}

@Composable
private fun SavedPaymentMethodTrailingContent(
    displayedSavedPaymentMethod: DisplayableSavedPaymentMethod,
    savedPaymentMethodAction: PaymentMethodVerticalLayoutInteractor.SavedPaymentMethodAction,
    onViewMorePaymentMethods: () -> Unit,
    onManageOneSavedPaymentMethod: () -> Unit,
    onEditPaymentMethod: (DisplayableSavedPaymentMethod) -> Unit,
) {
    when (savedPaymentMethodAction) {
        PaymentMethodVerticalLayoutInteractor.SavedPaymentMethodAction.NONE -> Unit
        PaymentMethodVerticalLayoutInteractor.SavedPaymentMethodAction.EDIT_CARD_BRAND -> {
            EditButton(onClick = { onEditPaymentMethod(displayedSavedPaymentMethod) })
        }
        PaymentMethodVerticalLayoutInteractor.SavedPaymentMethodAction.MANAGE_ONE -> {
            EditButton(onClick = onManageOneSavedPaymentMethod)
        }
        PaymentMethodVerticalLayoutInteractor.SavedPaymentMethodAction.MANAGE_ALL -> {
            ViewMoreButton(
                onViewMorePaymentMethods = onViewMorePaymentMethods
            )
        }
    }
}

@Composable
private fun EditButton(onClick: () -> Unit) {
    Text(
        stringResource(id = com.stripe.android.R.string.stripe_edit),
        color = MaterialTheme.colors.primary,
        style = MaterialTheme.typography.subtitle1,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .testTag(TEST_TAG_EDIT_SAVED_CARD)
            .clickable(onClick = onClick)
            .padding(4.dp)
    )
}

@Composable
private fun ViewMoreButton(
    onViewMorePaymentMethods: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .testTag(TEST_TAG_VIEW_MORE)
            .clickable(onClick = onViewMorePaymentMethods)
            .padding(4.dp)
    ) {
        Text(
            stringResource(id = R.string.stripe_view_more),
            color = MaterialTheme.colors.primary,
            style = MaterialTheme.typography.subtitle1,
            fontWeight = FontWeight.Medium,
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colors.primary,
        )
    }
}

@OptIn(ExperimentalEmbeddedPaymentElementApi::class)
@Composable
private fun OptionalEmbeddedDivider(rowStyle: Embedded.RowStyle) {
    if (rowStyle !is Embedded.RowStyle.FloatingButton) {
        val color = Color(rowStyle.separatorColor())
        val thickness = rowStyle.separatorThickness()
        val modifier = if (rowStyle is Embedded.RowStyle.FlatWithRadio) {
            Modifier.padding(start = rowStyle.separatorInsets() + 32.dp, end = rowStyle.separatorInsets())
        } else {
            Modifier.padding(horizontal = rowStyle.separatorInsets())
        }
        Divider(
            color = color,
            thickness = thickness,
            modifier = modifier
        )
    }
}

@OptIn(ExperimentalEmbeddedPaymentElementApi::class)
private fun Embedded.RowStyle.bottomSeparatorEnabled(): Boolean {
    return when (this) {
        is Embedded.RowStyle.FloatingButton -> false
        is Embedded.RowStyle.FlatWithRadio -> this.bottomSeparatorEnabled
        is Embedded.RowStyle.FlatWithCheckmark -> this.bottomSeparatorEnabled
    }
}

@OptIn(ExperimentalEmbeddedPaymentElementApi::class)
private fun Embedded.RowStyle.topSeparatorEnabled(): Boolean {
    return when (this) {
        is Embedded.RowStyle.FloatingButton -> false
        is Embedded.RowStyle.FlatWithRadio -> this.topSeparatorEnabled
        is Embedded.RowStyle.FlatWithCheckmark -> this.topSeparatorEnabled
    }
}

@OptIn(ExperimentalEmbeddedPaymentElementApi::class)
private fun Embedded.RowStyle.separatorThickness(): Dp {
    return when (this) {
        is Embedded.RowStyle.FloatingButton -> 0.dp
        is Embedded.RowStyle.FlatWithRadio -> this.separatorThicknessDp.dp
        is Embedded.RowStyle.FlatWithCheckmark -> this.separatorThicknessDp.dp
    }
}

@OptIn(ExperimentalEmbeddedPaymentElementApi::class)
private fun Embedded.RowStyle.separatorColor(): Int {
    return when (this) {
        is Embedded.RowStyle.FloatingButton -> 0
        is Embedded.RowStyle.FlatWithRadio -> this.separatorColor
        is Embedded.RowStyle.FlatWithCheckmark -> this.separatorColor
    }
}

@OptIn(ExperimentalEmbeddedPaymentElementApi::class)
private fun Embedded.RowStyle.separatorInsets(): Dp {
    return when (this) {
        is Embedded.RowStyle.FloatingButton -> 0.dp
        is Embedded.RowStyle.FlatWithRadio -> this.separatorInsetsDp.dp
        is Embedded.RowStyle.FlatWithCheckmark -> this.separatorInsetsDp.dp
    }
}
