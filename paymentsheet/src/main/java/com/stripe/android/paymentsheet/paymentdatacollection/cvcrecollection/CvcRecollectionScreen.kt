package com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection

import androidx.annotation.RestrictTo
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.AutofillType
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stripe.android.model.CardBrand
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.paymentsheet.ui.TestModeBadge
import com.stripe.android.ui.core.elements.H4Text
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.elements.Placeholder
import com.stripe.android.uicore.elements.SectionCard
import com.stripe.android.uicore.elements.TextFieldColors
import com.stripe.android.uicore.elements.TrailingIcon
import com.stripe.android.uicore.stripeColors
import com.stripe.android.uicore.text.autofill
import com.stripe.android.uicore.utils.collectAsState

@Composable
internal fun CvcRecollectionScreen(
    lastFour: String,
    isTestMode: Boolean,
    cvcState: CvcState,
    viewActionHandler: (action: CvcRecollectionViewAction) -> Unit
) {
    StripeTheme {
        Column(
            Modifier
                .background(MaterialTheme.stripeColors.materialColors.surface)
                .padding(horizontal = 20.dp)
        ) {
            CvcRecollectionTopBar(isTestMode) {
                viewActionHandler.invoke(CvcRecollectionViewAction.OnBackPressed)
            }
            CvcRecollectionTitle()
            CvcRecollectionField(
                lastFour = lastFour,
                enabled = true,
                cvcState = cvcState,
                onValueChanged = {
                    viewActionHandler(CvcRecollectionViewAction.OnCvcChanged(it))
                }
            )
            CvcRecollectionButton(cvcState.isValid) {
                viewActionHandler.invoke(CvcRecollectionViewAction.OnConfirmPressed)
            }
        }
    }
}

@Composable
internal fun CvcRecollectionPaymentSheetScreen(
    interactor: CvcRecollectionInteractor,
) {
    val state by interactor.viewState.collectAsState()

    StripeTheme {
        Column(
            Modifier
                .background(MaterialTheme.stripeColors.materialColors.surface)
                .padding(horizontal = 20.dp)
        ) {
            CvcRecollectionTitle()
            CvcRecollectionField(
                lastFour = state.lastFour,
                enabled = state.isEnabled,
                cvcState = state.cvcState,
                onValueChanged = interactor::onCvcChanged,
            )
        }
    }
}

@Suppress("MagicNumber", "LongMethod")
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun CvcRecollectionField(
    lastFour: String,
    enabled: Boolean,
    cvcState: CvcState,
    onValueChanged: (String) -> Unit
) {
    val backgroundColor = if (isSystemInDarkTheme()) {
        Color.White.copy(alpha = 0.075f)
    } else {
        Color.Black.copy(alpha = 0.075f)
    }

    val focusRequester = remember {
        FocusRequester()
    }

    if (!LocalInspectionMode.current) {
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
    }

    return SectionCard {
        Row(
            Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            Box(
                modifier = Modifier
                    .weight(.5f, true)
                    .fillMaxSize()
                    .background(backgroundColor)
            ) {
                Row(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = cvcState.cardBrand.icon),
                        contentDescription = "",
                    )
                    Text(
                        text = stringResource(
                            R.string.stripe_paymentsheet_payment_method_item_card_number,
                            lastFour
                        ),
                        style = MaterialTheme.typography.body1,
                        modifier = Modifier.testTag(TEST_TAG_CVC_LAST_FOUR)
                    )
                }
            }
            Divider(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight(),
                color = MaterialTheme.stripeColors.componentBorder
            )
            TextField(
                modifier = Modifier
                    .autofill(
                        types = listOf(AutofillType.CreditCardSecurityCode),
                        onFill = {
                            onValueChanged(it)
                        }
                    )
                    .fillMaxWidth()
                    .weight(.5f, true)
                    .focusRequester(focusRequester)
                    .testTag(TEST_TAG_CVC_FIELD),
                enabled = enabled,
                value = cvcState.cvc,
                onValueChange = onValueChanged,
                shape = MaterialTheme.shapes.large,
                colors = TextFieldColors(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.NumberPassword
                ),
                singleLine = true,
                label = {
                    Placeholder(
                        text = stringResource(id = cvcState.label),
                        modifier = Modifier.testTag(TEST_TAG_CVC_LABEL)
                    )
                },
                trailingIcon = {
                    TrailingIcon(
                        trailingIcon = cvcState.cvcIcon,
                        loading = false
                    )
                }
            )
        }
    }
}

@Composable
private fun CvcRecollectionTopBar(
    isTestMode: Boolean,
    onClosePressed: () -> Unit
) {
    Row(
        modifier = Modifier
            .padding(0.dp, 16.dp, 0.dp, 0.dp)
            .height(32.dp)
    ) {
        if (isTestMode) {
            TestModeBadge()
        }
        Spacer(modifier = Modifier.weight(1f))
        IconButton(
            onClick = { onClosePressed.invoke() },
            Modifier.offset(16.dp, -8.dp)
        ) {
            Icon(painterResource(id = R.drawable.stripe_ic_paymentsheet_close), contentDescription = null)
        }
    }
}

@Composable
private fun CvcRecollectionTitle() {
    H4Text(
        text = stringResource(R.string.stripe_paymentsheet_confirm_your_cvc),
        modifier = Modifier
            .padding(0.dp, 0.dp, 0.dp, 16.dp)
            .testTag(TEST_TAG_CONFIRM_CVC)
    )
}

@Composable
private fun CvcRecollectionButton(
    isComplete: Boolean,
    onConfirmPressed: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth(1f)
            .padding(0.dp, 32.dp, 0.dp, 20.dp)
            .testTag(CVC_RECOLLECTION_SCREEN_CONFIRM),
        contentAlignment = Alignment.Center
    ) {
        PrimaryButton(
            label = stringResource(R.string.stripe_paymentsheet_confirm),
            locked = false,
            enabled = isComplete
        ) {
            onConfirmPressed.invoke()
        }
    }
}

@Composable
@Preview
private fun CvcRecollectionFieldPreview() {
    StripeTheme {
        CvcRecollectionScreen(
            lastFour = "4242",
            isTestMode = false,
            cvcState = CvcState(
                cvc = "",
                cardBrand = CardBrand.Visa
            ),
            viewActionHandler = { }
        )
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
const val CVC_RECOLLECTION_SCREEN_CONFIRM = "CVC_CONFIRM"

internal const val TEST_TAG_CONFIRM_CVC = "TEST_TAG_CONFIRM_CVC"
internal const val TEST_TAG_CVC_FIELD = "TEST_TAG_CVC_FIELD"
internal const val TEST_TAG_CVC_LAST_FOUR = "TEST_TAG_CVC_LAST_FOUR"
internal const val TEST_TAG_CVC_LABEL = "TEST_TAG_CVC_LABEL"
