package com.stripe.android.paymentsheet.ui

import android.content.res.Resources
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.stripe.android.CardBrandFilter
import com.stripe.android.R
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.uicore.getBorderStroke
import com.stripe.android.uicore.stripeColors
import com.stripe.android.uicore.stripeShapes
import com.stripe.android.uicore.utils.collectAsState

@Composable
internal fun CardDetailsEditUI(
    cardEditUIHandler: CardEditUIHandler,
    isExpiredCard: Boolean,
    modifier: Modifier = Modifier
) {
    val state by cardEditUIHandler.state.collectAsState()
    val dividerHeight = remember { mutableStateOf(0.dp) }

    Card(
        border = MaterialTheme.getBorderStroke(false),
        elevation = 0.dp,
        modifier = modifier.testTag(UPDATE_PM_CARD_TEST_TAG),
    ) {
        Column {
            CardNumberField(
                card = state.card,
                selectedBrand = state.selectedCardBrand,
                shouldShowCardBrandDropdown = cardEditUIHandler.showCardBrandDropdown,
                cardBrandFilter = cardEditUIHandler.cardBrandFilter,
                savedPaymentMethodIcon = cardEditUIHandler.paymentMethodIcon,
                onBrandChoiceChanged = {
                    cardEditUIHandler.onBrandChoiceChanged(it)
                },
            )
            Divider(
                color = MaterialTheme.stripeColors.componentDivider,
                thickness = MaterialTheme.stripeShapes.borderStrokeWidth.dp,
            )
            Row(modifier = Modifier.fillMaxWidth()) {
                ExpiryField(
                    expiryMonth = state.card.expiryMonth,
                    expiryYear = state.card.expiryYear,
                    isExpired = isExpiredCard,
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
}

@Composable
private fun CardNumberField(
    card: PaymentMethod.Card,
    selectedBrand: CardBrandChoice,
    cardBrandFilter: CardBrandFilter,
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
                    availableBrands = card.getAvailableNetworks(cardBrandFilter),
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
    expiryMonth: Int?,
    expiryYear: Int?,
    isExpired: Boolean,
    modifier: Modifier
) {
    CommonTextField(
        modifier = modifier.testTag(UPDATE_PM_EXPIRY_FIELD_TEST_TAG),
        value = formattedExpiryDate(expiryMonth = expiryMonth, expiryYear = expiryYear),
        label = stringResource(id = com.stripe.android.uicore.R.string.stripe_expiration_date_hint),
        shape = MaterialTheme.shapes.small.copy(
            topStart = ZeroCornerSize,
            topEnd = ZeroCornerSize,
            bottomEnd = ZeroCornerSize,
        ),
        shouldShowError = isExpired,
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

private const val JANUARY = 1
private const val OCTOBER = 10
private const val DECEMBER = 12
private const val YEAR_2000 = 2000
private const val YEAR_2100 = 2100
