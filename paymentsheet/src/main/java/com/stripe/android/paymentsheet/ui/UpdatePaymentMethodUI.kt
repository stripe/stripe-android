package com.stripe.android.paymentsheet.ui

import android.content.res.Resources
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.material.Card
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import com.stripe.android.uicore.elements.TextFieldColors
import com.stripe.android.uicore.getBorderStroke
import com.stripe.android.uicore.stripeColors
import com.stripe.android.uicore.stripeShapes

@Composable
internal fun UpdatePaymentMethodUI(interactor: UpdatePaymentMethodInteractor, modifier: Modifier) {
    val horizontalPadding = dimensionResource(
        id = com.stripe.android.paymentsheet.R.dimen.stripe_paymentsheet_outer_spacing_horizontal
    )
    val dividerHeight = remember { mutableStateOf(0.dp) }

    Column(
        modifier = modifier.padding(horizontal = horizontalPadding),
    ) {
        Card(
            border = MaterialTheme.getBorderStroke(false),
            elevation = 0.dp
        ) {
            Column {
                CardNumberField(
                    last4 = interactor.card.last4,
                    savedPaymentMethodIcon = interactor
                        .displayableSavedPaymentMethod
                        .paymentMethod
                        .getSavedPaymentMethodIcon(forVerticalMode = true),
                )
                Divider(
                    color = MaterialTheme.stripeColors.componentDivider,
                    thickness = MaterialTheme.stripeShapes.borderStrokeWidth.dp,
                )
                Row(modifier = Modifier.fillMaxWidth()) {
                    ExpiryField(
                        expiryMonth = interactor.card.expiryMonth,
                        expiryYear = interactor.card.expiryYear,
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
                    CvcField(cardBrand = interactor.card.brand, modifier = Modifier.weight(1F))
                }
            }
        }
        Text(
            text = resolvableString(
                com.stripe.android.paymentsheet.R.string.stripe_paymentsheet_card_details_cannot_be_changed
            ).resolve(LocalContext.current),
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.stripeColors.subtitle,
            fontWeight = FontWeight.Normal,
            modifier = Modifier.padding(top = 12.dp)
        )
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
    val exampleCard = DisplayableSavedPaymentMethod(
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
            displayableSavedPaymentMethod = exampleCard,
            card = exampleCard.paymentMethod.card!!,
        ),
        modifier = Modifier
    )
}

private const val JANUARY = 1
private const val OCTOBER = 10
private const val DECEMBER = 12
private const val YEAR_2000 = 2000
private const val YEAR_2100 = 2100

internal const val UPDATE_PM_EXPIRY_FIELD_TEST_TAG = "update_payment_method_expiry_date"
internal const val UPDATE_PM_CVC_FIELD_TEST_TAG = "update_payment_method_cvc"
