package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterValuesProvider
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures.CLIENT_ATTRIBUTION_METADATA
import com.stripe.android.lpmfoundations.paymentmethod.TestUiDefinitionFactoryArgumentsFactory
import com.stripe.android.lpmfoundations.paymentmethod.UiDefinitionFactory
import com.stripe.android.model.Address
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.paymentsheet.forms.FormArgumentsFactory
import com.stripe.android.paymentsheet.forms.FormViewModel
import com.stripe.android.paymentsheet.ui.transformToPaymentMethodCreateParams
import com.stripe.android.paymentsheet.utils.ViewModelStoreTestRule
import com.stripe.android.uicore.elements.AddressFieldsElement
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.SectionElement
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestParameterInjector

@RunWith(RobolectricTestParameterInjector::class)
internal class LpmBillingAddressFormValuesToParamsTest {
    @get:Rule
    val viewModelStoreRule = ViewModelStoreTestRule()

    @Test
    fun `creates the expected payment method params`(
        @TestParameter(valuesProvider = TestCaseProvider::class)
        testCase: LpmBillingAddressFormValuesToParamsTestCase,
    ) = runTest {
        assertThat(
            createPaymentMethodCreateParamsFromFormValues(
                paymentMethodType = testCase.paymentMethodType,
                mode = testCase.mode,
                rawValues = testCase.rawValues,
            ),
        ).isEqualTo(testCase.expectedPaymentMethodParams)
    }

    private suspend fun createPaymentMethodCreateParamsFromFormValues(
        paymentMethodType: PaymentMethod.Type,
        mode: LpmBillingAddressBaselineMode,
        rawValues: Map<IdentifierSpec, String?>,
    ): PaymentMethodCreateParams {
        val metadata = createMetadata(paymentMethodType, mode)
        val formViewModel = createFormViewModel(
            paymentMethodType = paymentMethodType,
            metadata = metadata,
            uiDefinitionFactoryArgumentsFactory = TestUiDefinitionFactoryArgumentsFactory.create(),
        )

        val sectionFields = formViewModel.elements
            .filterIsInstance<SectionElement>()
            .flatMap { it.fields }

        sectionFields.forEach { it.setRawValue(rawValues) }
        sectionFields
            .filterIsInstance<AddressFieldsElement>()
            .forEach { it.countryElement.setRawValue(rawValues) }

        return formViewModel.createPaymentMethodCreateParams(paymentMethodType, metadata)
    }

