package com.stripe.android.link.ui.updatecard

import androidx.compose.runtime.rememberCoroutineScope
import com.stripe.android.DefaultCardBrandFilter
import com.stripe.android.core.model.CountryCode
import com.stripe.android.link.ui.LinkScreenshotSurface
import com.stripe.android.model.CardBrand
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.CvcCheck
import com.stripe.android.paymentsheet.CardUpdateParams
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
        SystemAppearance.entries,
        listOf(FontSize.DefaultFont)
    )

    @Test
    fun testScreen() {
        paparazziRule.snapshot {
            LinkScreenshotSurface {
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
                    state = testCase.state,
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
                    state = state(),
                    card = card()
                ),
                TestCase(
                    name = "Default",
                    state = state(isDefault = true),
                    card = card()
                ),
                TestCase(
                    name = "Processing",
                    state = state(processing = true),
                    card = card()
                ),
                TestCase(
                    name = "Error",
                    state = state(error = Exception("Error")),
                    card = card()
                ),
                TestCase(
                    name = "Unchanged",
                    state = state(cardUpdateParams = null),
                    card = card()
                ),
            )
        }

        private fun state(
            isDefault: Boolean = false,
            processing: Boolean = false,
            error: Exception? = null,
            cardUpdateParams: CardUpdateParams? = CardUpdateParams(expiryMonth = 11),
        ): UpdateCardScreenState {
            return UpdateCardScreenState(
                paymentDetailsId = "card_id_1234",
                isDefault = isDefault,
                cardUpdateParams = cardUpdateParams,
                preferredCardBrand = null,
                error = error,
                processing = processing
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
            nickname = null,
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
        )
    }

    internal data class TestCase(
        val name: String,
        val state: UpdateCardScreenState,
        val card: ConsumerPaymentDetails.Card,
    ) {
        override fun toString(): String = name
    }
}
