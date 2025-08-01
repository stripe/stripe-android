package com.stripe.android.paymentsheet.verticalmode

import androidx.annotation.RestrictTo
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.PaymentSheet.Appearance.Embedded
import com.stripe.android.paymentsheet.PaymentSheet.Appearance.Embedded.RowStyle
import com.stripe.android.paymentsheet.R
import com.stripe.android.ui.core.elements.Mandate
import com.stripe.android.uicore.image.StripeImageLoader
import com.stripe.android.uicore.strings.resolve
import com.stripe.android.uicore.utils.collectAsState
import org.jetbrains.annotations.VisibleForTesting

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
const val TEST_TAG_PAYMENT_METHOD_EMBEDDED_LAYOUT = "TEST_TAG_PAYMENT_METHOD_EMBEDDED_LAYOUT"

internal const val EMBEDDED_MANDATE_TEXT_TEST_TAG = "EMBEDDED_MANDATE"

@Composable
internal fun ColumnScope.PaymentMethodEmbeddedLayoutUI(
    interactor: PaymentMethodVerticalLayoutInteractor,
    embeddedViewDisplaysMandateText: Boolean,
    modifier: Modifier = Modifier,
    appearance: Embedded
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
        appearance = appearance
    )

    EmbeddedMandate(
        embeddedViewDisplaysMandateText = embeddedViewDisplaysMandateText,
        mandate = state.mandate,
    )
}

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
    appearance: Embedded,
    modifier: Modifier = Modifier,
) {
    val arrangement = if (appearance.style is RowStyle.FloatingButton) {
        Arrangement.spacedBy(appearance.style.spacingDp.dp)
    } else {
        Arrangement.Top
    }
    Column(modifier = modifier, verticalArrangement = arrangement) {
        if (appearance.style.topSeparatorEnabled()) OptionalEmbeddedDivider(appearance.style)

        EmbeddedSavedPaymentMethodRowButton(
            paymentMethods = paymentMethods,
            displayedSavedPaymentMethod = displayedSavedPaymentMethod,
            savedPaymentMethodAction = savedPaymentMethodAction,
            selection = selection,
            isEnabled = isEnabled,
            onViewMorePaymentMethods = onViewMorePaymentMethods,
            onManageOneSavedPaymentMethod = onManageOneSavedPaymentMethod,
            onSelectSavedPaymentMethod = onSelectSavedPaymentMethod,
            appearance = appearance
        )

        EmbeddedNewPaymentMethodRowButtonsLayoutUi(
            paymentMethods = paymentMethods,
            selection = selection,
            isEnabled = isEnabled,
            imageLoader = imageLoader,
            appearance = appearance,
        )

        if (appearance.style.bottomSeparatorEnabled()) OptionalEmbeddedDivider(appearance.style)
    }
}

@Composable
private fun OptionalEmbeddedDivider(rowStyle: Embedded.RowStyle) {
    if (rowStyle.hasSeparators()) {
        val color = Color(rowStyle.separatorColor(isSystemInDarkTheme()))
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
                .padding(top = 8.dp)
                .testTag(EMBEDDED_MANDATE_TEXT_TEST_TAG),
        )
    }
}

private fun Embedded.RowStyle.bottomSeparatorEnabled(): Boolean {
    return when (this) {
        is Embedded.RowStyle.FloatingButton -> false
        is Embedded.RowStyle.FlatWithRadio -> bottomSeparatorEnabled
        is Embedded.RowStyle.FlatWithCheckmark -> bottomSeparatorEnabled
        is Embedded.RowStyle.FlatWithDisclosure -> bottomSeparatorEnabled
    }
}

private fun Embedded.RowStyle.topSeparatorEnabled(): Boolean {
    return when (this) {
        is Embedded.RowStyle.FloatingButton -> false
        is Embedded.RowStyle.FlatWithRadio -> topSeparatorEnabled
        is Embedded.RowStyle.FlatWithCheckmark -> topSeparatorEnabled
        is Embedded.RowStyle.FlatWithDisclosure -> topSeparatorEnabled
    }
}

private fun Embedded.RowStyle.separatorThickness(): Dp {
    return when (this) {
        is Embedded.RowStyle.FloatingButton -> 0.dp
        is Embedded.RowStyle.FlatWithRadio -> separatorThicknessDp.dp
        is Embedded.RowStyle.FlatWithCheckmark -> separatorThicknessDp.dp
        is Embedded.RowStyle.FlatWithDisclosure -> separatorThicknessDp.dp
    }
}

private fun Embedded.RowStyle.separatorColor(isDarkMode: Boolean): Int {
    return when (this) {
        is Embedded.RowStyle.FloatingButton -> 0
        is Embedded.RowStyle.FlatWithRadio -> getColors(isDarkMode).separatorColor
        is Embedded.RowStyle.FlatWithCheckmark -> getColors(isDarkMode).separatorColor
        is Embedded.RowStyle.FlatWithDisclosure -> getColors(isDarkMode).separatorColor
    }
}

private fun Embedded.RowStyle.startSeparatorInset(): Dp {
    return when (this) {
        is Embedded.RowStyle.FloatingButton -> 0.dp
        is Embedded.RowStyle.FlatWithRadio -> startSeparatorInsetDp.dp
        is Embedded.RowStyle.FlatWithCheckmark -> startSeparatorInsetDp.dp
        is Embedded.RowStyle.FlatWithDisclosure -> startSeparatorInsetDp.dp
    }
}

