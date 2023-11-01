package com.stripe.android.paymentsheet.paymentdatacollection.ach

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import com.stripe.android.ui.core.elements.SaveForFutureUseController
import com.stripe.android.ui.core.elements.SaveForFutureUseElement
import com.stripe.android.uicore.elements.AddressController
import com.stripe.android.uicore.elements.EmailConfig
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.NameConfig
import com.stripe.android.uicore.elements.PhoneNumberController
import com.stripe.android.uicore.elements.SameAsShippingController
import com.stripe.android.uicore.elements.SameAsShippingElement
import com.stripe.android.uicore.elements.SectionSingleFieldElement
import com.stripe.android.utils.screenshots.FontSize
import com.stripe.android.utils.screenshots.PaparazziRule
import com.stripe.android.utils.screenshots.PaymentSheetAppearance
import com.stripe.android.utils.screenshots.SystemAppearance
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test

internal class SavedAccountScreenshotTest {
    @get:Rule
    val paparazzi = PaparazziRule(
        SystemAppearance.values(),
        arrayOf(FontSize.DefaultFont),
        arrayOf(PaymentSheetAppearance.DefaultAppearance),
        boxModifier = Modifier
            .padding(0.dp)
            .fillMaxWidth(),
    )

    private val formArguments = FormArguments(
        paymentMethodCode = PaymentMethod.Type.USBankAccount.code,
        showCheckbox = false,
        showCheckboxControlledFields = false,
        merchantName = "Test Merchant",
        amount = null,
        billingDetails = null,
        cbcEligibility = CardBrandChoiceEligibility.Ineligible,
    )

    private val nameController = NameConfig.createController(null)
    private val emailController = EmailConfig.createController(null)
    private val phoneController = PhoneNumberController()
    private val addressController = AddressController(MutableStateFlow(listOf<SectionSingleFieldElement>()))
    private val sameAsShippingElement = SameAsShippingElement(
        identifier = IdentifierSpec.SameAsShipping,
        controller = SameAsShippingController(false),
    )
    private val saveForFutureUseElement = SaveForFutureUseElement(
        identifier = IdentifierSpec.SaveForFutureUse,
        controller = SaveForFutureUseController(
            saveForFutureUseInitialValue = false,
        ),
        merchantName = "Test Merchant",
    )
    private val screenState = USBankAccountFormScreenState.SavedAccount(
        financialConnectionsSessionId = "session_id",
        intentId = null,
        bankName = "My Bank",
        last4 = "3456",
        primaryButtonText = "Primary button text",
        mandateText = "Mandate text",
    )

    @Test
    fun testPaymentFlow() {
        paparazzi.snapshot {
            SavedAccountScreen(
                formArgs = formArguments,
                isProcessing = false,
                isPaymentFlow = true,
                screenState = screenState,
                nameController = nameController,
                emailController = emailController,
                phoneController = phoneController,
                addressController = addressController,
                lastTextFieldIdentifier = null,
                sameAsShippingElement = sameAsShippingElement,
                saveForFutureUseElement = saveForFutureUseElement,
                onRemoveAccount = { },
            )
        }
    }

    @Test
    fun testSetupFlow() {
        paparazzi.snapshot {
            SavedAccountScreen(
                formArgs = formArguments,
                isProcessing = false,
                isPaymentFlow = false,
                screenState = screenState,
                nameController = nameController,
                emailController = emailController,
                phoneController = phoneController,
                addressController = addressController,
                lastTextFieldIdentifier = null,
                sameAsShippingElement = sameAsShippingElement,
                saveForFutureUseElement = saveForFutureUseElement,
                onRemoveAccount = { },
            )
        }
    }
}
