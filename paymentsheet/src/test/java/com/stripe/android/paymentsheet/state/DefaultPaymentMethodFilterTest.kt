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

    private suspend fun filter(
        paymentMethods: List<PaymentMethod>,
        billingDetailsCollectionConfiguration: PaymentSheet.BillingDetailsCollectionConfiguration =
            PaymentSheet.BillingDetailsCollectionConfiguration(),
        isPaymentMethodSetAsDefaultEnabled: Boolean = false,
        remoteDefaultPaymentMethodId: String? = null,
        localSavedSelection: SavedSelection = SavedSelection.None,
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
                customerMetadata = CustomerMetadata(
                    accessInfo = CustomerMetadata.AccessInfo.CustomerSession(
                        customerId = "cus_1",
                        ephemeralKeySecret = "ek_123",
                        customerSessionClientSecret = "cuss_123",
                    ),
                    isPaymentMethodSetAsDefaultEnabled = isPaymentMethodSetAsDefaultEnabled,
                    permissions = CustomerMetadata.Permissions(
                        removePaymentMethod = PaymentMethodRemovePermission.Full,
                        saveConsent = PaymentMethodSaveConsentBehavior.Legacy,
                        canRemoveLastPaymentMethod = false,
                        canRemoveDuplicates = false,
                        canUpdateFullPaymentMethodDetails = false,
                    )
                ),
                remoteDefaultPaymentMethodId = remoteDefaultPaymentMethodId,
                localSavedSelection = CompletableDeferred(localSavedSelection),
                cardBrandFilter = cardBrandFilter,
                cardFundingFilter = cardFundingFilter,
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