    private fun createMetadata(
        paymentMethodType: PaymentMethod.Type,
        mode: LpmBillingAddressBaselineMode,
    ) = PaymentMethodMetadataFactory.create(
        stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
            paymentMethodTypes = listOf(paymentMethodType.code),
        ),
        billingDetailsCollectionConfiguration = mode.billingDetailsCollectionConfiguration(),
    )

    private fun createFormViewModel(
        paymentMethodType: PaymentMethod.Type,
        metadata: PaymentMethodMetadata,
        uiDefinitionFactoryArgumentsFactory: UiDefinitionFactory.Arguments.Factory,
    ): FormViewModel {
        val formElements = requireNotNull(
            metadata.formElementsForCode(
                code = paymentMethodType.code,
                uiDefinitionFactoryArgumentsFactory = uiDefinitionFactoryArgumentsFactory,
            ),
        )

        return viewModelStoreRule.track(
            FormViewModel(
                formElements = formElements,
                formArguments = FormArgumentsFactory.create(
                    paymentMethodCode = paymentMethodType.code,
                    metadata = metadata,
                ),
            ),
        )
    }

    private suspend fun FormViewModel.createPaymentMethodCreateParams(
        paymentMethodType: PaymentMethod.Type,
        metadata: PaymentMethodMetadata,
    ): PaymentMethodCreateParams {
        return requireNotNull(completeFormValues.first())
            .transformToPaymentMethodCreateParams(
                paymentMethodCode = paymentMethodType.code,
                paymentMethodMetadata = metadata,
            )
    }

    private companion object {
        object TestCaseProvider : TestParameterValuesProvider() {
            override fun provideValues(context: Context?): List<LpmBillingAddressFormValuesToParamsTestCase> {
                return testCases()
            }
        }

        private fun testCases() = listOf(
            LpmBillingAddressFormValuesToParamsTestCase(
                name = "Boleto Never",
                paymentMethodType = PaymentMethod.Type.Boleto,
                mode = LpmBillingAddressBaselineMode.Never,
                rawValues = boletoNoBillingAddressRawValues,
                expectedPaymentMethodParams = boletoNoBillingAddressExpectedPaymentMethodParams,
            ),
            LpmBillingAddressFormValuesToParamsTestCase(
                name = "Boleto Automatic without tax",
                paymentMethodType = PaymentMethod.Type.Boleto,
                mode = LpmBillingAddressBaselineMode.AutomaticWithoutTax,
                rawValues = boletoWithBillingAddressRawValues,
                expectedPaymentMethodParams = boletoWithBillingAddressExpectedPaymentMethodParams,
            ),
            LpmBillingAddressFormValuesToParamsTestCase(
                name = "Boleto Full",
                paymentMethodType = PaymentMethod.Type.Boleto,
                mode = LpmBillingAddressBaselineMode.Full,
                rawValues = boletoWithBillingAddressRawValues,
                expectedPaymentMethodParams = boletoWithBillingAddressExpectedPaymentMethodParams,
            ),
            LpmBillingAddressFormValuesToParamsTestCase(
                name = "SEPA Debit Never",
                paymentMethodType = PaymentMethod.Type.SepaDebit,
                mode = LpmBillingAddressBaselineMode.Never,
                rawValues = sepaDebitNoBillingAddressRawValues,
                expectedPaymentMethodParams = sepaDebitNoBillingAddressExpectedPaymentMethodParams,
            ),
            LpmBillingAddressFormValuesToParamsTestCase(
                name = "SEPA Debit Automatic without tax",
                paymentMethodType = PaymentMethod.Type.SepaDebit,
                mode = LpmBillingAddressBaselineMode.AutomaticWithoutTax,
                rawValues = sepaDebitWithBillingAddressRawValues,
                expectedPaymentMethodParams = sepaDebitWithBillingAddressExpectedPaymentMethodParams,
            ),
            LpmBillingAddressFormValuesToParamsTestCase(
                name = "SEPA Debit Full",
                paymentMethodType = PaymentMethod.Type.SepaDebit,
                mode = LpmBillingAddressBaselineMode.Full,
                rawValues = sepaDebitWithBillingAddressRawValues,
                expectedPaymentMethodParams = sepaDebitWithBillingAddressExpectedPaymentMethodParams,
            ),
        )

        private val boletoNoBillingAddressRawValues = mapOf(
            IdentifierSpec.Generic("boleto[tax_id]") to "123.456.789-09",
        )

        private val boletoWithBillingAddressRawValues = boletoNoBillingAddressRawValues + mapOf(
            IdentifierSpec.Name to "Jane Doe",
            IdentifierSpec.Email to "jane@example.com",
            IdentifierSpec.Line1 to "Avenida Paulista 123",
            IdentifierSpec.Line2 to "Apto 45",
            IdentifierSpec.City to "Sao Paulo",
            IdentifierSpec.State to "SP",
            IdentifierSpec.Country to "BR",
            IdentifierSpec.PostalCode to "01311000",
        )

        private val boletoNoBillingAddressExpectedPaymentMethodParams = PaymentMethodCreateParams.createWithOverride(
            code = PaymentMethod.Type.Boleto.code,
            billingDetails = null,
            requiresMandate = false,
            overrideParamMap = mapOf(
                "type" to PaymentMethod.Type.Boleto.code,
                "boleto" to mapOf("tax_id" to "123.456.789-09"),
            ),
            productUsage = emptySet(),
            allowRedisplay = PaymentMethod.AllowRedisplay.UNSPECIFIED,
            clientAttributionMetadata = CLIENT_ATTRIBUTION_METADATA,
        )

        private val boletoWithBillingAddressExpectedPaymentMethodParams = PaymentMethodCreateParams.createWithOverride(
            code = PaymentMethod.Type.Boleto.code,
            billingDetails = PaymentMethod.BillingDetails(
                name = "Jane Doe",
                email = "jane@example.com",
                address = Address(
                    line1 = "Avenida Paulista 123",
                    line2 = "Apto 45",
                    city = "Sao Paulo",
                    state = "SP",
                    country = "BR",
                    postalCode = "01311000",
                ),
            ),
            requiresMandate = false,
            overrideParamMap = mapOf(
                "type" to PaymentMethod.Type.Boleto.code,
                "boleto" to mapOf("tax_id" to "123.456.789-09"),
                "billing_details" to mapOf(
                    "name" to "Jane Doe",
                    "email" to "jane@example.com",
                    "address" to mapOf(
                        "line1" to "Avenida Paulista 123",
                        "line2" to "Apto 45",
                        "city" to "Sao Paulo",
                        "state" to "SP",
                        "country" to "BR",
                        "postal_code" to "01311000",
                    ),
                ),
            ),
            productUsage = emptySet(),
            allowRedisplay = PaymentMethod.AllowRedisplay.UNSPECIFIED,
            clientAttributionMetadata = CLIENT_ATTRIBUTION_METADATA,
        )

        private val sepaDebitNoBillingAddressRawValues = mapOf(
            IdentifierSpec.Generic("sepa_debit[iban]") to "DE89370400440532013000",
        )

        private val sepaDebitWithBillingAddressRawValues = sepaDebitNoBillingAddressRawValues + mapOf(
            IdentifierSpec.Name to "Jane Doe",
            IdentifierSpec.Email to "jane@example.com",
            IdentifierSpec.Line1 to "Unter den Linden 1",
            IdentifierSpec.Line2 to "Wohnung 2",
            IdentifierSpec.City to "Berlin",
            IdentifierSpec.Country to "DE",
            IdentifierSpec.PostalCode to "10117",
        )

        private val sepaDebitNoBillingAddressExpectedPaymentMethodParams = PaymentMethodCreateParams.createWithOverride(
            code = PaymentMethod.Type.SepaDebit.code,
            billingDetails = null,
            requiresMandate = true,
            overrideParamMap = mapOf(
                "type" to PaymentMethod.Type.SepaDebit.code,
                "sepa_debit" to mapOf("iban" to "DE89370400440532013000"),
            ),
            productUsage = emptySet(),
            allowRedisplay = PaymentMethod.AllowRedisplay.UNSPECIFIED,
            clientAttributionMetadata = CLIENT_ATTRIBUTION_METADATA,
        )

        private val sepaDebitWithBillingAddressExpectedPaymentMethodParams =
            PaymentMethodCreateParams.createWithOverride(
            code = PaymentMethod.Type.SepaDebit.code,
            billingDetails = PaymentMethod.BillingDetails(
                name = "Jane Doe",
                email = "jane@example.com",
                address = Address(
                    line1 = "Unter den Linden 1",
                    line2 = "Wohnung 2",
                    city = "Berlin",
                    country = "DE",
                    postalCode = "10117",
                ),
            ),
            requiresMandate = true,
            overrideParamMap = mapOf(
                "type" to PaymentMethod.Type.SepaDebit.code,
                "sepa_debit" to mapOf("iban" to "DE89370400440532013000"),
                "billing_details" to mapOf(
                    "name" to "Jane Doe",
                    "email" to "jane@example.com",
                    "address" to mapOf(
                        "line1" to "Unter den Linden 1",
                        "line2" to "Wohnung 2",
                        "city" to "Berlin",
                        "country" to "DE",
                        "postal_code" to "10117",
                    ),
                ),
            ),
            productUsage = emptySet(),
            allowRedisplay = PaymentMethod.AllowRedisplay.UNSPECIFIED,
            clientAttributionMetadata = CLIENT_ATTRIBUTION_METADATA,
        )
    }
}

internal data class LpmBillingAddressFormValuesToParamsTestCase(
    val name: String,
    val paymentMethodType: PaymentMethod.Type,
    val mode: LpmBillingAddressBaselineMode,
    val rawValues: Map<IdentifierSpec, String?>,
    val expectedPaymentMethodParams: PaymentMethodCreateParams,
) {
    override fun toString(): String = name
}
