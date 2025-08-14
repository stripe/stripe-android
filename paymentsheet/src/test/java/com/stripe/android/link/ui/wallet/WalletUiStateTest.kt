package com.stripe.android.link.ui.wallet

import com.google.common.truth.Truth.assertThat
import com.stripe.android.CardBrandFilter
import com.stripe.android.core.model.CountryCode
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.TestFactory
import com.stripe.android.link.TestFactory.LINK_WALLET_PRIMARY_BUTTON_LABEL
import com.stripe.android.link.TestFactory.LINK_WALLET_SECONDARY_BUTTON_LABEL
import com.stripe.android.link.ui.PrimaryButtonState
import com.stripe.android.model.CardBrand
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.CvcCheck
import com.stripe.android.paymentelement.AllowedBillingCountriesInPaymentElementPreview
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.R
import com.stripe.android.uicore.forms.FormFieldEntry
import org.junit.Test

class WalletUiStateTest {

    @Test
    fun testCompletedButtonState() {
        val state = walletUiState(
            selectedItem = TestFactory.CONSUMER_PAYMENT_DETAILS.paymentDetails.firstOrNull(),
            hasCompleted = true,
        )

        assertThat(state.primaryButtonState).isEqualTo(PrimaryButtonState.Completed)
    }

    @Test
    fun testProcessingButtonState() {
        val state = walletUiState(
            selectedItem = TestFactory.CONSUMER_PAYMENT_DETAILS.paymentDetails.firstOrNull(),
            isProcessing = true
        )

        assertThat(state.primaryButtonState).isEqualTo(PrimaryButtonState.Processing)
    }

    @Test
    fun testDisabledButtonStateForExpiredCard() {
        val selectedItem = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD.copy(expiryYear = 1900)

        val state = walletUiState(
            paymentDetailsList = listOf(selectedItem),
            selectedItem = selectedItem,
        )

        assertThat(state.primaryButtonState).isEqualTo(PrimaryButtonState.Disabled)
    }

    @Test
    fun testDisabledButtonStateForCvcRecollection() {
        val selectedItem = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD.copy(cvcCheck = CvcCheck.Unchecked)

        val state = walletUiState(
            paymentDetailsList = listOf(selectedItem),
            selectedItem = selectedItem,
        )

        assertThat(state.primaryButtonState).isEqualTo(PrimaryButtonState.Disabled)
    }

    @Test
    fun testDisabledButtonStateWhenAddingBankAccount() {
        val state = walletUiState(addBankAccountState = AddBankAccountState.Processing())

        assertThat(state.primaryButtonState).isEqualTo(PrimaryButtonState.Disabled)
    }

    @Test
    fun testEnabledButtonState() {
        val state = walletUiState(
            selectedItem = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD.copy(expiryYear = 2099),
        )

        assertThat(state.primaryButtonState).isEqualTo(PrimaryButtonState.Enabled)
    }

    @Test
    fun testShowTermsForSelectedBankPaymentMethodIfNotReusing() {
        val state = walletUiState(
            selectedItem = TestFactory.CONSUMER_PAYMENT_DETAILS_BANK_ACCOUNT
        )

        assertThat(state.mandate).isEqualTo(
            resolvableString(R.string.stripe_wallet_bank_account_terms)
        )
    }

    @Test
    fun testShowTermsForSelectedBankPaymentMethodIfReusing() {
        val state = walletUiState(
            selectedItem = TestFactory.CONSUMER_PAYMENT_DETAILS_BANK_ACCOUNT,
            isSettingUp = true,
        )

        assertThat(state.mandate).isEqualTo(
            resolvableString(R.string.stripe_wallet_bank_account_terms)
        )
    }

    @Test
    fun testShowNoTermsForSelectedCardIfNotReusing() {
        val state = walletUiState(isSettingUp = false)

        assertThat(state.mandate).isNull()
    }

    @Test
    fun testShowTermsForSelectedCardIfReusing() {
        val state = walletUiState(isSettingUp = true)

        assertThat(state.mandate).isEqualTo(
            resolvableString(R.string.stripe_paymentsheet_card_mandate, "Example Inc.")
        )
    }

    @Test
    fun testDisabledButtonStateForExpiredCardWithIncompleteExpiryDate() {
        val selectedItem = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD.copy(expiryYear = 1900)

        val state = walletUiState(
            paymentDetailsList = listOf(selectedItem),
            selectedItem = selectedItem,
            expiryDateInput = FormFieldEntry("", isComplete = false)
        )

        assertThat(state.primaryButtonState).isEqualTo(PrimaryButtonState.Disabled)
    }

    @Test
    fun testEnabledButtonStateForExpiredCardWithCompleteExpiryDateAndIncompleteCvc() {
        val selectedItem = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD.copy(expiryYear = 1900)

        val state = walletUiState(
            paymentDetailsList = listOf(selectedItem),
            selectedItem = selectedItem,
            expiryDateInput = FormFieldEntry("12/25", isComplete = true),
        )

        assertThat(state.primaryButtonState).isEqualTo(PrimaryButtonState.Disabled)
    }

