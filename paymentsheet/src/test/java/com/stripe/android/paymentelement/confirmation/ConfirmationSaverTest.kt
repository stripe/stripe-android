package com.stripe.android.paymentelement.confirmation

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.DefaultCardFundingFilter
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParamsFixtures
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.StripeIntent
import com.stripe.android.model.wallets.Wallet
import com.stripe.android.paymentelement.confirmation.gpay.GooglePayConfirmationOption
import com.stripe.android.paymentelement.confirmation.link.LinkConfirmationOption
import com.stripe.android.paymentsheet.PrefsRepository
import com.stripe.android.paymentsheet.model.SavedSelection
import com.stripe.android.testing.PaymentIntentFactory
import com.stripe.android.testing.SetupIntentFactory
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.mock

class ConfirmationSaverTest {
    @Test
    fun `save with new payment method and alwaysSave true should save payment method`() = runScenario {
        val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        val paymentIntent = PaymentIntentFactory.create(
            paymentMethod = paymentMethod
        )
        val confirmationOption = PaymentMethodConfirmationOption.New(
            createParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
            optionsParams = null,
            extraParams = null,
            shouldSave = false,
        )

        confirmationSaver.save(
            stripeIntent = paymentIntent,
            confirmationOption = confirmationOption,
            alwaysSave = true,
        )

        val call = setSavedSelectionCalls.awaitItem()
        assertThat(call.customerId).isEqualTo(paymentMethod.customerId)
        assertThat(call.savedSelection).isEqualTo(SavedSelection.PaymentMethod(paymentMethod.id))
    }

    @Test
    fun `save with new payment method and shouldSave true should save payment method`() = runScenario {
        val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        val paymentIntent = PaymentIntentFactory.create(
            paymentMethod = paymentMethod
        )
        val confirmationOption = PaymentMethodConfirmationOption.New(
            createParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
            optionsParams = null,
            extraParams = null,
            shouldSave = true,
        )

        confirmationSaver.save(
            stripeIntent = paymentIntent,
            confirmationOption = confirmationOption,
            alwaysSave = false,
        )

        val call = setSavedSelectionCalls.awaitItem()
        assertThat(call.customerId).isEqualTo(paymentMethod.customerId)
        assertThat(call.savedSelection).isEqualTo(SavedSelection.PaymentMethod(paymentMethod.id))
    }

    @Test
    fun `save with new payment method and setupFutureUsage set should save payment method`() = runScenario {
        val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        val paymentIntent = PaymentIntentFactory.create(
            paymentMethod = paymentMethod,
            setupFutureUsage = StripeIntent.Usage.OffSession,
        )
        val confirmationOption = PaymentMethodConfirmationOption.New(
            createParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
            optionsParams = null,
            extraParams = null,
            shouldSave = false,
        )

        confirmationSaver.save(
            stripeIntent = paymentIntent,
            confirmationOption = confirmationOption,
            alwaysSave = false,
        )

        val call = setSavedSelectionCalls.awaitItem()
        assertThat(call.customerId).isEqualTo(paymentMethod.customerId)
        assertThat(call.savedSelection).isEqualTo(SavedSelection.PaymentMethod(paymentMethod.id))
    }

    @Test
    fun `save with new payment method and setupFutureUsage OneTime should not save payment method`() = runScenario {
        val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        val paymentIntent = PaymentIntentFactory.create(
            paymentMethod = paymentMethod,
            setupFutureUsage = StripeIntent.Usage.OneTime,
        )
        val confirmationOption = PaymentMethodConfirmationOption.New(
            createParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
            optionsParams = null,
            extraParams = null,
            shouldSave = false,
        )

        confirmationSaver.save(
            stripeIntent = paymentIntent,
            confirmationOption = confirmationOption,
            alwaysSave = false,
        )

        setSavedSelectionCalls.expectNoEvents()
    }

    @Test
    fun `save with SetupIntent should always save payment method`() = runScenario {
        val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        val setupIntent = SetupIntentFactory.create(
            paymentMethod = paymentMethod
        )
        val confirmationOption = PaymentMethodConfirmationOption.New(
            createParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
            optionsParams = null,
            extraParams = null,
            shouldSave = false,
        )

        confirmationSaver.save(
            stripeIntent = setupIntent,
            confirmationOption = confirmationOption,
            alwaysSave = false,
        )

        val call = setSavedSelectionCalls.awaitItem()
        assertThat(call.customerId).isEqualTo(paymentMethod.customerId)
        assertThat(call.savedSelection).isEqualTo(SavedSelection.PaymentMethod(paymentMethod.id))
    }

    @Test
    fun `save with new payment method and no save conditions should not save`() = runScenario {
        val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        val paymentIntent = PaymentIntentFactory.create(
            paymentMethod = paymentMethod,
            setupFutureUsage = null,
        )
        val confirmationOption = PaymentMethodConfirmationOption.New(
            createParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
            optionsParams = null,
            extraParams = null,
            shouldSave = false,
        )

        confirmationSaver.save(
            stripeIntent = paymentIntent,
            confirmationOption = confirmationOption,
            alwaysSave = false,
        )

        setSavedSelectionCalls.expectNoEvents()
    }

