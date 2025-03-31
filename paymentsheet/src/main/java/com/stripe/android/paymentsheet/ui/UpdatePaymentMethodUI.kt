package com.stripe.android.paymentsheet.ui

import android.content.Context
import androidx.annotation.RestrictTo
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stripe.android.DefaultCardBrandFilter
import com.stripe.android.R
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.SavedPaymentMethod
import com.stripe.android.paymentsheet.utils.testMetadata
import com.stripe.android.uicore.elements.CheckboxElementUI
import com.stripe.android.uicore.getBorderStroke
import com.stripe.android.uicore.strings.resolve
import com.stripe.android.uicore.stripeColors
import com.stripe.android.uicore.utils.collectAsState
import com.stripe.android.uicore.utils.mapAsStateFlow
import com.stripe.android.common.ui.PrimaryButton as PrimaryButton
import com.stripe.android.paymentsheet.R as PaymentSheetR

@Composable
internal fun UpdatePaymentMethodUI(interactor: UpdatePaymentMethodInteractor, modifier: Modifier) {
    val context = LocalContext.current
    val horizontalPadding = dimensionResource(
        id = PaymentSheetR.dimen.stripe_paymentsheet_outer_spacing_horizontal
    )
    val state by interactor.state.collectAsState()
    val shouldShowCardBrandDropdown = interactor.isModifiablePaymentMethod &&
        interactor.displayableSavedPaymentMethod.isModifiable()

    Column(
        modifier = modifier.padding(horizontal = horizontalPadding).testTag(UPDATE_PM_SCREEN_TEST_TAG),
    ) {
        when (val savedPaymentMethod = interactor.displayableSavedPaymentMethod.savedPaymentMethod) {
            is SavedPaymentMethod.Card -> {
                CardDetailsUI(
                    savedPaymentMethod = savedPaymentMethod,
                    interactor = interactor,
                )
            }
            is SavedPaymentMethod.SepaDebit -> SepaDebitUI(
                name = interactor.displayableSavedPaymentMethod.paymentMethod.billingDetails?.name,
                email = interactor.displayableSavedPaymentMethod.paymentMethod.billingDetails?.email,
                sepaDebit = savedPaymentMethod.sepaDebit,
            )
            is SavedPaymentMethod.USBankAccount -> USBankAccountUI(
                name = interactor.displayableSavedPaymentMethod.paymentMethod.billingDetails?.name,
                email = interactor.displayableSavedPaymentMethod.paymentMethod.billingDetails?.email,
                usBankAccount = savedPaymentMethod.usBankAccount,
            )
            SavedPaymentMethod.Unexpected -> {}
        }

        if (!interactor.isExpiredCard) {
            DetailsCannotBeChangedText(interactor, shouldShowCardBrandDropdown, context)
        }

        if (interactor.shouldShowSetAsDefaultCheckbox) {
            SetAsDefaultPaymentMethodCheckbox(
                isChecked = state.setAsDefaultCheckboxChecked,
                isEnabled = interactor.setAsDefaultCheckboxEnabled,
                onCheckChanged = { newCheckedValue ->
                    interactor.handleViewAction(
                        UpdatePaymentMethodInteractor.ViewAction.SetAsDefaultCheckboxChanged(newCheckedValue)
                    )
                }
            )
        }

        state.error?.let {
            ErrorMessage(
                error = it.resolve(context),
                modifier = Modifier
                    .padding(top = 12.dp)
                    .testTag(UPDATE_PM_ERROR_MESSAGE_TEST_TAG)
            )
        }

        UpdatePaymentMethodButtons(interactor)
    }
}

@Composable
private fun DetailsCannotBeChangedText(
    interactor: UpdatePaymentMethodInteractor,
    shouldShowCardBrandDropdown: Boolean,
    context: Context
) {
    interactor.displayableSavedPaymentMethod.getDetailsCannotBeChangedText(
        canUpdateCardBrand = shouldShowCardBrandDropdown && interactor.hasValidBrandChoices,
    )?.let {
        Text(
            text = it.resolve(context),
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.stripeColors.subtitle,
            fontWeight = FontWeight.Normal,
            modifier = Modifier
                .padding(top = 8.dp)
                .testTag(UPDATE_PM_DETAILS_SUBTITLE_TEST_TAG)
        )
    }
}

