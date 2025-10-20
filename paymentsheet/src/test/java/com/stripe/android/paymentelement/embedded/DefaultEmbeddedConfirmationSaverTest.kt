package com.stripe.android.paymentelement.embedded

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethodCreateParamsFixtures.DEFAULT_CARD
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.FakePrefsRepository
import com.stripe.android.paymentsheet.PrefsRepository
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.testing.PaymentIntentFactory
import com.stripe.android.testing.PaymentMethodFactory
import com.stripe.android.testing.SetupIntentFactory
import org.junit.Test

class DefaultEmbeddedConfirmationSaverTest {
    private val newCardPaymentSelection = PaymentSelection.New.Card(
        DEFAULT_CARD,
        CardBrand.Discover,
        customerRequestedSave = PaymentSelection.CustomerRequestedSave.NoRequest
    )

    @Test
    fun `when paymentMethodMetadata is null, should not save anything`() = testScenario(
        paymentMethodMetadataProvider = { null }
    ) {
        saver.save(PaymentIntentFactory.create())

        assertThat(prefsRepository.paymentSelectionArgs).isEmpty()
    }

    @Test
    fun `when current selection is null, should not save anything`() = testScenario(
        selection = null
    ) {
        saver.save(PaymentIntentFactory.create())

        assertThat(prefsRepository.paymentSelectionArgs).isEmpty()
    }

    @Test
    fun `when selection is New Card and can save with SetupIntent, should save as Saved`() = testScenario(
        selection = newCardPaymentSelection,
        initializationMode = PaymentElementLoader.InitializationMode.SetupIntent(clientSecret = "si_123")
    ) {
        val paymentMethod = PaymentMethodFactory.card()

        saver.save(SetupIntentFactory.create(paymentMethod = paymentMethod))

        assertThat(prefsRepository.paymentSelectionArgs).hasSize(1)
        val savedSelection = prefsRepository.paymentSelectionArgs[0] as? PaymentSelection.Saved
        assertThat(savedSelection?.paymentMethod).isEqualTo(paymentMethod)
    }

    @Test
    fun `when selection is New Card and can save with PaymentIntent and customer requested, should save as Saved`() =
        testScenario(
            selection = newCardPaymentSelection.copy(
                customerRequestedSave = PaymentSelection.CustomerRequestedSave.RequestReuse
            ),
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(clientSecret = "pi_123")
        ) {
            val paymentMethod = PaymentMethodFactory.card()

            saver.save(PaymentIntentFactory.create(paymentMethod = paymentMethod))

            assertThat(prefsRepository.paymentSelectionArgs).hasSize(1)
            val savedSelection = prefsRepository.paymentSelectionArgs[0] as? PaymentSelection.Saved
            assertThat(savedSelection?.paymentMethod).isEqualTo(paymentMethod)
        }

    @Test
    fun `when selection is New Card but cannot save with PaymentIntent, should not save`() = testScenario(
        selection = newCardPaymentSelection,
        initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(clientSecret = "pi_123")
    ) {
        val paymentMethod = PaymentMethodFactory.card()

        saver.save(PaymentIntentFactory.create(paymentMethod = paymentMethod))

        assertThat(prefsRepository.paymentSelectionArgs).isEmpty()
    }

    @Test
    fun `when selection is New Card with force setup future use, should save even if not requested`() = testScenario(
        selection = newCardPaymentSelection,
        initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(clientSecret = "pi_123"),
        forceSetupFutureUseBehaviorAndNewMandate = true
    ) {
        val paymentMethod = PaymentMethodFactory.card()

        saver.save(PaymentIntentFactory.create(paymentMethod = paymentMethod))

        assertThat(prefsRepository.paymentSelectionArgs).hasSize(1)
        val savedSelection = prefsRepository.paymentSelectionArgs[0] as? PaymentSelection.Saved
        assertThat(savedSelection?.paymentMethod).isEqualTo(paymentMethod)
    }

    @Test
    fun `when selection is New Card but payment method is null, should not save`() = testScenario(
        selection = newCardPaymentSelection.copy(
            customerRequestedSave = PaymentSelection.CustomerRequestedSave.RequestReuse
        ),
        initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(clientSecret = "pi_123")
    ) {
        saver.save(PaymentIntentFactory.create(paymentMethod = null))

        assertThat(prefsRepository.paymentSelectionArgs).isEmpty()
    }

    @Test
    fun `when selection is Saved with GooglePay wallet type, should save as GooglePay`() = testScenario(
        selection = PaymentSelection.Saved(
            paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
            walletType = PaymentSelection.Saved.WalletType.GooglePay
        )
    ) {
        saver.save(PaymentIntentFactory.create())

        assertThat(prefsRepository.paymentSelectionArgs).hasSize(1)
        assertThat(prefsRepository.paymentSelectionArgs[0]).isEqualTo(PaymentSelection.GooglePay)
    }

