package com.stripe.android.lpmfoundations.paymentmethod

import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.definitions.CardDefinition
import com.stripe.android.lpmfoundations.paymentmethod.definitions.KlarnaDefinition
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.SetupIntentFixtures
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.PaymentSheet
import org.junit.Test

internal class PaymentMethodDefinitionTest {
    @Test
    fun `isSupported returns false for PaymentMethodDefinitions that are not in stripeIntent#paymentMethodTypes`() {
        assertThat(KlarnaDefinition.isSupported(PaymentMethodMetadataFactory.create())).isFalse()
    }

    @Test
    fun `isSupported returns false for PaymentMethodDefinitions that do not meet add requirements`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("card", "klarna")
            )
        )
        assertThat(KlarnaDefinition.isSupported(metadata)).isFalse()
    }

    @Test
    fun `isSupported returns true for supported PaymentMethodDefinitions`() {
        assertThat(CardDefinition.isSupported(PaymentMethodMetadataFactory.create())).isTrue()
    }

    @Test
    fun `getFormLayoutConfiguration returns the config for merchantRequestedSave for SetupIntents`() {
        assertThat(CardDefinition.supportedAsSavedPaymentMethod).isTrue()
        val config = CardDefinition.getSetupFutureUsageFieldConfiguration(
            metadata = PaymentMethodMetadataFactory.create(
                stripeIntent = SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD,
            ),
            customerConfiguration = null,
        )!!
        assertThat(config.isSaveForFutureUseValueChangeable).isFalse()
        assertThat(config.saveForFutureUseInitialValue).isTrue()
    }

    @Test
    fun `getFormLayoutConfiguration returns null for SetupIntents when supportedAsSavedPaymentMethod is false`() {
        assertThat(KlarnaDefinition.supportedAsSavedPaymentMethod).isFalse()
        val config = KlarnaDefinition.getSetupFutureUsageFieldConfiguration(
            metadata = PaymentMethodMetadataFactory.create(
                stripeIntent = SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD,
            ),
            customerConfiguration = null,
        )
        assertThat(config).isNull()
    }

    @Test
    fun `getFormLayoutConfiguration returns the config for merchantRequestedSave for PaymentIntents`() {
        assertThat(CardDefinition.supportedAsSavedPaymentMethod).isTrue()
        val config = CardDefinition.getSetupFutureUsageFieldConfiguration(
            metadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                    setupFutureUsage = StripeIntent.Usage.OnSession,
                ),
            ),
            customerConfiguration = null,
        )!!
        assertThat(config.isSaveForFutureUseValueChangeable).isFalse()
        assertThat(config.saveForFutureUseInitialValue).isTrue()
    }

    @Test
    fun `getFormLayoutConfiguration returns null for PaymentIntents when supportedAsSavedPaymentMethod is false and setupFutureUsage is set`() {
        assertThat(KlarnaDefinition.supportedAsSavedPaymentMethod).isFalse()
        val config = KlarnaDefinition.getSetupFutureUsageFieldConfiguration(
            metadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                    setupFutureUsage = StripeIntent.Usage.OnSession,
                ),
            ),
            customerConfiguration = null,
        )
        assertThat(config).isNull()
    }

    @Test
    fun `getFormLayoutConfiguration returns config for userSelectableSave for PaymentIntents when no setupFutureUsage is set and customerConfig is not null`() {
        val config = CardDefinition.getSetupFutureUsageFieldConfiguration(
            metadata = PaymentMethodMetadataFactory.create(),
            customerConfiguration = PaymentSheet.CustomerConfiguration("123", "123"),
        )!!
        assertThat(config.isSaveForFutureUseValueChangeable).isTrue()
        assertThat(config.saveForFutureUseInitialValue).isFalse()
    }

    @Test
    fun `getSetupFutureUsageFieldConfiguration returns config for oneTimeUse for PaymentIntents when no setupFutureUsage is set and customerConfig is null`() {
        val config = CardDefinition.getSetupFutureUsageFieldConfiguration(
            metadata = PaymentMethodMetadataFactory.create(),
            customerConfiguration = null,
        )!!
        assertThat(config.isSaveForFutureUseValueChangeable).isFalse()
        assertThat(config.saveForFutureUseInitialValue).isFalse()
    }
}
