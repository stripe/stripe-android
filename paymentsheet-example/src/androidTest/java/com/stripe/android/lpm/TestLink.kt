package com.stripe.android.lpm

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.BasePlaygroundTest
import com.stripe.android.paymentsheet.example.playground.settings.Merchant
import com.stripe.android.paymentsheet.example.playground.settings.MerchantSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.DefaultBillingAddress
import com.stripe.android.paymentsheet.example.playground.settings.DefaultBillingAddressSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.LinkSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.LinkType
import com.stripe.android.paymentsheet.example.playground.settings.LinkTypeSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.SupportedPaymentMethodsSettingsDefinition
import com.stripe.android.test.core.TestParameters
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
internal class TestLink : BasePlaygroundTest() {

    @Test
    fun testLinkPaymentWithBankAccountInPaymentMethodMode() {
        val email = "email_${UUID.randomUUID()}@email.com"

        // Sign up for Link by completing a card payment with the random email.
        testDriver.confirmNewOrGuestComplete(
            testParameters = makeSignUpTestParameters(passthroughMode = false, email = email),
            populateCustomLpmFields = {
                populateCardDetails()
            },
        )

        // Re-use the Link account to confirm with a bank account (triggers OTP flow).
        testDriver.confirmWithBankAccountInLink(
            makeLinkTestParameters(passthroughMode = false, email = email)
        )
    }

    @Test
    fun testLinkPaymentWithBankAccountInPassthroughMode() {
        val email = "email_${UUID.randomUUID()}@email.com"

        // Sign up for Link by completing a card payment with the random email.
        testDriver.confirmNewOrGuestComplete(
            testParameters = makeSignUpTestParameters(passthroughMode = true, email = email),
            populateCustomLpmFields = {
                populateCardDetails()
            },
        )

        // Re-use the Link account to confirm with a bank account (triggers OTP flow).
        testDriver.confirmWithBankAccountInLink(
            makeLinkTestParameters(passthroughMode = true, email = email)
        )
    }

    private fun makeSignUpTestParameters(passthroughMode: Boolean, email: String): TestParameters {
        return TestParameters.create(
            paymentMethodCode = "card",
            authorizationAction = null,
            saveForFutureUseCheckboxVisible = true,
        ) { settings ->
            settings[SupportedPaymentMethodsSettingsDefinition] = if (passthroughMode) "card" else "card,link"
            settings[MerchantSettingsDefinition] = Merchant.US
            settings[LinkSettingsDefinition] = true
            settings[DefaultBillingAddressSettingsDefinition] = DefaultBillingAddress.WithEmail(email)
        }
    }

    private fun makeLinkTestParameters(passthroughMode: Boolean, email: String): TestParameters {
        return TestParameters.create(
            paymentMethodCode = "card",
            authorizationAction = null,
        ) { settings ->
            settings[SupportedPaymentMethodsSettingsDefinition] = if (passthroughMode) "card" else "card,link"
            settings[MerchantSettingsDefinition] = Merchant.US
            settings[LinkSettingsDefinition] = true
            settings[LinkTypeSettingsDefinition] = LinkType.Native
            settings[DefaultBillingAddressSettingsDefinition] = DefaultBillingAddress.WithEmail(email)
        }
    }
}
