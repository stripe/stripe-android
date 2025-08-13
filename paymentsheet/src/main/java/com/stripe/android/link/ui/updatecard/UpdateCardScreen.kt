package com.stripe.android.link.ui.updatecard

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stripe.android.DefaultCardBrandFilter
import com.stripe.android.core.model.CountryCode
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.model.LinkAppearance
import com.stripe.android.link.theme.DefaultLinkTheme
import com.stripe.android.link.theme.LinkAppearanceTheme
import com.stripe.android.link.theme.LinkTheme
import com.stripe.android.link.theme.StripeThemeForLink
import com.stripe.android.link.ui.ErrorText
import com.stripe.android.link.ui.LinkLoadingScreen
import com.stripe.android.link.ui.PrimaryButton
import com.stripe.android.link.ui.ScrollableTopLevelColumn
import com.stripe.android.model.CardBrand
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.CvcCheck
import com.stripe.android.paymentsheet.PaymentSheet.BillingDetailsCollectionConfiguration
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.ui.CardDetailsEditUI
import com.stripe.android.paymentsheet.ui.CardEditConfiguration
import com.stripe.android.paymentsheet.ui.DefaultEditCardDetailsInteractor
import com.stripe.android.paymentsheet.ui.EditCardDetailsInteractor
import com.stripe.android.paymentsheet.ui.EditCardPayload
import com.stripe.android.uicore.strings.resolve
import com.stripe.android.uicore.utils.collectAsState

@Composable
internal fun UpdateCardScreen(
    viewModel: UpdateCardScreenViewModel,
    appearance: LinkAppearance?
) {
    val state by viewModel.state.collectAsState()
    when (val interactor = viewModel.interactor) {
        null -> LinkLoadingScreen()
        else -> UpdateCardScreenBody(
            interactor = interactor,
            state = state,
            appearance = appearance,
            onUpdateClicked = viewModel::onUpdateClicked,
        )
    }
}

@Composable
internal fun UpdateCardScreenBody(
    interactor: EditCardDetailsInteractor,
    state: UpdateCardScreenState,
    appearance: LinkAppearance?,
    onUpdateClicked: () -> Unit,
) {
    val focusManager = LocalFocusManager.current

    ScrollableTopLevelColumn {
        StripeThemeForLink(appearance = appearance) {
            CardDetailsEditUI(
                editCardDetailsInteractor = interactor,
            )
        }

        if (state.shouldShowDefaultTag) {
            Text(
                modifier = Modifier.padding(top = 8.dp),
                text = stringResource(R.string.stripe_link_update_card_default_card),
                style = LinkTheme.typography.bodyEmphasized,
                color = LinkTheme.colors.textSecondary,
            )
        }

        state.error?.let {
            ErrorText(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                text = it.resolve(),
            )
        }

        LinkAppearanceTheme(appearance = appearance) {
            PrimaryButton(
                modifier = Modifier.padding(vertical = 16.dp),
                label = state.primaryButtonLabel.resolve(),
                state = state.primaryButtonState,
                onButtonClick = {
                    focusManager.clearFocus()
                    onUpdateClicked()
                }
            )
        }
    }
}

@Preview
@Composable
internal fun UpdateCardScreenBodyPreview() {
    DefaultLinkTheme(darkTheme = false) {
        Surface(
            color = LinkTheme.colors.surfacePrimary
        ) {
            UpdateCardScreenBody(
                interactor = DefaultEditCardDetailsInteractor.Factory().create(
                    coroutineScope = rememberCoroutineScope(),
                    cardEditConfiguration = CardEditConfiguration(
                        cardBrandFilter = DefaultCardBrandFilter,
                        isCbcModifiable = false,
                        areExpiryDateAndAddressModificationSupported = true,
                    ),
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
                    billingDetailsCollectionConfiguration = BillingDetailsCollectionConfiguration(),
                    requiresModification = true
                ),
                state = UpdateCardScreenState(
                    paymentDetailsId = "card_id_1234",
                    billingDetailsUpdateFlow = null,
                    primaryButtonLabel = R.string.stripe_link_update_card_confirm_cta.resolvableString,
                    isDefault = false,
                    cardUpdateParams = null,
                    preferredCardBrand = null,
                    error = "Random error.".resolvableString,
                    processing = false,
                ),
                onUpdateClicked = {},
                appearance = null,
            )
        }
    }
}
