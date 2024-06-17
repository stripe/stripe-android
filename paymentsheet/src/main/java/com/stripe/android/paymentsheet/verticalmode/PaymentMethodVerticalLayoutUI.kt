package com.stripe.android.paymentsheet.verticalmode

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.analytics.code
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.isSaved
import com.stripe.android.uicore.image.StripeImageLoader
import com.stripe.android.uicore.stripeColors
import com.stripe.android.uicore.utils.collectAsState
import org.jetbrains.annotations.VisibleForTesting

internal const val TEST_TAG_VIEW_MORE = "TEST_TAG_VIEW_MORE"

@Composable
internal fun PaymentMethodVerticalLayoutUI(interactor: PaymentMethodVerticalLayoutInteractor) {
    val context = LocalContext.current
    val imageLoader = remember {
        StripeImageLoader(context.applicationContext)
    }

    val state by interactor.state.collectAsState()

    PaymentMethodVerticalLayoutUI(
        paymentMethods = state.displayablePaymentMethods,
        displayedSavedPaymentMethod = state.displayedSavedPaymentMethod,
        selection = state.selection,
        isEnabled = !state.isProcessing,
        onViewMorePaymentMethods = {
            interactor.handleViewAction(
                PaymentMethodVerticalLayoutInteractor.ViewAction.TransitionToManageSavedPaymentMethods
            )
        },
        onSelectSavedPaymentMethod = {
            interactor.handleViewAction(
                PaymentMethodVerticalLayoutInteractor.ViewAction.SavedPaymentMethodSelected(it)
            )
        },
        imageLoader = imageLoader,
        modifier = Modifier.padding(horizontal = 20.dp)
    )
}

@VisibleForTesting
@Composable
internal fun PaymentMethodVerticalLayoutUI(
    paymentMethods: List<DisplayablePaymentMethod>,
    displayedSavedPaymentMethod: DisplayableSavedPaymentMethod?,
    selection: PaymentSelection?,
    isEnabled: Boolean,
    onViewMorePaymentMethods: () -> Unit,
    onSelectSavedPaymentMethod: (DisplayableSavedPaymentMethod) -> Unit,
    imageLoader: StripeImageLoader,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val textStyle = MaterialTheme.typography.subtitle1
        val textColor = MaterialTheme.stripeColors.onComponent

        if (displayedSavedPaymentMethod != null) {
            Text(stringResource(id = R.string.stripe_paymentsheet_saved), style = textStyle, color = textColor)
            SavedPaymentMethodRowButton(
                displayableSavedPaymentMethod = displayedSavedPaymentMethod,
                resources = LocalContext.current.resources,
                isEnabled = isEnabled,
                isSelected = selection?.isSaved == true,
                trailingContent = {
                    ViewMoreButton(onViewMorePaymentMethods = onViewMorePaymentMethods)
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
            imageLoader = imageLoader
        )
    }
}

@Composable
private fun ViewMoreButton(
    onViewMorePaymentMethods: () -> Unit,
) {
    TextButton(
        onClick = onViewMorePaymentMethods,
        modifier = Modifier.testTag(TEST_TAG_VIEW_MORE),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(id = R.string.stripe_view_more),
                color = MaterialTheme.colors.primary,
                style = MaterialTheme.typography.button
            )
            Icon(
                imageVector = Icons.Filled.KeyboardArrowRight,
                contentDescription = null,
            )
        }
    }
}
