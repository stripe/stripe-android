package com.stripe.android.paymentsheet.paymentdatacollection.ach

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.SystemAppearance
import com.stripe.android.uicore.elements.EmailConfig
import com.stripe.android.uicore.elements.NameConfig
import com.stripe.android.uicore.elements.PhoneNumberController
import com.stripe.android.uicore.elements.TextFieldController
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
                fieldsState = BankFormFieldsState(
                    showNameField = true,
                    showEmailField = true,
                    showPhoneField = false,
                    showAddressFields = false,
                ),
                isProcessing = false,
                isPaymentFlow = true,
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
                fieldsState = BankFormFieldsState(
                    showNameField = true,
                    showEmailField = true,
                    showPhoneField = false,
                    showAddressFields = false,
                ),
                isProcessing = false,
                isPaymentFlow = false,
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
                fieldsState = BankFormFieldsState(
                    showNameField = true,
                    showEmailField = true,
                    showPhoneField = false,
                    showAddressFields = false,
                ),
                isProcessing = false,
                isPaymentFlow = true,
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
                fieldsState = BankFormFieldsState(
                    showNameField = true,
                    showEmailField = true,
                    showPhoneField = false,
                    showAddressFields = true,
                ),
                isProcessing = false,
                isPaymentFlow = true,
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
                fieldsState = BankFormFieldsState(
                    showNameField = true,
                    showEmailField = true,
                    showPhoneField = false,
                    showAddressFields = true,
                ),
                isProcessing = false,
                isPaymentFlow = true,
                nameController = createNameController("John Doe"),
                emailController = createEmailController("email@email.com"),
                phoneController = createPhoneNumberController(),
                addressController = createAddressController(fillAddress = true),
                lastTextFieldIdentifier = null,
                sameAsShippingElement = null
            )
        }
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
}
