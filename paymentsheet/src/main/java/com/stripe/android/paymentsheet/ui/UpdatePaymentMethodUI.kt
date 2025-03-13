package com.stripe.android.paymentsheet.ui

import android.content.Context
import android.content.res.Resources
import androidx.annotation.RestrictTo
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.material.Card
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stripe.android.CardBrandFilter
import com.stripe.android.DefaultCardBrandFilter
import com.stripe.android.R
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.SavedPaymentMethod
import com.stripe.android.paymentsheet.utils.testMetadata
import com.stripe.android.uicore.elements.CheckboxElementUI
import com.stripe.android.uicore.elements.SectionElementUI
import com.stripe.android.uicore.elements.SectionError
import com.stripe.android.uicore.elements.TextFieldColors
import com.stripe.android.uicore.elements.TextFieldState
import com.stripe.android.uicore.getBorderStroke
import com.stripe.android.uicore.strings.resolve
import com.stripe.android.uicore.stripeColors
import com.stripe.android.uicore.stripeShapes
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
                val cardUIHandler = remember(savedPaymentMethod) {
                    interactor.cardUiHandlerFactory(savedPaymentMethod)
                }
                CardDetailsUI(
                    displayableSavedPaymentMethod = interactor.displayableSavedPaymentMethod,
                    shouldShowCardBrandDropdown = shouldShowCardBrandDropdown,
                    cardEditUIHandler = cardUIHandler,
                    handleViewAction = interactor::handleViewAction
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
    onCheckChanged: (Boolean) -> Unit,
) {
    CheckboxElementUI(
        isChecked = isChecked,
        onValueChange = onCheckChanged,
        isEnabled = true,
        label = (com.stripe.android.ui.core.R.string.stripe_set_as_default_payment_method).resolvableString.resolve(),
        modifier = Modifier.padding(top = 12.dp).testTag(UPDATE_PM_SET_AS_DEFAULT_CHECKBOX_TEST_TAG)
    )
}

