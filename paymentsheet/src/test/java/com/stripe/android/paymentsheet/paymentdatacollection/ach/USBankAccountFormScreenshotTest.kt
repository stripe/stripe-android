package com.stripe.android.paymentsheet.paymentdatacollection.ach

import com.stripe.android.financialconnections.model.BankAccount
import com.stripe.android.financialconnections.model.FinancialConnectionsAccount
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import com.stripe.android.ui.core.elements.SaveForFutureUseController
import com.stripe.android.ui.core.elements.SaveForFutureUseElement
import com.stripe.android.uicore.elements.AddressController
import com.stripe.android.uicore.elements.EmailConfig
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.NameConfig
import com.stripe.android.uicore.elements.PhoneNumberController
import com.stripe.android.utils.screenshots.FontSize
import com.stripe.android.utils.screenshots.PaparazziRule
import com.stripe.android.utils.screenshots.PaymentSheetAppearance
import com.stripe.android.utils.screenshots.SystemAppearance
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test

class USBankAccountFormScreenshotTest {

    @get:Rule
    val paparazziRule = PaparazziRule(
        arrayOf(SystemAppearance.LightTheme),
        arrayOf(PaymentSheetAppearance.DefaultAppearance),
        arrayOf(FontSize.DefaultFont),
    )

    @Test
    fun testBillingDetailsCollection() {
        paparazziRule.snapshot {
            USBankAccountForm(
                screenState = USBankAccountFormScreenState.BillingDetailsCollection(
                    name = "Test",
                    email = "test@test.ca",
                    phone = null,
                    address = null,
                    primaryButtonText = "Continue",
                ),
                formArgs = FormArguments(
                    paymentMethodCode = PaymentMethod.Type.USBankAccount.code,
                    showCheckbox = false,
                    showCheckboxControlledFields = false,
                    merchantName = "Merchant Inc.",
                ),
                processing = false,
                nameController = NameConfig.createController("Test"),
                emailController = EmailConfig.createController("test@test.ca"),
                phoneController = PhoneNumberController.createPhoneNumberController(),
                addressController = AddressController(fieldsFlowable = flowOf(emptyList())),
                lastTextFieldIdentifier = null,
                sameAsShippingElement = null,
                saveForFutureUseElement = SaveForFutureUseElement(
                    identifier = IdentifierSpec.SaveForFutureUse,
                    controller = SaveForFutureUseController(false),
                    merchantName = "Merchant Inc.",
                ),
                onRemoveAccount = {},
            )
        }
    }

    @Test
    fun testMandateCollection() {
        paparazziRule.snapshot {
            USBankAccountForm(
                screenState = USBankAccountFormScreenState.MandateCollection(
                    name = "Test",
                    email = "test@test.ca",
                    phone = null,
                    address = null,
                    primaryButtonText = "Continue",
                    paymentAccount = FinancialConnectionsAccount(
                        id = "fc_account_1234",
                        created = 1,
                        institutionName = "Stripe",
                        livemode = true,
                        supportedPaymentMethodTypes = listOf(
                            FinancialConnectionsAccount.SupportedPaymentMethodTypes.US_BANK_ACCOUNT,
                        ),
                        last4 = "4242",
                    ),
                    financialConnectionsSessionId = "fc_session_1234",
                    intentId = "intent_1234",
                    mandateText = null,
                    saveForFutureUsage = false,
                ),
                formArgs = FormArguments(
                    paymentMethodCode = PaymentMethod.Type.USBankAccount.code,
                    showCheckbox = false,
                    showCheckboxControlledFields = false,
                    merchantName = "Merchant Inc.",
                ),
                processing = false,
                nameController = NameConfig.createController("Test"),
                emailController = EmailConfig.createController("test@test.ca"),
                phoneController = PhoneNumberController.createPhoneNumberController(),
                addressController = AddressController(fieldsFlowable = flowOf(emptyList())),
                lastTextFieldIdentifier = null,
                sameAsShippingElement = null,
                saveForFutureUseElement = SaveForFutureUseElement(
                    identifier = IdentifierSpec.SaveForFutureUse,
                    controller = SaveForFutureUseController(false),
                    merchantName = "Merchant Inc.",
                ),
                onRemoveAccount = {},
            )
        }
    }

    @Test
    fun testVerifyWithMicrodeposits() {
        paparazziRule.snapshot {
            USBankAccountForm(
                screenState = USBankAccountFormScreenState.VerifyWithMicrodeposits(
                    name = "Test",
                    email = "test@test.ca",
                    phone = null,
                    address = null,
                    primaryButtonText = "Continue",
                    paymentAccount = BankAccount(
                        id = "fc_account_1234",
                        bankName = "Stripe",
                        last4 = "4242",
                    ),
                    financialConnectionsSessionId = "fc_session_1234",
                    intentId = "intent_1234",
                    mandateText = null,
                    saveForFutureUsage = false,
                ),
                formArgs = FormArguments(
                    paymentMethodCode = PaymentMethod.Type.USBankAccount.code,
                    showCheckbox = false,
                    showCheckboxControlledFields = false,
                    merchantName = "Merchant Inc.",
                ),
                processing = false,
                nameController = NameConfig.createController("Test"),
                emailController = EmailConfig.createController("test@test.ca"),
                phoneController = PhoneNumberController.createPhoneNumberController(),
                addressController = AddressController(fieldsFlowable = flowOf(emptyList())),
                lastTextFieldIdentifier = null,
                sameAsShippingElement = null,
                saveForFutureUseElement = SaveForFutureUseElement(
                    identifier = IdentifierSpec.SaveForFutureUse,
                    controller = SaveForFutureUseController(false),
                    merchantName = "Merchant Inc.",
                ),
                onRemoveAccount = {},
            )
        }
    }

    @Test
    fun testSavedAccount() {
        paparazziRule.snapshot {
            USBankAccountForm(
                screenState = USBankAccountFormScreenState.SavedAccount(
                    name = "Test",
                    email = "test@test.ca",
                    phone = null,
                    address = null,
                    primaryButtonText = "Continue",
                    bankName = "Stripe",
                    last4 = "4242",
                    financialConnectionsSessionId = "fc_session_1234",
                    intentId = "intent_1234",
                    mandateText = null,
                    saveForFutureUsage = false,
                ),
                formArgs = FormArguments(
                    paymentMethodCode = PaymentMethod.Type.USBankAccount.code,
                    showCheckbox = false,
                    showCheckboxControlledFields = false,
                    merchantName = "Merchant Inc.",
                ),
                processing = false,
                nameController = NameConfig.createController("Test"),
                emailController = EmailConfig.createController("test@test.ca"),
                phoneController = PhoneNumberController.createPhoneNumberController(),
                addressController = AddressController(fieldsFlowable = flowOf(emptyList())),
                lastTextFieldIdentifier = null,
                sameAsShippingElement = null,
                saveForFutureUseElement = SaveForFutureUseElement(
                    identifier = IdentifierSpec.SaveForFutureUse,
                    controller = SaveForFutureUseController(false),
                    merchantName = "Merchant Inc.",
                ),
                onRemoveAccount = {},
            )
        }
    }
}
