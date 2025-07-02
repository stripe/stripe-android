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
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import com.stripe.android.R
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.CardBrand
import com.stripe.android.paymentsheet.ui.EditCardDetailsInteractor.ViewAction
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.Section
import com.stripe.android.uicore.elements.SectionElement
import com.stripe.android.uicore.elements.SectionElementUI
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
        payload = state.payload,
        availableNetworks = state.availableNetworks,
        paymentMethodIcon = state.paymentMethodIcon,
        expiryDateState = state.expiryDateState,
        billingDetailsForm = state.billingDetailsForm,
        onBrandChoiceChanged = {
            editCardDetailsInteractor.handleViewAction(ViewAction.BrandChoiceChanged(it))
        },
        onExpDateChanged = {
            editCardDetailsInteractor.handleViewAction(ViewAction.DateChanged(it))
        },
        onAddressChanged = {
            editCardDetailsInteractor.handleViewAction(ViewAction.BillingDetailsChanged(it))
        }
    )
}

@Composable
private fun CardDetailsEditUI(
    shouldShowCardBrandDropdown: Boolean,
    selectedBrand: CardBrandChoice,
    payload: EditCardPayload,
    expiryDateState: ExpiryDateState,
    billingDetailsForm: BillingDetailsForm?,
    availableNetworks: List<CardBrandChoice>,
    @DrawableRes paymentMethodIcon: Int,
    onBrandChoiceChanged: (CardBrandChoice) -> Unit,
    onExpDateChanged: (String) -> Unit,
    onAddressChanged: (BillingDetailsFormState) -> Unit,
) {
    val dividerHeight = remember { mutableStateOf(0.dp) }

    val hiddenBillingDetailsFields: State<Set<IdentifierSpec>>? = billingDetailsForm
        ?.hiddenElements
        ?.collectAsState()

    Column {
        if (billingDetailsForm?.emailElement != null || billingDetailsForm?.phoneElement != null) {
            ContactInformationSection(billingDetailsForm)
        }

        Section(
            title = billingDetailsForm?.let {
                resolvableString(CoreR.string.stripe_paymentsheet_add_payment_method_card_information)
            },
            error = expiryDateState.sectionError()?.resolve(),
            modifier = Modifier.testTag(UPDATE_PM_CARD_TEST_TAG),
        ) {
            Column {
                if (billingDetailsForm?.nameElement != null) {
                    SectionElementUI(
                        enabled = true,
                        element = SectionElement.wrap(sectionFieldElement = billingDetailsForm.nameElement),
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
                    selectedBrand = selectedBrand,
                    shouldShowCardBrandDropdown = shouldShowCardBrandDropdown,
                    availableNetworks = availableNetworks,
                    savedPaymentMethodIcon = paymentMethodIcon,
                    onBrandChoiceChanged = onBrandChoiceChanged,
                    isFirstField = billingDetailsForm?.nameElement == null,
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
                        state = expiryDateState,
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
private fun ContactInformationSection(billingDetailsForm: BillingDetailsForm) {
    val contactElements = listOfNotNull(
        billingDetailsForm.emailElement,
        billingDetailsForm.phoneElement
    )

    if (contactElements.isNotEmpty()) {
        SectionElementUI(
            enabled = true,
            element = SectionElement.wrap(
                sectionFieldElements = contactElements,
                label = resolvableString(CoreR.string.stripe_contact_information)
            ),
            hiddenIdentifiers = emptySet(),
            lastTextFieldIdentifier = null
        )
        Spacer(Modifier.height(32.dp))
    }
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
