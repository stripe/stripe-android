package com.stripe.android.paymentsheet.verticalmode

import androidx.annotation.RestrictTo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.PaymentSheet.Appearance.Embedded
import com.stripe.android.paymentsheet.analytics.code
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.isSaved
import com.stripe.android.uicore.image.StripeImageLoader
import com.stripe.android.uicore.utils.collectAsState
import org.jetbrains.annotations.VisibleForTesting

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal const val TEST_TAG_PAYMENT_METHOD_EMBEDDED_LAYOUT = "TEST_TAG_PAYMENT_METHOD_EMBEDDED_LAYOUT"

@OptIn(ExperimentalEmbeddedPaymentElementApi::class)
@Composable
internal fun PaymentMethodEmbeddedLayoutUI(
    interactor: PaymentMethodVerticalLayoutInteractor,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val imageLoader = remember {
        StripeImageLoader(context.applicationContext)
    }

    val state by interactor.state.collectAsState()

    PaymentMethodEmbeddedLayoutUI(
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
            .testTag(TEST_TAG_PAYMENT_METHOD_EMBEDDED_LAYOUT),
        rowStyle = state.rowType
    )
}

@OptIn(ExperimentalEmbeddedPaymentElementApi::class)
@VisibleForTesting
@Composable
internal fun PaymentMethodEmbeddedLayoutUI(
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

@OptIn(ExperimentalEmbeddedPaymentElementApi::class)
@Composable
private fun OptionalEmbeddedDivider(rowStyle: Embedded.RowStyle) {
    if (rowStyle.hasSeparators()) {
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
        is Embedded.RowStyle.FlatWithRadio -> bottomSeparatorEnabled
        is Embedded.RowStyle.FlatWithCheckmark -> bottomSeparatorEnabled
    }
}

@OptIn(ExperimentalEmbeddedPaymentElementApi::class)
private fun Embedded.RowStyle.topSeparatorEnabled(): Boolean {
    return when (this) {
        is Embedded.RowStyle.FloatingButton -> false
        is Embedded.RowStyle.FlatWithRadio -> topSeparatorEnabled
        is Embedded.RowStyle.FlatWithCheckmark -> topSeparatorEnabled
    }
}

@OptIn(ExperimentalEmbeddedPaymentElementApi::class)
private fun Embedded.RowStyle.separatorThickness(): Dp {
    return when (this) {
        is Embedded.RowStyle.FloatingButton -> 0.dp
        is Embedded.RowStyle.FlatWithRadio -> separatorThicknessDp.dp
        is Embedded.RowStyle.FlatWithCheckmark -> separatorThicknessDp.dp
    }
}

@OptIn(ExperimentalEmbeddedPaymentElementApi::class)
private fun Embedded.RowStyle.separatorColor(): Int {
    return when (this) {
        is Embedded.RowStyle.FloatingButton -> 0
        is Embedded.RowStyle.FlatWithRadio -> separatorColor
        is Embedded.RowStyle.FlatWithCheckmark -> separatorColor
    }
}

@OptIn(ExperimentalEmbeddedPaymentElementApi::class)
private fun Embedded.RowStyle.separatorInsets(): Dp {
    return when (this) {
        is Embedded.RowStyle.FloatingButton -> 0.dp
        is Embedded.RowStyle.FlatWithRadio -> separatorInsetsDp.dp
        is Embedded.RowStyle.FlatWithCheckmark -> separatorInsetsDp.dp
    }
}
