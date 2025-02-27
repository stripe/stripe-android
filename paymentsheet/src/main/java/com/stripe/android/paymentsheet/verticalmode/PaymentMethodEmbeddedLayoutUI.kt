package com.stripe.android.paymentsheet.verticalmode

import androidx.annotation.RestrictTo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.PaymentSheet.Appearance.Embedded
import com.stripe.android.ui.core.elements.Mandate
import com.stripe.android.uicore.image.StripeImageLoader
import com.stripe.android.uicore.strings.resolve
import com.stripe.android.uicore.utils.collectAsState
import org.jetbrains.annotations.VisibleForTesting

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
const val TEST_TAG_PAYMENT_METHOD_EMBEDDED_LAYOUT = "TEST_TAG_PAYMENT_METHOD_EMBEDDED_LAYOUT"

internal const val EMBEDDED_MANDATE_TEXT_TEST_TAG = "EMBEDDED_MANDATE"

@OptIn(ExperimentalEmbeddedPaymentElementApi::class)
@Composable
internal fun ColumnScope.PaymentMethodEmbeddedLayoutUI(
    interactor: PaymentMethodVerticalLayoutInteractor,
    embeddedViewDisplaysMandateText: Boolean,
    modifier: Modifier = Modifier,
    rowStyle: Embedded.RowStyle
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
        rowStyle = rowStyle
    )

    EmbeddedMandate(
        embeddedViewDisplaysMandateText = embeddedViewDisplaysMandateText,
        mandate = state.mandate,
    )
}

@OptIn(ExperimentalEmbeddedPaymentElementApi::class)
@VisibleForTesting
@Composable
internal fun PaymentMethodEmbeddedLayoutUI(
    paymentMethods: List<DisplayablePaymentMethod>,
    displayedSavedPaymentMethod: DisplayableSavedPaymentMethod?,
    savedPaymentMethodAction: PaymentMethodVerticalLayoutInteractor.SavedPaymentMethodAction,
    selection: PaymentMethodVerticalLayoutInteractor.Selection?,
    isEnabled: Boolean,
    onViewMorePaymentMethods: () -> Unit,
    onManageOneSavedPaymentMethod: (DisplayableSavedPaymentMethod) -> Unit,
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
                        savedPaymentMethodAction = savedPaymentMethodAction,
                        onViewMorePaymentMethods = onViewMorePaymentMethods,
                        onManageOneSavedPaymentMethod = { onManageOneSavedPaymentMethod(displayedSavedPaymentMethod) },
                        addPaddingForCheckmarkRow = rowStyle.addPaddingForCheckmarkRowEditButton()
                    )
                },
                onClick = { onSelectSavedPaymentMethod(displayedSavedPaymentMethod) },
                rowStyle = rowStyle
            )

            if (paymentMethods.isNotEmpty()) OptionalEmbeddedDivider(rowStyle)
        }

        val selectedIndex = remember(selection, paymentMethods) {
            if (selection is PaymentMethodVerticalLayoutInteractor.Selection.New) {
                val code = selection.code
                paymentMethods.indexOfFirst { it.code == code }
            } else {
                -1
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

            if (index != paymentMethods.lastIndex) OptionalEmbeddedDivider(rowStyle)
        }

        if (rowStyle.bottomSeparatorEnabled()) OptionalEmbeddedDivider(rowStyle)
    }
}

@OptIn(ExperimentalEmbeddedPaymentElementApi::class)
@Composable
private fun OptionalEmbeddedDivider(rowStyle: Embedded.RowStyle) {
    if (rowStyle.hasSeparators()) {
        val color = Color(rowStyle.separatorColor())
        val thickness = rowStyle.separatorThickness()
        Divider(
            color = color,
            thickness = thickness,
            modifier = Modifier.padding(
                start = rowStyle.startSeparatorInset(),
                end = rowStyle.endSeparatorInset()
            ),
            startIndent = if (rowStyle.startSeparatorHasDefaultInset()) 32.dp else 0.dp
        )
    }
}

@Composable
private fun EmbeddedMandate(
    embeddedViewDisplaysMandateText: Boolean,
    mandate: ResolvableString?,
) {
    if (embeddedViewDisplaysMandateText) {
        Mandate(
            mandateText = mandate?.resolve(),
            modifier = Modifier
                .padding(bottom = 8.dp)
                .testTag(EMBEDDED_MANDATE_TEXT_TEST_TAG),
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
private fun Embedded.RowStyle.startSeparatorInset(): Dp {
    return when (this) {
        is Embedded.RowStyle.FloatingButton -> 0.dp
        is Embedded.RowStyle.FlatWithRadio -> startSeparatorInsetDp.dp
        is Embedded.RowStyle.FlatWithCheckmark -> startSeparatorInsetDp.dp
    }
}

@OptIn(ExperimentalEmbeddedPaymentElementApi::class)
private fun Embedded.RowStyle.endSeparatorInset(): Dp {
    return when (this) {
        is Embedded.RowStyle.FloatingButton -> 0.dp
        is Embedded.RowStyle.FlatWithRadio -> endSeparatorInsetDp.dp
        is Embedded.RowStyle.FlatWithCheckmark -> endSeparatorInsetDp.dp
    }
}

@OptIn(ExperimentalEmbeddedPaymentElementApi::class)
private fun Embedded.RowStyle.addPaddingForCheckmarkRowEditButton(): Boolean {
    return when (this) {
        is Embedded.RowStyle.FloatingButton,
        is Embedded.RowStyle.FlatWithRadio -> false
        is Embedded.RowStyle.FlatWithCheckmark -> true
    }
}
