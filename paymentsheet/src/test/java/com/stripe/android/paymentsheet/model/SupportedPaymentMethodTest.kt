package com.stripe.android.paymentsheet.model

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD_CARD_SFU_SET
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import com.stripe.android.paymentsheet.PaymentSheetFixtures.CONFIG_CUSTOMER
import com.stripe.android.testing.PaymentIntentFactory
import com.stripe.android.ui.core.forms.resources.LpmRepository
import com.stripe.android.ui.core.forms.resources.LpmRepository.SupportedPaymentMethod
import kotlinx.serialization.Serializable
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.File

@RunWith(AndroidJUnit4::class)
class SupportedPaymentMethodTest {
    private val card = LpmRepository.HardcodedCard

    @Test
    fun `If the intent has SFU set on top level or on LPM`() {
        assertThat(
            LpmRepository.HardcodedCard
                .getSpecWithFullfilledRequirements(
                    PI_REQUIRES_PAYMENT_METHOD_CARD_SFU_SET,
                    CONFIG_CUSTOMER
                )?.showCheckbox
        ).isFalse()
    }

    /**
     * To create a baseline for a payment method, create a new file <payment_method>-support.csv,
     * comment out the assert in this test, and copy the output into the new csv file
     */
    @Test
    fun `Test supported payment method baseline`() {
        val lpmRepository = LpmRepository(
            arguments = LpmRepository.LpmRepositoryArguments(
                resources = ApplicationProvider.getApplicationContext<Application>().resources,
            ),
            lpmInitialFormData = LpmRepository.LpmInitialFormData(),
        ).apply {
            this.update(
                PaymentIntentFactory.create(
                    paymentMethodTypes = PaymentMethod.Type.values().map { it.code },
                ),
                null
            )
        }
        for (lpm in lpmRepository.values()) {
            val resource = File(requireNotNull(javaClass.classLoader).getResource("${lpm.code}-support.csv").file)
            val baseline = resource.readText()
            val baselineLines = baseline.split("\n")

            val csvOutput = StringBuilder()
            csvOutput.append(
                "lpm, ${PaymentIntentTestInput.toCsvHeader()}, ${TestOutput.toCsvHeader()}\n"
            )

            generatePaymentIntentScenarios().mapIndexed { index, testInput ->
                val formDescriptor = lpm.getSpecWithFullfilledRequirements(testInput.getIntent(lpm), testInput.getConfig())
                val testOutput = TestOutput.create(
                    supportCustomerSavedCard = getSupportedSavedCustomerPMs(
                        testInput.getIntent(lpm),
                        testInput.getConfig(),
                        lpmRepository
                    ).contains(lpm),
                    formExists = formDescriptor != null,
                    formShowsSaveCheckbox = formDescriptor?.showCheckbox,
                    formShowsCheckboxControlledFields = formDescriptor?.showCheckboxControlledFields,
                    supportsAdding = getPMsToAdd(
                        testInput.getIntent(lpm),
                        testInput.getConfig(),
                        lpmRepository
                    ).contains(lpm)
                )

                val input = testInput.copy(intentPMs = testInput.intentPMs.plus(lpm.code))
                val line = "${lpm.code}, ${input.toCsv()}, ${testOutput.toCsv()}\n"
                csvOutput.append(line)

                assertWithMessage("Line: ${index + 2}")
                    .that(line.trim())
                    .isEqualTo(baselineLines[index + 1])
            }

            if (baseline != csvOutput.toString()) {
                println(csvOutput.toString())
            }
        }
    }

    @Test
    fun `Test supported payment method doesn't filter with empty unactivated list in test mode`() {
        val lpmRepository = LpmRepository(
            arguments = LpmRepository.LpmRepositoryArguments(
                resources = ApplicationProvider.getApplicationContext<Application>().resources,
            ),
            lpmInitialFormData = LpmRepository.LpmInitialFormData(),
        ).apply {
            this.update(
                PaymentIntentFactory.create(
                    paymentMethodTypes = PaymentMethod.Type.values().map { it.code },
                ),
                null
            )
        }
        val mockIntent = mock<PaymentIntent>()
        whenever(mockIntent.paymentMethodTypes).thenReturn(listOf("card"))
        whenever(mockIntent.isLiveMode).thenReturn(false)
        whenever(mockIntent.unactivatedPaymentMethods).thenReturn(emptyList())

        val expected = listOf<SupportedPaymentMethod>().plus(card)

        assertThat(getPMsToAdd(mockIntent, PaymentSheet.Configuration("Some Name"), lpmRepository)).isEqualTo(expected)
    }

