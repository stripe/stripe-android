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
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.NameConfig
import com.stripe.android.uicore.elements.PhoneNumberController
import com.stripe.android.uicore.elements.SameAsShippingController
import com.stripe.android.uicore.elements.SameAsShippingElement
import com.stripe.android.uicore.elements.TextFieldController
import com.stripe.android.utils.BankFormScreenStateFactory
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

    @Test
    fun testPaymentFlow() {
        takeAccountPreviewScreenShot(
            state = BankFormScreenStateFactory.createWithSession(
                sessionId = "session_1234",
            ),
            instantDebits = false,
            isPaymentFlow = true,
            enabled = true,
        )
    }

    @Test
    fun testPaymentFlowDisabled() {
        takeAccountPreviewScreenShot(
            state = BankFormScreenStateFactory.createWithSession(
                sessionId = "session_1234",
            ),
            instantDebits = false,
            isPaymentFlow = true,
            enabled = false,
        )
    }

    @Test
    fun testSetupFlow() {
        takeAccountPreviewScreenShot(
            state = BankFormScreenStateFactory.createWithSession(
                sessionId = "session_1234",
            ),
            instantDebits = false,
            isPaymentFlow = false,
        )
    }

    @Test
    fun testWithBillingAddress() {
        takeAccountPreviewScreenShot(
            state = BankFormScreenStateFactory.createWithSession(
                sessionId = "session_1234",
            ),
            instantDebits = false,
            isPaymentFlow = true,
            formArguments = defaultFormArguments.copy(
                billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                    address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
                ),
            ),
            fillAddress = true,
        )
    }

    @Test
    fun testWithPromoBadge() {
        takeAccountPreviewScreenShot(
            state = BankFormScreenStateFactory.createWithSession(
                sessionId = "session_1234",
                promoText = "$5",
            ),
            instantDebits = true,
            isPaymentFlow = true,
        )
    }

    @Test
    fun testWithPromoBadgeNextToSuperLongAccountName() {
        takeAccountPreviewScreenShot(
            state = BankFormScreenStateFactory.createWithSession(
                sessionId = "session_1234",
                promoText = "$5",
                eligibleForPromo = false,
                bankName = "SuperDuperUltraLongBankName",
            ),
            instantDebits = true,
            isPaymentFlow = true,
        )
    }

    @Test
    fun testWithIneligiblePromoBadge() {
        takeAccountPreviewScreenShot(
            state = BankFormScreenStateFactory.createWithSession(
                sessionId = "session_1234",
                promoText = "$5",
                eligibleForPromo = false,
            ),
            instantDebits = true,
            isPaymentFlow = true,
        )
    }

    @Test
    fun testPaymentFlowWithSaveForFutureUseOnly() {
        takeAccountPreviewScreenShot(
            state = BankFormScreenStateFactory.createWithSession(
                sessionId = "session_1234",
            ),
            instantDebits = false,
            isPaymentFlow = true,
            enabled = true,
            showCheckboxes = true,
        )
    }

    @Test
    fun testPaymentFlowWithSaveForFutureUseChecked() {
        takeAccountPreviewScreenShot(
            state = BankFormScreenStateFactory.createWithSession(
                sessionId = "session_1234",
            ),
            instantDebits = false,
            isPaymentFlow = true,
            enabled = true,
            showCheckboxes = true,
            beforeSnapshot = {
                saveForFutureUseElement.controller.onValueChange(true)
            }
        )
    }

    @Test
    fun testPaymentFlowWithBothCheckboxesChecked() {
        takeAccountPreviewScreenShot(
            state = BankFormScreenStateFactory.createWithSession(
                sessionId = "session_1234",
            ),
            instantDebits = false,
            isPaymentFlow = true,
            enabled = true,
            showCheckboxes = true,
            beforeSnapshot = {
                saveForFutureUseElement.controller.onValueChange(true)

                setAsDefaultPaymentMethodElement.controller.onValueChange(true)
            }
        )
    }

    private val defaultFormArguments = FormArguments(
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

    private val setAsDefaultPaymentMethodElement = SetAsDefaultPaymentMethodElement(
        initialValue = false,
        shouldShowElementFlow = saveForFutureUseElement.controller.saveForFutureUse
    )

    private fun takeAccountPreviewScreenShot(
        state: BankFormScreenState,
        instantDebits: Boolean,
        isPaymentFlow: Boolean,
        formArguments: FormArguments = defaultFormArguments,
        fillAddress: Boolean = false,
        enabled: Boolean = true,
        showCheckboxes: Boolean = false,
        beforeSnapshot: () -> Unit = {},
    ) {
        paparazzi.snapshot {
            beforeSnapshot()
            BankAccountForm(
                state = state,
                instantDebits = instantDebits,
                isPaymentFlow = isPaymentFlow,
                formArgs = formArguments,
                nameController = createNameController(),
                emailController = createEmailController(),
                phoneController = createPhoneNumberController(),
                addressController = createAddressController(fillAddress = fillAddress),
                sameAsShippingElement = sameAsShippingElement,
                saveForFutureUseElement = saveForFutureUseElement,
                setAsDefaultPaymentMethodElement = setAsDefaultPaymentMethodElement,
                showCheckboxes = showCheckboxes,
                lastTextFieldIdentifier = null,
                onRemoveAccount = {},
                enabled = enabled,
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
