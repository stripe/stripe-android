package com.stripe.android.lpm

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.BasePlaygroundTest
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.example.playground.settings.AutomaticPaymentMethodsSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.Merchant
import com.stripe.android.paymentsheet.example.playground.settings.MerchantSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.Currency
import com.stripe.android.paymentsheet.example.playground.settings.CurrencySettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.DefaultBillingAddress
import com.stripe.android.paymentsheet.example.playground.settings.DefaultBillingAddressSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.LinkSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.SupportedPaymentMethodsSettingsDefinition
import com.stripe.android.paymentsheet.paymentdatacollection.ach.TEST_TAG_ACCOUNT_DETAILS
import com.stripe.android.paymentsheet.ui.PAYMENT_SHEET_PRIMARY_BUTTON_TEST_TAG
import com.stripe.android.test.core.AuthorizeAction
import com.stripe.android.test.core.DEFAULT_UI_TIMEOUT
import com.stripe.android.test.core.TestParameters
import com.stripe.android.test.core.ui.ComposeButton
import com.stripe.android.utils.ForceNativeBankFlowTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
internal class TestInstantDebits : BasePlaygroundTest() {

    @get:Rule
    val forceNativeBankFlowTestRule = ForceNativeBankFlowTestRule(
        context = ApplicationProvider.getApplicationContext()
    )

    @Test
    fun testInstantDebitsSuccess() {
        val email = "email_${UUID.randomUUID()}@email.com"

        // Sign up for Link by completing a card payment with the random email.
        testDriver.confirmNewOrGuestComplete(
            testParameters = makeSignUpTestParameters(email),
            populateCustomLpmFields = {
                populateCardDetails()
            },
        )

        // Re-use the Link account to confirm with instant debits.
        testDriver.confirmLinkBankPayment(
            testParameters = makeLinkTestParameters(email),
            afterAuthorization = { _, _ ->
                rules.compose.waitUntil(DEFAULT_UI_TIMEOUT.inWholeMilliseconds) {
                    rules.compose
                        .onAllNodesWithTag(TEST_TAG_ACCOUNT_DETAILS)
                        .fetchSemanticsNodes(atLeastOneRootRequired = false)
                        .isNotEmpty()
                }
            }
        )
    }

    @Test
    fun testInstantDebitsCancelAllowsUserToContinue() {
        val email = "email_${UUID.randomUUID()}@email.com"

        // Sign up for Link by completing a card payment with the random email.
        testDriver.confirmNewOrGuestComplete(
            testParameters = makeSignUpTestParameters(email),
            populateCustomLpmFields = {
                populateCardDetails()
            },
        )

        // Re-use the Link account to test cancel behavior.
        testDriver.confirmLinkBankPayment(
            testParameters = makeLinkTestParameters(email).copy(
                authorizationAction = AuthorizeAction.Cancel,
            ),
            afterAuthorization = { _, _ ->
                ComposeButton(rules.compose, hasTestTag(PAYMENT_SHEET_PRIMARY_BUTTON_TEST_TAG))
                    .waitFor(isEnabled())
            }
        )
    }

    private fun makeSignUpTestParameters(email: String): TestParameters {
        return TestParameters.create(
            paymentMethodCode = "card",
            authorizationAction = null,
            saveForFutureUseCheckboxVisible = true,
        ) { settings ->
            settings[MerchantSettingsDefinition] = Merchant.US
            settings[CurrencySettingsDefinition] = Currency.USD
            settings[AutomaticPaymentMethodsSettingsDefinition] = false
            settings[DefaultBillingAddressSettingsDefinition] = DefaultBillingAddress.WithEmail(email)
            settings[LinkSettingsDefinition] = true
            settings[SupportedPaymentMethodsSettingsDefinition] = listOf(
                PaymentMethod.Type.Card,
                PaymentMethod.Type.Link
            ).joinToString(",")
        }
    }

    private fun makeLinkTestParameters(email: String): TestParameters {
        return TestParameters.create(
            paymentMethodCode = "link",
        ) { settings ->
            settings[MerchantSettingsDefinition] = Merchant.US
            settings[CurrencySettingsDefinition] = Currency.USD
            settings[AutomaticPaymentMethodsSettingsDefinition] = false
            settings[DefaultBillingAddressSettingsDefinition] = DefaultBillingAddress.WithEmail(email)
            settings[LinkSettingsDefinition] = true
            settings[SupportedPaymentMethodsSettingsDefinition] = listOf(
                PaymentMethod.Type.Card,
                PaymentMethod.Type.Link
            ).joinToString(",")
        }
    }
}
