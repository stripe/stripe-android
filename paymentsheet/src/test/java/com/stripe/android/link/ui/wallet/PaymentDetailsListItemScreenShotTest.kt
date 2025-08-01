package com.stripe.android.link.ui.wallet

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.core.model.CountryCode
import com.stripe.android.link.theme.DefaultLinkTheme
import com.stripe.android.model.CardBrand
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.CvcCheck
import com.stripe.android.screenshottesting.FontSize
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.SystemAppearance
import org.junit.Rule
import org.junit.Test

internal class PaymentDetailsListItemScreenShotTest {
    @get:Rule
    val paparazziRule = PaparazziRule(
        SystemAppearance.entries,
        FontSize.entries,
        boxModifier = Modifier
            .padding(0.dp)
            .fillMaxWidth(),
    )

    @Test
    fun testCardEnabled() {
        snapshot(
            state = State(
                details = ConsumerPaymentDetails.Card(
                    id = "QAAAKJ6",
                    expiryYear = 2023,
                    expiryMonth = 12,
                    isDefault = true,
                    brand = CardBrand.MasterCard,
                    last4 = "4444",
                    cvcCheck = CvcCheck.Pass,
                    networks = emptyList(),
                    funding = "CREDIT",
                    nickname = null,
                    billingAddress = ConsumerPaymentDetails.BillingAddress(
                        name = null,
                        line1 = null,
                        line2 = null,
                        locality = null,
                        administrativeArea = null,
                        countryCode = CountryCode.US,
                        postalCode = "12312"
                    )
                ),
                enabled = true,
                isSelected = false,
                isAvailable = true,
                isUpdating = false
            )
        )
    }

    @Test
    fun testCardEnabledAndSelected() {
        snapshot(
            state = State(
                details = ConsumerPaymentDetails.Card(
                    id = "QAAAKJ6",
                    expiryYear = 2023,
                    expiryMonth = 12,
                    isDefault = true,
                    brand = CardBrand.MasterCard,
                    last4 = "4444",
                    cvcCheck = CvcCheck.Pass,
                    networks = emptyList(),
                    funding = "CREDIT",
                    nickname = null,
                    billingAddress = ConsumerPaymentDetails.BillingAddress(
                        name = null,
                        line1 = null,
                        line2 = null,
                        locality = null,
                        administrativeArea = null,
                        countryCode = CountryCode.US,
                        postalCode = "12312"
                    )
                ),
                enabled = true,
                isSelected = true,
                isAvailable = true,
                isUpdating = false
            )
        )
    }

    @Test
    fun testCardEnabledAndUpdating() {
        snapshot(
            state = State(
                details = ConsumerPaymentDetails.Card(
                    id = "QAAAKJ6",
                    expiryYear = 2023,
                    expiryMonth = 12,
                    isDefault = true,
                    brand = CardBrand.MasterCard,
                    last4 = "4444",
                    cvcCheck = CvcCheck.Pass,
                    networks = emptyList(),
                    funding = "CREDIT",
                    nickname = "My Personal Card",
                    billingAddress = ConsumerPaymentDetails.BillingAddress(
                        name = null,
                        line1 = null,
                        line2 = null,
                        locality = null,
                        administrativeArea = null,
                        countryCode = CountryCode.US,
                        postalCode = "12312"
                    )
                ),
                enabled = true,
                isSelected = false,
                isAvailable = true,
                isUpdating = true
            )
        )
    }

    @Test
    fun testCardDisabledAndSelected() {
        snapshot(
            state = State(
                details = ConsumerPaymentDetails.Card(
                    id = "QAAAKJ6",
                    expiryYear = 2023,
                    expiryMonth = 12,
                    isDefault = true,
                    brand = CardBrand.MasterCard,
                    last4 = "4444",
                    cvcCheck = CvcCheck.Pass,
                    networks = emptyList(),
                    funding = "CREDIT",
                    nickname = null,
                    billingAddress = ConsumerPaymentDetails.BillingAddress(
                        name = null,
                        line1 = null,
                        line2 = null,
                        locality = null,
                        administrativeArea = null,
                        countryCode = CountryCode.US,
                        postalCode = "12312"
                    )
                ),
                enabled = false,
                isSelected = true,
                isAvailable = true,
                isUpdating = false
            )
        )
    }

    @Test
    fun testCardEnabledAndUnavailable() {
        snapshot(
            state = State(
                details = ConsumerPaymentDetails.Card(
                    id = "QAAAKJ6",
                    expiryYear = 2023,
                    expiryMonth = 12,
                    isDefault = true,
                    brand = CardBrand.MasterCard,
                    last4 = "4444",
                    cvcCheck = CvcCheck.Pass,
                    networks = emptyList(),
                    funding = "CREDIT",
                    nickname = null,
                    billingAddress = ConsumerPaymentDetails.BillingAddress(
                        name = null,
                        line1 = null,
                        line2 = null,
                        locality = null,
                        administrativeArea = null,
                        countryCode = CountryCode.US,
                        postalCode = "12312"
                    )
                ),
                enabled = true,
                isSelected = true,
                isAvailable = false,
                isUpdating = false
            )
        )
    }

    @Test
    fun testBankAccountEnabled() {
        snapshot(
            state = State(
                details = ConsumerPaymentDetails.BankAccount(
                    id = "wAAACGA",
                    last4 = "6789",
                    bankName = "STRIPE TEST BANK",
                    bankIconCode = null,
                    isDefault = false,
                    nickname = null,
                    billingAddress = null,
                    billingEmailAddress = null,
                ),
                enabled = true,
                isSelected = false,
                isAvailable = true,
                isUpdating = false
            )
        )
    }

    @Test
    fun testBankAccountLiveModeEnabled() {
        snapshot(
            state = State(
                details = ConsumerPaymentDetails.BankAccount(
                    id = "wAAACGA",
                    last4 = "6789",
                    bankName = "Chase",
                    bankIconCode = "morganchase",
                    isDefault = false,
                    nickname = null,
                    billingAddress = null,
                    billingEmailAddress = null,
                ),
                enabled = true,
                isSelected = false,
                isAvailable = true,
                isUpdating = false
            )
        )
    }

    @Test
    fun testPassThroughEnabled() {
        snapshot(
            state = State(
                details = ConsumerPaymentDetails.Passthrough(
                    id = "csmrpd_wAAACGA",
                    last4 = "6789",
                    paymentMethodId = "pm_123",
                ),
                enabled = true,
                isSelected = false,
                isAvailable = true,
                isUpdating = false
            )
        )
    }

    private fun snapshot(state: State) {
        paparazziRule.snapshot {
            DefaultLinkTheme {
                PaymentDetailsListItem(
                    paymentDetails = state.details,
                    isClickable = state.enabled,
                    isMenuButtonClickable = true,
                    isAvailable = state.isAvailable,
                    isSelected = state.isSelected,
                    isUpdating = state.isUpdating,
                    onClick = {},
                    onMenuButtonClick = {}
                )
            }
        }
    }

    internal data class State(
        val details: ConsumerPaymentDetails.PaymentDetails,
        val enabled: Boolean,
        val isSelected: Boolean,
        val isAvailable: Boolean,
        val isUpdating: Boolean,
    )
}
