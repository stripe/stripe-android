@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package com.stripe.android.paymentsheet.ui

import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.PaymentOptionsItem
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.key
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen.SelectSavedPaymentMethods.CvcRecollectionState
import com.stripe.android.paymentsheet.toPaymentSelection
import com.stripe.android.ui.core.elements.CvcController
import com.stripe.android.ui.core.elements.CvcElement
import com.stripe.android.uicore.DefaultStripeTheme
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.SectionCard
import com.stripe.android.uicore.elements.SectionError
import com.stripe.android.uicore.shouldUseDarkDynamicColor
import com.stripe.android.uicore.strings.resolve
import com.stripe.android.uicore.stripeColors
import com.stripe.android.uicore.utils.collectAsState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import com.stripe.android.R as StripeR

@Composable
internal fun SavedPaymentMethodTabLayoutUI(
    interactor: SelectSavedPaymentMethodsInteractor,
    cvcRecollectionState: CvcRecollectionState,
    modifier: Modifier,
) {
    val state by interactor.state.collectAsState()

    SavedPaymentMethodTabLayoutUI(
        paymentOptionsItems = state.paymentOptionsItems,
        selectedPaymentOptionsItem = state.selectedPaymentOptionsItem,
        isEditing = state.isEditing,
        isProcessing = state.isProcessing,
        onAddCardPressed = {
            interactor.handleViewAction(
                SelectSavedPaymentMethodsInteractor.ViewAction.AddCardPressed
            )
        },
        onItemSelected = {
            interactor.handleViewAction(
                SelectSavedPaymentMethodsInteractor.ViewAction.SelectPaymentMethod(
                    it
                )
            )
        },
        onModifyItem = {
            interactor.handleViewAction(
                SelectSavedPaymentMethodsInteractor.ViewAction.EditPaymentMethod(it)
            )
        },
        modifier = modifier,
    )

    if (
        cvcRecollectionState is CvcRecollectionState.Required &&
        (state.selectedPaymentOptionsItem as? PaymentOptionsItem.SavedPaymentMethod)
            ?.paymentMethod?.type == PaymentMethod.Type.Card
    ) {
        CvcRecollectionField(
            cvcControllerFlow = cvcRecollectionState.cvcControllerFlow,
            state.isProcessing
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
internal fun SavedPaymentMethodTabLayoutUI(
    paymentOptionsItems: List<PaymentOptionsItem>,
    selectedPaymentOptionsItem: PaymentOptionsItem?,
    isEditing: Boolean,
    isProcessing: Boolean,
    onAddCardPressed: () -> Unit,
    onItemSelected: (PaymentSelection?) -> Unit,
    onModifyItem: (DisplayableSavedPaymentMethod) -> Unit,
    modifier: Modifier = Modifier,
    scrollState: LazyListState = rememberLazyListState(),
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(isEditing) {
        if (isEditing) {
            focusRequester.requestFocus()
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .testTag(SAVED_PAYMENT_OPTION_TAB_LAYOUT_TEST_TAG)
            .focusRequester(focusRequester)
    ) {
        val width = rememberItemWidth(maxWidth)

        LazyRow(
            state = scrollState,
            userScrollEnabled = !isProcessing,
            contentPadding = PaddingValues(horizontal = 17.dp),
        ) {
            items(
                items = paymentOptionsItems,
                key = { it.key },
            ) { item ->
                val isEnabled =
                    !isProcessing && (!isEditing || item.isEnabledDuringEditing)
                val isSelected = item == selectedPaymentOptionsItem && !isEditing

                SavedPaymentMethodTab(
                    item = item,
                    width = width,
                    isEditing = isEditing,
                    isEnabled = isEnabled,
                    isSelected = isSelected,
                    onAddCardPressed = onAddCardPressed,
                    onItemSelected = onItemSelected,
                    onModifyItem = onModifyItem,
                    modifier = Modifier
                        .semantics { testTagsAsResourceId = true }
                        .testTag(item.viewType.name)
                        .animateItemPlacement(),
                )
            }
        }
    }
}

private val PREVIEW_PAYMENT_OPTION_ITEMS = listOf(
    PaymentOptionsItem.AddCard,
    PaymentOptionsItem.Link,
    PaymentOptionsItem.GooglePay,
    PaymentOptionsItem.SavedPaymentMethod(
        DisplayableSavedPaymentMethod.create(
            displayName = "4242".resolvableString,
            paymentMethod = PaymentMethod(
                id = "001",
                created = null,
                liveMode = false,
                code = PaymentMethod.Type.Card.code,
                type = PaymentMethod.Type.Card,
                card = PaymentMethod.Card(
                    brand = CardBrand.Visa,
                    last4 = "4242",
                )
            ),
            shouldShowDefaultBadge = true
        ),
    ),
    PaymentOptionsItem.SavedPaymentMethod(
        DisplayableSavedPaymentMethod.create(
            displayName = "4242".resolvableString,
            paymentMethod = PaymentMethod(
                id = "002",
                created = null,
                liveMode = false,
                code = PaymentMethod.Type.SepaDebit.code,
                type = PaymentMethod.Type.SepaDebit,
            )
        ),
    ),
    PaymentOptionsItem.SavedPaymentMethod(
        DisplayableSavedPaymentMethod.create(
            displayName = "5555".resolvableString,
            paymentMethod = PaymentMethod(
                id = "003",
                created = null,
                liveMode = false,
                code = PaymentMethod.Type.Card.code,
                type = PaymentMethod.Type.Card,
                card = PaymentMethod.Card(
                    brand = CardBrand.MasterCard,
                    last4 = "4242",
                )
            )
        ),
    ),
)

@Preview(widthDp = 700)
@Composable
private fun SavedPaymentMethodsTabLayoutPreview() {
    DefaultStripeTheme {
        SavedPaymentMethodTabLayoutUI(
            paymentOptionsItems = PREVIEW_PAYMENT_OPTION_ITEMS,
            selectedPaymentOptionsItem = PaymentOptionsItem.AddCard,
            isEditing = false,
            isProcessing = false,
            onAddCardPressed = { },
            onItemSelected = { },
            onModifyItem = { },
        )
    }
}

@Preview(widthDp = 700)
@Composable
private fun SavedPaymentMethodsTabLayoutWithDefaultPreview() {
    DefaultStripeTheme {
        SavedPaymentMethodTabLayoutUI(
            paymentOptionsItems = PREVIEW_PAYMENT_OPTION_ITEMS,
            selectedPaymentOptionsItem = PaymentOptionsItem.AddCard,
            isEditing = true,
            isProcessing = false,
            onAddCardPressed = { },
            onItemSelected = { },
            onModifyItem = { },
        )
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
private fun SavedPaymentMethodTab(
    item: PaymentOptionsItem,
    width: Dp,
    isEnabled: Boolean,
    isEditing: Boolean,
    isSelected: Boolean,
    onAddCardPressed: () -> Unit,
    onItemSelected: (PaymentSelection?) -> Unit,
    onModifyItem: (DisplayableSavedPaymentMethod) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (item) {
        is PaymentOptionsItem.AddCard -> {
            AddCardTab(
                width = width,
                isEnabled = isEnabled,
                onAddCardPressed = onAddCardPressed,
                modifier = modifier,
            )
        }
        is PaymentOptionsItem.GooglePay -> {
            GooglePayTab(
                width = width,
                isEnabled = isEnabled,
                isSelected = isSelected,
                onItemSelected = onItemSelected,
                modifier = modifier,
            )
        }
        is PaymentOptionsItem.Link -> {
            LinkTab(
                width = width,
                isEnabled = isEnabled,
                isSelected = isSelected,
                onItemSelected = onItemSelected,
                modifier = modifier,
            )
        }
        is PaymentOptionsItem.SavedPaymentMethod -> {
            SavedPaymentMethodTab(
                paymentMethod = item,
                width = width,
                isEnabled = isEnabled,
                isEditing = isEditing,
                isSelected = isSelected,
                onItemSelected = onItemSelected,
                onModifyItem = onModifyItem,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun AddCardTab(
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

    SavedPaymentMethodTab(
        viewWidth = width,
        shouldShowModifyBadge = false,
        shouldShowDefaultBadge = false,
        isSelected = false,
        labelText = stringResource(R.string.stripe_paymentsheet_add_payment_method_button_label),
        isEnabled = isEnabled,
        iconRes = iconRes,
        onItemSelectedListener = onAddCardPressed,
        description = stringResource(R.string.stripe_add_new_payment_method),
        modifier = modifier,
    )
}

@Composable
private fun GooglePayTab(
    width: Dp,
    isEnabled: Boolean,
    isSelected: Boolean,
    onItemSelected: (PaymentSelection?) -> Unit,
    modifier: Modifier = Modifier,
) {
    SavedPaymentMethodTab(
        viewWidth = width,
        shouldShowModifyBadge = false,
        shouldShowDefaultBadge = false,
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
private fun LinkTab(
    width: Dp,
    isEnabled: Boolean,
    isSelected: Boolean,
    onItemSelected: (PaymentSelection?) -> Unit,
    modifier: Modifier = Modifier,
) {
    SavedPaymentMethodTab(
        viewWidth = width,
        shouldShowModifyBadge = false,
        shouldShowDefaultBadge = false,
        isSelected = isSelected,
        isEnabled = isEnabled,
        iconRes = getLinkIcon(showNightIcon = !MaterialTheme.stripeColors.component.shouldUseDarkDynamicColor()),
        iconTint = null,
        labelText = stringResource(StripeR.string.stripe_link),
        description = stringResource(StripeR.string.stripe_link),
        onItemSelectedListener = { onItemSelected(PaymentSelection.Link()) },
        modifier = modifier,
    )
}

@Composable
private fun SavedPaymentMethodTab(
    paymentMethod: PaymentOptionsItem.SavedPaymentMethod,
    width: Dp,
    isEnabled: Boolean,
    isEditing: Boolean,
    isSelected: Boolean,
    onItemSelected: (PaymentSelection?) -> Unit,
    onModifyItem: (DisplayableSavedPaymentMethod) -> Unit,
    modifier: Modifier = Modifier,
) {
    val labelIcon = paymentMethod.paymentMethod.getLabelIcon()
    val labelText = paymentMethod.paymentMethod.getLabel()?.resolve() ?: return

    Box(
        modifier = Modifier.semantics {
            testTag = SAVED_PAYMENT_OPTION_TEST_TAG
            selected = isSelected
            text = AnnotatedString(labelText)

            if (!isEnabled) {
                disabled()
            }
        }
    ) {
        SavedPaymentMethodTab(
            viewWidth = width,
            shouldShowModifyBadge = isEnabled && isEditing,
            shouldShowDefaultBadge = paymentMethod.displayableSavedPaymentMethod.shouldShowDefaultBadge && isEditing,
            isSelected = isSelected,
            isEnabled = isEnabled,
            isClickable = !isEditing,
            iconRes = paymentMethod.paymentMethod.getSavedPaymentMethodIcon(),
            labelIcon = labelIcon,
            labelText = labelText,
            description = paymentMethod
                .displayableSavedPaymentMethod
                .getDescription()
                .resolve()
                .readNumbersAsIndividualDigits(),
            onModifyListener = { onModifyItem(paymentMethod.displayableSavedPaymentMethod) },
            onModifyAccessibilityDescription = paymentMethod
                .displayableSavedPaymentMethod
                .getModifyDescription()
                .resolve()
                .readNumbersAsIndividualDigits(),
            onItemSelectedListener = {
                onItemSelected(paymentMethod.toPaymentSelection())
            },
            modifier = modifier,
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun CvcRecollectionField(
    cvcControllerFlow: StateFlow<CvcController>,
    isProcessing: Boolean,
    animationDuration: Int = ANIMATION_DURATION,
    animationDelay: Int = ANIMATION_DELAY
) {
    val controller by cvcControllerFlow.collectAsState()
    val error = controller.error.collectAsState()
    val element = CvcElement(
        IdentifierSpec(),
        controller
    )
    val focusRequester = remember { FocusRequester() }
    var visible by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    LaunchedEffect(isProcessing) {
        // Clear focus once primary button is clicked
        if (isProcessing) {
            focusManager.clearFocus()
        }
    }

    LaunchedEffect(key1 = Unit) {
        delay(animationDelay.toLong())
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(tween(animationDuration, animationDelay)) {
            it
        }
    ) {
        Column(
            Modifier.padding(20.dp, 20.dp, 20.dp, 0.dp)
        ) {
            Text(
                text = stringResource(R.string.stripe_paymentsheet_confirm_your_cvc),
                style = MaterialTheme.typography.body1,
                color = MaterialTheme.stripeColors.subtitle
            )
            SectionCard(
                Modifier
                    .padding(0.dp, 8.dp, 0.dp, 8.dp)
                    .height(IntrinsicSize.Min)
            ) {
                element.controller.ComposeUI(
                    enabled = !isProcessing,
                    field = element,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    hiddenIdentifiers = setOf(),
                    lastTextFieldIdentifier = null,
                    nextFocusDirection = FocusDirection.Exit,
                    previousFocusDirection = FocusDirection.Previous
                )
            }
            error.value?.errorMessage?.let {
                Row {
                    SectionError(error = stringResource(id = it))
                }
            }
        }
    }
}

internal const val SAVED_PAYMENT_OPTION_TAB_LAYOUT_TEST_TAG = "PaymentSheetSavedPaymentOptionTabLayout"

@VisibleForTesting
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
const val SAVED_PAYMENT_OPTION_TEST_TAG = "PaymentSheetSavedPaymentOption"
private const val ANIMATION_DELAY = 400
private const val ANIMATION_DURATION = 500
