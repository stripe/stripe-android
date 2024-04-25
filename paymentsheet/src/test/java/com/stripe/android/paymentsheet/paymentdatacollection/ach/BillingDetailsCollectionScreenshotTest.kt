package com.stripe.android.paymentsheet.paymentdatacollection.ach

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.SystemAppearance
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import com.stripe.android.uicore.address.CountryAddressSchema
import com.stripe.android.uicore.address.FieldSchema
import com.stripe.android.uicore.address.FieldType
import com.stripe.android.uicore.address.NameType
import com.stripe.android.uicore.address.transformToElementList
import com.stripe.android.uicore.elements.AddressController
import com.stripe.android.uicore.elements.CountryConfig
import com.stripe.android.uicore.elements.CountryElement
import com.stripe.android.uicore.elements.DropdownFieldController
import com.stripe.android.uicore.elements.EmailConfig
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.NameConfig
import com.stripe.android.uicore.elements.PhoneNumberController
import com.stripe.android.uicore.elements.RowElement
import com.stripe.android.uicore.elements.SectionFieldElement
import com.stripe.android.uicore.elements.TextFieldController
import com.stripe.android.uicore.utils.stateFlowOf
import org.junit.Rule
import org.junit.Test

internal class BillingDetailsCollectionScreenshotTest {
    @get:Rule
    val paparazzi = PaparazziRule(
        SystemAppearance.entries,
        boxModifier = Modifier
            .padding(0.dp)
            .fillMaxWidth(),
    )

    @Test
    fun testEmpty() {
        paparazzi.snapshot {
            BillingDetailsCollectionScreen(
                formArgs = createFormArguments(),
                isProcessing = false,
                isPaymentFlow = true,
                instantDebits = false,
                nameController = createNameController(),
                emailController = createEmailController(),
                phoneController = createPhoneNumberController(),
                addressController = createAddressController(),
                lastTextFieldIdentifier = null,
                sameAsShippingElement = null
            )
        }
    }

    @Test
    fun testEmptySetupFlow() {
        paparazzi.snapshot {
            BillingDetailsCollectionScreen(
                formArgs = createFormArguments(),
                isProcessing = false,
                isPaymentFlow = false,
                instantDebits = false,
                nameController = createNameController(),
                emailController = createEmailController(),
                phoneController = createPhoneNumberController(),
                addressController = createAddressController(),
                lastTextFieldIdentifier = null,
                sameAsShippingElement = null
            )
        }
    }

    @Test
    fun testFilled() {
        paparazzi.snapshot {
            BillingDetailsCollectionScreen(
                formArgs = createFormArguments(),
                isProcessing = false,
                isPaymentFlow = true,
                instantDebits = false,
                nameController = createNameController("John Doe"),
                emailController = createEmailController("email@email.com"),
                phoneController = createPhoneNumberController(),
                addressController = createAddressController(),
                lastTextFieldIdentifier = null,
                sameAsShippingElement = null
            )
        }
    }

    @Test
    fun testEmptyWithBillingAddress() {
        paparazzi.snapshot {
            BillingDetailsCollectionScreen(
                formArgs = createFormArguments(
                    billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                        address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full
                    ),
                ),
                isProcessing = false,
                isPaymentFlow = true,
                instantDebits = false,
                nameController = createNameController(),
                emailController = createEmailController(),
                phoneController = createPhoneNumberController(),
                addressController = createAddressController(),
                lastTextFieldIdentifier = null,
                sameAsShippingElement = null
            )
        }
    }

    @Test
    fun testFilledWithBillingAddress() {
        paparazzi.snapshot {
            BillingDetailsCollectionScreen(
                formArgs = createFormArguments(
                    billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                        address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full
                    ),
                ),
                isProcessing = false,
                isPaymentFlow = true,
                instantDebits = false,
                nameController = createNameController("John Doe"),
                emailController = createEmailController("email@email.com"),
                phoneController = createPhoneNumberController(),
                addressController = createAddressController(fillAddress = true),
                lastTextFieldIdentifier = null,
                sameAsShippingElement = null
            )
        }
    }

    private fun createFormArguments(
        billingDetailsCollectionConfiguration: PaymentSheet.BillingDetailsCollectionConfiguration =
            PaymentSheet.BillingDetailsCollectionConfiguration(),
    ): FormArguments {
        return FormArguments(
            paymentMethodCode = PaymentMethod.Type.USBankAccount.code,
            merchantName = "Test Merchant",
            amount = null,
            billingDetails = null,
            billingDetailsCollectionConfiguration = billingDetailsCollectionConfiguration,
            cbcEligibility = CardBrandChoiceEligibility.Ineligible,
        )
    }

    private fun createNameController(initialValue: String? = null): TextFieldController {
        return NameConfig.createController(initialValue)
    }

    private fun createEmailController(initialValue: String? = null): TextFieldController {
        return EmailConfig.createController(initialValue)
    }

    private fun createPhoneNumberController(initialValue: String = ""): PhoneNumberController {
        return PhoneNumberController.createPhoneNumberController(initialValue)
    }

    private fun createAddressController(
        fillAddress: Boolean = false,
    ): AddressController {
        val elements = listOf(
            CountryAddressSchema(
                type = FieldType.AddressLine1,
                required = true,
            ),
            CountryAddressSchema(
                type = FieldType.AddressLine2,
                required = false,
            ),
            CountryAddressSchema(
                type = FieldType.Locality,
                required = true,
                schema = FieldSchema(nameType = NameType.City),
            ),
            CountryAddressSchema(
                type = FieldType.PostalCode,
                required = true,
                schema = FieldSchema(
                    isNumeric = true,
                    nameType = NameType.Zip,
                ),
            ),
            CountryAddressSchema(
                type = FieldType.AdministrativeArea,
                required = true,
                schema = FieldSchema(nameType = NameType.State),
            )
        ).transformToElementList(countryCode = "US")

        if (fillAddress) {
            elements.forEach { element ->
                if (element is RowElement) {
                    element.fields.forEach(::setAddressValueForElement)
                } else {
                    setAddressValueForElement(element)
                }
            }
        }

        return AddressController(
            stateFlowOf(
                listOf(
                    CountryElement(
                        identifier = IdentifierSpec.BillingAddress,
                        controller = DropdownFieldController(
                            config = CountryConfig(setOf("US", "CA")),
                            initialValue = "US"
                        )
                    ),
                ).plus(elements)
            )
        )
    }

    private fun setAddressValueForElement(element: SectionFieldElement) {
        val identifier = element.identifier

        element.setRawValue(mapOf(identifier to identifier.toTestAddressValue()))
    }

    private fun IdentifierSpec.toTestAddressValue(): String {
        return when (this) {
            FieldType.AddressLine1.identifierSpec -> "354 Oyster Point Blvd"
            FieldType.AddressLine2.identifierSpec -> "Levels 1-5"
            FieldType.Locality.identifierSpec -> "South San Francisco"
            FieldType.AdministrativeArea.identifierSpec -> "CA"
            FieldType.PostalCode.identifierSpec -> "94080"
            else -> ""
        }
    }
}
