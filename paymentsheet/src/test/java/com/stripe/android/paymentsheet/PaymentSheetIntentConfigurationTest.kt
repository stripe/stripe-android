package com.stripe.android.paymentsheet

import com.google.common.truth.Truth.assertThat
import com.stripe.android.SharedPaymentTokenSessionPreview
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentelement.PaymentMethodOptionsSetupFutureUsagePreview
import com.stripe.android.paymentsheet.PaymentSheet.IntentConfiguration
import org.junit.Test

@OptIn(SharedPaymentTokenSessionPreview::class, PaymentMethodOptionsSetupFutureUsagePreview::class)
internal class PaymentSheetIntentConfigurationTest {
    @Test
    fun `build returns defaults when only mode is provided`() {
        val state = IntentConfiguration(IntentConfiguration.Mode.Setup()).build()

        assertThat(state).isEqualTo(
            IntentConfiguration.State(
                mode = IntentConfiguration.Mode.Setup.State(
                    currency = null,
                    setupFutureUse = IntentConfiguration.SetupFutureUse.OffSession,
                ),
                paymentMethodTypes = emptyList(),
                paymentMethodConfigurationId = null,
                onBehalfOf = null,
                requireCvcRecollection = false,
                intentBehavior = IntentConfiguration.IntentBehavior.Default,
            )
        )
    }

    @Test
    fun `sharedPaymentTokenSession sets shared payment token intent behavior`() {
        val state = IntentConfiguration(IntentConfiguration.Mode.Setup())
            .sharedPaymentTokenSession(
                IntentConfiguration.SellerDetails(
                    businessName = "Business",
                    networkId = "network",
                    externalId = "external",
                )
            )
            .build()

        assertThat(state.intentBehavior).isEqualTo(
            IntentConfiguration.IntentBehavior.SharedPaymentToken(
                sellerDetails = IntentConfiguration.SellerDetails.State(
                    businessName = "Business",
                    networkId = "network",
                    externalId = "external",
                )
            )
        )
    }

    @Test
    fun `build snapshots payment method types at build time`() {
        val paymentMethodTypes = mutableListOf("card")
        val configuration = IntentConfiguration(IntentConfiguration.Mode.Setup())
            .paymentMethodTypes(paymentMethodTypes)
        paymentMethodTypes.add("us_bank_account")

        val state = configuration.build()
        paymentMethodTypes.clear()
        paymentMethodTypes.add("sepa_debit")

        assertThat(state.paymentMethodTypes).containsExactly("card", "us_bank_account").inOrder()
        assertThat(configuration.build().paymentMethodTypes).containsExactly("sepa_debit")

        configuration.paymentMethodTypes(listOf("us_bank_account"))

        assertThat(state.paymentMethodTypes).containsExactly("card", "us_bank_account").inOrder()
        assertThat(configuration.build().paymentMethodTypes).containsExactly("us_bank_account")
    }

    @Test
    fun `build snapshots merchant-owned setup future usage values at build time`() {
        val setupFutureUsageValues = mutableMapOf(
            PaymentMethod.Type.Card to IntentConfiguration.SetupFutureUse.OffSession,
        )
        val configuration = IntentConfiguration(
            IntentConfiguration.Mode.Payment(
                amount = 1000,
                currency = "usd",
                captureMethod = IntentConfiguration.CaptureMethod.Automatic,
            )
                .paymentMethodOptions(
                    IntentConfiguration.Mode.Payment.PaymentMethodOptions()
                        .setupFutureUsageValues(setupFutureUsageValues)
                )
        )
        setupFutureUsageValues[PaymentMethod.Type.USBankAccount] = IntentConfiguration.SetupFutureUse.OnSession

        val state = configuration.build().mode as IntentConfiguration.Mode.Payment.State
        setupFutureUsageValues.clear()
        setupFutureUsageValues[PaymentMethod.Type.Card] = IntentConfiguration.SetupFutureUse.None

        assertThat(state.paymentMethodOptions?.setupFutureUsageValues).containsExactly(
            PaymentMethod.Type.Card,
            IntentConfiguration.SetupFutureUse.OffSession,
            PaymentMethod.Type.USBankAccount,
            IntentConfiguration.SetupFutureUse.OnSession,
        )
        val updatedState = configuration.build().mode as IntentConfiguration.Mode.Payment.State
        assertThat(updatedState.paymentMethodOptions?.setupFutureUsageValues).containsExactly(
            PaymentMethod.Type.Card,
            IntentConfiguration.SetupFutureUse.None,
        )
    }