    @Test
    fun `Test supported payment method doesn't filter with empty unactivated list in live mode`() {
        val lpmRepository = LpmRepository(
            arguments = LpmRepository.LpmRepositoryArguments(
                resources = ApplicationProvider.getApplicationContext<Application>().resources,
            ),
            lpmInitialFormData = LpmRepository.LpmInitialFormData(),
        ).apply {
            this.update(
                PaymentIntentFactory.create(
                    paymentMethodTypes = PaymentMethod.Type.values().map { it.code },
                ),
                null
            )
        }
        val mockIntent = mock<PaymentIntent>()
        whenever(mockIntent.paymentMethodTypes).thenReturn(listOf("card"))
        whenever(mockIntent.isLiveMode).thenReturn(true)
        whenever(mockIntent.unactivatedPaymentMethods).thenReturn(emptyList())

        val expected = listOf<SupportedPaymentMethod>().plus(card)

        assertThat(getPMsToAdd(mockIntent, PaymentSheet.Configuration("Some Name"), lpmRepository)).isEqualTo(expected)
    }

    @Test
    fun `Test supported payment method does filter with unactivated list in live mode`() {
        val lpmRepository = LpmRepository(
            arguments = LpmRepository.LpmRepositoryArguments(
                resources = ApplicationProvider.getApplicationContext<Application>().resources,
            ),
            lpmInitialFormData = LpmRepository.LpmInitialFormData(),
        ).apply {
            this.update(
                PaymentIntentFactory.create(
                    paymentMethodTypes = PaymentMethod.Type.values().map { it.code },
                ),
                null
            )
        }
        val mockIntent = mock<PaymentIntent>()
        whenever(mockIntent.paymentMethodTypes).thenReturn(listOf("card"))
        whenever(mockIntent.isLiveMode).thenReturn(true)
        whenever(mockIntent.unactivatedPaymentMethods).thenReturn(listOf("card"))

        val expected = listOf<SupportedPaymentMethod>()

        assertThat(getPMsToAdd(mockIntent, PaymentSheet.Configuration("Some Name"), lpmRepository)).isEqualTo(expected)
    }

    @Test
    fun `Test supported payment method doesn't filter with unactivated list in test mode`() {
        val lpmRepository = LpmRepository(
            arguments = LpmRepository.LpmRepositoryArguments(
                resources = ApplicationProvider.getApplicationContext<Application>().resources,
            ),
            lpmInitialFormData = LpmRepository.LpmInitialFormData(),
        ).apply {
            this.update(
                PaymentIntentFactory.create(
                    paymentMethodTypes = PaymentMethod.Type.values().map { it.code },
                ),
                null
            )
        }
        val mockIntent = mock<PaymentIntent>()
        whenever(mockIntent.paymentMethodTypes).thenReturn(listOf("card"))
        whenever(mockIntent.isLiveMode).thenReturn(false)
        whenever(mockIntent.unactivatedPaymentMethods).thenReturn(listOf("card"))

        val expected = listOf<SupportedPaymentMethod>().plus(card)

        assertThat(getPMsToAdd(mockIntent, PaymentSheet.Configuration("Some Name"), lpmRepository)).isEqualTo(expected)
    }

    @Test
    fun `Test supported payment method filters us bank account when FC SDK not available`() {
        val lpmRepository = LpmRepository(
            arguments = LpmRepository.LpmRepositoryArguments(
                resources = ApplicationProvider.getApplicationContext<Application>().resources,
            ),
            lpmInitialFormData = LpmRepository.LpmInitialFormData(),
        ).apply {
            initializeWithPaymentMethods(
                mapOf(
                    PaymentMethod.Type.Card.code to card,
                    PaymentMethod.Type.USBankAccount.code to LpmRepository.hardCodedUsBankAccount
                )
            )
        }

        val paymentIntent = PaymentIntentFactory.create(
            paymentMethodTypes = listOf("card", "us_bank_account")
        )

        val expected = listOf(card)

        assertThat(
            getPMsToAdd(
                stripeIntent = paymentIntent,
                config = PaymentSheet.Configuration("Test", allowsDelayedPaymentMethods = true),
                lpmRepository = lpmRepository,
                isFinancialConnectionsAvailable = { false },
            )
        ).isEqualTo(expected)
    }

    @Test
    fun `Test supported payment method contains us bank account when FC SDK available`() {
        val lpmRepository = LpmRepository(
            arguments = LpmRepository.LpmRepositoryArguments(
                resources = ApplicationProvider.getApplicationContext<Application>().resources,
            ),
            lpmInitialFormData = LpmRepository.LpmInitialFormData(),
        ).apply {
            initializeWithPaymentMethods(
                mapOf(
                    PaymentMethod.Type.Card.code to LpmRepository.HardcodedCard,
                    PaymentMethod.Type.USBankAccount.code to LpmRepository.hardCodedUsBankAccount
                )
            )
        }

        val paymentIntent = PaymentIntentFactory.create(
            paymentMethodTypes = listOf("card", "us_bank_account")
        )

        val expected = listOf(card, LpmRepository.hardCodedUsBankAccount)

        assertThat(
            getPMsToAdd(
                stripeIntent = paymentIntent,
                config = PaymentSheet.Configuration("Test", allowsDelayedPaymentMethods = true),
                lpmRepository = lpmRepository,
                isFinancialConnectionsAvailable = { true },
            )
        ).isEqualTo(expected)
    }

