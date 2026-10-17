package com.stripe.android.paymentelement.confirmation.linkinline

import com.google.common.truth.Truth.assertThat
import com.stripe.android.isInstanceOf
import com.stripe.android.link.TestFactory
import com.stripe.android.link.ui.inline.SignUpConsentAction
import com.stripe.android.link.ui.inline.UserInput
import com.stripe.android.model.Address
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParamsFixtures
import com.stripe.android.model.PaymentMethodExtraParams
import com.stripe.android.testing.PaymentMethodFactory
import org.junit.Test

internal class LinkInlineSignupConfirmationOptionTest {
    @Test
    fun `sanitizedUserInput leaves sign-in input unchanged`() {
        val userInput = UserInput.SignIn(email = "user@example.com")
        val option = createNewOption(userInput = userInput)

        assertThat(option.sanitizedUserInput).isSameInstanceAs(userInput)
    }

    @Test
    fun `sanitizedUserInput preserves phone and country from full signup form`() {
        val option = createNewOption(
            userInput = signupInput(
                phone = "+14165550123",
                country = "CA",
            ),
            billingDetails = billingDetails(
                phone = "+12065550123",
                country = "US",
            ),
            extraParams = PaymentMethodExtraParams.Card(phoneNumberCountry = "GB"),
        )

        assertThat(option.sanitizedUserInput).isInstanceOf<UserInput.SignUp>()

        val signupInput = option.sanitizedUserInput as UserInput.SignUp
        assertThat(signupInput.phone).isEqualTo("+14165550123")
        assertThat(signupInput.country).isEqualTo("CA")
        assertThat(signupInput.countryInferringMethod).isEqualTo("PHONE_NUMBER")
    }

    @Test
    fun `sanitizedUserInput fills abbreviated signup from billing details and card country`() {
        val option = createNewOption(
            userInput = signupInput(phone = null, country = null),
            billingDetails = billingDetails(
                phone = "+12065550123",
                country = "US",
            ),
            extraParams = PaymentMethodExtraParams.Card(phoneNumberCountry = "CA"),
        )

        assertThat(option.sanitizedUserInput).isInstanceOf<UserInput.SignUp>()

        val signupInput = option.sanitizedUserInput as UserInput.SignUp
        assertThat(signupInput.phone).isEqualTo("+12065550123")
        assertThat(signupInput.country).isEqualTo("CA")
        assertThat(signupInput.countryInferringMethod).isEqualTo("PHONE_NUMBER")
    }

    @Test
    fun `sanitizedUserInput infers country from billing address when no phone is available`() {
        val option = createSavedOption(
            userInput = signupInput(phone = null, country = null),
            billingDetails = billingDetails(phone = null, country = "US"),
        )

        assertThat(option.sanitizedUserInput).isInstanceOf<UserInput.SignUp>()

        val signupInput = option.sanitizedUserInput as UserInput.SignUp
        assertThat(signupInput.phone).isNull()
        assertThat(signupInput.country).isEqualTo("US")
        assertThat(signupInput.countryInferringMethod).isEqualTo("BILLING_ADDRESS")
    }

    private fun createNewOption(
        userInput: UserInput,
        billingDetails: PaymentMethod.BillingDetails = billingDetails(),
        extraParams: PaymentMethodExtraParams? = null,
    ): LinkInlineSignupConfirmationOption.New {
        return LinkInlineSignupConfirmationOption.New(
            createParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD.copy(
                billingDetails = billingDetails,
            ),
            optionsParams = null,
            extraParams = extraParams,
            saveOption = LinkInlineSignupConfirmationOption.PaymentMethodSaveOption.NoRequest,
            linkConfiguration = TestFactory.LINK_CONFIGURATION,
            userInput = userInput,
        )
    }

    private fun createSavedOption(
        userInput: UserInput,
        billingDetails: PaymentMethod.BillingDetails,
    ): LinkInlineSignupConfirmationOption.Saved {
        return LinkInlineSignupConfirmationOption.Saved(
            paymentMethod = PaymentMethodFactory.card(
                last4 = "4242",
                billingDetails = billingDetails,
            ),
            optionsParams = null,
            linkConfiguration = TestFactory.LINK_CONFIGURATION,
            userInput = userInput,
        )
    }

    private fun signupInput(
        phone: String?,
        country: String?,
        countryInferringMethod: String = "PHONE_NUMBER",
    ) = UserInput.SignUp(
        email = "user@example.com",
        phone = phone,
        country = country,
        name = "Jenny Rosen",
        consentAction = SignUpConsentAction.Checkbox,
        countryInferringMethod = countryInferringMethod,
    )

    private fun billingDetails(
        phone: String? = "+14165550123",
        country: String? = "CA",
    ) = PaymentMethod.BillingDetails(
        name = "Jenny Rosen",
        email = "user@example.com",
        phone = phone,
        address = Address(country = country),
    )
}
