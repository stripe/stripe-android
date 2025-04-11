package com.stripe.android.paymentsheet.ui

import android.content.res.Resources
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.stripe.android.R
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.uicore.elements.Section
import com.stripe.android.uicore.strings.resolve
import com.stripe.android.uicore.stripeColors
import com.stripe.android.uicore.stripeShapes
import com.stripe.android.uicore.utils.collectAsState
import com.stripe.android.ui.core.R as CoreR

@Composable
internal fun CardDetailsEditUI(
    editCardDetailsInteractor: EditCardDetailsInteractor,
) {
    val state by editCardDetailsInteractor.state.collectAsState()

    CardDetailsEditUI(
        shouldShowCardBrandDropdown = state.shouldShowCardBrandDropdown,
        selectedBrand = state.selectedCardBrand,
        card = state.card,
        availableNetworks = state.availableNetworks,
        paymentMethodIcon = state.paymentMethodIcon,
        expiryDateState = state.expiryDateState,
        billingDetailsForm = state.billingDetailsForm,
        onBrandChoiceChanged = {
            editCardDetailsInteractor.handleViewAction(EditCardDetailsInteractor.ViewAction.BrandChoiceChanged(it))
        },
        onExpDateChanged = {
            editCardDetailsInteractor.handleViewAction(EditCardDetailsInteractor.ViewAction.DateChanged(it))
        },
        onAddressChanged = {
            editCardDetailsInteractor.handleViewAction(EditCardDetailsInteractor.ViewAction.BillingDetailsChanged(it))
        }
    )
}

@Composable
private fun CardDetailsEditUI(
    shouldShowCardBrandDropdown: Boolean,
    selectedBrand: CardBrandChoice,
    card: PaymentMethod.Card,
    expiryDateState: ExpiryDateState,
    billingDetailsForm: BillingDetailsForm?,
    availableNetworks: List<CardBrandChoice>,
    @DrawableRes paymentMethodIcon: Int,
    onBrandChoiceChanged: (CardBrandChoice) -> Unit,
    onExpDateChanged: (String) -> Unit,
    onAddressChanged: (BillingDetailsFormState) -> Unit
) {
    val dividerHeight = remember { mutableStateOf(0.dp) }

    Column {
        Section(
            title = billingDetailsForm?.let {
                CoreR.string.stripe_paymentsheet_add_payment_method_card_information
            },
            error = expiryDateState.sectionError()?.resolve(),
            modifier = Modifier.testTag(UPDATE_PM_CARD_TEST_TAG),
        ) {
            Column {
                CardNumberField(
                    card = card,
                    selectedBrand = selectedBrand,
                    shouldShowCardBrandDropdown = shouldShowCardBrandDropdown,
                    availableNetworks = availableNetworks,
                    savedPaymentMethodIcon = paymentMethodIcon,
                    onBrandChoiceChanged = onBrandChoiceChanged,
                )
                Divider(
                    color = MaterialTheme.stripeColors.componentDivider,
                    thickness = MaterialTheme.stripeShapes.borderStrokeWidth.dp,
                )
                Row(modifier = Modifier.fillMaxWidth()) {
                    ExpiryField(
                        modifier = Modifier
                            .weight(1F)
                            .onSizeChanged {
                                dividerHeight.value =
                                    (it.height / Resources.getSystem().displayMetrics.density).dp
                            },
                        state = expiryDateState,
                        onValueChanged = onExpDateChanged
                    )
                    Divider(
                        modifier = Modifier
                            .height(dividerHeight.value)
                            .width(MaterialTheme.stripeShapes.borderStrokeWidth.dp),
                        color = MaterialTheme.stripeColors.componentDivider,
                    )
                    CvcField(cardBrand = card.brand, modifier = Modifier.weight(1F))
                }
            }
        }

        if (billingDetailsForm != null) {
            Spacer(Modifier.height(32.dp))

            BillingDetailsFormUI(
                form = billingDetailsForm,
                onValuesChanged = onAddressChanged
            )
        }
    }
}

@Composable
private fun CardNumberField(
    card: PaymentMethod.Card,
    selectedBrand: CardBrandChoice,
    availableNetworks: List<CardBrandChoice>,
    shouldShowCardBrandDropdown: Boolean,
    savedPaymentMethodIcon: Int,
    onBrandChoiceChanged: (CardBrandChoice) -> Unit,
) {
    CommonTextField(
        value = "•••• •••• •••• ${card.last4}",
        label = stringResource(id = R.string.stripe_acc_label_card_number),
        trailingIcon = {
            if (shouldShowCardBrandDropdown) {
                CardBrandDropdown(
                    selectedBrand = selectedBrand,
                    availableBrands = availableNetworks,
                    onBrandChoiceChanged = onBrandChoiceChanged,
                )
            } else {
                PaymentMethodIconFromResource(
                    iconRes = savedPaymentMethodIcon,
                    colorFilter = null,
                    alignment = Alignment.Center,
                    modifier = Modifier,
                )
            }
        },
    )
}

@Composable
private fun ExpiryField(
    modifier: Modifier,
    state: ExpiryDateState,
    onValueChanged: (String) -> Unit
) {
    ExpiryTextField(
        modifier = modifier
            .testTag(UPDATE_PM_EXPIRY_FIELD_TEST_TAG),
        state = state,
        onValueChange = onValueChanged,
    )
}

@Composable
private fun CvcField(cardBrand: CardBrand, modifier: Modifier) {
    val cvc = buildString {
        repeat(cardBrand.maxCvcLength) {
            append("•")
        }
    }
    CommonTextField(
        modifier = modifier.testTag(UPDATE_PM_CVC_FIELD_TEST_TAG),
        value = cvc,
        label = stringResource(id = R.string.stripe_cvc_number_hint),
        shape = MaterialTheme.shapes.small.copy(
            topStart = ZeroCornerSize,
            topEnd = ZeroCornerSize,
            bottomStart = ZeroCornerSize
        ),
        trailingIcon = {
            Image(
                painter = painterResource(cardBrand.cvcIcon),
                contentDescription = null,
            )
        },
    )
}

internal const val CARD_EDIT_UI_ERROR_MESSAGE = "card_edit_ui_error_message"
internal const val CARD_EDIT_UI_FALLBACK_EXPIRY_DATE = "•• / ••"