    /**
     * This will generate payment intent scenarios for all combinations of customers, lpm types in the intent, shipping, and SFU states
     */
    private fun generatePaymentIntentScenarios(): List<PaymentIntentTestInput> {
        val scenarios = mutableListOf<PaymentIntentTestInput>()
        val customerStates = setOf(true, false)
        val setupFutureUsage = setOf(StripeIntent.Usage.OffSession, StripeIntent.Usage.OnSession, null)
        val allowsDelayedPayment = setOf(true, false)
        val hasShippingAddress = setOf(false, true)

        hasShippingAddress.forEach { hasShipping ->
            customerStates.forEach { customer ->
                setupFutureUsage.forEach { usage ->
                    allowsDelayedPayment.forEach { delayed ->
                        scenarios.addAll(
                            listOf(
                                PaymentIntentTestInput(
                                    hasCustomer = customer,
                                    intentPMs = setOf(card.code),
                                    intentSetupFutureUsage = usage,
                                    intentHasShipping = hasShipping,
                                    allowsDelayedPayment = delayed
                                ),
                                PaymentIntentTestInput(
                                    hasCustomer = customer,
                                    intentPMs = setOf(
                                        "card",
                                        "eps"
                                    ),
                                    intentSetupFutureUsage = usage,
                                    intentHasShipping = hasShipping,
                                    allowsDelayedPayment = delayed
                                )
                            )
                        )
                    }
                }
            }
        }
        return scenarios
    }

    @Serializable
    internal data class SetupIntentTestCase(
        val testInput: SetupIntentTestInput,
        val testOutput: TestOutput
    )

    @Serializable
    internal data class SetupIntentTestInput(
        val lpmTypeFormCode: String,
        val intentLpms: List<String>
    )

    @Serializable
    internal data class PaymentIntentTestInput(
        val hasCustomer: Boolean,
        val intentSetupFutureUsage: StripeIntent.Usage?,
        val intentPMs: Set<String>,
        val intentHasShipping: Boolean,
        val allowsDelayedPayment: Boolean = true
    ) {
        companion object {
            fun toCsvHeader() = "hasCustomer, allowsDelayedPayment, intentSetupFutureUsage, intentHasShipping, intentLpms"
        }

        fun toCsv() = "$hasCustomer, $allowsDelayedPayment, $intentSetupFutureUsage, $intentHasShipping, ${intentPMs.joinToString("/")}"

        fun getIntent(lpm: SupportedPaymentMethod) = when (intentHasShipping) {
            false ->
                PaymentIntentFixtures.PI_WITH_SHIPPING.copy(
                    shipping = null,
                    setupFutureUsage = intentSetupFutureUsage,
                    paymentMethodTypes = intentPMs.plus(lpm.code).toList()
                )
            true ->
                PaymentIntentFixtures.PI_WITH_SHIPPING.copy(
                    setupFutureUsage = intentSetupFutureUsage,
                    paymentMethodTypes = intentPMs.plus(lpm.code).toList()
                )
        }

        fun getConfig() = if (hasCustomer) {
            PaymentSheetFixtures.ARGS_CUSTOMER_WITHOUT_GOOGLEPAY.config.copy(
                allowsDelayedPaymentMethods = allowsDelayedPayment
            )
        } else {
            PaymentSheetFixtures.ARGS_WITHOUT_CUSTOMER.config.copy(
                allowsDelayedPaymentMethods = allowsDelayedPayment
            )
        }
    }

    @Serializable
    internal data class TestOutput(
        val supportCustomerSavedCard: Boolean,
        val formExists: Boolean,
        val supportsAdding: Boolean,
        val formType: String
    ) {
        companion object {
            fun create(
                supportCustomerSavedCard: Boolean,
                formExists: Boolean,
                formShowsSaveCheckbox: Boolean?, // null if no form
                formShowsCheckboxControlledFields: Boolean?, // null if let form decide
                supportsAdding: Boolean
            ) = TestOutput(
                supportCustomerSavedCard = supportCustomerSavedCard,
                formExists = formExists,
                supportsAdding = supportsAdding,
                formType = when {
                    formShowsSaveCheckbox == false && formShowsCheckboxControlledFields == false -> "oneTime"
                    formShowsSaveCheckbox == false && formShowsCheckboxControlledFields == true -> "merchantRequiredSave"
                    formShowsSaveCheckbox == true -> "userSelectedSave"
                    else -> "not available"
                }
            )

            fun toCsvHeader() =
                "supportCustomerSavedCard, formExists, formType, supportsAdding"
        }

        fun toCsv() =
            "$supportCustomerSavedCard, $formExists, $formType, $supportsAdding"
    }
}
