package com.stripe.android.paymentsheet.utils

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentelement.PaymentMethodOptionsSetupFutureUsagePreview
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import org.junit.Test

class SelectionUtilsTest {
    @Test
    fun `should not be able to save if mode is payment intent`() {
        assertThat(
            NEW_SELECTION.canSave(
                PaymentElementLoader.InitializationMode.PaymentIntent(clientSecret = "pi_12345")
            )
        ).isFalse()
    }

    @Test
    fun `should be able to save if mode is payment intent but customer requested it`() {
        assertThat(
            NEW_SELECTION_WITH_CUSTOMER_REQUEST.canSave(
                PaymentElementLoader.InitializationMode.PaymentIntent(clientSecret = "pi_12345")
            )
        ).isTrue()
    }

    @Test
    fun `should be able to save if initialization mode is setup intent`() {
        assertThat(
            NEW_SELECTION.canSave(
                PaymentElementLoader.InitializationMode.SetupIntent(clientSecret = "si_12345")
            )
        ).isTrue()
    }

    @Test
    fun `should be able to save if initialization mode is PI deferred with future setup`() {
        assertThat(
            NEW_SELECTION.canSave(
                PaymentElementLoader.InitializationMode.DeferredIntent(
                    intentConfiguration = PaymentSheet.IntentConfiguration(
                        mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                            amount = 10L,
                            currency = "USD",
                            setupFutureUse = PaymentSheet.IntentConfiguration.SetupFutureUse.OffSession
                        )
                    )
                )
            )
        ).isTrue()
    }

    @Test
    fun `should not be able to save if initialization mode is PI deferred without future setup`() {
        assertThat(
            NEW_SELECTION.canSave(
                PaymentElementLoader.InitializationMode.DeferredIntent(
                    intentConfiguration = PaymentSheet.IntentConfiguration(
                        mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                            amount = 10L,
                            currency = "USD",
                            setupFutureUse = null
                        )
                    )
                )
            )
        ).isFalse()
    }

    @Test
    fun `should not be able to save if initialization mode is PI deferred with 'None' SFU`() {
        assertThat(
            NEW_SELECTION.canSave(
                PaymentElementLoader.InitializationMode.DeferredIntent(
                    intentConfiguration = PaymentSheet.IntentConfiguration(
                        mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                            amount = 10L,
                            currency = "USD",
                            setupFutureUse = PaymentSheet.IntentConfiguration.SetupFutureUse.None,
                        ),
                    ),
                )
            )
        ).isFalse()
    }

    @Test
    fun `should be able to save if customer requested in PI deferred without future setup mode`() {
        assertThat(
            NEW_SELECTION_WITH_CUSTOMER_REQUEST.canSave(
                PaymentElementLoader.InitializationMode.DeferredIntent(
                    intentConfiguration = PaymentSheet.IntentConfiguration(
                        mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                            amount = 10L,
                            currency = "USD",
                            setupFutureUse = null
                        )
                    ),
                )
            )
        ).isTrue()
    }

    @Test
    fun `should be able to save if customer requested in SI deferred mode`() {
        assertThat(
            NEW_SELECTION_WITH_CUSTOMER_REQUEST.canSave(
                PaymentElementLoader.InitializationMode.DeferredIntent(
                    intentConfiguration = PaymentSheet.IntentConfiguration(
                        mode = PaymentSheet.IntentConfiguration.Mode.Setup()
                    ),
                )
            )
        ).isTrue()
    }

    @Test
    fun `should be savable if type has 'OffSession' SFU in payment method options for payment mode`() = perLpmSfuTest(
        setupFutureUse = PaymentSheet.IntentConfiguration.SetupFutureUse.OffSession,
        shouldBeSavable = true,
    )

    @Test
    fun `should be savable if type has 'OnSession' SFU in payment method options for payment mode`() = perLpmSfuTest(
        setupFutureUse = PaymentSheet.IntentConfiguration.SetupFutureUse.OffSession,
        shouldBeSavable = true,
    )

    @Test
    fun `should not be savable if type has 'None' SFU in payment method options for payment mode`() = perLpmSfuTest(
        setupFutureUse = PaymentSheet.IntentConfiguration.SetupFutureUse.None,
        shouldBeSavable = false,
    )

    @OptIn(PaymentMethodOptionsSetupFutureUsagePreview::class)
    private fun perLpmSfuTest(
        setupFutureUse: PaymentSheet.IntentConfiguration.SetupFutureUse,
        shouldBeSavable: Boolean,
    ) {
        assertThat(
            NEW_SELECTION.canSave(
                PaymentElementLoader.InitializationMode.DeferredIntent(
                    intentConfiguration = PaymentSheet.IntentConfiguration(
                        mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                            amount = 5000,
                            currency = "CAD",
                            paymentMethodOptions = PaymentSheet.IntentConfiguration.Mode.Payment.PaymentMethodOptions(
                                setupFutureUsageValues = mapOf(
                                    PaymentMethod.Type.Card to setupFutureUse,
                                ),
                            ),
                        ),
                    )
                )
            )
        ).isEqualTo(shouldBeSavable)

        assertThat(
            NEW_BANK_SELECTION.canSave(
                PaymentElementLoader.InitializationMode.DeferredIntent(
                    intentConfiguration = PaymentSheet.IntentConfiguration(
                        mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                            amount = 5000,
                            currency = "CAD",
                            paymentMethodOptions = PaymentSheet.IntentConfiguration.Mode.Payment.PaymentMethodOptions(
                                setupFutureUsageValues = mapOf(PaymentMethod.Type.USBankAccount to setupFutureUse),
                            ),
                        ),
                    ),
                )
            )
        ).isEqualTo(shouldBeSavable)
    }

    private companion object {
        private val NEW_SELECTION = PaymentSelection.New.Card(
            brand = CardBrand.Visa,
            customerRequestedSave = PaymentSelection.CustomerRequestedSave.NoRequest,
            paymentMethodCreateParams = PaymentMethodCreateParams.Companion.create(
                card = PaymentMethodCreateParams.Card()
            )
        )

        private val NEW_BANK_SELECTION = PaymentMethodFixtures.US_BANK_PAYMENT_SELECTION

        private val NEW_SELECTION_WITH_CUSTOMER_REQUEST = NEW_SELECTION.copy(
            customerRequestedSave = PaymentSelection.CustomerRequestedSave.RequestReuse
        )
    }
}
