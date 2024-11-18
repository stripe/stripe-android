package com.stripe.android.paymentsheet.ui

import android.content.res.Resources
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stripe.android.R
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.SavedPaymentMethod
import com.stripe.android.uicore.elements.TextFieldColors
import com.stripe.android.uicore.getBorderStroke
import com.stripe.android.uicore.stripeColors
import com.stripe.android.uicore.stripeShapes
import com.stripe.android.uicore.utils.collectAsState
import com.stripe.android.uicore.utils.mapAsStateFlow
import com.stripe.android.paymentsheet.R as PaymentSheetR

@Composable
internal fun UpdatePaymentMethodUI(interactor: UpdatePaymentMethodInteractor, modifier: Modifier) {
    val context = LocalContext.current
    val horizontalPadding = dimensionResource(
        id = com.stripe.android.paymentsheet.R.dimen.stripe_paymentsheet_outer_spacing_horizontal
    )
    val state by interactor.state.collectAsState()

    Column(
        modifier = modifier.padding(horizontal = horizontalPadding),
    ) {
        when (val savedPaymentMethod = interactor.displayableSavedPaymentMethod.savedPaymentMethod) {
            is SavedPaymentMethod.Card -> CardDetailsUI(
                displayableSavedPaymentMethod = interactor.displayableSavedPaymentMethod,
                card = savedPaymentMethod.card,
            )
            is SavedPaymentMethod.SepaDebit -> SepaDebitUI()
            is SavedPaymentMethod.USBankAccount -> USBankAccountUI()
            SavedPaymentMethod.Unexpected -> {}
        }

        interactor.displayableSavedPaymentMethod.getDetailsCannotBeChangedText()?.let {
            Text(
                text = it.resolve(context),
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.stripeColors.subtitle,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.padding(top = 12.dp).testTag(UPDATE_PM_DETAILS_SUBTITLE_TEST_TAG)
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

        if (interactor.canRemove) {
            DeletePaymentMethodUi(interactor)
        }
    }
}

@Composable
private fun CardDetailsUI(
    displayableSavedPaymentMethod: DisplayableSavedPaymentMethod,
    card: PaymentMethod.Card,
) {
    val dividerHeight = remember { mutableStateOf(0.dp) }

    Card(
        border = MaterialTheme.getBorderStroke(false),
        elevation = 0.dp,
        modifier = Modifier.testTag(UPDATE_PM_CARD_TEST_TAG),
    ) {
        Column {
            CardNumberField(
                last4 = card.last4,
                savedPaymentMethodIcon = displayableSavedPaymentMethod
                    .paymentMethod
                    .getSavedPaymentMethodIcon(forVerticalMode = true),
            )
            Divider(
                color = MaterialTheme.stripeColors.componentDivider,
                thickness = MaterialTheme.stripeShapes.borderStrokeWidth.dp,
            )
            Row(modifier = Modifier.fillMaxWidth()) {
                ExpiryField(
                    expiryMonth = card.expiryMonth,
                    expiryYear = card.expiryYear,
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
                CvcField(cardBrand = card.brand, modifier = Modifier.weight(1F))
            }
        }
    }
}

@Composable
private fun USBankAccountUI() {
    Text(
        text = "This is a US bank account",
        modifier = Modifier.testTag(UPDATE_PM_US_BANK_ACCOUNT_TEST_TAG),
    )
}

@Composable
private fun SepaDebitUI() {
    Text(
        text = "This is a SEPA debit account",
        modifier = Modifier.testTag(UPDATE_PM_SEPA_DEBIT_TEST_TAG),
    )
}

@Composable
private fun DeletePaymentMethodUi(interactor: UpdatePaymentMethodInteractor) {
    val openDialogValue = rememberSaveable { mutableStateOf(false) }
    val isRemoving by interactor.state.mapAsStateFlow { it.isRemoving }.collectAsState()

    Spacer(modifier = Modifier.requiredHeight(32.dp))

    RemoveButton(
        title = R.string.stripe_remove.resolvableString,
        borderColor = MaterialTheme.colors.error,
        idle = true,
        removing = openDialogValue.value || isRemoving,
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
private fun CardNumberField(last4: String?, savedPaymentMethodIcon: Int) {
    TextField(
        modifier = Modifier.fillMaxWidth(),
        value = "•••• •••• •••• $last4",
        enabled = false,
        label = {
            Label(
                text = stringResource(id = R.string.stripe_acc_label_card_number),
                modifier = Modifier
            )
        },
        trailingIcon = {
            PaymentMethodIconFromResource(
                iconRes = savedPaymentMethodIcon,
                colorFilter = null,
                alignment = Alignment.Center,
                modifier = Modifier,
            )
        },
        colors = TextFieldColors(shouldShowError = false),
        onValueChange = {}
    )
}

@Composable
private fun ExpiryField(expiryMonth: Int?, expiryYear: Int?, modifier: Modifier) {
    TextField(
        modifier = modifier.testTag(UPDATE_PM_EXPIRY_FIELD_TEST_TAG),
        value = formattedExpiryDate(expiryMonth = expiryMonth, expiryYear = expiryYear),
        enabled = false,
        label = {
            Label(
                text = stringResource(id = com.stripe.android.uicore.R.string.stripe_expiration_date_hint),
                modifier = Modifier
            )
        },
        colors = TextFieldColors(shouldShowError = false),
        shape = MaterialTheme.shapes.small.copy(
            topStart = ZeroCornerSize,
            topEnd = ZeroCornerSize,
            bottomEnd = ZeroCornerSize,
        ),
        onValueChange = {},
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
    TextField(
        modifier = modifier.testTag(UPDATE_PM_CVC_FIELD_TEST_TAG),
        value = cvc,
        enabled = false,
        label = {
            Label(
                text = stringResource(id = R.string.stripe_cvc_number_hint),
                modifier = Modifier
            )
        },
        colors = TextFieldColors(shouldShowError = false),
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
        onValueChange = {}
    )
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
        ),
        modifier = Modifier
    )
}

private fun DisplayableSavedPaymentMethod.getDetailsCannotBeChangedText(): ResolvableString? {
    return (
        when (savedPaymentMethod) {
            is SavedPaymentMethod.Card ->
                PaymentSheetR.string.stripe_paymentsheet_card_details_cannot_be_changed
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
internal const val UPDATE_PM_REMOVE_BUTTON_TEST_TAG = "update_payment_method_remove_button"
internal const val UPDATE_PM_ERROR_MESSAGE_TEST_TAG = "update_payment_method_error_message"
internal const val UPDATE_PM_US_BANK_ACCOUNT_TEST_TAG = "update_payment_method_bank_account_ui"
internal const val UPDATE_PM_SEPA_DEBIT_TEST_TAG = "update_payment_method_sepa_debit_ui"
internal const val UPDATE_PM_CARD_TEST_TAG = "update_payment_method_card_ui"
internal const val UPDATE_PM_DETAILS_SUBTITLE_TEST_TAG = "update_payment_method_subtitle"
