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
    fun `build returns values set through builder methods`() {
        val state = IntentConfiguration(IntentConfiguration.Mode.Payment(amount = 1000, currency = "usd"))
            .paymentMethodTypes(listOf("card"))
            .paymentMethodConfigurationId("pmc_123")
            .onBehalfOf("acct_123")
            .requireCvcRecollection(true)
            .build()

        assertThat(state.paymentMethodTypes).containsExactly("card")
        assertThat(state.paymentMethodConfigurationId).isEqualTo("pmc_123")
        assertThat(state.onBehalfOf).isEqualTo("acct_123")
        assertThat(state.requireCvcRecollection).isTrue()
        assertThat(state.intentBehavior).isEqualTo(IntentConfiguration.IntentBehavior.Default)
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
    fun `build returns a snapshot that later builder calls do not change`() {
        val configuration = IntentConfiguration(IntentConfiguration.Mode.Setup())
            .paymentMethodTypes(listOf("card"))
        val state = configuration.build()

        configuration.paymentMethodTypes(listOf("us_bank_account"))

        assertThat(state.paymentMethodTypes).containsExactly("card")
        assertThat(configuration.build().paymentMethodTypes).containsExactly("us_bank_account")
    }

    @Test
    fun `build uses nested builder values at build time`() {
        val mode = IntentConfiguration.Mode.Payment(amount = 1000, currency = "usd")
        val configuration = IntentConfiguration(mode)

        mode.captureMethod(IntentConfiguration.CaptureMethod.Manual)

        assertThat(configuration.build().mode.captureMethod).isEqualTo(IntentConfiguration.CaptureMethod.Manual)
    }

    @Test
    fun `payment mode build returns defaults when only required values are provided`() {
        val state = IntentConfiguration.Mode.Payment(amount = 1000, currency = "usd").build()

        assertThat(state).isEqualTo(
            IntentConfiguration.Mode.Payment.State(
                amount = 1000,
                currency = "usd",
                setupFutureUse = null,
                captureMethod = IntentConfiguration.CaptureMethod.Automatic,
                paymentMethodOptions = null,
            )
        )
    }

    @Test
    fun `payment mode build returns values set through builder methods`() {
        val state = IntentConfiguration.Mode.Payment(amount = 1000, currency = "usd")
            .setupFutureUse(IntentConfiguration.SetupFutureUse.OnSession)
            .captureMethod(IntentConfiguration.CaptureMethod.Manual)
            .paymentMethodOptions(
                IntentConfiguration.Mode.Payment.PaymentMethodOptions()
                    .setupFutureUsageValues(
                        mapOf(PaymentMethod.Type.Card to IntentConfiguration.SetupFutureUse.OffSession)
                    )
            )
            .build()

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
            IntentConfiguration.Mode.Payment(amount = 1000, currency = "usd")
                .setupFutureUse(IntentConfiguration.SetupFutureUse.OffSession)
                .captureMethod(IntentConfiguration.CaptureMethod.Manual)
        )
            .paymentMethodTypes(listOf("card"))
            .paymentMethodConfigurationId("pmc_123")
            .onBehalfOf("acct_123")
            .requireCvcRecollection(true)

        assertThat(deprecated.build()).isEqualTo(builder.build())
    }
}