@Composable
private fun SetAsDefaultPaymentMethodCheckbox(
    isChecked: Boolean,
    isEnabled: Boolean,
    onCheckChanged: (Boolean) -> Unit,
) {
    CheckboxElementUI(
        isChecked = isChecked,
        onValueChange = onCheckChanged,
        isEnabled = isEnabled,
        label = (com.stripe.android.ui.core.R.string.stripe_set_as_default_payment_method).resolvableString.resolve(),
        modifier = Modifier.padding(top = 12.dp).testTag(UPDATE_PM_SET_AS_DEFAULT_CHECKBOX_TEST_TAG)
    )
}

@Composable
private fun UpdatePaymentMethodButtons(
    interactor: UpdatePaymentMethodInteractor,
) {
    val shouldShowUpdatePaymentMethodUi = interactor.shouldShowSaveButton

    if (shouldShowUpdatePaymentMethodUi) {
        Spacer(modifier = Modifier.requiredHeight(32.dp))
        UpdatePaymentMethodUi(interactor)
    }

    if (interactor.canRemove) {
        val spacerHeight = if (shouldShowUpdatePaymentMethodUi) {
            16.dp
        } else {
            32.dp
        }

        Spacer(modifier = Modifier.requiredHeight(spacerHeight))
        DeletePaymentMethodUi(interactor)
    }
}

@Composable
private fun CardDetailsUI(
    savedPaymentMethod: SavedPaymentMethod.Card,
    interactor: UpdatePaymentMethodInteractor,
) {
    val cardEditUIHandler = remember(savedPaymentMethod) {
        interactor.cardUiHandlerFactory(savedPaymentMethod)
    }
    CardDetailsEditUI(
        cardEditUIHandler = cardEditUIHandler,
        isExpiredCard = interactor.isExpiredCard
    )
}

@Composable
private fun USBankAccountUI(
    name: String?,
    email: String?,
    usBankAccount: PaymentMethod.USBankAccount,
) {
    BankAccountUI(
        name = name,
        email = email,
        bankAccountFieldText = resolvableString(
            PaymentSheetR.string.stripe_paymentsheet_bank_account_info,
            usBankAccount.bankName,
            usBankAccount.last4,
        ).resolve(),
        bankAccountFieldLabel = stringResource(R.string.stripe_title_bank_account),
        modifier = Modifier.testTag(UPDATE_PM_US_BANK_ACCOUNT_TEST_TAG),
    )
}

@Composable
private fun SepaDebitUI(
    name: String?,
    email: String?,
    sepaDebit: PaymentMethod.SepaDebit,
) {
    BankAccountUI(
        name = name,
        email = email,
        bankAccountFieldText = resolvableString(
            PaymentSheetR.string.stripe_paymentsheet_bank_account_last_4,
            sepaDebit.last4,
        ).resolve(),
        bankAccountFieldLabel = stringResource(PaymentSheetR.string.stripe_paymentsheet_iban),
        modifier = Modifier.testTag(UPDATE_PM_SEPA_DEBIT_TEST_TAG),
    )
}

@Composable
private fun BankAccountUI(
    name: String?,
    email: String?,
    bankAccountFieldLabel: String,
    bankAccountFieldText: String,
    modifier: Modifier,
) {
    Column(
        modifier = modifier,
    ) {
        BankAccountTextField(
            value = name ?: "",
            label = stringResource(id = com.stripe.android.core.R.string.stripe_address_label_full_name),
        )
        BankAccountTextField(
            value = email ?: "",
            label = stringResource(com.stripe.android.uicore.R.string.stripe_email),
            modifier = Modifier.padding(vertical = 8.dp),
        )
        BankAccountTextField(
            value = bankAccountFieldText,
            label = bankAccountFieldLabel,
        )
    }
}

@Composable
private fun BankAccountTextField(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Card(
        border = MaterialTheme.getBorderStroke(false),
        elevation = 0.dp,
        modifier = modifier,
    ) {
        CommonTextField(
            value = value,
            label = label,
        )
    }
}

@Composable
private fun UpdatePaymentMethodUi(interactor: UpdatePaymentMethodInteractor) {
    val state by interactor.state.collectAsState()

    val isLoading = state.status == UpdatePaymentMethodInteractor.Status.Updating
    val isEnabled = state.isSaveButtonEnabled

    PrimaryButton(
        label = stringResource(id = PaymentSheetR.string.stripe_paymentsheet_save),
        isLoading = isLoading,
        isEnabled = state.isSaveButtonEnabled,
        onButtonClick = { interactor.handleViewAction(UpdatePaymentMethodInteractor.ViewAction.SaveButtonPressed) },
        modifier = Modifier
            .testTag(UPDATE_PM_SAVE_BUTTON_TEST_TAG)
            .testMetadata("isLoading=$isLoading")
    )
}

