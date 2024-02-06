package com.stripe.android.paymentsheet.paymentdatacollection.ach

import android.os.Build
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.customersheet.CustomerSheet
import com.stripe.android.customersheet.ExperimentalCustomerSheetApi
import com.stripe.android.paymentsheet.forms.FormArgumentsFactory
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import com.stripe.android.uicore.analytics.LocalUiEventReporter
import com.stripe.android.uicore.analytics.UiEventReporter
import com.stripe.android.utils.MockPaymentMethodsFactory
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCustomerSheetApi::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class USBankAccountFormTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `on clear account button clicked, should report interaction`() {
        val eventReporter: UiEventReporter = mock()

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalUiEventReporter provides eventReporter
            ) {
                USBankAccountForm(
                    formArgs = FormArgumentsFactory.create(
                        paymentMethod = MockPaymentMethodsFactory.mockPaymentMethod(
                            code = "us_bank_account",
                            displayNameResource = R.string.stripe_paymentsheet_payment_method_us_bank_account,
                            iconResource = R.drawable.stripe_ic_paymentsheet_pm_bank
                        ),
                        cbcEligibility = CardBrandChoiceEligibility.Ineligible,
                        configuration = CustomerSheet.Configuration(
                            merchantDisplayName = "Some merchant"
                        ),
                        merchantName = "Some merchant"
                    ),
                    usBankAccountFormArgs = USBankAccountFormArguments(
                        onBehalfOf = null,
                        isCompleteFlow = false,
                        isPaymentFlow = false,
                        stripeIntentId = null,
                        clientSecret = null,
                        shippingDetails = null,
                        draftPaymentSelection = useBankAccountSelection,
                        onMandateTextChanged = { _, _ -> },
                        onCollectBankAccountResult = { },
                        onConfirmUSBankAccount = { },
                        onUpdatePrimaryButtonState = { },
                        onUpdatePrimaryButtonUIState = { },
                        onError = { },
                    ),
                    modifier = Modifier.testTag(TEST_TAG),
                )
            }
        }

        composeTestRule
            .onNodeWithTag(US_BANK_ACCOUNT_CLEAR_BUTTON_TEST_TAG)
            .performClick()

        verify(eventReporter).onFieldInteracted()
    }

    private companion object {
        const val TEST_TAG = "USBankAccountForm"

        private val useBankAccountSelection = PaymentSelection.New.USBankAccount(
            labelResource = "Test",
            iconResource = 0,
            paymentMethodCreateParams = mock(),
            customerRequestedSave = mock(),
            input = PaymentSelection.New.USBankAccount.Input(
                name = "",
                email = null,
                phone = null,
                address = null,
                saveForFutureUse = false,
            ),
            screenState = USBankAccountFormScreenState.SavedAccount(
                financialConnectionsSessionId = "session_1234",
                intentId = "intent_1234",
                bankName = "Stripe Bank",
                last4 = "6789",
                primaryButtonText = "Continue",
                mandateText = null,
            ),
        )
    }
}
