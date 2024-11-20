@file:RestrictTo(RestrictTo.Scope.LIBRARY)

package com.stripe.android.paymentsheet.ui

import androidx.annotation.RestrictTo
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stripe.android.common.ui.PrimaryButton
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.CardBrand
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.ui.EditPaymentMethodViewAction.OnRemoveConfirmationDismissed
import com.stripe.android.paymentsheet.ui.EditPaymentMethodViewAction.OnRemoveConfirmed
import com.stripe.android.paymentsheet.ui.EditPaymentMethodViewAction.OnRemovePressed
import com.stripe.android.paymentsheet.ui.EditPaymentMethodViewAction.OnUpdatePressed
import com.stripe.android.ui.core.elements.SimpleDialogElementUI
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.elements.SectionCard
import com.stripe.android.uicore.elements.TextFieldColors
import com.stripe.android.uicore.strings.resolve
import com.stripe.android.uicore.stripeColors
import com.stripe.android.uicore.utils.collectAsState
import com.stripe.android.R as StripeR

@Composable
internal fun EditPaymentMethod(
    interactor: EditPaymentMethodViewInteractor,
    modifier: Modifier = Modifier
) {
    val viewState by interactor.viewState.collectAsState()

    EditPaymentMethodUi(
        modifier = modifier,
        viewState = viewState,
        viewActionHandler = interactor::handleViewAction
    )
}

@Composable
internal fun EditPaymentMethodUi(
    viewState: EditPaymentMethodViewState,
    viewActionHandler: (action: EditPaymentMethodViewAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val padding = dimensionResource(id = R.dimen.stripe_paymentsheet_outer_spacing_horizontal)

    val isIdle = viewState.status == EditPaymentMethodViewState.Status.Idle

    Column(
        modifier = modifier
            .padding(horizontal = padding)
            .testTag(TEST_TAG_PAYMENT_SHEET_EDIT_SCREEN)
    ) {
        SectionCard {
            val colors = TextFieldColors(false)

            TextField(
                modifier = Modifier.fillMaxWidth(),
                value = "•••• •••• •••• ${viewState.last4}",
                enabled = false,
                colors = colors,
                label = {
                    Label(
                        text = stringResource(id = StripeR.string.stripe_acc_label_card_number),
                        modifier = modifier
                    )
                },
                trailingIcon = {
                    Dropdown(
                        selectedBrand = viewState.selectedBrand,
                        availableBrands = viewState.availableBrands,
                        viewActionHandler
                    )
                },
                onValueChange = {}
            )
        }

        Spacer(modifier = Modifier.requiredHeight(32.dp))

        viewState.error?.let { resolvableError ->
            ErrorMessage(
                error = resolvableError.resolve(),
                modifier = Modifier.padding(
                    bottom = 8.dp
                ),
            )
        }

        PrimaryButton(
            label = stringResource(id = StripeR.string.stripe_update),
            isLoading = viewState.status == EditPaymentMethodViewState.Status.Updating,
            isEnabled = viewState.canUpdate && isIdle,
            onButtonClick = { viewActionHandler.invoke(OnUpdatePressed) },
            modifier = Modifier.testTag(TEST_TAG_EDIT_SCREEN_UPDATE_BUTTON)
        )

        if (viewState.canRemove) {
            RemoveButton(
                title = R.string.stripe_paymentsheet_remove_card.resolvableString,
                borderColor = Color.Transparent,
                idle = isIdle,
                removing = viewState.status == EditPaymentMethodViewState.Status.Removing,
                onRemove = { viewActionHandler(OnRemovePressed) },
                testTag = PAYMENT_SHEET_EDIT_SCREEN_REMOVE_BUTTON,
            )
        }
    }

    if (viewState.confirmRemoval) {
        val title = stringResource(
            R.string.stripe_paymentsheet_remove_pm,
            viewState.displayName.resolve(),
        )

        val message = stringResource(
            StripeR.string.stripe_card_ending_in,
            viewState.selectedBrand.brand.displayName,
            viewState.last4,
        )

        SimpleDialogElementUI(
            titleText = title,
            messageText = message,
            confirmText = stringResource(StripeR.string.stripe_remove),
            dismissText = stringResource(StripeR.string.stripe_cancel),
            destructive = true,
            onConfirmListener = { viewActionHandler(OnRemoveConfirmed) },
            onDismissListener = { viewActionHandler(OnRemoveConfirmationDismissed) },
        )
    }
}

@Composable
private fun Label(
    text: String,
    modifier: Modifier
) {
    Text(
        text = text,
        modifier = modifier,
        color = MaterialTheme.stripeColors.placeholderText.copy(alpha = ContentAlpha.disabled),
        style = MaterialTheme.typography.subtitle1
    )
}

@Composable
@Preview(showBackground = true)
private fun EditPaymentMethodPreview() {
    StripeTheme {
        EditPaymentMethodUi(
            viewState = EditPaymentMethodViewState(
                status = EditPaymentMethodViewState.Status.Idle,
                last4 = "4242",
                displayName = "Card".resolvableString,
                selectedBrand = EditPaymentMethodViewState.CardBrandChoice(
                    brand = CardBrand.CartesBancaires
                ),
                canUpdate = true,
                availableBrands = listOf(
                    EditPaymentMethodViewState.CardBrandChoice(
                        brand = CardBrand.Visa
                    ),
                    EditPaymentMethodViewState.CardBrandChoice(
                        brand = CardBrand.CartesBancaires
                    )
                ),
                canRemove = true,
            ),
            viewActionHandler = {}
        )
    }
}

internal const val PAYMENT_SHEET_EDIT_SCREEN_REMOVE_BUTTON = "PaymentSheetEditScreenRemoveButton"
internal const val TEST_TAG_PAYMENT_SHEET_EDIT_SCREEN = "TEST_TAG_PAYMENT_SHEET_EDIT_SCREEN"
internal const val TEST_TAG_EDIT_SCREEN_UPDATE_BUTTON = "TEST_TAG_EDIT_SCREEN_UPDATE_BUTTON"
