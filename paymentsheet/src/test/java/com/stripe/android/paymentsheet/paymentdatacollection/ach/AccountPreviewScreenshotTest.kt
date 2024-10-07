package com.stripe.android.paymentsheet.paymentdatacollection.ach

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodSaveConsentBehavior
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.SystemAppearance
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import com.stripe.android.ui.core.elements.SaveForFutureUseElement
import com.stripe.android.uicore.elements.EmailConfig
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.NameConfig
import com.stripe.android.uicore.elements.PhoneNumberController
import com.stripe.android.uicore.elements.SameAsShippingController
import com.stripe.android.uicore.elements.SameAsShippingElement
import com.stripe.android.uicore.elements.TextFieldController
import org.junit.Rule
import org.junit.Test

internal class AccountPreviewScreenshotTest {
    @get:Rule
    val paparazzi = PaparazziRule(
        SystemAppearance.entries,
        boxModifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(),
    )

    private val formArguments = FormArguments(
        paymentMethodCode = PaymentMethod.Type.USBankAccount.code,
        merchantName = "Test Merchant",
        amount = null,
        billingDetails = null,
        cbcEligibility = CardBrandChoiceEligibility.Ineligible,
        hasIntentToSetup = false,
        paymentMethodSaveConsentBehavior = PaymentMethodSaveConsentBehavior.Legacy,
    )

    private val sameAsShippingElement = SameAsShippingElement(
        identifier = IdentifierSpec.SameAsShipping,
        controller = SameAsShippingController(false),
    )

    private val saveForFutureUseElement = SaveForFutureUseElement(
        initialValue = false,
        merchantName = "Test Merchant",
    )

    @Test
    fun testPaymentFlow() {
        paparazzi.snapshot {
            AccountPreviewScreen(
                formArgs = formArguments,
                bankName = "My Bank",
                last4 = "3456",
                isProcessing = false,
                isPaymentFlow = true,
                instantDebits = false,
                nameController = createNameController(),
                emailController = createEmailController(),
                phoneController = createPhoneNumberController(),
                addressController = createAddressController(),
                lastTextFieldIdentifier = null,
                sameAsShippingElement = sameAsShippingElement,
                saveForFutureUseElement = saveForFutureUseElement,
                showCheckbox = false,
                onRemoveAccount = { },
            )
        }
    }

    @Test
    fun testSetupFlow() {
        paparazzi.snapshot {
            AccountPreviewScreen(
                formArgs = formArguments,
                bankName = "My Bank",
                last4 = "3456",
                isProcessing = false,
                isPaymentFlow = false,
                instantDebits = false,
                nameController = createNameController(),
                emailController = createEmailController(),
                phoneController = createPhoneNumberController(),
                addressController = createAddressController(),
                lastTextFieldIdentifier = null,
                sameAsShippingElement = sameAsShippingElement,
                saveForFutureUseElement = saveForFutureUseElement,
                showCheckbox = false,
                onRemoveAccount = { },
            )
        }
    }

    @Test
    fun testWithBillingAddress() {
        paparazzi.snapshot {
            AccountPreviewScreen(
                formArgs = formArguments.copy(
                    billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                        address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
                    ),
                ),
                bankName = "My Bank",
                last4 = "3456",
                isProcessing = false,
                isPaymentFlow = true,
                instantDebits = false,
                nameController = createNameController(),
                emailController = createEmailController(),
                phoneController = createPhoneNumberController(),
                addressController = createAddressController(fillAddress = true),
                lastTextFieldIdentifier = null,
                sameAsShippingElement = sameAsShippingElement,
                saveForFutureUseElement = saveForFutureUseElement,
                showCheckbox = false,
                onRemoveAccount = { },
            )
        }
    }

    private fun createNameController(): TextFieldController {
        return NameConfig.createController("John Doe")
    }

    private fun createEmailController(): TextFieldController {
        return EmailConfig.createController("email@email.com")
    }

    private fun createPhoneNumberController(): PhoneNumberController {
        return PhoneNumberController.createPhoneNumberController("")
    }
}
