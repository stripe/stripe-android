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
            BankAccountForm(
                state = BankFormScreenState(isPaymentFlow = true),
                instantDebits = false,
                isPaymentFlow = true,
                formArgs = createFormArguments(),
                nameController = createNameController(),
                emailController = createEmailController(),
                phoneController = createPhoneNumberController(),
                addressController = createAddressController(),
                sameAsShippingElement = null,
                saveForFutureUseElement = SaveForFutureUseElement(
                    initialValue = false,
                    merchantName = "Test Merchant",
                ),
                showCheckbox = false,
                lastTextFieldIdentifier = null,
                onRemoveAccount = {},
            )
        }
    }

    @Test
    fun testEmptySetupFlow() {
        paparazzi.snapshot {
            BankAccountForm(
                state = BankFormScreenState(isPaymentFlow = false),
                instantDebits = false,
                isPaymentFlow = false,
                formArgs = createFormArguments(),
                nameController = createNameController(),
                emailController = createEmailController(),
                phoneController = createPhoneNumberController(),
                addressController = createAddressController(),
                sameAsShippingElement = null,
                saveForFutureUseElement = SaveForFutureUseElement(
                    initialValue = false,
                    merchantName = "Test Merchant",
                ),
                showCheckbox = false,
                lastTextFieldIdentifier = null,
                onRemoveAccount = {},
            )
        }
    }

    @Test
    fun testPromoDisclaimer() {
        paparazzi.snapshot {
            BankAccountForm(
                state = BankFormScreenState(
                    isPaymentFlow = true,
                    promoText = "$5",
                ),
                instantDebits = false,
                isPaymentFlow = true,
                formArgs = createFormArguments(),
                nameController = createNameController("John Doe"),
                emailController = createEmailController("email@email.com"),
                phoneController = createPhoneNumberController(),
                addressController = createAddressController(),
                sameAsShippingElement = null,
                saveForFutureUseElement = SaveForFutureUseElement(
                    initialValue = false,
                    merchantName = "Test Merchant",
                ),
                showCheckbox = false,
                lastTextFieldIdentifier = null,
                onRemoveAccount = {},
            )
        }
    }

    @Test
    fun testPromoDisclaimerSetupFlow() {
        paparazzi.snapshot {
            BankAccountForm(
                state = BankFormScreenState(
                    isPaymentFlow = false,
                    promoText = "$5",
                ),
                instantDebits = false,
                isPaymentFlow = false,
                formArgs = createFormArguments(),
                nameController = createNameController("John Doe"),
                emailController = createEmailController("email@email.com"),
                phoneController = createPhoneNumberController(),
                addressController = createAddressController(),
                sameAsShippingElement = null,
                saveForFutureUseElement = SaveForFutureUseElement(
                    initialValue = false,
                    merchantName = "Test Merchant",
                ),
                showCheckbox = false,
                lastTextFieldIdentifier = null,
                onRemoveAccount = {},
            )
        }
    }

    @Test
    fun testFilled() {
        paparazzi.snapshot {
            BankAccountForm(
                state = BankFormScreenState(isPaymentFlow = true),
                instantDebits = false,
                isPaymentFlow = true,
                formArgs = createFormArguments(),
                nameController = createNameController("John Doe"),
                emailController = createEmailController("email@email.com"),
                phoneController = createPhoneNumberController(),
                addressController = createAddressController(),
                sameAsShippingElement = null,
                saveForFutureUseElement = SaveForFutureUseElement(
                    initialValue = false,
                    merchantName = "Test Merchant",
                ),
                showCheckbox = false,
                lastTextFieldIdentifier = null,
                onRemoveAccount = {},
            )
        }
    }

    @Test
    fun testEmptyWithBillingAddress() {
        paparazzi.snapshot {
            BankAccountForm(
                state = BankFormScreenState(isPaymentFlow = true),
                instantDebits = false,
                isPaymentFlow = true,
                formArgs = createFormArguments(
                    billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                        address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full
                    ),
                ),
                nameController = createNameController(),
                emailController = createEmailController(),
                phoneController = createPhoneNumberController(),
                addressController = createAddressController(),
                sameAsShippingElement = null,
                saveForFutureUseElement = SaveForFutureUseElement(
                    initialValue = false,
                    merchantName = "Test Merchant",
                ),
                showCheckbox = false,
                lastTextFieldIdentifier = null,
                onRemoveAccount = {},
            )
        }
    }

    @Test
    fun testFilledWithBillingAddress() {
        paparazzi.snapshot {
            BankAccountForm(
                state = BankFormScreenState(isPaymentFlow = true),
                formArgs = createFormArguments(
                    billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                        address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full
                    ),
                ),
                instantDebits = false,
                isPaymentFlow = true,
                nameController = createNameController("John Doe"),
                emailController = createEmailController("email@email.com"),
                phoneController = createPhoneNumberController(),
                addressController = createAddressController(fillAddress = true),
                sameAsShippingElement = null,
                saveForFutureUseElement = SaveForFutureUseElement(
                    initialValue = false,
                    merchantName = "Test Merchant",
                ),
                showCheckbox = false,
                lastTextFieldIdentifier = null,
                onRemoveAccount = {},
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
            hasIntentToSetup = false,
            paymentMethodSaveConsentBehavior = PaymentMethodSaveConsentBehavior.Legacy,
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
}
