package com.stripe.android.link.ui.updatecard

import androidx.compose.runtime.rememberCoroutineScope
import com.stripe.android.DefaultCardBrandFilter
import com.stripe.android.core.model.CountryCode
import com.stripe.android.link.theme.DefaultLinkTheme
import com.stripe.android.link.ui.PrimaryButtonState
import com.stripe.android.model.CardBrand
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.CvcCheck
import com.stripe.android.paymentsheet.PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode
import com.stripe.android.paymentsheet.ui.DefaultEditCardDetailsInteractor
import com.stripe.android.paymentsheet.ui.EditCardPayload
import com.stripe.android.screenshottesting.FontSize
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.SystemAppearance
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
internal class UpdateCardScreenshotTest(
    private val testCase: TestCase
) {
    @get:Rule
    val paparazziRule = PaparazziRule(
        listOf(SystemAppearance.DarkTheme),
        listOf(FontSize.DefaultFont)
    )

    @Test
    fun testScreen() {
        paparazziRule.snapshot {
            DefaultLinkTheme {
                UpdateCardScreenBody(
                    interactor = DefaultEditCardDetailsInteractor.Factory().create(
                        coroutineScope = rememberCoroutineScope(),
                        isCbcModifiable = false,
                        areExpiryDateAndAddressModificationSupported = true,
                        cardBrandFilter = DefaultCardBrandFilter,
                        payload = EditCardPayload.create(
                            testCase.card,
                            billingPhoneNumber = null
                        ),
                        onBrandChoiceChanged = {},
                        onCardUpdateParamsChanged = {},
                        addressCollectionMode = AddressCollectionMode.Automatic
                    ),
                    isDefault = true,
                    primaryButtonState = PrimaryButtonState.Enabled,
                    secondaryButtonEnabled = true,
                    onUpdateClicked = {},
                    onCancelClicked = {},
                )
            }
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data(): List<TestCase> {
            return listOf(
                TestCase(
                    name = "Canonical",
                    isDefault = false,
                    card = card()
                ),
                TestCase(
                    name = "Default",
                    isDefault = true,
                    card = card()
                )
            )
        }

        private fun card(): ConsumerPaymentDetails.Card = ConsumerPaymentDetails.Card(
            id = "card_id_1234",
            last4 = "4242",
            expiryYear = 2500,
            expiryMonth = 4,
            brand = CardBrand.Visa,
            cvcCheck = CvcCheck.Fail,
            isDefault = false,
            networks = listOf("VISA"),
            billingAddress = ConsumerPaymentDetails.BillingAddress(
                name = null,
                line1 = null,
                line2 = null,
                locality = null,
                administrativeArea = null,
                countryCode = CountryCode.US,
                postalCode = "42424"
            )
        )
    }

    internal data class TestCase(
        val name: String,
        val card: ConsumerPaymentDetails.Card,
        val isDefault: Boolean,
    ) {
        override fun toString(): String = name
    }
}