@Composable
private fun DeletePaymentMethodUi(interactor: UpdatePaymentMethodInteractor) {
    val openDialogValue = rememberSaveable { mutableStateOf(false) }
    val status by interactor.state.mapAsStateFlow { it.status }.collectAsState()

    RemoveButton(
        title = R.string.stripe_remove.resolvableString,
        borderColor = MaterialTheme.colors.error,
        idle = status == UpdatePaymentMethodInteractor.Status.Idle,
        removing = status == UpdatePaymentMethodInteractor.Status.Removing,
        onRemove = { openDialogValue.value = true },
        testTag = UPDATE_PM_REMOVE_BUTTON_TEST_TAG,
    )

    if (openDialogValue.value) {
        RemovePaymentMethodDialogUI(paymentMethod = interactor.displayableSavedPaymentMethod, onConfirmListener = {
            openDialogValue.value = false
            interactor.handleViewAction(UpdatePaymentMethodInteractor.ViewAction.RemovePaymentMethod)
        }, onDismissListener = {
            openDialogValue.value = false
        })
    }
}

@Preview
@Composable
private fun PreviewUpdatePaymentMethodUI() {
    val exampleCard = DisplayableSavedPaymentMethod.create(
        displayName = "4242".resolvableString,
        paymentMethod = PaymentMethod(
            id = "002",
            created = null,
            liveMode = false,
            code = PaymentMethod.Type.Card.code,
            type = PaymentMethod.Type.Card,
            card = PaymentMethod.Card(CardBrand.Visa)
        )
    )
    UpdatePaymentMethodUI(
        interactor = DefaultUpdatePaymentMethodInteractor(
            isLiveMode = false,
            canRemove = true,
            displayableSavedPaymentMethod = exampleCard,
            removeExecutor = { null },
            updatePaymentMethodExecutor = { paymentMethod, _ -> Result.success(paymentMethod) },
            setDefaultPaymentMethodExecutor = { _ -> Result.success(Unit) },
            cardBrandFilter = DefaultCardBrandFilter,
            onBrandChoiceSelected = {},
            shouldShowSetAsDefaultCheckbox = true,
            isDefaultPaymentMethod = false,
            onUpdateSuccess = {},
        ),
        modifier = Modifier
    )
}

private fun DisplayableSavedPaymentMethod.getDetailsCannotBeChangedText(
    canUpdateCardBrand: Boolean,
): ResolvableString? {
    return (
        when (savedPaymentMethod) {
            is SavedPaymentMethod.Card ->
                if (canUpdateCardBrand) {
                    PaymentSheetR.string.stripe_paymentsheet_only_card_brand_can_be_changed
                } else {
                    PaymentSheetR.string.stripe_paymentsheet_card_details_cannot_be_changed
                }
            is SavedPaymentMethod.USBankAccount ->
                PaymentSheetR.string.stripe_paymentsheet_bank_account_details_cannot_be_changed
            is SavedPaymentMethod.SepaDebit ->
                PaymentSheetR.string.stripe_paymentsheet_sepa_debit_details_cannot_be_changed
            SavedPaymentMethod.Unexpected -> null
        }
        )?.resolvableString
}

internal const val UPDATE_PM_EXPIRY_FIELD_TEST_TAG = "update_payment_method_expiry_date"
internal const val UPDATE_PM_CVC_FIELD_TEST_TAG = "update_payment_method_cvc"

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
const val UPDATE_PM_REMOVE_BUTTON_TEST_TAG = "update_payment_method_remove_button"

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
const val UPDATE_PM_SAVE_BUTTON_TEST_TAG = "update_payment_method_save_button"
internal const val UPDATE_PM_ERROR_MESSAGE_TEST_TAG = "update_payment_method_error_message"
internal const val UPDATE_PM_US_BANK_ACCOUNT_TEST_TAG = "update_payment_method_bank_account_ui"
internal const val UPDATE_PM_SEPA_DEBIT_TEST_TAG = "update_payment_method_sepa_debit_ui"
internal const val UPDATE_PM_CARD_TEST_TAG = "update_payment_method_card_ui"
internal const val UPDATE_PM_DETAILS_SUBTITLE_TEST_TAG = "update_payment_method_subtitle"

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
const val UPDATE_PM_SCREEN_TEST_TAG = "update_payment_method_screen"

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
const val UPDATE_PM_SET_AS_DEFAULT_CHECKBOX_TEST_TAG = "update_payment_method_set_as_default_checkbox"