@Composable
private fun UpdatePaymentMethodButtons(
    interactor: UpdatePaymentMethodInteractor,
) {
    val shouldShowUpdatePaymentMethodUi =
        interactor.isModifiablePaymentMethod || interactor.shouldShowSetAsDefaultCheckbox

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
    displayableSavedPaymentMethod: DisplayableSavedPaymentMethod,
    shouldShowCardBrandDropdown: Boolean,
    cardEditUIHandler: CardEditUIHandler,
    handleViewAction: (UpdatePaymentMethodInteractor.ViewAction) -> Unit
) {
    val state by cardEditUIHandler.state.collectAsState()
    val dividerHeight = remember { mutableStateOf(0.dp) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column {
        Card(
            border = MaterialTheme.getBorderStroke(false),
            elevation = 0.dp,
            modifier = Modifier.testTag(UPDATE_PM_CARD_TEST_TAG),
        ) {
            Column {
                CardNumberField(
                    card = state.card,
                    selectedBrand = state.selectedCardBrand,
                    shouldShowCardBrandDropdown = shouldShowCardBrandDropdown,
                    cardBrandFilter = cardEditUIHandler.cardBrandFilter,
                    savedPaymentMethodIcon = displayableSavedPaymentMethod
                        .paymentMethod
                        .getSavedPaymentMethodIcon(forVerticalMode = true),
                    onBrandOptionsShown = {
                        cardEditUIHandler.onBrandChoiceOptionsShown()
                    },
                    onBrandChoiceChanged = {
                        cardEditUIHandler.onBrandChoiceChanged(it)
                    },
                    onBrandChoiceOptionsDismissed = {
                        cardEditUIHandler.onBrandChoiceOptionsDismissed()
                    },
                )
                Divider(
                    color = MaterialTheme.stripeColors.componentDivider,
                    thickness = MaterialTheme.stripeShapes.borderStrokeWidth.dp,
                )
                Row(modifier = Modifier.fillMaxWidth()) {
                    ExpiryField(
                        expDate = state.expDate,
                        onValueChange = cardEditUIHandler::dateChanged,
                        onErrorChanged = {
                            errorMessage = it
                        },
                        validator = cardEditUIHandler::dateValidator,
                        modifier = Modifier
                            .weight(1F)
                            .onSizeChanged {
                                dividerHeight.value =
                                    (it.height / Resources.getSystem().displayMetrics.density).dp
                            },
                    )
                    Divider(
                        modifier = Modifier
                            .height(dividerHeight.value)
                            .width(MaterialTheme.stripeShapes.borderStrokeWidth.dp),
                        color = MaterialTheme.stripeColors.componentDivider,
                    )
                    CvcField(cardBrand = state.card.brand, modifier = Modifier.weight(1F))
                }
            }
        }

        AnimatedVisibility(errorMessage != null) {
            SectionError(errorMessage.orEmpty())
        }

        if (state.collectAddress) {
            Spacer(Modifier.height(32.dp))

            SectionElementUI(
                enabled = true,
                element = state.addressElement,
                hiddenIdentifiers = state.hiddenAddressFields,
                lastTextFieldIdentifier = null
            )
        }
    }
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
    PrimaryButton(
        label = stringResource(id = PaymentSheetR.string.stripe_paymentsheet_save),
        isLoading = isLoading,
        isEnabled = (state.cardBrandHasBeenChanged || state.setAsDefaultCheckboxChecked || state.cardInputHasChanged) &&
            state.status == UpdatePaymentMethodInteractor.Status.Idle,
        onButtonClick = { interactor.handleViewAction(UpdatePaymentMethodInteractor.ViewAction.SaveButtonPressed) },
        modifier = Modifier.testTag(UPDATE_PM_SAVE_BUTTON_TEST_TAG).testMetadata("isLoading=$isLoading")
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

@Composable
private fun CardNumberField(
    card: PaymentMethod.Card,
    selectedBrand: CardBrandChoice,
    cardBrandFilter: CardBrandFilter,
    shouldShowCardBrandDropdown: Boolean,
    savedPaymentMethodIcon: Int,
    onBrandOptionsShown: () -> Unit,
    onBrandChoiceChanged: (CardBrandChoice) -> Unit,
    onBrandChoiceOptionsDismissed: () -> Unit,
) {
    CommonTextField(
        value = "•••• •••• •••• ${card.last4}",
        label = stringResource(id = R.string.stripe_acc_label_card_number),
        trailingIcon = {
            if (shouldShowCardBrandDropdown) {
                CardBrandDropdown(
                    selectedBrand = selectedBrand,
                    availableBrands = card.getAvailableNetworks(cardBrandFilter),
                    onBrandOptionsShown = onBrandOptionsShown,
                    onBrandChoiceChanged = onBrandChoiceChanged,
                    onBrandChoiceOptionsDismissed = onBrandChoiceOptionsDismissed,
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
    expDate: String,
    modifier: Modifier,
    onValueChange: (String) -> Unit,
    validator: (String) -> TextFieldState,
    onErrorChanged: (String?) -> Unit
) {
    ExpiryTextField(
        modifier = modifier.testTag(UPDATE_PM_EXPIRY_FIELD_TEST_TAG),
        expDate = expDate,
        onValueChange = onValueChange,
        validator = validator,
        onErrorChanged = onErrorChanged
    )
}

private fun formattedExpiryDate(expiryMonth: Int?, expiryYear: Int?): String {
    @Suppress("ComplexCondition")
    if (
        expiryMonth == null ||
        monthIsInvalid(expiryMonth) ||
        expiryYear == null ||
        yearIsInvalid(expiryYear)
    ) {
        return "••/••"
    }

    val formattedExpiryMonth = if (expiryMonth < OCTOBER) {
        "0$expiryMonth"
    } else {
        expiryMonth.toString()
    }

    @Suppress("MagicNumber")
    val formattedExpiryYear = expiryYear.toString().substring(2, 4)

    return "$formattedExpiryMonth/$formattedExpiryYear"
}

private fun monthIsInvalid(expiryMonth: Int): Boolean {
    return expiryMonth < JANUARY || expiryMonth > DECEMBER
}

private fun yearIsInvalid(expiryYear: Int): Boolean {
    // Since we use 2-digit years to represent the expiration year, we should keep dates to
    // this century.
    return expiryYear < YEAR_2000 || expiryYear > YEAR_2100
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

@Composable
internal fun CommonTextField(
    value: String,
    label: String,
    onValueChange: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    trailingIcon: @Composable (() -> Unit)? = null,
    shouldShowError: Boolean = false,
    enabled: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    shape: Shape =
        MaterialTheme.shapes.small.copy(bottomEnd = ZeroCornerSize, bottomStart = ZeroCornerSize),
    colors: TextFieldColors = TextFieldColors(
        shouldShowError = shouldShowError,
    ),
) {
    TextField(
        modifier = modifier.fillMaxWidth(),
        value = value,
        enabled = enabled,
        label = {
            Label(
                text = label,
            )
        },
        trailingIcon = trailingIcon,
        shape = shape,
        colors = colors,
        visualTransformation = visualTransformation,
        onValueChange = onValueChange,
    )
}

@Composable
private fun Label(
    text: String,
) {
    Text(
        text = text,
        color = MaterialTheme.stripeColors.placeholderText.copy(alpha = ContentAlpha.disabled),
        style = MaterialTheme.typography.subtitle1
    )
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
        interactor = DefaultUpdatePaymentMethodInteractor.factory(
            isLiveMode = false,
            canRemove = true,
            displayableSavedPaymentMethod = exampleCard,
            addressCollectionMode = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic,
            removeExecutor = { null },
            updateCardExecutor = { paymentMethod, _ -> Result.success(paymentMethod) },
            setDefaultPaymentMethodExecutor = { _ -> Result.success(Unit) },
            cardBrandFilter = DefaultCardBrandFilter,
            onBrandChoiceOptionsShown = {},
            onBrandChoiceOptionsDismissed = {},
            shouldShowSetAsDefaultCheckbox = true,
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

private const val JANUARY = 1
private const val OCTOBER = 10
private const val DECEMBER = 12
private const val YEAR_2000 = 2000
private const val YEAR_2100 = 2100

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
