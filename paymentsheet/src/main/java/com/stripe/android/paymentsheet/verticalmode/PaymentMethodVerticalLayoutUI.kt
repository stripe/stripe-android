package com.stripe.android.paymentsheet.verticalmode

import androidx.annotation.RestrictTo
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.uicore.image.StripeImageLoader
import com.stripe.android.uicore.stripeColors
import com.stripe.android.uicore.utils.collectAsState
import org.jetbrains.annotations.VisibleForTesting

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
const val TEST_TAG_PAYMENT_METHOD_VERTICAL_LAYOUT = "TEST_TAG_PAYMENT_METHOD_VERTICAL_LAYOUT"

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
const val TEST_TAG_VIEW_MORE = "TEST_TAG_VIEW_MORE"

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
const val TEST_TAG_EDIT_SAVED_CARD = "TEST_TAG_VERTICAL_MODE_SAVED_PM_EDIT"

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
const val TEST_TAG_SAVED_TEXT = "TEST_TAG_SAVED_TEXT"

@Composable
internal fun PaymentMethodVerticalLayoutUI(
    interactor: PaymentMethodVerticalLayoutInteractor,
    modifier: Modifier = Modifier
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
        selection = state.temporarySelection,
        isEnabled = !state.isProcessing,
        onViewMorePaymentMethods = {
            interactor.handleViewAction(
                PaymentMethodVerticalLayoutInteractor.ViewAction.TransitionToManageSavedPaymentMethods
            )
        },
        onSelectSavedPaymentMethod = {
            interactor.handleViewAction(
                PaymentMethodVerticalLayoutInteractor.ViewAction.SavedPaymentMethodSelected(
                    PaymentSelection.Saved(it.paymentMethod)
                )
            )
        },
        onManageOneSavedPaymentMethod = {
            interactor.handleViewAction(
                PaymentMethodVerticalLayoutInteractor.ViewAction.OnManageOneSavedPaymentMethod(it)
            )
        },
        imageLoader = imageLoader,
        modifier = modifier
            .testTag(TEST_TAG_PAYMENT_METHOD_VERTICAL_LAYOUT)
    )
}

@OptIn(ExperimentalEmbeddedPaymentElementApi::class)
@VisibleForTesting
@Composable
internal fun PaymentMethodVerticalLayoutUI(
    paymentMethods: List<DisplayablePaymentMethod>,
    displayedSavedPaymentMethod: DisplayableSavedPaymentMethod?,
    savedPaymentMethodAction: PaymentMethodVerticalLayoutInteractor.SavedPaymentMethodAction,
    selection: PaymentMethodVerticalLayoutInteractor.Selection?,
    isEnabled: Boolean,
    onViewMorePaymentMethods: () -> Unit,
    onManageOneSavedPaymentMethod: (DisplayableSavedPaymentMethod) -> Unit,
    onSelectSavedPaymentMethod: (DisplayableSavedPaymentMethod) -> Unit,
    imageLoader: StripeImageLoader,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        val textStyle = MaterialTheme.typography.subtitle1
        val textColor = MaterialTheme.stripeColors.onComponent

        if (displayedSavedPaymentMethod != null) {
            Text(
                text = stringResource(id = R.string.stripe_paymentsheet_saved),
                style = textStyle,
                color = textColor,
                modifier = Modifier.testTag(TEST_TAG_SAVED_TEXT),
            )
            Spacer(Modifier.size(16.dp))
            SavedPaymentMethodRowButton(
                displayableSavedPaymentMethod = displayedSavedPaymentMethod,
                isEnabled = isEnabled,
                isSelected = selection?.isSaved == true,
                onClick = { onSelectSavedPaymentMethod(displayedSavedPaymentMethod) },
                trailingContent = {
                    SavedPaymentMethodTrailingContent(
                        savedPaymentMethodAction = savedPaymentMethodAction,
                        onViewMorePaymentMethods = onViewMorePaymentMethods,
                        onManageOneSavedPaymentMethod = { onManageOneSavedPaymentMethod(displayedSavedPaymentMethod) },
                    )
                }
            )
            Spacer(Modifier.size(24.dp))
            Text(stringResource(id = R.string.stripe_paymentsheet_new_pm), style = textStyle, color = textColor)
            Spacer(Modifier.size(16.dp))
        }

        val selectedIndex = remember(selection, paymentMethods) {
            if (selection is PaymentMethodVerticalLayoutInteractor.Selection.New) {
                val code = selection.code
                paymentMethods.indexOfFirst { it.code == code }
            } else {
                -1
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

@Composable
internal fun SavedPaymentMethodTrailingContent(
    savedPaymentMethodAction: PaymentMethodVerticalLayoutInteractor.SavedPaymentMethodAction,
    onViewMorePaymentMethods: () -> Unit,
    onManageOneSavedPaymentMethod: () -> Unit,
) {
    when (savedPaymentMethodAction) {
        PaymentMethodVerticalLayoutInteractor.SavedPaymentMethodAction.NONE -> Unit
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
            .padding(vertical = 4.dp)
            .fillMaxHeight()
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
            .padding(vertical = 4.dp)
            .fillMaxHeight()
    ) {
        Text(
            stringResource(id = R.string.stripe_view_more),
            color = MaterialTheme.colors.primary,
            style = MaterialTheme.typography.subtitle1,
            fontWeight = FontWeight.Medium,
        )
        Icon(
            painter = painterResource(R.drawable.stripe_ic_chevron_right),
            contentDescription = null,
            tint = MaterialTheme.colors.primary,
            modifier = Modifier.padding(start = 4.dp, top = 2.dp)
        )
    }
}
