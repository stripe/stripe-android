package com.stripe.android.link.ui.updatecard

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stripe.android.DefaultCardBrandFilter
import com.stripe.android.core.model.CountryCode
import com.stripe.android.link.theme.DefaultLinkTheme
import com.stripe.android.link.theme.LinkTheme
import com.stripe.android.link.theme.StripeThemeForLink
import com.stripe.android.link.ui.ErrorText
import com.stripe.android.link.ui.Loader
import com.stripe.android.link.ui.PrimaryButton
import com.stripe.android.link.ui.ScrollableTopLevelColumn
import com.stripe.android.link.ui.SecondaryButton
import com.stripe.android.model.CardBrand
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.CvcCheck
import com.stripe.android.paymentsheet.PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.ui.CardDetailsEditUI
import com.stripe.android.paymentsheet.ui.DefaultEditCardDetailsInteractor
import com.stripe.android.paymentsheet.ui.EditCardDetailsInteractor
import com.stripe.android.paymentsheet.ui.EditCardPayload
import com.stripe.android.uicore.strings.resolve
import com.stripe.android.uicore.utils.collectAsState
import com.stripe.android.R as StripeR

@Composable
internal fun UpdateCardScreen(viewModel: UpdateCardScreenViewModel) {
    val state by viewModel.state.collectAsState()
    when (val interactor = viewModel.interactor) {
        null -> Loader()
        else -> UpdateCardScreenBody(
            interactor = interactor,
            state = state,
            onUpdateClicked = viewModel::onUpdateClicked,
            onCancelClicked = viewModel::onCancelClicked,
        )
    }
}

@Composable
internal fun UpdateCardScreenBody(
    interactor: EditCardDetailsInteractor,
    state: UpdateCardScreenState,
    onUpdateClicked: () -> Unit,
    onCancelClicked: () -> Unit,
) {
    ScrollableTopLevelColumn {
        Text(
            modifier = Modifier
                .padding(bottom = 32.dp),
            text = stringResource(R.string.stripe_link_update_card_title),
            style = LinkTheme.typography.title,
            color = LinkTheme.colors.textPrimary,
        )

        StripeThemeForLink {
            CardDetailsEditUI(
                editCardDetailsInteractor = interactor,
            )
        }

        if (state.isDefault) {
            Text(
                modifier = Modifier.padding(top = 8.dp),
                text = stringResource(R.string.stripe_link_update_card_default_card),
                style = LinkTheme.typography.bodyEmphasized,
                color = LinkTheme.colors.textSecondary,
            )
        }

        state.errorMessage?.let {
            ErrorText(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                text = it.resolve(),
            )
        }

        PrimaryButton(
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
            label = stringResource(R.string.stripe_link_update_card_confirm_cta),
            state = state.primaryButtonState,
            onButtonClick = onUpdateClicked
        )

        SecondaryButton(
            label = stringResource(StripeR.string.stripe_cancel),
            enabled = state.processing.not(),
            onClick = onCancelClicked
        )
    }
}

@Preview
@Composable
internal fun UpdateCardScreenBodyPreview() {
    DefaultLinkTheme(darkTheme = true) {
        Surface(
            color = LinkTheme.colors.surfacePrimary
        ) {
            UpdateCardScreenBody(
                interactor = DefaultEditCardDetailsInteractor.Factory().create(
                    coroutineScope = rememberCoroutineScope(),
                    isCbcModifiable = false,
                    areExpiryDateAndAddressModificationSupported = true,
                    cardBrandFilter = DefaultCardBrandFilter,
                    payload = EditCardPayload.create(
                        ConsumerPaymentDetails.Card(
                            id = "card_id_1234",
                            last4 = "4242",
                            expiryYear = 2500,
                            expiryMonth = 4,
                            brand = CardBrand.Visa,
                            cvcCheck = CvcCheck.Pass,
                            isDefault = false,
                            networks = listOf("VISA"),
                            nickname = "Fancy Card",
                            funding = "credit",
                            billingAddress = ConsumerPaymentDetails.BillingAddress(
                                name = null,
                                line1 = null,
                                line2 = null,
                                locality = null,
                                administrativeArea = null,
                                countryCode = CountryCode.US,
                                postalCode = "42424"
                            )
                        ),
                        billingPhoneNumber = null
                    ),
                    onBrandChoiceChanged = {},
                    onCardUpdateParamsChanged = {},
                    addressCollectionMode = AddressCollectionMode.Automatic
                ),
                state = UpdateCardScreenState(
                    paymentDetailsId = "card_id_1234",
                    isDefault = false,
                    cardUpdateParams = null,
                    preferredCardBrand = null,
                    error = IllegalArgumentException("Random error."),
                    processing = false,
                ),
                onUpdateClicked = {},
                onCancelClicked = {},
            )
        }
    }
}
