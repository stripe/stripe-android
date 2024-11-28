package com.stripe.android.link.ui.cardedit

import com.google.common.truth.Truth.assertThat
import com.stripe.android.cards.CardAccountRangeRepository
import com.stripe.android.core.Logger
import com.stripe.android.link.LinkActivityResult
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.TestFactory
import com.stripe.android.link.TestFactory.CONSUMER_PAYMENT_DETAILS_BANK_ACCOUNT
import com.stripe.android.link.TestFactory.CONSUMER_PAYMENT_DETAILS_CARD
import com.stripe.android.link.TestFactory.CONSUMER_PAYMENT_DETAILS_PASSTHROUGH
import com.stripe.android.link.account.FakeLinkAccountManager
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.model.supportedPaymentMethodTypes
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.testing.FakeLogger
import com.stripe.android.testing.NullCardAccountRangeRepositoryFactory
import com.stripe.android.uicore.elements.IdentifierSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class CardEditViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `card loaded on start`() = runTest(dispatcher) {
        val card = CONSUMER_PAYMENT_DETAILS_CARD
        val linkAccountManager = object : FakeLinkAccountManager() {
            var paymentMethodTypes: Set<String>? = null
            override suspend fun listPaymentDetails(paymentMethodTypes: Set<String>): Result<ConsumerPaymentDetails> {
                this.paymentMethodTypes = paymentMethodTypes
                return super.listPaymentDetails(paymentMethodTypes)
            }
        }
        val viewModel = createViewModel(linkAccountManager = linkAccountManager)

        assertThat(linkAccountManager.paymentMethodTypes)
            .containsExactlyElementsIn(
                TestFactory.LINK_CONFIGURATION.stripeIntent.supportedPaymentMethodTypes(TestFactory.LINK_ACCOUNT)
            )

        assertThat(
            viewModel.entries()
        ).containsAtLeastEntriesIn(
            mapOf(
                IdentifierSpec.CardNumber to card.last4,
                IdentifierSpec.CardBrand to card.brand.code,
                IdentifierSpec.CardExpMonth to card.expiryMonth.toString().padStart(length = 2, padChar = '0'),
                IdentifierSpec.CardExpYear to card.expiryYear.toString(),
                IdentifierSpec.Country to card.billingAddress?.countryCode?.value,
                IdentifierSpec.PostalCode to card.billingAddress?.postalCode
            )
        )
    }

    @Test
    fun `screen should dismiss when card to be updated is not amongst consumer's payment details`() =
        runTest(dispatcher) {
            val linkAccountManager = FakeLinkAccountManager()
            var dismissResult: LinkActivityResult? = null
            fun dismissWithResult(result: LinkActivityResult) {
                dismissResult = result
            }
            linkAccountManager.listPaymentDetailsResult = Result.success(
                value = ConsumerPaymentDetails(
                    paymentDetails = listOf(
                        CONSUMER_PAYMENT_DETAILS_BANK_ACCOUNT,
                        CONSUMER_PAYMENT_DETAILS_PASSTHROUGH,
                    )
                )
            )
            createViewModel(linkAccountManager = linkAccountManager, dismissWithResult = ::dismissWithResult)

            assertThat(dismissResult).isInstanceOf(LinkActivityResult.Failed::class.java)
            assertThat((dismissResult as? LinkActivityResult.Failed)?.error)
                .isInstanceOf(CardNotFoundException::class.java)
        }

    @Test
    fun `screen should dismiss when payment details fails to load`() = runTest(dispatcher) {
        val error = Throwable("oops")
        val linkAccountManager = FakeLinkAccountManager()
        var dismissResult: LinkActivityResult? = null
        fun dismissWithResult(result: LinkActivityResult) {
            dismissResult = result
        }
        linkAccountManager.listPaymentDetailsResult = Result.failure(error)
        val logger = FakeLogger()
        createViewModel(
            linkAccountManager = linkAccountManager,
            logger = logger,
            dismissWithResult = ::dismissWithResult
        )

        assertThat(logger.errorLogs).containsExactly("CardEditViewModel error: " to error)
        assertThat(dismissResult).isEqualTo(LinkActivityResult.Failed(error))
    }

    private fun CardEditViewModel.entries(): Map<IdentifierSpec, String?> {
        return formElements.value.flatMap {
            it.getFormFieldValueFlow().value.map { (identifierSpec, entry) ->
                identifierSpec to entry.value
            }
        }.associate { it }
    }

    private fun createViewModel(
        paymentDetailsId: String = CONSUMER_PAYMENT_DETAILS_CARD.id,
        linkAccount: LinkAccount = TestFactory.LINK_ACCOUNT,
        linkAccountManager: LinkAccountManager = FakeLinkAccountManager(),
        configuration: LinkConfiguration = TestFactory.LINK_CONFIGURATION,
        logger: Logger = FakeLogger(),
        cardAccountRangeRepositoryFactory: CardAccountRangeRepository.Factory = NullCardAccountRangeRepositoryFactory,
        dismissWithResult: (LinkActivityResult) -> Unit = {}
    ): CardEditViewModel {
        return CardEditViewModel(
            paymentDetailsId = paymentDetailsId,
            linkAccount = linkAccount,
            configuration = configuration,
            logger = logger,
            cardAccountRangeRepositoryFactory = cardAccountRangeRepositoryFactory,
            dismissWithResult = dismissWithResult,
            linkAccountManager = linkAccountManager
        )
    }
}
