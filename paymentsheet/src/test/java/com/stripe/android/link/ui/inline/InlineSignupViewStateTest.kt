package com.stripe.android.link.ui.inline

import com.google.common.truth.Truth.assertThat
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.TestFactory
import com.stripe.android.testing.PaymentIntentFactory
import org.junit.Test

class InlineSignupViewStateTest {

    @Test
    fun `Allows full prefill if showing instead of save-for-future-use for US customers`() {
        val linkConfig = createLinkConfig(
            countryCode = "US",
        )

        val viewState = InlineSignupViewState.create(
            signupMode = LinkSignupMode.InsteadOfSaveForFutureUse,
            config = linkConfig
        )

        assertThat(viewState.prefillEligibleFields).containsExactly(
            LinkSignupField.Email,
            LinkSignupField.Phone,
        )
    }

    @Test
    fun `Allows full prefill if showing instead of save-for-future-use for non-US customers`() {
        val linkConfig = createLinkConfig(
            countryCode = "CA",
        )

        val viewState = InlineSignupViewState.create(
            signupMode = LinkSignupMode.InsteadOfSaveForFutureUse,
            config = linkConfig
        )

        assertThat(viewState.prefillEligibleFields).containsExactly(
            LinkSignupField.Email,
            LinkSignupField.Phone,
            LinkSignupField.Name,
        )
    }

    @Test
    fun `Limits prefill if showing alongside save-for-future-use if all fields have prefills`() {
        val linkConfig = createLinkConfig(
            countryCode = "CA",
        )

        val viewState = InlineSignupViewState.create(
            signupMode = LinkSignupMode.AlongsideSaveForFutureUse,
            config = linkConfig
        )

        assertThat(viewState.prefillEligibleFields).containsExactly(
            LinkSignupField.Email,
            LinkSignupField.Name,
        )
    }

    @Test
    fun `Correct prefill if showing alongside save-for-future-use if not all fields have prefills`() {
        val linkConfig = createLinkConfig(
            countryCode = "CA",
            email = null,
        )

        val viewState = InlineSignupViewState.create(
            signupMode = LinkSignupMode.AlongsideSaveForFutureUse,
            config = linkConfig
        )

        assertThat(viewState.prefillEligibleFields).containsExactly(
            LinkSignupField.Phone,
            LinkSignupField.Name,
        )
    }

    @Test
    fun `Allows default opt-in for eligible users`() {
        testDefaultOptInAllowed(
            countryCode = "US",
            signupMode = LinkSignupMode.InsteadOfSaveForFutureUse,
            merchantEligible = true,
            expectedResult = true
        )
    }

    @Test
    fun `Disables default opt-in if showing the save-future-use checkbox`() {
        testDefaultOptInAllowed(
            countryCode = "US",
            signupMode = LinkSignupMode.AlongsideSaveForFutureUse,
            merchantEligible = true,
            expectedResult = false
        )
    }

    @Test
    fun `Disables default opt-in if outside US`() {
        testDefaultOptInAllowed(
            countryCode = "CA",
            signupMode = LinkSignupMode.InsteadOfSaveForFutureUse,
            merchantEligible = true,
            expectedResult = false
        )
    }

    @Test
    fun `Disables default opt-in if merchant is not eligible`() {
        testDefaultOptInAllowed(
            countryCode = "US",
            signupMode = LinkSignupMode.InsteadOfSaveForFutureUse,
            merchantEligible = false,
            expectedResult = false
        )
    }

    @Test
    fun `Uses Link if using default opt-in and all fields filled out`() {
        val linkConfig = createLinkConfig(
            countryCode = "US",
            allowsDefaultOptIn = true,
        )

        val viewState = InlineSignupViewState.create(
            signupMode = LinkSignupMode.InsteadOfSaveForFutureUse,
            config = linkConfig
        ).copy(
            userInput = UserInput.SignUp(
                email = "email@email.com",
                phone = "5555555555",
                name = null,
                country = "US",
                consentAction = SignUpConsentAction.DefaultOptInWithAllPrefilled,
            )
        )

        assertThat(viewState.useLink).isTrue()
    }

    @Test
    fun `Does not use Link if using default opt-in but not filling out all fields`() {
        val linkConfig = createLinkConfig(
            countryCode = "US",
            allowsDefaultOptIn = true,
        )

        val viewState = InlineSignupViewState.create(
            signupMode = LinkSignupMode.InsteadOfSaveForFutureUse,
            config = linkConfig
        ).copy(
            userInput = null,
        )

        assertThat(viewState.useLink).isFalse()
    }

    private fun testDefaultOptInAllowed(
        countryCode: String,
        signupMode: LinkSignupMode,
        merchantEligible: Boolean,
        expectedResult: Boolean,
    ) {
        val linkConfig = createLinkConfig(
            countryCode = countryCode,
            allowsDefaultOptIn = merchantEligible,
        )

        val viewState = InlineSignupViewState.create(
            signupMode = signupMode,
            config = linkConfig
        )

        assertThat(viewState.allowsDefaultOptIn).isEqualTo(expectedResult)
    }

    private fun createLinkConfig(
        countryCode: String,
        email: String? = "john@doe.ca",
        allowsDefaultOptIn: Boolean = false,
    ): LinkConfiguration {
        return TestFactory.LINK_CONFIGURATION.copy(
            stripeIntent = PaymentIntentFactory.create(countryCode = countryCode),
            customerInfo = TestFactory.LINK_CONFIGURATION.customerInfo.copy(
                email = email
            ),
            allowDefaultOptIn = allowsDefaultOptIn,
        )
    }
}
