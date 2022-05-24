package com.stripe.android.paymentsheet.model

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import com.stripe.android.ui.core.elements.LpmRepository
import com.stripe.android.ui.core.elements.LpmRepository.SupportedPaymentMethod
import kotlinx.serialization.Serializable
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.File

@RunWith(AndroidJUnit4::class)
class SupportedPaymentMethodTest {
    private val lpmRepository = LpmRepository(ApplicationProvider.getApplicationContext<Application>().resources)
    private val card = lpmRepository.getCard()
    private val eps = lpmRepository.fromCode("eps")!!
    /**
     * To create a baseline for a payment method, create a new file <payment_method>-support.csv,
     * comment out the assert in this test, and copy the output into the new csv file
     */
    @Test
    fun `Test supported payment method baseline`() {
        lpmRepository.values()
            .forEach { lpm ->
                val resource = File(requireNotNull(javaClass.classLoader).getResource("${lpm.type.code}-support.csv").file)
                val baseline = resource.readText()
                val baselineLines = baseline.split("\n")

                val csvOutput = StringBuilder()
                csvOutput.append(
                    "lpm, ${PaymentIntentTestInput.toCsvHeader()}, ${TestOutput.toCsvHeader()}\n"
                )
                generatePaymentIntentScenarios()
                    .mapIndexed { index, testInput ->

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
                        val actualLine = "${lpm.type.code}, ${
                            testInput.copy(
                                intentPMs = testInput.intentPMs.plus(lpm.type.code)
                            ).toCsv()
                        }, ${testOutput.toCsv()}\n"

                        csvOutput.append(actualLine)

                        PaymentIntentTestCase(
                            testInput,
                            testOutput
                        )

                        assertWithMessage("Line: ${index + 2}")
                            .that(actualLine.trim())
                            .isEqualTo(baselineLines[index + 1])
                    }

                if (baseline != csvOutput.toString()) {
                    println(csvOutput.toString())
                }
            }
    }

    @Test
    fun `Test supported payment method doesn't filter with empty unactivated list in test mode`() {
        val mockIntent = mock<PaymentIntent>()
        whenever(mockIntent.paymentMethodTypes).thenReturn(listOf("card"))
        whenever(mockIntent.isLiveMode).thenReturn(false)
        whenever(mockIntent.unactivatedPaymentMethods).thenReturn(emptyList())

        val expected = listOf<SupportedPaymentMethod>().plus(card)

        assertThat(getPMsToAdd(mockIntent, null, lpmRepository)).isEqualTo(expected)
    }

    @Test
    fun `Test supported payment method doesn't filter with empty unactivated list in live mode`() {
        val mockIntent = mock<PaymentIntent>()
        whenever(mockIntent.paymentMethodTypes).thenReturn(listOf("card"))
        whenever(mockIntent.isLiveMode).thenReturn(true)
        whenever(mockIntent.unactivatedPaymentMethods).thenReturn(emptyList())

        val expected = listOf<SupportedPaymentMethod>().plus(card)

        assertThat(getPMsToAdd(mockIntent, null, lpmRepository)).isEqualTo(expected)
    }

    @Test
    fun `Test supported payment method does filter with unactivated list in live mode`() {
        val mockIntent = mock<PaymentIntent>()
        whenever(mockIntent.paymentMethodTypes).thenReturn(listOf("card"))
        whenever(mockIntent.isLiveMode).thenReturn(true)
        whenever(mockIntent.unactivatedPaymentMethods).thenReturn(listOf("card"))

        val expected = listOf<SupportedPaymentMethod>()

        assertThat(getPMsToAdd(mockIntent, null, lpmRepository)).isEqualTo(expected)
    }

    @Test
    fun `Test supported payment method doesn't filter with unactivated list in test mode`() {
        val mockIntent = mock<PaymentIntent>()
        whenever(mockIntent.paymentMethodTypes).thenReturn(listOf("card"))
        whenever(mockIntent.isLiveMode).thenReturn(false)
        whenever(mockIntent.unactivatedPaymentMethods).thenReturn(listOf("card"))

        val expected = listOf<SupportedPaymentMethod>().plus(card)

        assertThat(getPMsToAdd(mockIntent, null, lpmRepository)).isEqualTo(expected)
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
                                    intentPMs = setOf(card.type.code),
                                    intentSetupFutureUsage = usage,
                                    intentHasShipping = hasShipping,
                                    allowsDelayedPayment = delayed
                                ),
                                PaymentIntentTestInput(
                                    hasCustomer = customer,
                                    intentPMs = setOf(
                                        card.type.code,
                                        eps.type.code
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
        val intentLpms: List<String>,
    )

    @Serializable
    internal data class PaymentIntentTestCase(
        val testInput: PaymentIntentTestInput,
        val testOutput: TestOutput
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
                    paymentMethodTypes = intentPMs.plus(lpm.type.code).toList()
                )
            true ->
                PaymentIntentFixtures.PI_WITH_SHIPPING.copy(
                    setupFutureUsage = intentSetupFutureUsage,
                    paymentMethodTypes = intentPMs.plus(lpm.type.code).toList()
                )
        }

        fun getConfig() = if (hasCustomer) {
            PaymentSheetFixtures.ARGS_CUSTOMER_WITHOUT_GOOGLEPAY.config?.copy(
                allowsDelayedPaymentMethods = allowsDelayedPayment
            )
        } else {
            PaymentSheetFixtures.ARGS_WITHOUT_CUSTOMER.config?.copy(
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
                supportsAdding: Boolean,
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
