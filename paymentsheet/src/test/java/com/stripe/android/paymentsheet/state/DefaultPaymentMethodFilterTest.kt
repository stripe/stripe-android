package com.stripe.android.paymentsheet.state

import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.model.PaymentMethodRemovePermission
import com.stripe.android.lpmfoundations.paymentmethod.CustomerMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodSaveConsentBehavior
import com.stripe.android.lpmfoundations.paymentmethod.PaymentSheetCardBrandFilter
import com.stripe.android.lpmfoundations.paymentmethod.PaymentSheetCardFundingFilter
import com.stripe.android.model.Address
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.CardFundingFilteringPrivatePreview
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.SavedSelection
import com.stripe.android.testing.PaymentMethodFactory
import com.stripe.android.testing.PaymentMethodFactory.update
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class DefaultPaymentMethodFilterTest {
    @Test
    fun `When DefaultPaymentMethod not null, no saved selection, defaultPaymentMethod first`() = runTest {
        val observedElements = filter(
            paymentMethods = paymentMethodsForTestingOrdering,
            isPaymentMethodSetAsDefaultEnabled = true,
            remoteDefaultPaymentMethodId = paymentMethodsForTestingOrdering[2].id,
        )

        val expectedElements = expectedPaymentMethodsWithDefaultPaymentMethod
        assertThat(observedElements).containsExactlyElementsIn(expectedElements).inOrder()
    }

    @Test
    fun `When DefaultPaymentMethod not null, saved selection, defaultPaymentMethod first`() = runTest {
        val observedElements = filter(
            paymentMethods = paymentMethodsForTestingOrdering,
            localSavedSelection = SavedSelection.PaymentMethod(paymentMethodsForTestingOrdering[1].id),
            isPaymentMethodSetAsDefaultEnabled = true,
            remoteDefaultPaymentMethodId = paymentMethodsForTestingOrdering[2].id,
        )

        val expectedElements = expectedPaymentMethodsWithDefaultPaymentMethod
        assertThat(observedElements).containsExactlyElementsIn(expectedElements).inOrder()
    }

    @Test
    fun `When DefaultPaymentMethod not null, saved selection is defaultPaymentMethod, defaultPaymentMethod first`() =
        runTest {
            val observedElements = filter(
                paymentMethods = paymentMethodsForTestingOrdering,
                localSavedSelection = SavedSelection.PaymentMethod(paymentMethodsForTestingOrdering[2].id),
                remoteDefaultPaymentMethodId = paymentMethodsForTestingOrdering[2].id,
            )

            val expectedElements = expectedPaymentMethodsWithDefaultPaymentMethod
            assertThat(observedElements).containsExactlyElementsIn(expectedElements).inOrder()
        }

    @Test
    fun `Should filter out saved cards with disallowed brands`() = runTest {
        val paymentMethods = listOf(
            PaymentMethodFactory.card(id = "pm_12345").update(
                last4 = "1001",
                addCbcNetworks = false,
                brand = CardBrand.Visa,
            ),
            PaymentMethodFactory.card(id = "pm_123456").update(
                last4 = "1000",
                addCbcNetworks = false,
                brand = CardBrand.AmericanExpress,
            )
        )

        val observedElements = filter(
            paymentMethods = paymentMethods,
            cardBrandFilter = PaymentSheetCardBrandFilter(
                cardBrandAcceptance = PaymentSheet.CardBrandAcceptance.disallowed(
                    brands = listOf(PaymentSheet.CardBrandAcceptance.BrandCategory.Visa),
                ),
            ),
        )

        assertThat(observedElements).hasSize(1)
        assertThat(observedElements.first().card?.brand).isEqualTo(CardBrand.AmericanExpress)
    }

    @Test
    fun `Filters out countries not in 'allowedCountries' array`() = runTest {
        val paymentMethods = createCardsWithDifferentBillingDetails()

        val observedElements = filter(
            paymentMethods = paymentMethods,
            billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                allowedCountries = setOf("CA", "mx"),
            )
        )

        assertThat(observedElements).containsExactly(
            paymentMethods[1],
            paymentMethods[4],
        )
    }

    @OptIn(CardFundingFilteringPrivatePreview::class)
    @Test
    fun `Should filter saved cards with credit funding type only`() = runTest {
        val paymentMethodsData = createFundingPaymentMethods()

        val observedElements = filter(
            paymentMethods = paymentMethodsData.all,
            cardFundingFilter = PaymentSheetCardFundingFilter(
                allowedCardFundingTypes = listOf(PaymentSheet.CardFundingType.Credit),
            ),
        )

        val expectedElements = listOf(paymentMethodsData.credit, paymentMethodsData.noFunding, paymentMethodsData.bank)
        assertThat(observedElements).containsExactlyElementsIn(expectedElements).inOrder()
    }

    @OptIn(CardFundingFilteringPrivatePreview::class)
    @Test
    fun `Should filter saved cards with debit funding type only`() = runTest {
        val paymentMethodsData = createFundingPaymentMethods()

        val observedElements = filter(
            paymentMethods = paymentMethodsData.all,
            cardFundingFilter = PaymentSheetCardFundingFilter(
                allowedCardFundingTypes = listOf(PaymentSheet.CardFundingType.Debit),
            ),
        )

        val expectedElements = listOf(paymentMethodsData.debit, paymentMethodsData.noFunding, paymentMethodsData.bank)
        assertThat(observedElements).containsExactlyElementsIn(expectedElements).inOrder()
    }

    @OptIn(CardFundingFilteringPrivatePreview::class)
    @Test
    fun `Should filter saved cards with prepaid funding type only`() = runTest {
        val paymentMethodsData = createFundingPaymentMethods()

        val observedElements = filter(
            paymentMethods = paymentMethodsData.all,
            cardFundingFilter = PaymentSheetCardFundingFilter(
                allowedCardFundingTypes = listOf(PaymentSheet.CardFundingType.Prepaid),
            ),
        )

        val expectedElements = listOf(paymentMethodsData.prepaid, paymentMethodsData.noFunding, paymentMethodsData.bank)
        assertThat(observedElements).containsExactlyElementsIn(expectedElements).inOrder()
    }

    @OptIn(CardFundingFilteringPrivatePreview::class)
    @Test
    fun `Should filter saved cards with credit and debit funding type only`() = runTest {
        val paymentMethodsData = createFundingPaymentMethods()

        val observedElements = filter(
            paymentMethods = paymentMethodsData.all,
            cardFundingFilter = PaymentSheetCardFundingFilter(
                allowedCardFundingTypes = listOf(
                    PaymentSheet.CardFundingType.Credit,
                    PaymentSheet.CardFundingType.Debit,
                ),
            ),
        )

        val expectedElements = listOf(
            paymentMethodsData.credit,
            paymentMethodsData.debit,
            paymentMethodsData.noFunding,
            paymentMethodsData.bank
        )
        assertThat(observedElements).containsExactlyElementsIn(expectedElements).inOrder()
    }

    @Test
    fun `Moves last-used customer payment method to the front of the list`() = runTest {
        val paymentMethods = PaymentMethodFixtures.createCards(10)
        val lastUsed = paymentMethods[6]

        val observedElements = filter(
            paymentMethods = paymentMethods,
            localSavedSelection = SavedSelection.PaymentMethod(lastUsed.id)
        )

        val expectedElements = listOf(lastUsed) + (paymentMethods - lastUsed)

        assertThat(observedElements).containsExactlyElementsIn(expectedElements).inOrder()
    }

    private fun usCard(id: String, state: String?): PaymentMethod = PaymentMethodFactory.card(id = id).update(
        last4 = "1000",
        addCbcNetworks = false,
        brand = CardBrand.Visa,
    ).copy(
        billingDetails = PaymentMethod.BillingDetails(
            address = Address(
                line1 = "123 Main St",
                city = "San Francisco",
                state = state,
                postalCode = "94111",
                country = "US",
            ),
        ),
    )

    @Test
    fun `Drops SPM lacking automatic-tax-required fields when automatic tax requires a billing address`() = runTest {
        val complete = usCard(id = "pm_complete", state = "CA")
        val incomplete = usCard(id = "pm_incomplete", state = null)

        val observed = filter(
            paymentMethods = listOf(complete, incomplete),
            requiresBillingAddressForAutomaticTax = true,
        )

        assertThat(observed).containsExactly(complete)
    }

    @Test
    fun `Does not filter by automatic-tax fields when automatic tax does not require a billing address`() = runTest {
        val complete = usCard(id = "pm_complete", state = "CA")
        val incomplete = usCard(id = "pm_incomplete", state = null)

        val observed = filter(
            paymentMethods = listOf(complete, incomplete),
            requiresBillingAddressForAutomaticTax = false,
        )

        assertThat(observed).containsExactly(complete, incomplete)
    }

    @Test
    fun `Keeps a country-only-sufficient SPM when automatic tax requires a billing address`() = runTest {
        // FR has no additional automatic-tax fields, so country alone is sufficient. Proves the
        // clause does not over-drop valid cards.
        val frCard = PaymentMethodFactory.card(id = "pm_fr").update(
            last4 = "1000",
            addCbcNetworks = false,
            brand = CardBrand.Visa,
        ).copy(
            billingDetails = PaymentMethod.BillingDetails(
                address = Address(country = "FR"),
            ),
        )

        val observed = filter(
            paymentMethods = listOf(frCard),
            requiresBillingAddressForAutomaticTax = true,
        )

        assertThat(observed).containsExactly(frCard)
    }

    @Test
    fun `Drops SPM with null billing address when automatic tax requires a billing address`() = runTest {
        val noAddress = PaymentMethodFactory.card(id = "pm_no_addr").update(
            last4 = "1000",
            addCbcNetworks = false,
            brand = CardBrand.Visa,
        ).copy(billingDetails = PaymentMethod.BillingDetails(address = null))

        val observed = filter(
            paymentMethods = listOf(noAddress),
            requiresBillingAddressForAutomaticTax = true,
        )

        assertThat(observed).isEmpty()
    }

    @Test
    fun `Applies automatic-tax filter to non-card SPMs too`() = runTest {
        // pay-server's confirm-time tax guard is not payment-method-type gated, so a bank-account
        // SPM with an insufficient tax address 400s at confirm just like a card; the filter must
        // drop it as well. Guards against a future card-only regression.
        val completeBank = PaymentMethodFactory.usBankAccount().copy(
            id = "pm_bank_complete",
            billingDetails = PaymentMethod.BillingDetails(
                address = Address(
                    line1 = "1 Market St",
                    city = "San Francisco",
                    state = "CA",
                    postalCode = "94111",
                    country = "US",
                ),
            ),
        )
        val incompleteBank = PaymentMethodFactory.usBankAccount().copy(
            id = "pm_bank_incomplete",
            billingDetails = PaymentMethod.BillingDetails(
                // Missing line1 and state for a US address.
                address = Address(city = "San Francisco", postalCode = "94111", country = "US"),
            ),
        )

        val observed = filter(
            paymentMethods = listOf(completeBank, incompleteBank),
            requiresBillingAddressForAutomaticTax = true,
        )

        assertThat(observed).containsExactly(completeBank)
    }

    @Test
    fun `Applies per-country automatic-tax fields at the filter boundary`() = runTest {
        // CA requires only a postal code (not the US line1/city/state set), proving the per-country
        // lookup is consulted at the filter boundary rather than hardcoding US behavior.
        val caComplete = PaymentMethodFactory.card(id = "pm_ca_complete").update(
            last4 = "1000",
            addCbcNetworks = false,
            brand = CardBrand.Visa,
        ).copy(billingDetails = PaymentMethod.BillingDetails(address = Address(postalCode = "K1A0B1", country = "CA")))
        val caMissingPostal = PaymentMethodFactory.card(id = "pm_ca_missing").update(
            last4 = "1000",
            addCbcNetworks = false,
            brand = CardBrand.Visa,
        ).copy(billingDetails = PaymentMethod.BillingDetails(address = Address(country = "CA")))

        val observed = filter(
            paymentMethods = listOf(caComplete, caMissingPostal),
            requiresBillingAddressForAutomaticTax = true,
        )

        assertThat(observed).containsExactly(caComplete)
    }

    private suspend fun filter(
        paymentMethods: List<PaymentMethod>,
        billingDetailsCollectionConfiguration: PaymentSheet.BillingDetailsCollectionConfiguration =
            PaymentSheet.BillingDetailsCollectionConfiguration(),
        isPaymentMethodSetAsDefaultEnabled: Boolean = false,
        remoteDefaultPaymentMethodId: String? = null,
        localSavedSelection: SavedSelection = SavedSelection.None,
        requiresBillingAddressForAutomaticTax: Boolean = false,
        cardBrandFilter: PaymentSheetCardBrandFilter = PaymentSheetCardBrandFilter(
            cardBrandAcceptance = PaymentSheet.CardBrandAcceptance.all(),
        ),
        cardFundingFilter: PaymentSheetCardFundingFilter = PaymentSheetCardFundingFilter(
            allowedCardFundingTypes = PaymentSheet.CardFundingType.entries,
        )
    ): List<PaymentMethod> {
        return DefaultPaymentMethodFilter().filter(
            paymentMethods = paymentMethods,
            params = PaymentMethodFilter.FilterParams(
                billingDetailsCollectionConfiguration = billingDetailsCollectionConfiguration,
                customerMetadata = CustomerMetadata.CustomerSession(
                    id = "cus_1",
                    ephemeralKeySecret = "ek_123",
                    customerSessionClientSecret = "cuss_123",
                    isPaymentMethodSetAsDefaultEnabled = isPaymentMethodSetAsDefaultEnabled,
                    removePaymentMethod = PaymentMethodRemovePermission.Full,
                    saveConsent = PaymentMethodSaveConsentBehavior.Legacy,
                    canRemoveLastPaymentMethod = false,
                    canUpdateCardExpiryAndBillingDetails = false,
                ),
                remoteDefaultPaymentMethodId = remoteDefaultPaymentMethodId,
                localSavedSelection = CompletableDeferred(localSavedSelection),
                cardBrandFilter = cardBrandFilter,
                cardFundingFilter = cardFundingFilter,
                requiresBillingAddressForAutomaticTax = requiresBillingAddressForAutomaticTax,
            )
        )
    }

    private fun createFundingPaymentMethods(): FundingPaymentMethods {
        val credit = PaymentMethodFactory.card(id = "pm_credit").update(
            last4 = "1000",
            addCbcNetworks = false,
            brand = CardBrand.Visa,
            funding = "credit",
        )
        val debit = PaymentMethodFactory.card(id = "pm_debit").update(
            last4 = "1001",
            addCbcNetworks = false,
            brand = CardBrand.Visa,
            funding = "debit",
        )
        val prepaid = PaymentMethodFactory.card(id = "pm_prepaid").update(
            last4 = "1002",
            addCbcNetworks = false,
            brand = CardBrand.Visa,
            funding = "prepaid",
        )
        val noFunding = PaymentMethodFactory.card(id = "pm_no_funding").update(
            last4 = "1003",
            addCbcNetworks = false,
            brand = CardBrand.Visa,
            funding = null,
        )
        val bank = PaymentMethodFactory.usBankAccount().copy(
            id = "pm_bank"
        )

        return FundingPaymentMethods(
            credit = credit,
            debit = debit,
            prepaid = prepaid,
            noFunding = noFunding,
            bank = bank
        )
    }

    private fun createCardsWithDifferentBillingDetails(): List<PaymentMethod> = listOf(
        PaymentMethodFactory.card(
            last4 = "4242",
            billingDetails = null,
        ),
        PaymentMethodFactory.card(
            last4 = "4444",
            billingDetails = PaymentMethod.BillingDetails(
                address = Address(
                    country = "CA",
                )
            )
        ),
        PaymentMethodFactory.card(
            last4 = "4444",
            billingDetails = PaymentMethod.BillingDetails(
                address = Address(
                    country = "US",
                )
            )
        ),
        PaymentMethodFactory.card(
            last4 = "4444",
            billingDetails = PaymentMethod.BillingDetails(
                address = Address(
                    country = "US",
                )
            )
        ),
        PaymentMethodFactory.card(
            last4 = "4444",
            billingDetails = PaymentMethod.BillingDetails(
                address = Address(
                    country = "MX",
                )
            )
        ),
    )

    private val paymentMethodsForTestingOrdering = listOf(
        PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(id = "a1", customerId = "alice"),
        PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(id = "b2", customerId = "bob"),
        PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(id = "c3", customerId = "carol"),
        PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(id = "d4", customerId = "dan")
    )

    private val expectedPaymentMethodsWithDefaultPaymentMethod = listOf(
        PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(id = "c3", customerId = "carol"),
        PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(id = "a1", customerId = "alice"),
        PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(id = "b2", customerId = "bob"),
        PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(id = "d4", customerId = "dan")
    )

    private data class FundingPaymentMethods(
        val credit: PaymentMethod,
        val debit: PaymentMethod,
        val prepaid: PaymentMethod,
        val noFunding: PaymentMethod,
        val bank: PaymentMethod,
    ) {
        val all: List<PaymentMethod> = listOf(credit, debit, prepaid, noFunding, bank)
    }
}
