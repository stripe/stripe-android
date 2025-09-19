package com.stripe.android.paymentsheet.ui

import android.content.res.Resources
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.stripe.android.R
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.CardBrand
import com.stripe.android.paymentsheet.ui.EditCardDetailsInteractor.ViewAction
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.Section
import com.stripe.android.uicore.elements.SectionFieldElement
import com.stripe.android.uicore.elements.SectionFieldElementUI
import com.stripe.android.uicore.strings.resolve
import com.stripe.android.uicore.stripeColors
import com.stripe.android.uicore.stripeShapes
import com.stripe.android.uicore.utils.collectAsState
import com.stripe.android.uicore.utils.stateFlowOf
import com.stripe.android.ui.core.R as CoreR

@Composable
internal fun CardDetailsEditUI(
    editCardDetailsInteractor: EditCardDetailsInteractor,
    spacing: Dp = 32.dp,
) {
    val state by editCardDetailsInteractor.state.collectAsState()
    val dividerHeight = remember { mutableStateOf(0.dp) }

    val hiddenBillingDetailsFields: State<Set<IdentifierSpec>>? = state.billingDetailsForm
        ?.hiddenElements
        ?.collectAsState()

    Column {
        // Card section - show if we have card details
        state.cardDetailsState?.let { cardDetails ->
            CardDetailsFormUI(
                billingDetailsForm = state.billingDetailsForm,
                cardDetailsState = cardDetails,
                payload = state.payload,
                paymentMethodIcon = state.paymentMethodIcon,
                onBrandChoiceChanged = {
                    editCardDetailsInteractor.handleViewAction(ViewAction.BrandChoiceChanged(it))
                },
                dividerHeight = dividerHeight,
                hiddenBillingDetailsFields = hiddenBillingDetailsFields,
                onExpDateChanged = {
                    editCardDetailsInteractor.handleViewAction(ViewAction.DateChanged(it))
                },
                nameElementForCardSection = state.nameElementForCardSection
            )
        }

        // Billing section - show if we have a billing form
        state.billingDetailsForm?.let { billingForm ->
            if (state.needsSpacerBeforeBilling) {
                Spacer(Modifier.height(spacing))
            }
            BillingDetailsFormUI(
                form = billingForm,
                onValuesChanged = {
                    editCardDetailsInteractor.handleViewAction(ViewAction.BillingDetailsChanged(it))
                }
            )
        }
    }
}

@Composable
private fun CardDetailsFormUI(
    billingDetailsForm: BillingDetailsForm?,
    cardDetailsState: EditCardDetailsInteractor.CardDetailsState,
    payload: EditCardPayload,
    paymentMethodIcon: Int,
    onBrandChoiceChanged: (CardBrandChoice) -> Unit,
    dividerHeight: MutableState<Dp>,
    hiddenBillingDetailsFields: State<Set<IdentifierSpec>>?,
    onExpDateChanged: (String) -> Unit,
    nameElementForCardSection: SectionFieldElement?,
) {
    val error = rememberError(cardDetailsState, billingDetailsForm)

    Section(
        title = billingDetailsForm?.let {
            resolvableString(CoreR.string.stripe_paymentsheet_add_payment_method_card_information)
        },
        error = error,
        modifier = Modifier.testTag(UPDATE_PM_CARD_TEST_TAG),
    ) {
        Column {
            nameElementForCardSection?.let { nameElement ->
                SectionFieldElementUI(
                    enabled = true,
                    field = nameElement,
                    hiddenIdentifiers = emptySet(),
                    lastTextFieldIdentifier = null
                )
                Divider(
                    color = MaterialTheme.stripeColors.componentDivider,
                    thickness = MaterialTheme.stripeShapes.borderStrokeWidth.dp,
                )
            }
            CardNumberField(
                last4 = payload.last4,
                selectedBrand = cardDetailsState.selectedCardBrand,
                shouldShowCardBrandDropdown = cardDetailsState.shouldShowCardBrandDropdown,
                availableNetworks = cardDetailsState.availableNetworks,
                savedPaymentMethodIcon = paymentMethodIcon,
                onBrandChoiceChanged = onBrandChoiceChanged,
                isFirstField = nameElementForCardSection == null,
            )
            Divider(
                color = MaterialTheme.stripeColors.componentDivider,
                thickness = MaterialTheme.stripeShapes.borderStrokeWidth.dp,
            )
            Row(modifier = Modifier.fillMaxWidth()) {
                ExpiryTextField(
                    modifier = Modifier
                        .testTag(UPDATE_PM_EXPIRY_FIELD_TEST_TAG)
                        .weight(1F)
                        .onSizeChanged {
                            dividerHeight.value =
                                (it.height / Resources.getSystem().displayMetrics.density).dp
                        },
                    state = cardDetailsState.expiryDateState,
                    hasNextField = hiddenBillingDetailsFields?.value?.hasFocusableFields() == true,
                    onValueChange = onExpDateChanged,
                )
                Divider(
                    modifier = Modifier
                        .height(dividerHeight.value)
                        .width(MaterialTheme.stripeShapes.borderStrokeWidth.dp),
                    color = MaterialTheme.stripeColors.componentDivider,
                )
                CvcField(cardBrand = payload.brand, modifier = Modifier.weight(1F))
            }
        }
    }
}

@Composable
private fun rememberError(
    cardDetailsState: EditCardDetailsInteractor.CardDetailsState,
    billingDetailsForm: BillingDetailsForm?
): String? {
    val nameErrorState = remember(billingDetailsForm?.nameElement) {
        billingDetailsForm?.nameElement?.controller?.error ?: stateFlowOf(null)
    }

    val nameError by nameErrorState.collectAsState()

    val error = nameError?.let {
        resolvableString(it.errorMessage, it.formatArgs)
    } ?: cardDetailsState.expiryDateState.sectionError()

    return error?.resolve()
}

/**
 * Checks if the billing details form has any fields that are focusable.
 */
@Composable
private fun Set<IdentifierSpec>.hasFocusableFields(): Boolean = listOf(
    IdentifierSpec.PostalCode
).none { contains(it) }

@Composable
private fun CardNumberField(
    last4: String?,
    selectedBrand: CardBrandChoice,
    availableNetworks: List<CardBrandChoice>,
    shouldShowCardBrandDropdown: Boolean,
    savedPaymentMethodIcon: Int,
    onBrandChoiceChanged: (CardBrandChoice) -> Unit,
    isFirstField: Boolean,
) {
    CommonTextField(
        value = "•••• •••• •••• ${last4 ?: "••••"}",
        label = stringResource(id = R.string.stripe_acc_label_card_number),
        shape = if (isFirstField) {
            MaterialTheme.shapes.small.copy(
                bottomStart = ZeroCornerSize,
                bottomEnd = ZeroCornerSize
            )
        } else {
            MaterialTheme.shapes.small.copy(
                topStart = ZeroCornerSize,
                topEnd = ZeroCornerSize,
                bottomStart = ZeroCornerSize,
                bottomEnd = ZeroCornerSize
            )
        },
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
        }
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
