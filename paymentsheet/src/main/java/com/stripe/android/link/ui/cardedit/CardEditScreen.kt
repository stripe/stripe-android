package com.stripe.android.link.ui.cardedit

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.theme.StripeThemeForLink
import com.stripe.android.link.ui.ErrorText
import com.stripe.android.link.ui.PrimaryButton
import com.stripe.android.link.ui.ScrollableTopLevelColumn
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.ui.CardNumberField
import com.stripe.android.paymentsheet.ui.PaymentMethodIconFromResource
import com.stripe.android.ui.core.elements.CardBillingAddressElement
import com.stripe.android.uicore.LocalShapes
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.RowElement
import com.stripe.android.uicore.elements.SectionElement
import com.stripe.android.uicore.elements.SectionElementUI
import com.stripe.android.uicore.elements.menu.Checkbox
import com.stripe.android.uicore.utils.collectAsState

@Composable
internal fun CardEditScreen(viewModel: CardEditViewModel) {
    val state by viewModel.cardEditState.collectAsState()

    CardEditBody(
        state = state,
        cvcExpiryElement = viewModel.cvcExpiryElement,
        cardBillingAddressElement = viewModel.cardBillingAddressElement,
        onSetAsDefaultChanged = viewModel::defaultValueChanged,
        onUpdateClicked = viewModel::updateCardClicked
    )
}

@Composable
private fun CardEditBody(
    state: CardEditState,
    cvcExpiryElement: RowElement,
    cardBillingAddressElement: CardBillingAddressElement,
    onSetAsDefaultChanged: (Boolean) -> Unit,
    onUpdateClicked: () -> Unit
) {
    val context = LocalContext.current
    ScrollableTopLevelColumn {
        if (state.paymentDetail == null) {
            CircularProgressIndicator()
            return@ScrollableTopLevelColumn
        }

        StripeThemeForLink {
            Text(
                text = R.string.stripe_wallet_update_card.resolvableString.resolve(context),
                style = MaterialTheme.typography.h2
            )

            Spacer(Modifier.height(32.dp))

            CardSection(
                card = state.paymentDetail,
                cardDefaultState = state.cardDefaultState,
                cvcExpiryElement = cvcExpiryElement,
                cardBillingAddressElement = cardBillingAddressElement,
                onSetAsDefaultChanged = onSetAsDefaultChanged
            )

            Spacer(Modifier.height(32.dp))

            AnimatedVisibility(state.errorMessage != null) {
                ErrorText(
                    text = state.errorMessage?.resolve(context).orEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                )
            }

            PrimaryButton(
                label = R.string.stripe_wallet_update_card.resolvableString.resolve(context),
                state = state.primaryButtonState,
                onButtonClick = onUpdateClicked
            )
        }
    }
}

@Composable
private fun CardSection(
    card: ConsumerPaymentDetails.Card,
    cardDefaultState: CardDefaultState?,
    cvcExpiryElement: RowElement,
    cardBillingAddressElement: CardBillingAddressElement,
    onSetAsDefaultChanged: (Boolean) -> Unit,
) {
    Column {
        CardNumberField(
            last4 = card.last4,
            shape = LocalShapes.current.roundedCornerShape
        ) {
            PaymentMethodIconFromResource(
                iconRes = card.brand.icon,
                colorFilter = null,
                alignment = Alignment.Center,
                modifier = Modifier,
            )
        }

        Spacer(Modifier.height(8.dp))

        SectionElementUI(
            modifier = Modifier,
            enabled = true,
            element = SectionElement.wrap(cvcExpiryElement),
            hiddenIdentifiers = emptySet(),
            lastTextFieldIdentifier = cvcExpiryElement.fields.last().identifier
        )

        Spacer(Modifier.height(32.dp))

        SectionElementUI(
            enabled = true,
            element = SectionElement.wrap(cardBillingAddressElement),
            hiddenIdentifiers = setOf(
                IdentifierSpec.Line1,
                IdentifierSpec.Line2,
                IdentifierSpec.City,
                IdentifierSpec.State,
            ),
            lastTextFieldIdentifier = null
        )

        CardCheckBox(
            state = cardDefaultState,
            onSetAsDefaultChanged = onSetAsDefaultChanged
        )
    }
}

@Composable
private fun CardCheckBox(
    state: CardDefaultState?,
    onSetAsDefaultChanged: (Boolean) -> Unit
) {
    when (state) {
        CardDefaultState.CardIsDefault -> {
            Text("Set As Default")
        }
        is CardDefaultState.Value -> {
            Row(
                modifier = Modifier
                    .padding(
                        top = 8.dp
                    )
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = state.enabled,
                    onCheckedChange = onSetAsDefaultChanged,
                )

                Text("Set As Default")
            }
        }
        null -> Unit
    }
}
