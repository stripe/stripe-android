package com.stripe.android.link.ui.wallet

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.core.model.CountryCode
import com.stripe.android.model.CardBrand
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.CvcCheck
import com.stripe.android.screenshottesting.FontSize
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.SystemAppearance
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
internal class PaymentDetailsListItemScreenShotTest(
    private val testCase: TestCase
) {
    @get:Rule
    val paparazziRule = PaparazziRule(
        SystemAppearance.entries,
        FontSize.entries,
        boxModifier = Modifier
            .padding(0.dp)
            .fillMaxWidth(),
    )

    @Test
    fun test() {
        paparazziRule.snapshot {
            PaymentDetailsListItem(
                paymentDetails = testCase.state.details,
                enabled = testCase.state.enabled,
                isSelected = testCase.state.isSelected,
                isUpdating = testCase.state.isUpdating,
                onClick = {},
                onMenuButtonClick = {}
            )
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data(): List<TestCase> {
            return listOf(
                TestCase(
                    name = "CardEnabled",
                    state = State(
                        details = ConsumerPaymentDetails.Card(
                            id = "QAAAKJ6",
                            expiryYear = 2023,
                            expiryMonth = 12,
                            isDefault = true,
                            brand = CardBrand.MasterCard,
                            last4 = "4444",
                            cvcCheck = CvcCheck.Pass,
                            billingAddress = ConsumerPaymentDetails.BillingAddress(
                                countryCode = CountryCode.US,
                                postalCode = "12312"
                            )
                        ),
                        enabled = true,
                        isSelected = false,
                        isUpdating = false
                    )
                ),
                TestCase(
                    name = "CardEnabledSelected",
                    state = State(
                        details = ConsumerPaymentDetails.Card(
                            id = "QAAAKJ6",
                            expiryYear = 2023,
                            expiryMonth = 12,
                            isDefault = true,
                            brand = CardBrand.MasterCard,
                            last4 = "4444",
                            cvcCheck = CvcCheck.Pass,
                            billingAddress = ConsumerPaymentDetails.BillingAddress(
                                countryCode = CountryCode.US,
                                postalCode = "12312"
                            )
                        ),
                        enabled = true,
                        isSelected = true,
                        isUpdating = false
                    )
                ),
                TestCase(
                    name = "CardEnabledUpdating",
                    state = State(
                        details = ConsumerPaymentDetails.Card(
                            id = "QAAAKJ6",
                            expiryYear = 2023,
                            expiryMonth = 12,
                            isDefault = true,
                            brand = CardBrand.MasterCard,
                            last4 = "4444",
                            cvcCheck = CvcCheck.Pass,
                            billingAddress = ConsumerPaymentDetails.BillingAddress(
                                countryCode = CountryCode.US,
                                postalCode = "12312"
                            )
                        ),
                        enabled = true,
                        isSelected = true,
                        isUpdating = false
                    )
                ),
                TestCase(
                    name = "CardDisabledSelected",
                    state = State(
                        details = ConsumerPaymentDetails.Card(
                            id = "QAAAKJ6",
                            expiryYear = 2023,
                            expiryMonth = 12,
                            isDefault = true,
                            brand = CardBrand.MasterCard,
                            last4 = "4444",
                            cvcCheck = CvcCheck.Pass,
                            billingAddress = ConsumerPaymentDetails.BillingAddress(
                                countryCode = CountryCode.US,
                                postalCode = "12312"
                            )
                        ),
                        enabled = false,
                        isSelected = true,
                        isUpdating = false
                    )
                ),
                TestCase(
                    name = "BankAccountEnabled",
                    state = State(
                        details = ConsumerPaymentDetails.BankAccount(
                            id = "wAAACGA",
                            last4 = "6789",
                            bankName = "STRIPE TEST BANK",
                            bankIconCode = null,
                            isDefault = false,
                        ),
                        enabled = true,
                        isSelected = false,
                        isUpdating = false
                    )
                ),
                TestCase(
                    name = "PassTThroughEnabled",
                    state = State(
                        details = ConsumerPaymentDetails.Passthrough(
                            id = "wAAACGA",
                            last4 = "6789",
                        ),
                        enabled = true,
                        isSelected = false,
                        isUpdating = false
                    )
                )
            )
        }
    }

    internal data class TestCase(val name: String, val state: State) {
        override fun toString(): String = name
    }

    internal data class State(
        val details: ConsumerPaymentDetails.PaymentDetails,
        val enabled: Boolean,
        val isSelected: Boolean,
        val isUpdating: Boolean,
    )
}