package com.stripe.android.paymentelement.embedded.content

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.model.CommonConfiguration
import com.stripe.android.common.model.asCommonConfiguration
import com.stripe.android.core.mainthread.MainThreadSavedStateHandle
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.SetupIntentFixtures
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentelement.embedded.EmbeddedFormHelperFactory
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.content.DefaultEmbeddedSelectionChooser.Companion.PREVIOUS_CONFIGURATION_KEY
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.testing.PaymentMethodFactory
import com.stripe.android.utils.FakeLinkConfigurationCoordinator
import com.stripe.android.utils.NullCardAccountRangeRepositoryFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalEmbeddedPaymentElementApi::class)
internal class DefaultEmbeddedSelectionChooserTest {
    private val defaultConfiguration = EmbeddedPaymentElement.Configuration.Builder("Example, Inc.")
        .build()
        .asCommonConfiguration()

    @Test
    fun `Uses new payment selection if there's no existing one`() = runScenario {
        val selection = chooser.choose(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(isGooglePayReady = true),
            paymentMethods = null,
            previousSelection = null,
            newSelection = PaymentSelection.GooglePay,
            newConfiguration = defaultConfiguration,
        )
        assertThat(selection).isEqualTo(PaymentSelection.GooglePay)
    }

    @Test
    fun `Can use existing payment selection if it's still supported`() = runScenario {
        val previousSelection = PaymentMethodFixtures.CASHAPP_PAYMENT_SELECTION

        val paymentMethod = PaymentMethodFixtures.createCard()
        val newSelection = PaymentSelection.Saved(paymentMethod)
        val selection = chooser.choose(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                    paymentMethodTypes = listOf("card", "cashapp")
                )
            ),
            paymentMethods = PaymentMethodFixtures.createCards(3) + paymentMethod,
            previousSelection = previousSelection,
            newSelection = newSelection,
            newConfiguration = defaultConfiguration,
        )
        assertThat(selection).isEqualTo(previousSelection)
    }

    @Test
    fun `Can use existing saved payment selection if it's still supported`() = runScenario {
        val paymentMethod = PaymentMethodFixtures.createCard()
        val previousSelection = PaymentSelection.Saved(paymentMethod)

        val selection = chooser.choose(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(isGooglePayReady = true),
            paymentMethods = PaymentMethodFixtures.createCards(3) + paymentMethod,
            previousSelection = previousSelection,
            newSelection = PaymentSelection.GooglePay,
            newConfiguration = defaultConfiguration,
        )
        assertThat(selection).isEqualTo(previousSelection)
    }

    @Test
    fun `Can't use existing saved payment method if it's no longer allowed`() = runScenario {
        val previousSelection = PaymentSelection.Saved(PaymentMethodFixtures.SEPA_DEBIT_PAYMENT_METHOD)

        val selection = chooser.choose(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
            paymentMethods = PaymentMethodFixtures.createCards(3) + PaymentMethodFixtures.SEPA_DEBIT_PAYMENT_METHOD,
            previousSelection = previousSelection,
            newSelection = null,
            newConfiguration = defaultConfiguration,
        )
        assertThat(selection).isNull()
    }

    @Test
    fun `Can't use existing saved payment method if it's no longer available`() = runScenario {
        val previousSelection = PaymentSelection.Saved(PaymentMethodFactory.cashAppPay())

        val selection = chooser.choose(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                    paymentMethodTypes = listOf("card", "cashapp")
                )
            ),
            paymentMethods = PaymentMethodFixtures.createCards(3),
            previousSelection = previousSelection,
            newSelection = null,
            newConfiguration = defaultConfiguration,
        )
        assertThat(selection).isNull()
    }

    @Test
    fun `Can use external payment method if it's supported`() = runScenario {
        val previousSelection =
            PaymentMethodFixtures.createExternalPaymentMethod(PaymentMethodFixtures.PAYPAL_EXTERNAL_PAYMENT_METHOD_SPEC)

        val selection = chooser.choose(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                externalPaymentMethodSpecs = listOf(PaymentMethodFixtures.PAYPAL_EXTERNAL_PAYMENT_METHOD_SPEC),
                isGooglePayReady = true,
            ),
            paymentMethods = PaymentMethodFixtures.createCards(3),
            previousSelection = previousSelection,
            newSelection = PaymentSelection.GooglePay,
            newConfiguration = defaultConfiguration,
        )
        assertThat(selection).isEqualTo(previousSelection)
    }

    @Test
    fun `Can't use external payment method if it's not returned by backend`() = runScenario {
        val previousSelection =
            PaymentMethodFixtures.createExternalPaymentMethod(PaymentMethodFixtures.PAYPAL_EXTERNAL_PAYMENT_METHOD_SPEC)

        val selection = chooser.choose(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                externalPaymentMethodSpecs = listOf(),
            ),
            paymentMethods = PaymentMethodFixtures.createCards(3),
            previousSelection = previousSelection,
            newSelection = null,
            newConfiguration = defaultConfiguration,
        )
        assertThat(selection).isNull()
    }

    @Test
    fun `PaymentSelection is preserved when config changes are not volatile`() = runScenario {
        val previousSelection = PaymentSelection.GooglePay
        val paymentMethod = PaymentMethodFixtures.createCard()
        val newSelection = PaymentSelection.Saved(paymentMethod)

        storePreviousChooseState(configuration = defaultConfiguration.copy(merchantDisplayName = "Hi"))
        val selection = chooser.choose(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(isGooglePayReady = true),
            paymentMethods = PaymentMethodFixtures.createCards(3) + paymentMethod,
            previousSelection = previousSelection,
            newSelection = newSelection,
            newConfiguration = defaultConfiguration,
        )
        assertThat(selection).isEqualTo(previousSelection)
    }

    @Test
    fun `PaymentSelection is not preserved when config changes are volatile`() = runScenario {
        val previousSelection = PaymentSelection.GooglePay
        val paymentMethod = PaymentMethodFixtures.createCard()
        val newSelection = PaymentSelection.Saved(paymentMethod)

        storePreviousChooseState(
            configuration = defaultConfiguration.copy(
                defaultBillingDetails = PaymentSheet.BillingDetails(email = "jaynewstrom@example.com")
            )
        )
        val selection = chooser.choose(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(isGooglePayReady = true),
            paymentMethods = PaymentMethodFixtures.createCards(3) + paymentMethod,
            previousSelection = previousSelection,
            newSelection = newSelection,
            newConfiguration = defaultConfiguration,
        )
        assertThat(selection).isEqualTo(newSelection)
    }

    @Test
    fun `PaymentSelection is not preserved when form type is user interaction required`() = runScenario {
        val previousSelection = PaymentMethodFixtures.CASHAPP_PAYMENT_SELECTION

        storePreviousChooseState()
        val selection = chooser.choose(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                stripeIntent = SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD.copy(
                    paymentMethodTypes = listOf("card", "cashapp"),
                ),
                billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                    name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                ),
            ),
            paymentMethods = emptyList(),
            previousSelection = previousSelection,
            newSelection = null,
            newConfiguration = defaultConfiguration,
        )
        assertThat(selection).isNull()
    }

    @Test
    fun `PaymentSelection is preserved when form type changes to mandate`() = runScenario {
        val previousSelection = PaymentMethodFixtures.CASHAPP_PAYMENT_SELECTION

        storePreviousChooseState()
        val selection = chooser.choose(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                stripeIntent = SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD.copy(
                    paymentMethodTypes = listOf("card", "cashapp"),
                ),
            ),
            paymentMethods = emptyList(),
            previousSelection = previousSelection,
            newSelection = null,
            newConfiguration = defaultConfiguration,
        )
        assertThat(selection).isEqualTo(previousSelection)
    }

    @Test
    fun `PaymentSelection is preserved when form type changes to empty`() = runScenario {
        val previousSelection = PaymentMethodFixtures.CASHAPP_PAYMENT_SELECTION

        storePreviousChooseState()
        val selection = chooser.choose(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                    paymentMethodTypes = listOf("card", "cashapp"),
                )
            ),
            paymentMethods = emptyList(),
            previousSelection = previousSelection,
            newSelection = null,
            newConfiguration = defaultConfiguration,
        )
        assertThat(selection).isEqualTo(previousSelection)
    }

    @Test
    fun `PaymentSelection is preserved when form type stays the same`() = runScenario {
        val previousSelection = PaymentMethodFixtures.CASHAPP_PAYMENT_SELECTION

        storePreviousChooseState()
        val selection = chooser.choose(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                stripeIntent = SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD.copy(
                    paymentMethodTypes = listOf("card", "cashapp"),
                ),
            ),
            paymentMethods = emptyList(),
            previousSelection = previousSelection,
            newSelection = null,
            newConfiguration = defaultConfiguration,
        )
        assertThat(selection).isEqualTo(previousSelection)
    }

    @Test
    fun `previousConfig is set when calling choose`() = runScenario {
        val previousSelection = PaymentSelection.GooglePay
        val paymentMethod = PaymentMethodFixtures.createCard()
        val newSelection = PaymentSelection.Saved(paymentMethod)

        assertThat(savedStateHandle.get<CommonConfiguration>(PREVIOUS_CONFIGURATION_KEY)).isNull()
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create(isGooglePayReady = true)
        val selection = chooser.choose(
            paymentMethodMetadata = paymentMethodMetadata,
            paymentMethods = PaymentMethodFixtures.createCards(3) + paymentMethod,
            previousSelection = previousSelection,
            newSelection = newSelection,
            newConfiguration = defaultConfiguration,
        )
        assertThat(selection).isEqualTo(previousSelection)
        assertThat(savedStateHandle.get<CommonConfiguration>(PREVIOUS_CONFIGURATION_KEY))
            .isEqualTo(defaultConfiguration)
    }

    private fun runScenario(
        block: Scenario.() -> Unit,
    ) = runTest {
        val savedStateHandle = SavedStateHandle()
        val formHelperFactory = EmbeddedFormHelperFactory(
            linkConfigurationCoordinator = FakeLinkConfigurationCoordinator(),
            embeddedSelectionHolder = EmbeddedSelectionHolder(MainThreadSavedStateHandle(savedStateHandle)),
            cardAccountRangeRepositoryFactory = NullCardAccountRangeRepositoryFactory,
        )
        Scenario(
            chooser = DefaultEmbeddedSelectionChooser(
                savedStateHandle = savedStateHandle,
                formHelperFactory = formHelperFactory,
                coroutineScope = CoroutineScope(Dispatchers.Unconfined),
            ),
            savedStateHandle = savedStateHandle,
        ).block()
    }

    private fun Scenario.storePreviousChooseState(
        configuration: CommonConfiguration = defaultConfiguration,
    ) {
        savedStateHandle[PREVIOUS_CONFIGURATION_KEY] = configuration
    }

    private class Scenario(
        val chooser: EmbeddedSelectionChooser,
        val savedStateHandle: SavedStateHandle,
    )
}
