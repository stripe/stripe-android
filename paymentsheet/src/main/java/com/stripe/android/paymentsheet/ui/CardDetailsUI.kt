package com.stripe.android.paymentsheet.ui

import android.content.res.Resources
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.setValue
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
import com.stripe.android.uicore.elements.SectionError
import com.stripe.android.uicore.elements.TextFieldState
import com.stripe.android.uicore.getBorderStroke
import com.stripe.android.uicore.stripeColors
import com.stripe.android.uicore.stripeShapes
import com.stripe.android.uicore.utils.collectAsState

@Composable
internal fun CardDetailsEditUI(
    editCardDetailsInteractor: EditCardDetailsInteractor,
) {
    val state by editCardDetailsInteractor.state.collectAsState()

    CardDetailsEditUI(
        shouldShowCardBrandDropdown = state.shouldShowCardBrandDropdown,
        selectedBrand = state.selectedCardBrand,
        card = state.card,
        expiryDateEditEnabled = state.expiryDateEditEnabled,
        availableNetworks = state.availableNetworks,
        paymentMethodIcon = state.paymentMethodIcon,
        validateDate = {
            state.dateValidator(it)
        },
        onBrandChoiceChanged = {
            editCardDetailsInteractor.handleViewAction(EditCardDetailsInteractor.ViewAction.BrandChoiceChanged(it))
        },
        onExpDateChanged = {
            editCardDetailsInteractor.handleViewAction(EditCardDetailsInteractor.ViewAction.DateChanged(it))
        }
    )
}

@Composable
private fun CardDetailsEditUI(
    shouldShowCardBrandDropdown: Boolean,
    selectedBrand: CardBrandChoice,
    card: PaymentMethod.Card,
    expiryDateEditEnabled: Boolean,
    availableNetworks: List<CardBrandChoice>,
    @DrawableRes paymentMethodIcon: Int,
    validateDate: (String) -> TextFieldState,
    onBrandChoiceChanged: (CardBrandChoice) -> Unit,
    onExpDateChanged: (String) -> Unit
) {
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
                        validator = validateDate,
                        expDate = card.formattedExpiryDate(expiryDateEditEnabled),
                        enabled = expiryDateEditEnabled,
                        onErrorChanged = {
                            errorMessage = it
                        },
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

        AnimatedVisibility(errorMessage != null) {
            SectionError(
                modifier = Modifier
                    .testTag(CARD_EDIT_UI_ERROR_MESSAGE),
                error = errorMessage.orEmpty()
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
    expDate: String,
    enabled: Boolean,
    modifier: Modifier,
    validator: (String) -> TextFieldState,
    onErrorChanged: (String?) -> Unit,
    onValueChanged: (String) -> Unit
) {
    ExpiryTextField(
        modifier = modifier
            .testTag(UPDATE_PM_EXPIRY_FIELD_TEST_TAG),
        expDate = expDate,
        enabled = enabled,
        validator = validator,
        onValueChange = onValueChanged,
        onErrorChanged = onErrorChanged
    )
}

private fun PaymentMethod.Card.formattedExpiryDate(
    expiryDateEditEnabled: Boolean
): String {
    val expiryMonth = this.expiryMonth
    val expiryYear = this.expiryYear
    @Suppress("ComplexCondition")
    if (
        expiryDateEditEnabled.not() &&
        (monthIsInvalid(expiryMonth) || yearIsInvalid(expiryYear))
    ) {
        return CARD_EDIT_UI_FALLBACK_EXPIRY_DATE
    }

    val formattedExpiryMonth = when {
        expiryMonth == null || monthIsInvalid(expiryMonth) -> {
            "00"
        }
        expiryMonth < OCTOBER -> {
            "0$expiryMonth"
        }
        else -> {
            expiryMonth.toString()
        }
    }

    val formattedExpiryYear = when {
        expiryYear == null || yearIsInvalid(expiryYear) -> {
            "00"
        }
        else -> {
            @Suppress("MagicNumber")
            expiryYear.toString().substring(2, 4)
        }
    }

    return "$formattedExpiryMonth$formattedExpiryYear"
}

private fun monthIsInvalid(expiryMonth: Int?): Boolean {
    return expiryMonth == null || expiryMonth < JANUARY || expiryMonth > DECEMBER
}

private fun yearIsInvalid(expiryYear: Int?): Boolean {
    // Since we use 2-digit years to represent the expiration year, we should keep dates to
    // this century.
    return expiryYear == null || expiryYear < YEAR_2000 || expiryYear > YEAR_2100
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

private const val JANUARY = 1
private const val OCTOBER = 10
private const val DECEMBER = 12
private const val YEAR_2000 = 2000
private const val YEAR_2100 = 2100

internal const val CARD_EDIT_UI_ERROR_MESSAGE = "card_edit_ui_error_message"
internal const val CARD_EDIT_UI_FALLBACK_EXPIRY_DATE = "•• / ••"
