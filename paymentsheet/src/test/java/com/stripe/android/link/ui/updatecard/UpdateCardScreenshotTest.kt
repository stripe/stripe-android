package com.stripe.android.link.ui.updatecard

import androidx.compose.runtime.rememberCoroutineScope
import com.stripe.android.DefaultCardBrandFilter
import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.core.model.CountryCode
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.ui.LinkScreenshotSurface
import com.stripe.android.model.CardBrand
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.CvcCheck
import com.stripe.android.paymentsheet.CardUpdateParams
import com.stripe.android.paymentsheet.PaymentSheet.BillingDetailsCollectionConfiguration
import com.stripe.android.paymentsheet.PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode
import com.stripe.android.paymentsheet.PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode
import com.stripe.android.paymentsheet.ui.CardEditConfiguration
import com.stripe.android.paymentsheet.ui.DefaultEditCardDetailsInteractor
import com.stripe.android.paymentsheet.ui.EditCardDetailsInteractor
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
                        cardEditConfiguration = testCase.cardEditConfiguration,
                        requiresModification = true,
                        payload = EditCardPayload.create(
                            testCase.payment,
                            billingPhoneNumber = null
                        ),
                        billingDetailsCollectionConfiguration = testCase.billingDetailsCollectionConfiguration,
                        onBrandChoiceChanged = {},
                        onCardUpdateParamsChanged = {},
                    ).apply {
                        if (testCase.validate) {
                            handleViewAction(EditCardDetailsInteractor.ViewAction.Validate)
                        }
                    },
                    state = testCase.state,
                    onUpdateClicked = {},
                    onDisabledButtonClicked = {},
                )
            }
        }
    }

    companion object {
        @SuppressWarnings("LongMethod")
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data(): List<TestCase> {
            return listOf(
                TestCase(
                    name = "Canonical",
                    state = state(),
                    payment = card(),
                    billingDetailsCollectionConfiguration = nothingCollected(),
                    cardEditConfiguration = cardEditConfiguration()
                ),
                TestCase(
                    name = "Default",
                    state = state(isDefault = true),
                    payment = card(),
                    billingDetailsCollectionConfiguration = nothingCollected(),
                    cardEditConfiguration = cardEditConfiguration()
                ),
                TestCase(
                    name = "Processing",
                    state = state(processing = true),
                    payment = card(),
                    billingDetailsCollectionConfiguration = nothingCollected(),
                    cardEditConfiguration = cardEditConfiguration()
                ),
                TestCase(
                    name = "Error",
                    state = state(error = Exception("Error")),
                    payment = card(),
                    billingDetailsCollectionConfiguration = nothingCollected(),
                    cardEditConfiguration = cardEditConfiguration()
                ),
                TestCase(
                    name = "Unchanged",
                    state = state(cardUpdateParams = null),
                    payment = card(),
                    billingDetailsCollectionConfiguration = nothingCollected(),
                    cardEditConfiguration = cardEditConfiguration()
                ),
                TestCase(
                    name = "All details collected",
                    state = state(),
                    payment = card(),
                    billingDetailsCollectionConfiguration = BillingDetailsCollectionConfiguration(
                        address = AddressCollectionMode.Full,
                        email = CollectionMode.Always,
                        phone = CollectionMode.Always,
                        name = CollectionMode.Always
                    ),
                    cardEditConfiguration = cardEditConfiguration()
                ),
                TestCase(
                    name = "Contact info collected",
                    state = state(),
                    payment = card(),
                    billingDetailsCollectionConfiguration = BillingDetailsCollectionConfiguration(
                        address = AddressCollectionMode.Never,
                        email = CollectionMode.Always,
                        phone = CollectionMode.Always,
                        name = CollectionMode.Always
                    ),
                    cardEditConfiguration = cardEditConfiguration()
                ),
                TestCase(
                    name = "Address collected",
                    state = state(),
                    payment = card(),
                    cardEditConfiguration = cardEditConfiguration(),
                    billingDetailsCollectionConfiguration = BillingDetailsCollectionConfiguration(
                        address = AddressCollectionMode.Full,
                        email = CollectionMode.Never,
                        phone = CollectionMode.Never,
                        name = CollectionMode.Never
                    )
                ),
                TestCase(
                    name = "Bank account",
                    state = state(),
                    payment = bankAccount(),
                    cardEditConfiguration = null,
                    billingDetailsCollectionConfiguration = BillingDetailsCollectionConfiguration(
                        address = AddressCollectionMode.Full,
                        email = CollectionMode.Never,
                        phone = CollectionMode.Never,
                        name = CollectionMode.Never
                    )
                ),
                TestCase(
                    name = "Bank account with contact info",
                    state = state(),
                    payment = bankAccount(),
                    cardEditConfiguration = null,
                    billingDetailsCollectionConfiguration = BillingDetailsCollectionConfiguration(
                        address = AddressCollectionMode.Full,
                        email = CollectionMode.Always,
                        phone = CollectionMode.Always,
                        name = CollectionMode.Always,
                    )
                ),
                TestCase(
                    name = "Card validation",
                    state = state(),
                    payment = card().copy(
                        expiryMonth = 12,
                        expiryYear = 2012,
                        billingAddress = null,
                    ),
                    cardEditConfiguration = cardEditConfiguration(),
                    billingDetailsCollectionConfiguration = BillingDetailsCollectionConfiguration(
                        address = AddressCollectionMode.Full,
                        email = CollectionMode.Always,
                        phone = CollectionMode.Always,
                        name = CollectionMode.Always,
                    ),
                    validate = true,
                ),
            )
        }

        private fun nothingCollected(): BillingDetailsCollectionConfiguration = BillingDetailsCollectionConfiguration(
            address = AddressCollectionMode.Automatic,
            email = CollectionMode.Never,
            phone = CollectionMode.Never,
            name = CollectionMode.Never
        )

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
                error = error?.stripeErrorMessage(),
                processing = processing,
                billingDetailsUpdateFlow = null,
                primaryButtonLabel = "Update card".resolvableString
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

        private fun bankAccount(): ConsumerPaymentDetails.BankAccount = ConsumerPaymentDetails.BankAccount(
            id = "bank_account_id_1234",
            last4 = "6789",
            isDefault = false,
            nickname = "My bank account",
            billingAddress = ConsumerPaymentDetails.BillingAddress(
                name = null,
                line1 = null,
                line2 = null,
                locality = null,
                administrativeArea = null,
                countryCode = CountryCode.US,
                postalCode = "42424"
            ),
            bankName = "My Bank",
            bankIconCode = "bank_icon_code",
            billingEmailAddress = null
        )

        private fun cardEditConfiguration(): CardEditConfiguration {
            return CardEditConfiguration(
                cardBrandFilter = DefaultCardBrandFilter,
                isCbcModifiable = false,
                areExpiryDateAndAddressModificationSupported = true,
            )
        }
    }

    internal data class TestCase(
        val name: String,
        val state: UpdateCardScreenState,
        val payment: ConsumerPaymentDetails.PaymentDetails,
        val billingDetailsCollectionConfiguration: BillingDetailsCollectionConfiguration,
        val cardEditConfiguration: CardEditConfiguration?,
        val validate: Boolean = false,
    ) {
        override fun toString(): String = name
    }
}