    @Test
    fun `save with saved payment method should save payment method id`() = runScenario {
        val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        val paymentIntent = PaymentIntentFactory.create(
            paymentMethod = paymentMethod
        )
        val confirmationOption = PaymentMethodConfirmationOption.Saved(
            paymentMethod = paymentMethod,
            optionsParams = null,
        )

        confirmationSaver.save(
            stripeIntent = paymentIntent,
            confirmationOption = confirmationOption,
            alwaysSave = false,
        )

        val call = setSavedSelectionCalls.awaitItem()
        assertThat(call.customerId).isEqualTo(paymentMethod.customerId)
        assertThat(call.savedSelection).isEqualTo(SavedSelection.PaymentMethod(paymentMethod.id))
    }

    @Test
    fun `save with saved GooglePay wallet should save GooglePay selection`() = runScenario {
        val googlePayCard = PaymentMethod.Card(
            brand = CardBrand.Visa,
            last4 = "4242",
            wallet = Wallet.GooglePayWallet("4242")
        )
        val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(
            card = googlePayCard
        )
        val paymentIntent = PaymentIntentFactory.create(
            paymentMethod = paymentMethod
        )
        val confirmationOption = PaymentMethodConfirmationOption.Saved(
            paymentMethod = paymentMethod,
            optionsParams = null,
        )

        confirmationSaver.save(
            stripeIntent = paymentIntent,
            confirmationOption = confirmationOption,
            alwaysSave = false,
        )

        val call = setSavedSelectionCalls.awaitItem()
        assertThat(call.customerId).isEqualTo(paymentMethod.customerId)
        assertThat(call.savedSelection).isEqualTo(SavedSelection.GooglePay)
    }

    @Test
    fun `save with saved Link wallet should save Link selection`() = runScenario {
        val linkCard = PaymentMethod.Card(
            brand = CardBrand.Visa,
            last4 = "4242",
            wallet = Wallet.LinkWallet("4242")
        )
        val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(
            card = linkCard
        )
        val paymentIntent = PaymentIntentFactory.create(
            paymentMethod = paymentMethod
        )
        val confirmationOption = PaymentMethodConfirmationOption.Saved(
            paymentMethod = paymentMethod,
            optionsParams = null,
        )

        confirmationSaver.save(
            stripeIntent = paymentIntent,
            confirmationOption = confirmationOption,
            alwaysSave = false,
        )

        val call = setSavedSelectionCalls.awaitItem()
        assertThat(call.customerId).isEqualTo(paymentMethod.customerId)
        assertThat(call.savedSelection).isEqualTo(SavedSelection.Link)
    }

    @Test
    fun `save with GooglePayConfirmationOption should save GooglePay selection`() = runScenario {
        val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        val paymentIntent = PaymentIntentFactory.create(
            paymentMethod = paymentMethod
        )
        val confirmationOption = GooglePayConfirmationOption(
            config = GooglePayConfirmationOption.Config(
                environment = null,
                merchantName = "Test Merchant",
                merchantCountryCode = "US",
                merchantCurrencyCode = "USD",
                customAmount = null,
                customLabel = null,
                billingDetailsCollectionConfiguration = mock(),
                cardBrandFilter = mock(),
                cardFundingFilter = DefaultCardFundingFilter,
            )
        )

        confirmationSaver.save(
            stripeIntent = paymentIntent,
            confirmationOption = confirmationOption,
            alwaysSave = false,
        )

        val call = setSavedSelectionCalls.awaitItem()
        assertThat(call.customerId).isEqualTo(paymentMethod.customerId)
        assertThat(call.savedSelection).isEqualTo(SavedSelection.GooglePay)
    }

    @Test
    fun `save with LinkConfirmationOption should save Link selection`() = runScenario {
        val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        val paymentIntent = PaymentIntentFactory.create(
            paymentMethod = paymentMethod
        )
        val confirmationOption = LinkConfirmationOption(
            configuration = mock(),
            linkExpressMode = mock(),
        )

        confirmationSaver.save(
            stripeIntent = paymentIntent,
            confirmationOption = confirmationOption,
            alwaysSave = false,
        )

        val call = setSavedSelectionCalls.awaitItem()
        assertThat(call.customerId).isEqualTo(paymentMethod.customerId)
        assertThat(call.savedSelection).isEqualTo(SavedSelection.Link)
    }

    @Test
    fun `save with unknown confirmation option should not save`() = runScenario {
        val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        val paymentIntent = PaymentIntentFactory.create(
            paymentMethod = paymentMethod
        )
        val confirmationOption = FakeConfirmationOption()

        confirmationSaver.save(
            stripeIntent = paymentIntent,
            confirmationOption = confirmationOption,
            alwaysSave = false,
        )

        setSavedSelectionCalls.expectNoEvents()
    }