    @Test
    fun testEnabledButtonStateForExpiredCardWithCompleteExpiryDate() {
        val state = walletUiState(
            selectedItem = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD.copy(expiryYear = 1900),
            expiryDateInput = FormFieldEntry("12/25", isComplete = true),
            cvcInput = FormFieldEntry("123", isComplete = true)
        )

        assertThat(state.primaryButtonState).isEqualTo(PrimaryButtonState.Enabled)
    }

    @Test
    fun testDisabledButtonStateForCardRequiringCvcWithIncompleteCvc() {
        val selectedItem = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD.copy(
            cvcCheck = CvcCheck.Unchecked
        )
        val state = walletUiState(
            paymentDetailsList = listOf(selectedItem),
            selectedItem = selectedItem,
            cvcInput = FormFieldEntry("12", isComplete = false)
        )

        assertThat(state.primaryButtonState).isEqualTo(PrimaryButtonState.Disabled)
    }

    @Test
    fun testEnabledButtonStateForCardRequiringCvcWithCompleteCvc() {
        val state = walletUiState(
            selectedItem = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD.copy(
                cvcCheck = CvcCheck.Unchecked
            ),
            cvcInput = FormFieldEntry("123", isComplete = true)
        )

        assertThat(state.primaryButtonState).isEqualTo(PrimaryButtonState.Enabled)
    }

    @Test
    fun testEnabledButtonStateForValidCardWithBothInputsComplete() {
        val state = walletUiState(
            selectedItem = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD.copy(expiryYear = 2099),
            expiryDateInput = FormFieldEntry("12/25", isComplete = true),
            cvcInput = FormFieldEntry("123", isComplete = true)
        )

        assertThat(state.primaryButtonState).isEqualTo(PrimaryButtonState.Enabled)
    }

    @Test
    fun testDisabledButtonStateWhenCardIsBeingUpdated() {
        val state = walletUiState(
            selectedItem = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD,
            cardBeingUpdated = "id"
        )

        assertThat(state.primaryButtonState).isEqualTo(PrimaryButtonState.Disabled)
    }

    @Test
    fun testPaymentMethodAvailability() {
        val paymentDetailsList = listOf(
            TestFactory.CONSUMER_PAYMENT_DETAILS_CARD,
            TestFactory.CONSUMER_PAYMENT_DETAILS_BANK_ACCOUNT,
            TestFactory.CONSUMER_PAYMENT_DETAILS_CARD.copy(
                brand = CardBrand.MasterCard,
                last4 = "1234"
            )
        )
        val state = walletUiState(
            paymentDetailsList = paymentDetailsList,
            selectedItem = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD,
            cardBrandFilter = RejectCardBrands(CardBrand.Visa)
        )

        assertThat(state.isItemAvailable(paymentDetailsList[0])).isFalse()
        assertThat(state.isItemAvailable(paymentDetailsList[1])).isTrue()
        assertThat(state.isItemAvailable(paymentDetailsList[2])).isTrue()
    }

    @OptIn(AllowedBillingCountriesInPaymentElementPreview::class)
    @Test
    fun testPaymentMethodAvailabilityForAllAllowedBillingCountries() {
        val paymentDetailsList = createPaymentMethodsWithDifferentBillingDetails()

        val state = walletUiState(
            paymentDetailsList = paymentDetailsList,
            selectedItem = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD,
            billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                allowedCountries = emptySet(),
            )
        )