    @Test
    fun `when selection is Saved with Link wallet type, should save the payment method`() = testScenario(
        selection = PaymentSelection.Saved(
            paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
            walletType = PaymentSelection.Saved.WalletType.Link
        )
    ) {
        saver.save(PaymentIntentFactory.create())

        assertThat(prefsRepository.paymentSelectionArgs).hasSize(1)
        val savedSelection = prefsRepository.paymentSelectionArgs[0] as? PaymentSelection.Saved
        assertThat(savedSelection?.paymentMethod).isEqualTo(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
        assertThat(savedSelection?.walletType).isEqualTo(PaymentSelection.Saved.WalletType.Link)
    }

    @Test
    fun `when selection is Saved with no wallet type, should save as-is`() = testScenario(
        selection = PaymentSelection.Saved(
            paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        )
    ) {
        saver.save(PaymentIntentFactory.create())

        assertThat(prefsRepository.paymentSelectionArgs).hasSize(1)
        val savedSelection = prefsRepository.paymentSelectionArgs[0] as? PaymentSelection.Saved
        assertThat(savedSelection?.paymentMethod).isEqualTo(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
        assertThat(savedSelection?.walletType).isNull()
    }

    @Test
    fun `when selection is GooglePay, should save as-is`() = testScenario(
        selection = PaymentSelection.GooglePay
    ) {
        saver.save(PaymentIntentFactory.create())

        assertThat(prefsRepository.paymentSelectionArgs).hasSize(1)
        assertThat(prefsRepository.paymentSelectionArgs[0]).isEqualTo(PaymentSelection.GooglePay)
    }

    @Test
    fun `when selection is Link, should save as-is`() = testScenario(
        selection = PaymentSelection.Link()
    ) {
        saver.save(PaymentIntentFactory.create())

        assertThat(prefsRepository.paymentSelectionArgs).hasSize(1)
        assertThat(prefsRepository.paymentSelectionArgs[0]).isEqualTo(PaymentSelection.Link())
    }

    @Test
    fun `when no customer id, should use null customer id for prefs repository`() = testScenario(
        hasCustomerConfiguration = false
    ) {
        saver.save(PaymentIntentFactory.create())

        assertThat(prefsRepositoryFactory.customerIdArgs).containsExactly(null)
    }

    @Test
    fun `when has customer id, should use customer id for prefs repository`() = testScenario(
        hasCustomerConfiguration = true
    ) {
        saver.save(PaymentIntentFactory.create())

        assertThat(prefsRepositoryFactory.customerIdArgs).containsExactly("cus_123")
    }

    private fun testScenario(
        selection: PaymentSelection? = PaymentSelection.GooglePay,
        paymentMethodMetadataProvider: () -> PaymentMethodMetadata? = {
            PaymentMethodMetadataFactory.create(
                hasCustomerConfiguration = true,
                forceSetupFutureUseBehaviorAndNewMandate = false
            )
        },
        initializationMode: PaymentElementLoader.InitializationMode? =
            PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = "pi_123"
            ),
        forceSetupFutureUseBehaviorAndNewMandate: Boolean = false,
        hasCustomerConfiguration: Boolean = true,
        block: Scenario.() -> Unit
    ) {
        val savedStateHandle = SavedStateHandle()
        val selectionHolder = EmbeddedSelectionHolder(savedStateHandle)
        selectionHolder.set(selection)

        val prefsRepository = FakePrefsRepository()
        val prefsRepositoryFactory = FakePrefsRepositoryFactory(prefsRepository)

        val metadata = paymentMethodMetadataProvider()?.copy(
            forceSetupFutureUseBehaviorAndNewMandate = forceSetupFutureUseBehaviorAndNewMandate,
            customerMetadata = if (hasCustomerConfiguration) {
                PaymentMethodMetadataFixtures.DEFAULT_CUSTOMER_METADATA
            } else {
                null
            }
        )

        val saver = DefaultEmbeddedConfirmationSaver(
            prefsRepositoryFactory = prefsRepositoryFactory,
            paymentMethodMetadataProvider = { metadata },
            selectionHolder = selectionHolder,
            initializationModeProvider = { initializationMode }
        )

        Scenario(
            saver = saver,
            prefsRepository = prefsRepository,
            prefsRepositoryFactory = prefsRepositoryFactory,
            selectionHolder = selectionHolder
        ).block()
    }

    private data class Scenario(
        val saver: DefaultEmbeddedConfirmationSaver,
        val prefsRepository: FakePrefsRepository,
        val prefsRepositoryFactory: FakePrefsRepositoryFactory,
        val selectionHolder: EmbeddedSelectionHolder
    )

    private class FakePrefsRepositoryFactory(
        private val prefsRepository: PrefsRepository
    ) : PrefsRepository.Factory {
        val customerIdArgs = mutableListOf<String?>()

        override fun create(customerId: String?): PrefsRepository {
            customerIdArgs.add(customerId)
            return prefsRepository
        }
    }
}