    @Test
    fun `save with null payment method should use null customerId`() = runScenario {
        val paymentIntent = PaymentIntentFactory.create(
            paymentMethod = null
        )
        val confirmationOption = GooglePayConfirmationOption(
            config = GooglePayConfirmationOption.Config(
                environment = null,
                merchantName = "Test Merchant",
                merchantCountryCode = "US",
                merchantCurrencyCode = "USD",
                customAmount = null,
                customLabel = null,
                billingDetailsCollectionConfiguration = mock(),
                cardBrandFilter = mock(),
                cardFundingFilter = DefaultCardFundingFilter,
            )
        )

        confirmationSaver.save(
            stripeIntent = paymentIntent,
            confirmationOption = confirmationOption,
            alwaysSave = false,
        )

        val call = setSavedSelectionCalls.awaitItem()
        assertThat(call.customerId).isNull()
        assertThat(call.savedSelection).isEqualTo(SavedSelection.GooglePay)
    }

    @Test
    fun `save with paymentMethod having null customerId should use null customerId`() = runScenario {
        val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(customerId = null)
        val paymentIntent = PaymentIntentFactory.create(
            paymentMethod = paymentMethod
        )
        val confirmationOption = PaymentMethodConfirmationOption.New(
            createParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
            optionsParams = null,
            extraParams = null,
            shouldSave = true,
        )

        confirmationSaver.save(
            stripeIntent = paymentIntent,
            confirmationOption = confirmationOption,
            alwaysSave = false,
        )

        val call = setSavedSelectionCalls.awaitItem()
        assertThat(call.customerId).isNull()
        assertThat(call.savedSelection).isEqualTo(SavedSelection.PaymentMethod(paymentMethod.id))
    }

    @Test
    fun `save with new payment method and null intent payment method should not save`() = runScenario {
        val paymentIntent = PaymentIntentFactory.create(
            paymentMethod = null
        )
        val confirmationOption = PaymentMethodConfirmationOption.New(
            createParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
            optionsParams = null,
            extraParams = null,
            shouldSave = true,
        )

        confirmationSaver.save(
            stripeIntent = paymentIntent,
            confirmationOption = confirmationOption,
            alwaysSave = false,
        )

        setSavedSelectionCalls.expectNoEvents()
    }

    @Test
    fun `save with new payment method having setupFutureUsage at PMO level should save`() = runScenario {
        val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        val paymentIntent = PaymentIntentFactory.create(
            paymentMethod = paymentMethod,
            setupFutureUsage = null,
            paymentMethodOptionsJsonString = """
                {
                  "card": {
                    "setup_future_usage": "off_session"
                  }
                }
            """.trimIndent()
        )
        val confirmationOption = PaymentMethodConfirmationOption.New(
            createParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
            optionsParams = null,
            extraParams = null,
            shouldSave = false,
        )

        confirmationSaver.save(
            stripeIntent = paymentIntent,
            confirmationOption = confirmationOption,
            alwaysSave = false,
        )

        val call = setSavedSelectionCalls.awaitItem()
        assertThat(call.customerId).isEqualTo(paymentMethod.customerId)
        assertThat(call.savedSelection).isEqualTo(SavedSelection.PaymentMethod(paymentMethod.id))
    }

    private data class SetSavedSelectionCall(
        val customerId: String?,
        val savedSelection: SavedSelection?,
    )

    private fun runScenario(
        block: suspend Scenario.() -> Unit,
    ) {
        val setSavedSelectionCalls = Turbine<SetSavedSelectionCall>()

        val prefsRepositoryFactory = object : PrefsRepository.Factory {
            override fun create(customerId: String?): PrefsRepository {
                return object : PrefsRepository {
                    override suspend fun getSavedSelection(
                        isGooglePayAvailable: Boolean,
                        isLinkAvailable: Boolean
                    ): SavedSelection {
                        throw IllegalStateException("Not expected to be called.")
                    }

                    override fun setSavedSelection(savedSelection: SavedSelection?): Boolean {
                        setSavedSelectionCalls.add(
                            SetSavedSelectionCall(
                                customerId = customerId,
                                savedSelection = savedSelection
                            )
                        )
                        return true
                    }
                }
            }
        }

        val confirmationSaver = ConfirmationSaver(prefsRepositoryFactory)

        Scenario(
            setSavedSelectionCalls = setSavedSelectionCalls,
            confirmationSaver = confirmationSaver,
        ).apply {
            runTest {
                block()
            }
            setSavedSelectionCalls.ensureAllEventsConsumed()
        }
    }

    private data class Scenario(
        val setSavedSelectionCalls: ReceiveTurbine<SetSavedSelectionCall>,
        val confirmationSaver: ConfirmationSaver,
    )
}