        assertThat(state.isItemAvailable(paymentDetailsList[0])).isTrue()
        assertThat(state.isItemAvailable(paymentDetailsList[1])).isTrue()
        assertThat(state.isItemAvailable(paymentDetailsList[2])).isTrue()
        assertThat(state.isItemAvailable(paymentDetailsList[3])).isTrue()
        assertThat(state.isItemAvailable(paymentDetailsList[4])).isTrue()
        assertThat(state.isItemAvailable(paymentDetailsList[5])).isTrue()
    }

    @OptIn(AllowedBillingCountriesInPaymentElementPreview::class)
    @Test
    fun testPaymentMethodAvailabilityForLimitedAllowedBillingCountries() {
        val paymentDetailsList = createPaymentMethodsWithDifferentBillingDetails()

        val state = walletUiState(
            paymentDetailsList = paymentDetailsList,
            selectedItem = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD,
            billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                allowedCountries = setOf("CA", "GB"),
            )
        )

        assertThat(state.isItemAvailable(paymentDetailsList[0])).isFalse()
        assertThat(state.isItemAvailable(paymentDetailsList[1])).isFalse()
        assertThat(state.isItemAvailable(paymentDetailsList[2])).isFalse()
        assertThat(state.isItemAvailable(paymentDetailsList[3])).isFalse()
        assertThat(state.isItemAvailable(paymentDetailsList[4])).isTrue()
        assertThat(state.isItemAvailable(paymentDetailsList[5])).isTrue()
    }

    @Test
    fun testIsExpanded() {
        val state = walletUiState(
            paymentDetailsList = listOf(TestFactory.CONSUMER_PAYMENT_DETAILS_CARD),
            selectedItem = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD,
        )
        val noVisa = RejectCardBrands(CardBrand.Visa)

        // Collapsed when there's a valid selection.
        assertThat(state.isExpanded).isFalse()

        // Expanded when there's no valid selection.
        assertThat(state.copy(cardBrandFilter = noVisa).isExpanded).isTrue()

        // User interaction is respected.
        assertThat(state.copy(userSetIsExpanded = false, cardBrandFilter = noVisa).isExpanded).isFalse()
    }

    private fun walletUiState(
        paymentDetailsList: List<ConsumerPaymentDetails.PaymentDetails> =
            TestFactory.CONSUMER_PAYMENT_DETAILS.paymentDetails,
        cardBrandFilter: CardBrandFilter = RejectCardBrands(),
        selectedItem: ConsumerPaymentDetails.PaymentDetails? = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD,
        hasCompleted: Boolean = false,
        isProcessing: Boolean = false,
        primaryButtonLabel: ResolvableString = LINK_WALLET_PRIMARY_BUTTON_LABEL,
        secondaryButtonLabel: ResolvableString = LINK_WALLET_SECONDARY_BUTTON_LABEL,
        expiryDateInput: FormFieldEntry = FormFieldEntry(null),
        cvcInput: FormFieldEntry = FormFieldEntry(null),
        addBankAccountState: AddBankAccountState = AddBankAccountState.Idle,
        addPaymentMethodOptions: List<AddPaymentMethodOption> = listOf(AddPaymentMethodOption.Card),
        cardBeingUpdated: String? = null,
        isSettingUp: Boolean = false,
        merchantName: String = "Example Inc.",
        collectMissingBillingDetailsForExistingPaymentMethods: Boolean = true,
        signupToggleEnabled: Boolean = false,
        billingDetailsCollectionConfiguration: PaymentSheet.BillingDetailsCollectionConfiguration =
            PaymentSheet.BillingDetailsCollectionConfiguration(),
    ): WalletUiState {
        return WalletUiState(
            paymentDetailsList = paymentDetailsList,
            email = "email@stripe.com",
            selectedItemId = selectedItem?.id,
            cardBrandFilter = cardBrandFilter,
            hasCompleted = hasCompleted,
            isProcessing = isProcessing,
            primaryButtonLabel = primaryButtonLabel,
            secondaryButtonLabel = secondaryButtonLabel,
            expiryDateInput = expiryDateInput,
            cvcInput = cvcInput,
            addBankAccountState = addBankAccountState,
            addPaymentMethodOptions = addPaymentMethodOptions,
            cardBeingUpdated = cardBeingUpdated,
            isSettingUp = isSettingUp,
            merchantName = merchantName,
            collectMissingBillingDetailsForExistingPaymentMethods =
            collectMissingBillingDetailsForExistingPaymentMethods,
            signupToggleEnabled = signupToggleEnabled,
            billingDetailsCollectionConfiguration = billingDetailsCollectionConfiguration,
        )
    }

    private fun createPaymentMethodsWithDifferentBillingDetails() = listOf(
        TestFactory.CONSUMER_PAYMENT_DETAILS_CARD.copy(
            id = "pm_1",
            last4 = "1235",
            billingAddress = null,
        ),
        TestFactory.CONSUMER_PAYMENT_DETAILS_CARD.copy(
            id = "pm_2",
            last4 = "4235",
            billingAddress = createBillingAddress(
                countryCode = CountryCode.US,
            ),
        ),
        TestFactory.CONSUMER_PAYMENT_DETAILS_BANK_ACCOUNT.copy(
            id = "pm_3",
            last4 = "9382",
            billingAddress = createBillingAddress(
                countryCode = CountryCode.US,
            ),
        ),
        TestFactory.CONSUMER_PAYMENT_DETAILS_PASSTHROUGH.copy(
            last4 = "1234",
            billingAddress = createBillingAddress(
                countryCode = CountryCode.US,
            ),
        ),
        TestFactory.CONSUMER_PAYMENT_DETAILS_CARD.copy(
            brand = CardBrand.MasterCard,
            last4 = "8902",
            billingAddress = createBillingAddress(
                countryCode = CountryCode.CA,
            ),
        ),
        TestFactory.CONSUMER_PAYMENT_DETAILS_CARD.copy(
            brand = CardBrand.MasterCard,
            last4 = "8902",
            billingAddress = createBillingAddress(
                countryCode = CountryCode.GB,
            ),
        )
    )

    private fun createBillingAddress(
        countryCode: CountryCode,
    ) = ConsumerPaymentDetails.BillingAddress(
        name = null,
        line1 = null,
        line2 = null,
        locality = null,
        administrativeArea = null,
        countryCode = countryCode,
        postalCode = null,
    )
}
