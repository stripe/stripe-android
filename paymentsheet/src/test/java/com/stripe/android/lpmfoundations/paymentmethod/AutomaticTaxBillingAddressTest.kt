package com.stripe.android.lpmfoundations.paymentmethod

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.lpmfoundations.paymentmethod.definitions.BacsDebitDefinition
import com.stripe.android.lpmfoundations.paymentmethod.definitions.BlikDefinition
import com.stripe.android.lpmfoundations.paymentmethod.definitions.KlarnaDefinition
import com.stripe.android.lpmfoundations.paymentmethod.definitions.WeroDefinition
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.forms.CompleteFormFieldValueFilter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import com.stripe.android.testing.FeatureFlagTestRule
import com.stripe.android.testing.PaymentIntentFactory
import com.stripe.android.ui.core.FieldValuesToParamsMapConverter
import com.stripe.android.ui.core.elements.BillingAddressElement
import com.stripe.android.ui.core.elements.additionalAutomaticTaxFieldsByCountry
import com.stripe.android.uicore.elements.AddressElement
import com.stripe.android.uicore.elements.AddressFieldsElement
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.SameAsShippingController
import com.stripe.android.uicore.elements.SameAsShippingElement
import com.stripe.android.uicore.elements.SectionElement
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AutomaticTaxBillingAddressTest {
    @get:Rule
    val enableKlarnaFormRemovalRule = FeatureFlagTestRule(
        featureFlag = FeatureFlags.enableKlarnaFormRemoval,
        isEnabled = false,
    )

    @Test
    fun `form without address collects US automatic tax minimums`() {
        val addressElement = createBlikTaxAddress(countryCode = "US")

        assertThat(addressElement.hiddenIdentifiers.value).doesNotContain(IdentifierSpec.Line1)
        assertThat(addressElement.hiddenIdentifiers.value).doesNotContain(IdentifierSpec.City)
        assertThat(addressElement.hiddenIdentifiers.value).doesNotContain(IdentifierSpec.State)
        assertThat(addressElement.hiddenIdentifiers.value).doesNotContain(IdentifierSpec.PostalCode)
    }

    @Test
    fun `form without address collects CA automatic tax minimums`() {
        val addressElement = createBlikTaxAddress(countryCode = "CA")

        assertThat(addressElement.hiddenIdentifiers.value).contains(IdentifierSpec.Line1)
        assertThat(addressElement.hiddenIdentifiers.value).contains(IdentifierSpec.City)
        assertThat(addressElement.hiddenIdentifiers.value).contains(IdentifierSpec.State)
        assertThat(addressElement.hiddenIdentifiers.value).doesNotContain(IdentifierSpec.PostalCode)
    }

    @Test
    fun `form without address collects only country when no additional tax fields are required`() {
        val addressElement = createBlikTaxAddress(countryCode = "FR")

        assertThat(addressElement.hiddenIdentifiers.value).containsAtLeast(
            IdentifierSpec.Line1,
            IdentifierSpec.City,
            IdentifierSpec.State,
            IdentifierSpec.PostalCode,
        )
    }

    @Test
    fun `automatic tax address is included in LPM create params`() = runTest {
        val metadata = createMetadata(
            paymentMethodCode = PaymentMethod.Type.Blik.code,
            checkoutSessionResponse = automaticTaxCheckoutSessionResponse,
            defaultBillingDetails = PaymentSheet.BillingDetails(
                address = PaymentSheet.Address(
                    line1 = "510 Townsend Street",
                    city = "San Francisco",
                    state = "CA",
                    postalCode = "94103",
                    country = "US",
                ),
            ),
        )
        val addressElement = BlikDefinition.formElements(metadata)
            .sectionFields()
            .filterIsInstance<BillingAddressElement>()
            .single()
        val createParams = addressElement.toCreateParams(metadata, PaymentMethod.Type.Blik.code)
        val billingDetails = createParams.toParamMap()["billing_details"] as Map<*, *>

        assertThat(billingDetails["address"]).isEqualTo(
            mapOf(
                "line1" to "510 Townsend Street",
                "city" to "San Francisco",
                "state" to "CA",
                "postal_code" to "94103",
                "country" to "US",
            )
        )
    }

    @Test
    fun `widened Klarna address is included in create params`() = runTest {
        val metadata = createMetadata(
            paymentMethodCode = PaymentMethod.Type.Klarna.code,
            checkoutSessionResponse = automaticTaxCheckoutSessionResponse,
            defaultBillingDetails = PaymentSheet.BillingDetails(
                address = PaymentSheet.Address(
                    line1 = "510 Townsend Street",
                    city = "San Francisco",
                    state = "CA",
                    postalCode = "94103",
                    country = "US",
                ),
            ),
        )
        val addressElement = KlarnaDefinition.formElements(metadata)
            .sectionFields()
            .filterIsInstance<BillingAddressElement>()
            .single()

        val createParams = addressElement.toCreateParams(metadata, PaymentMethod.Type.Klarna.code)
        val billingDetails = createParams.toParamMap()["billing_details"] as Map<*, *>

        assertThat(billingDetails["address"]).isEqualTo(
            mapOf(
                "line1" to "510 Townsend Street",
                "city" to "San Francisco",
                "state" to "CA",
                "postal_code" to "94103",
                "country" to "US",
            )
        )
    }

    @Test
    fun `tax disabled does not append billing address`() {
        val formElements = BlikDefinition.formElements(
            metadata = createMetadata(
                paymentMethodCode = PaymentMethod.Type.Blik.code,
                checkoutSessionResponse = CheckoutSessionResponseFactory.create(
                    automaticTaxEnabled = false,
                    taxAddressSource = CheckoutSessionResponse.TaxAddressSource.BILLING,
                ),
            )
        )

        assertThat(formElements.sectionFields().filterIsInstance<AddressFieldsElement>()).isEmpty()
    }

    @Test
    fun `appended tax address retains an existing same as shipping element once`() {
        val metadata = createMetadata(
            paymentMethodCode = PaymentMethod.Type.Blik.code,
            checkoutSessionResponse = automaticTaxCheckoutSessionResponse,
        )
        val sameAsShippingElement = SameAsShippingElement(
            identifier = IdentifierSpec.SameAsShipping,
            controller = SameAsShippingController(true),
        )

        val formElements = AutomaticTaxFormFinalizer.finalize(
            formElements = listOf(sameAsShippingElement),
            paymentMethodCode = PaymentMethod.Type.Blik.code,
            arguments = TestUiDefinitionFactoryArgumentsFactory.create().create(
                metadata = metadata,
                requiresMandate = false,
            ),
        )

        assertThat(formElements.filterIsInstance<SameAsShippingElement>()).containsExactly(sameAsShippingElement)
    }

    @Test
    fun `multiple billing addresses cause automatic tax finalization to fail`() {
        val metadata = createMetadata(
            paymentMethodCode = PaymentMethod.Type.Blik.code,
            checkoutSessionResponse = automaticTaxCheckoutSessionResponse,
        )
        val formElements = BlikDefinition.formElements(metadata)

        val exception = assertThrows(IllegalStateException::class.java) {
            AutomaticTaxFormFinalizer.finalize(
                formElements = formElements + formElements,
                paymentMethodCode = PaymentMethod.Type.Blik.code,
                arguments = TestUiDefinitionFactoryArgumentsFactory.create().create(
                    metadata = metadata,
                    requiresMandate = false,
                ),
            )
        }

        assertThat(exception).hasMessageThat()
            .contains("A payment method form must not contain multiple billing addresses.")
    }

    @Test
    fun `existing country section is widened without duplication`() {
        val formElements = WeroDefinition.formElements(
            metadata = createMetadata(
                paymentMethodCode = PaymentMethod.Type.Wero.code,
                checkoutSessionResponse = automaticTaxCheckoutSessionResponse,
            )
        )

        assertThat(formElements).hasSize(1)
        assertThat(formElements.sectionFields().filterIsInstance<BillingAddressElement>()).hasSize(1)
    }

    @Test
    fun `existing country section preserves its label`() {
        val formElements = WeroDefinition.formElements(
            metadata = createMetadata(
                paymentMethodCode = PaymentMethod.Type.Wero.code,
                checkoutSessionResponse = automaticTaxCheckoutSessionResponse,
            )
        )

        assertThat((formElements.single() as SectionElement).controller.label).isNull()
    }

    @Test
    fun `existing country section retains same as shipping behavior`() {
        val formElements = WeroDefinition.formElements(
            metadata = createMetadata(
                paymentMethodCode = PaymentMethod.Type.Wero.code,
                checkoutSessionResponse = automaticTaxCheckoutSessionResponse,
                shippingDetails = AddressDetails(
                    address = PaymentSheet.Address(country = "DE"),
                    isCheckboxSelected = true,
                ),
            )
        )

        assertThat(formElements.filterIsInstance<SameAsShippingElement>()).hasSize(1)
    }

    @Test
    fun `existing full address is preserved without appending tax address`() {
        val formElements = WeroDefinition.formElements(
            metadata = createMetadata(
                paymentMethodCode = PaymentMethod.Type.Wero.code,
                checkoutSessionResponse = automaticTaxCheckoutSessionResponse,
                addressCollectionMode = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
            )
        )

        val addressElement = formElements.sectionFields().filterIsInstance<BillingAddressElement>().single()
        assertThat(addressElement.addressElement).isInstanceOf(AddressElement::class.java)
    }

    @Test
    fun `specialized full address is preserved by automatic tax finalization`() {
        val baseMetadata = createMetadata(
            paymentMethodCode = PaymentMethod.Type.BacsDebit.code,
            checkoutSessionResponse = null,
        )
        val baseFormElements = requireNotNull(
            baseMetadata.formElementsForCode(
                code = PaymentMethod.Type.BacsDebit.code,
                uiDefinitionFactoryArgumentsFactory = TestUiDefinitionFactoryArgumentsFactory.create(),
            )
        )
        val specializedAddress = baseFormElements.sectionFields().filterIsInstance<AddressElement>().single()
        val taxMetadata = createMetadata(
            paymentMethodCode = PaymentMethod.Type.BacsDebit.code,
            checkoutSessionResponse = automaticTaxCheckoutSessionResponse,
        )

        val finalizedFormElements = AutomaticTaxFormFinalizer.finalize(
            formElements = baseFormElements,
            paymentMethodCode = PaymentMethod.Type.BacsDebit.code,
            arguments = TestUiDefinitionFactoryArgumentsFactory.create().create(
                metadata = taxMetadata,
                requiresMandate = BacsDebitDefinition.requiresMandate(taxMetadata),
            ),
        )

        assertThat(finalizedFormElements.sectionFields().filterIsInstance<AddressElement>())
            .containsExactly(specializedAddress)
    }

    @Test
    fun `Klarna country section is widened without duplication`() {
        val formElements = KlarnaDefinition.formElements(
            metadata = createMetadata(
                paymentMethodCode = PaymentMethod.Type.Klarna.code,
                checkoutSessionResponse = automaticTaxCheckoutSessionResponse,
            )
        )

        assertThat(formElements.sectionFields().filterIsInstance<BillingAddressElement>()).hasSize(1)
    }

    @Test
    fun `Klarna shared address is widened for US tax minimums`() {
        val formElements = KlarnaDefinition.formElements(
            metadata = createMetadata(
                paymentMethodCode = PaymentMethod.Type.Klarna.code,
                checkoutSessionResponse = automaticTaxCheckoutSessionResponse,
            )
        )
        val addressElement = formElements.sectionFields().filterIsInstance<BillingAddressElement>().single()

        addressElement.countryElement.controller.onRawValueChange("US")

        assertThat(addressElement.hiddenIdentifiers.value).doesNotContain(IdentifierSpec.Line1)
        assertThat(addressElement.hiddenIdentifiers.value).doesNotContain(IdentifierSpec.City)
        assertThat(addressElement.hiddenIdentifiers.value).doesNotContain(IdentifierSpec.State)
        assertThat(addressElement.hiddenIdentifiers.value).doesNotContain(IdentifierSpec.PostalCode)
    }

    @Test
    fun `external payment method without address receives one tax address`() {
        val externalPaymentMethod = PaymentMethodFixtures.PAYPAL_EXTERNAL_PAYMENT_METHOD_SPEC
        val metadata = PaymentMethodMetadataFactory.create(
            billingDetailsCollectionConfiguration = automaticAddressCollection,
            externalPaymentMethodSpecs = listOf(externalPaymentMethod),
            checkoutSessionResponse = automaticTaxCheckoutSessionResponse,
        )

        val formElements = requireNotNull(
            metadata.formElementsForCode(
                code = externalPaymentMethod.type,
                uiDefinitionFactoryArgumentsFactory = TestUiDefinitionFactoryArgumentsFactory.create(),
            )
        )

        assertThat(formElements.sectionFields().filterIsInstance<BillingAddressElement>()).hasSize(1)
    }

    @Test
    fun `custom payment method without address receives one tax address`() {
        val customPaymentMethod = PaymentMethodFixtures.PAYPAL_CUSTOM_PAYMENT_METHOD
        val metadata = PaymentMethodMetadataFactory.create(
            billingDetailsCollectionConfiguration = automaticAddressCollection,
            displayableCustomPaymentMethods = listOf(customPaymentMethod),
            checkoutSessionResponse = automaticTaxCheckoutSessionResponse,
        )

        val formElements = requireNotNull(
            metadata.formElementsForCode(
                code = customPaymentMethod.id,
                uiDefinitionFactoryArgumentsFactory = TestUiDefinitionFactoryArgumentsFactory.create(),
            )
        )

        assertThat(formElements.sectionFields().filterIsInstance<BillingAddressElement>()).hasSize(1)
    }

    @Test
    fun `every normally rendered supported payment method has exactly one tax address`() {
        val paymentMethodCodes = PaymentMethodRegistry.all.map { it.type.code }
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFactory.create(paymentMethodTypes = paymentMethodCodes),
            billingDetailsCollectionConfiguration = automaticAddressCollection,
            checkoutSessionResponse = automaticTaxCheckoutSessionResponse,
        )
        val normallyRenderedCodes = metadata.supportedPaymentMethodTypes().filterNot {
            it == PaymentMethod.Type.USBankAccount.code || it == PaymentMethod.Type.Link.code
        }

        assertThat(normallyRenderedCodes).isNotEmpty()
        normallyRenderedCodes.forEach { code ->
            val formElements = requireNotNull(
                metadata.formElementsForCode(
                    code = code,
                    uiDefinitionFactoryArgumentsFactory = TestUiDefinitionFactoryArgumentsFactory.create(),
                )
            )
            val addressFields = formElements.sectionFields().filterIsInstance<AddressFieldsElement>()
            assertWithMessage("$code should contain exactly one address").that(addressFields).hasSize(1)

            val addressElement = addressFields.single()
            if (addressElement !is BillingAddressElement) {
                assertWithMessage("$code should already contain a full address")
                    .that(addressElement)
                    .isInstanceOf(AddressElement::class.java)
                return@forEach
            }

            if (code == PaymentMethod.Type.Wero.code) {
                val controller = addressElement.countryElement.controller
                listOf("DE", "BE", "FR").forEach { allowedCountry ->
                    controller.onRawValueChange(allowedCountry)
                    assertThat(controller.rawFieldValue.value).isEqualTo(allowedCountry)
                }
                controller.onRawValueChange("US")
                assertThat(controller.rawFieldValue.value).isNotEqualTo("US")
            }

            val selectedCountry = requireNotNull(addressElement.countryElement.controller.rawFieldValue.value)
            additionalAutomaticTaxFieldsByCountry[selectedCountry].orEmpty().forEach { requiredField ->
                assertWithMessage("$code should show $requiredField for $selectedCountry")
                    .that(addressElement.hiddenIdentifiers.value)
                    .doesNotContain(requiredField)
            }
        }
    }

    private fun createBlikTaxAddress(countryCode: String): BillingAddressElement {
        val formElements = BlikDefinition.formElements(
            metadata = createMetadata(
                paymentMethodCode = PaymentMethod.Type.Blik.code,
                checkoutSessionResponse = automaticTaxCheckoutSessionResponse,
            )
        )
        val addressElement = formElements.sectionFields().filterIsInstance<BillingAddressElement>().single()
        addressElement.countryElement.controller.onRawValueChange(countryCode)
        return addressElement
    }

    private suspend fun BillingAddressElement.toCreateParams(
        metadata: PaymentMethodMetadata,
        paymentMethodCode: String,
    ): PaymentMethodCreateParams {
        val formValues = requireNotNull(
            CompleteFormFieldValueFilter(
                currentFieldValueMap = getFormFieldValueFlow().map { it.toMap() },
                hiddenIdentifiers = hiddenIdentifiers,
                userRequestedReuse = flowOf(PaymentSelection.CustomerRequestedSave.NoRequest),
                defaultValues = emptyMap(),
            ).filterFlow().first()
        )

        return FieldValuesToParamsMapConverter.transformToPaymentMethodCreateParams(
            fieldValuePairs = formValues.fieldValuePairs,
            code = paymentMethodCode,
            requiresMandate = false,
            clientAttributionMetadata = metadata.clientAttributionMetadata,
        )
    }

    private fun createMetadata(
        paymentMethodCode: String,
        checkoutSessionResponse: CheckoutSessionResponse?,
        addressCollectionMode: PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode =
            PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic,
        shippingDetails: AddressDetails? = null,
        defaultBillingDetails: PaymentSheet.BillingDetails = PaymentSheet.BillingDetails(),
    ): PaymentMethodMetadata {
        return PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFactory.create(paymentMethodTypes = listOf(paymentMethodCode)),
            billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                address = addressCollectionMode,
            ),
            shippingDetails = shippingDetails,
            defaultBillingDetails = defaultBillingDetails,
            checkoutSessionResponse = checkoutSessionResponse,
        )
    }

    private companion object {
        val automaticAddressCollection = PaymentSheet.BillingDetailsCollectionConfiguration(
            address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic,
        )
    }
}

@RunWith(RobolectricTestRunner::class)
class AutomaticTaxBillingAddressKlarnaFormRemovalTest {
    @get:Rule
    val enableKlarnaFormRemovalRule = FeatureFlagTestRule(
        featureFlag = FeatureFlags.enableKlarnaFormRemoval,
        isEnabled = true,
    )

    @Test
    fun `Klarna form removal path receives one tax address`() {
        val formElements = KlarnaDefinition.formElements(
            metadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFactory.create(
                    paymentMethodTypes = listOf(PaymentMethod.Type.Klarna.code),
                ),
                billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                    address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic,
                ),
                checkoutSessionResponse = automaticTaxCheckoutSessionResponse,
            )
        )

        assertThat(formElements.sectionFields().filterIsInstance<BillingAddressElement>()).hasSize(1)
    }
}

private fun List<FormElement>.sectionFields() = filterIsInstance<SectionElement>().flatMap { it.fields }

private val automaticTaxCheckoutSessionResponse = CheckoutSessionResponseFactory.create(
    automaticTaxEnabled = true,
    taxAddressSource = CheckoutSessionResponse.TaxAddressSource.BILLING,
)
