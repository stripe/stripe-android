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
import com.stripe.android.ui.core.elements.SetAsDefaultPaymentMethodElement
import com.stripe.android.uicore.elements.EmailConfig
import com.stripe.android.uicore.elements.NameConfig
import com.stripe.android.uicore.elements.PhoneNumberController
import com.stripe.android.uicore.elements.TextFieldController
import kotlinx.coroutines.flow.MutableStateFlow
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
        testBillingDetailsCollectionScreenShot(
            bankFormScreenState = BankFormScreenState(isPaymentFlow = true),
            formArgs = createFormArguments(),
        )
    }

    @Test
    fun testEmptySetupFlow() {
        testBillingDetailsCollectionScreenShot(
            bankFormScreenState = BankFormScreenState(isPaymentFlow = false),
            instantDebits = false,
            isPaymentFlow = false,
            formArgs = createFormArguments(),
        )
    }

    @Test
    fun testPromoDisclaimer() {
        testBillingDetailsCollectionScreenShot(
            bankFormScreenState = BankFormScreenState(
                isPaymentFlow = true,
                promoText = "$5",
            ),
            formArgs = createFormArguments(),
            nameControllerInitialValue = "John Doe",
            emailControllerInitialValue = "email@email.com",
        )
    }

    @Test
    fun testPromoDisclaimerSetupFlow() {
        testBillingDetailsCollectionScreenShot(
            bankFormScreenState = BankFormScreenState(
                isPaymentFlow = false,
                promoText = "$5",
            ),
            instantDebits = false,
            isPaymentFlow = false,
            formArgs = createFormArguments(),
            nameControllerInitialValue = "John Doe",
            emailControllerInitialValue = "email@email.com",
        )
    }

    @Test
    fun testFilled() {
        testBillingDetailsCollectionScreenShot(
            bankFormScreenState = BankFormScreenState(isPaymentFlow = true),
            formArgs = createFormArguments(),
            nameControllerInitialValue = "John Doe",
            emailControllerInitialValue = "email@email.com",
        )
    }

    @Test
    fun testFilledDisabled() {
        testBillingDetailsCollectionScreenShot(
            bankFormScreenState = BankFormScreenState(isPaymentFlow = true),
            formArgs = createFormArguments(),
            nameControllerInitialValue = "John Doe",
            emailControllerInitialValue = "email@email.com",
            enabled = false,
        )
    }

    @Test
    fun testEmptyWithBillingAddress() {
        testBillingDetailsCollectionScreenShot(
            bankFormScreenState = BankFormScreenState(isPaymentFlow = true),
            formArgs = createFormArguments(
                billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                    address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full
                ),
            ),
        )
    }

    @Test
    fun testFilledWithBillingAddress() {
        testBillingDetailsCollectionScreenShot(
            bankFormScreenState = BankFormScreenState(isPaymentFlow = true),
            formArgs = createFormArguments(
                billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                    address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full
                ),
            ),
            nameControllerInitialValue = "John Doe",
            emailControllerInitialValue = "email@email.com",
            fillAddress = true,
        )
    }

    private val saveForFutureUseElement = SaveForFutureUseElement(
        initialValue = false,
        merchantName = "Test Merchant",
    )

    private val shouldShowElementFlow = MutableStateFlow(false)
    private val setAsDefaultPaymentMethodElement = SetAsDefaultPaymentMethodElement(
        initialValue = false,
        shouldShowElementFlow = shouldShowElementFlow
    )

    private fun testBillingDetailsCollectionScreenShot(
        bankFormScreenState: BankFormScreenState,
        formArgs: FormArguments,
        instantDebits: Boolean = false,
        isPaymentFlow: Boolean = true,
        nameControllerInitialValue: String? = null,
        emailControllerInitialValue: String? = null,
        phoneControllerInitialValue: String = "",
        fillAddress: Boolean = false,
        enabled: Boolean = true,
    ) {
        paparazzi.snapshot {
            BankAccountForm(
                state = bankFormScreenState,
                formArgs = formArgs,
                instantDebits = instantDebits,
                isPaymentFlow = isPaymentFlow,
                nameController = createNameController(nameControllerInitialValue),
                emailController = createEmailController(emailControllerInitialValue),
                phoneController = createPhoneNumberController(phoneControllerInitialValue),
                addressController = createAddressController(fillAddress),
                sameAsShippingElement = null,
                saveForFutureUseElement = saveForFutureUseElement,
                setAsDefaultPaymentMethodElement = setAsDefaultPaymentMethodElement,
                showCheckboxes = false,
                lastTextFieldIdentifier = null,
                onRemoveAccount = {},
                enabled = enabled
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