private fun Embedded.RowStyle.endSeparatorInset(): Dp {
    return when (this) {
        is Embedded.RowStyle.FloatingButton -> 0.dp
        is Embedded.RowStyle.FlatWithRadio -> endSeparatorInsetDp.dp
        is Embedded.RowStyle.FlatWithCheckmark -> endSeparatorInsetDp.dp
        is Embedded.RowStyle.FlatWithDisclosure -> endSeparatorInsetDp.dp
    }
}

@Composable
internal fun EmbeddedSavedPaymentMethodRowButton(
    paymentMethods: List<DisplayablePaymentMethod>,
    displayedSavedPaymentMethod: DisplayableSavedPaymentMethod?,
    savedPaymentMethodAction: PaymentMethodVerticalLayoutInteractor.SavedPaymentMethodAction,
    selection: PaymentMethodVerticalLayoutInteractor.Selection?,
    isEnabled: Boolean,
    onViewMorePaymentMethods: () -> Unit,
    onManageOneSavedPaymentMethod: (DisplayableSavedPaymentMethod) -> Unit,
    onSelectSavedPaymentMethod: (DisplayableSavedPaymentMethod) -> Unit,
    appearance: Embedded,
) {
    if (displayedSavedPaymentMethod != null) {
        SavedPaymentMethodRowButton(
            displayableSavedPaymentMethod = displayedSavedPaymentMethod,
            isEnabled = isEnabled,
            isSelected = selection?.isSaved == true,
            trailingContent = {
                SavedPaymentMethodTrailingContent(
                    viewMoreShowChevron = appearance.style.viewMoreShowsChevron,
                    savedPaymentMethodAction = savedPaymentMethodAction,
                    onViewMorePaymentMethods = onViewMorePaymentMethods,
                    onManageOneSavedPaymentMethod = { onManageOneSavedPaymentMethod(displayedSavedPaymentMethod) },
                )
            },
            onClick = { onSelectSavedPaymentMethod(displayedSavedPaymentMethod) },
            appearance = appearance
        )

        if (paymentMethods.isNotEmpty()) OptionalEmbeddedDivider(appearance.style)
    }
}

@Composable
internal fun EmbeddedNewPaymentMethodRowButtonsLayoutUi(
    paymentMethods: List<DisplayablePaymentMethod>,
    selection: PaymentMethodVerticalLayoutInteractor.Selection?,
    isEnabled: Boolean,
    imageLoader: StripeImageLoader,
    appearance: Embedded,
) {
    val selectedIndex = remember(selection, paymentMethods) {
        if (selection is PaymentMethodVerticalLayoutInteractor.Selection.New) {
            val code = selection.code
            paymentMethods.indexOfFirst { it.code == code }
        } else {
            -1
        }
    }

    paymentMethods.forEachIndexed { index, item ->
        val isSelected = index == selectedIndex

        if (isSelected && selection is PaymentMethodVerticalLayoutInteractor.Selection.New && selection.canBeChanged) {
            val displayablePaymentMethod = item.copy(
                subtitle = selection.changeDetails?.resolvableString ?: item.subtitle
            )
            NewPaymentMethodRowButton(
                isEnabled = isEnabled,
                isSelected = true,
                displayablePaymentMethod = displayablePaymentMethod,
                imageLoader = imageLoader,
                appearance = appearance,
                trailingContent = {
                    EmbeddedNewPaymentMethodTrailingContent(
                        showChevron = appearance.style !is RowStyle.FlatWithCheckmark &&
                            appearance.style !is RowStyle.FlatWithDisclosure,
                    )
                }
            )
        } else {
            NewPaymentMethodRowButton(
                isEnabled = isEnabled,
                isSelected = isSelected,
                displayablePaymentMethod = item,
                imageLoader = imageLoader,
                appearance = appearance
            )
        }

        if (index != paymentMethods.lastIndex) OptionalEmbeddedDivider(appearance.style)
    }
}

private val RowStyle.viewMoreShowsChevron: Boolean
    get() = when (this) {
        is RowStyle.FloatingButton -> true
        is RowStyle.FlatWithRadio -> true
        is RowStyle.FlatWithCheckmark -> false
        is RowStyle.FlatWithDisclosure -> false
    }

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
const val TEST_TAG_CHANGE = "TEST_TAG_CHANGE"

@Composable
internal fun EmbeddedNewPaymentMethodTrailingContent(
    showChevron: Boolean,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .testTag(TEST_TAG_CHANGE)
            .padding(vertical = 4.dp)
            .wrapContentHeight()
    ) {
        Text(
            stringResource(id = com.stripe.android.uicore.R.string.stripe_change),
            color = MaterialTheme.colors.primary,
            style = MaterialTheme.typography.subtitle1,
            fontWeight = FontWeight.Medium,
        )
        if (showChevron) {
            Icon(
                painter = painterResource(R.drawable.stripe_ic_chevron_right),
                contentDescription = null,
                tint = MaterialTheme.colors.primary,
                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
            )
        }
    }
}