    @Test
    fun `payment mode build returns defaults when only required values are provided`() {
        val state = IntentConfiguration.Mode.Payment(
            amount = 1000,
            currency = "usd",
            captureMethod = IntentConfiguration.CaptureMethod.AutomaticAsync,
        ).build()

        assertThat(state).isEqualTo(
            IntentConfiguration.Mode.Payment.State(
                amount = 1000,
                currency = "usd",
                setupFutureUse = null,
                captureMethod = IntentConfiguration.CaptureMethod.AutomaticAsync,
                paymentMethodOptions = null,
            )
        )
    }

    @Suppress("DEPRECATION")
    @Test
    fun `deprecated payment constructor defaults to automatic capture`() {
        val state = IntentConfiguration.Mode.Payment(
            amount = 1000,
            currency = "usd",
            setupFutureUse = null,
        ).build()

        assertThat(state.captureMethod).isEqualTo(IntentConfiguration.CaptureMethod.Automatic)
    }

    @Test
    fun `build uses nested payment mode builder values at build time`() {
        val mode = IntentConfiguration.Mode.Payment(
            amount = 1000,
            currency = "usd",
            captureMethod = IntentConfiguration.CaptureMethod.Manual,
        )
        val configuration = IntentConfiguration(mode)

        mode.setupFutureUse(IntentConfiguration.SetupFutureUse.OnSession)
            .paymentMethodOptions(
                IntentConfiguration.Mode.Payment.PaymentMethodOptions()
                    .setupFutureUsageValues(
                        mapOf(PaymentMethod.Type.Card to IntentConfiguration.SetupFutureUse.OffSession)
                    )
            )
        val state = configuration.build().mode as IntentConfiguration.Mode.Payment.State

        assertThat(state.setupFutureUse).isEqualTo(IntentConfiguration.SetupFutureUse.OnSession)
        assertThat(state.captureMethod).isEqualTo(IntentConfiguration.CaptureMethod.Manual)
        assertThat(state.paymentMethodOptions?.setupFutureUsageValues).containsExactly(
            PaymentMethod.Type.Card,
            IntentConfiguration.SetupFutureUse.OffSession,
        )
    }

    @Test
    fun `setup mode build returns values set through builder methods`() {
        val state = IntentConfiguration.Mode.Setup()
            .currency("usd")
            .setupFutureUse(IntentConfiguration.SetupFutureUse.OnSession)
            .build()

        assertThat(state).isEqualTo(
            IntentConfiguration.Mode.Setup.State(
                currency = "usd",
                setupFutureUse = IntentConfiguration.SetupFutureUse.OnSession,
            )
        )
        assertThat(state.captureMethod).isNull()
    }

    @Suppress("DEPRECATION")
    @Test
    fun `deprecated constructors produce the same state as builder methods`() {
        val deprecated = IntentConfiguration(
            mode = IntentConfiguration.Mode.Payment(
                amount = 1000,
                currency = "usd",
                setupFutureUse = IntentConfiguration.SetupFutureUse.OffSession,
                captureMethod = IntentConfiguration.CaptureMethod.Manual,
            ),
            paymentMethodTypes = listOf("card"),
            paymentMethodConfigurationId = "pmc_123",
            onBehalfOf = "acct_123",
            requireCvcRecollection = true,
        )
        val builder = IntentConfiguration(
            IntentConfiguration.Mode.Payment(
                amount = 1000,
                currency = "usd",
                captureMethod = IntentConfiguration.CaptureMethod.Manual,
            ).setupFutureUse(IntentConfiguration.SetupFutureUse.OffSession)
        )
            .paymentMethodTypes(listOf("card"))
            .paymentMethodConfigurationId("pmc_123")
            .onBehalfOf("acct_123")
            .requireCvcRecollection(true)

        val state = builder.build()

        assertThat(state.paymentMethodTypes).containsExactly("card")
        assertThat(state.paymentMethodConfigurationId).isEqualTo("pmc_123")
        assertThat(state.onBehalfOf).isEqualTo("acct_123")
        assertThat(state.requireCvcRecollection).isTrue()
        assertThat(state.intentBehavior).isEqualTo(IntentConfiguration.IntentBehavior.Default)
        assertThat(deprecated.build()).isEqualTo(state)
    }
}
