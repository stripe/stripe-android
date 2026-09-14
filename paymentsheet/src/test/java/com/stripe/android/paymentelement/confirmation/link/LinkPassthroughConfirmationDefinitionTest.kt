package com.stripe.android.paymentelement.confirmation.link

import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.isInstanceOf
import com.stripe.android.link.account.FakeLinkAccountManager
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.SharePaymentDetails
import com.stripe.android.paymentelement.confirmation.CONFIRMATION_PARAMETERS
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.FakeConfirmationOption
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import com.stripe.android.paymentelement.confirmation.asFail
import com.stripe.android.paymentelement.confirmation.asLaunch
import com.stripe.android.paymentelement.confirmation.asNextStep
import com.stripe.android.paymentelement.confirmation.fakeLifecycleOwner
import com.stripe.android.paymentsheet.R
import com.stripe.android.testing.DummyActivityResultCaller
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class LinkPassthroughConfirmationDefinitionTest {
    @Test
    fun `key is LinkPassthrough`() = runScenario {
        assertThat(definition.key).isEqualTo("LinkPassthrough")
    }

    @Test
    fun `option returns LinkPassthroughConfirmationOption`() = runScenario {
        assertThat(definition.option(CONFIRMATION_OPTION))
            .isEqualTo(CONFIRMATION_OPTION)
    }

    @Test
    fun `option returns null for an unrelated option`() = runScenario {
        assertThat(definition.option(FakeConfirmationOption())).isNull()
    }

    @Test
    fun `action shares payment details and returns a launch action`() = runScenario {
        val action = definition.action(
            confirmationOption = CONFIRMATION_OPTION,
            confirmationArgs = CONFIRMATION_PARAMETERS,
        ).asLaunch()

        assertThat(linkAccountManager.sharePaymentDetailsCalls.awaitItem()).isEqualTo(
            SharePaymentDetailsCall(
                paymentDetailsId = "pd_123",
                expectedPaymentMethodType = "card",
                billingPhone = "+15555555555",
                cvc = "123",
                allowRedisplay = "always",
                apiKey = null,
            )
        )
        assertThat(action.receivesResultInProcess).isTrue()

        val option = action.launcherArguments.nextConfirmationOption
        assertThat(option).isInstanceOf<PaymentMethodConfirmationOption>()

        val savedOption = option as PaymentMethodConfirmationOption.Saved
        assertThat(savedOption.paymentMethod.id).isEqualTo("pm_123")
        assertThat(savedOption.paymentMethod.type).isEqualTo(PaymentMethod.Type.Card)
        assertThat(savedOption.shippingInformation).isNull()
        assertThat(savedOption.optionsParams).isNull()
        assertThat(savedOption.originatedFromWallet).isTrue()
        assertThat(savedOption.newPMTransformedForConfirmation).isTrue()
    }

    @Test
    fun `action returns failure when sharing payment details fails`() = runScenario(
        sharePaymentDetailsResult = Result.failure(EXCEPTION),
    ) {
        val action = definition.action(
            confirmationOption = CONFIRMATION_OPTION,
            confirmationArgs = CONFIRMATION_PARAMETERS,
        ).asFail()

        val call = linkAccountManager.sharePaymentDetailsCalls.awaitItem()

        call.assertWith(CONFIRMATION_OPTION)

        assertThat(action.cause).isSameInstanceAs(EXCEPTION)
        assertThat(action.message).isEqualTo(R.string.stripe_something_went_wrong.resolvableString)
        assertThat(action.errorType).isEqualTo(ConfirmationHandler.Result.Failed.ErrorType.Internal)
    }

    @Test
    fun `action returns failure when shared payment method cannot be parsed`() = runScenario(
        sharePaymentDetailsResult = Result.success(
            SharePaymentDetails(
                paymentMethodId = "pm_123",
                encodedPaymentMethod = "not-json",
            )
        ),
    ) {
        val action = definition.action(
            confirmationOption = CONFIRMATION_OPTION,
            confirmationArgs = CONFIRMATION_PARAMETERS,
        ).asFail()

        val call = linkAccountManager.sharePaymentDetailsCalls.awaitItem()

        call.assertWith(CONFIRMATION_OPTION)

        assertThat(action.cause).isInstanceOf(IllegalArgumentException::class.java)
        assertThat(action.message).isEqualTo(R.string.stripe_something_went_wrong.resolvableString)
        assertThat(action.errorType).isEqualTo(ConfirmationHandler.Result.Failed.ErrorType.Internal)
    }

    @Test
    fun `createLauncher returns a launcher that forwards results`() = runScenario {
        DummyActivityResultCaller.test {
            val launcher = definition.createLauncher(
                activityResultCaller = activityResultCaller,
                lifecycleOwner = fakeLifecycleOwner(),
                onResult = { launcherResults.add(it) },
            )
            val result = LinkPassthroughConfirmationDefinition.Result(NEXT_CONFIRMATION_OPTION)

            launcher.onResult(result)

            assertThat(launcherResults.awaitItem()).isEqualTo(result)
        }
    }

    @Test
    fun `launch immediately forwards the next confirmation option`() = runScenario {
        val launcher = LinkPassthroughConfirmationDefinition.Launcher { launcherResults.add(it) }
        val arguments = LinkPassthroughConfirmationDefinition.LauncherArguments(NEXT_CONFIRMATION_OPTION)

        definition.launch(
            launcher = launcher,
            arguments = arguments,
            confirmationOption = CONFIRMATION_OPTION,
            confirmationArgs = CONFIRMATION_PARAMETERS,
        )

        assertThat(launcherResults.awaitItem()).isEqualTo(
            LinkPassthroughConfirmationDefinition.Result(NEXT_CONFIRMATION_OPTION)
        )
    }

    @Test
    fun `toResult returns the next confirmation option and original arguments`() = runScenario {
        val result = definition.toResult(
            confirmationOption = CONFIRMATION_OPTION,
            confirmationArgs = CONFIRMATION_PARAMETERS,
            launcherArgs = LinkPassthroughConfirmationDefinition.LauncherArguments(NEXT_CONFIRMATION_OPTION),
            result = LinkPassthroughConfirmationDefinition.Result(NEXT_CONFIRMATION_OPTION),
        ).asNextStep()

        assertThat(result.confirmationOption).isEqualTo(NEXT_CONFIRMATION_OPTION)
        assertThat(result.arguments).isEqualTo(CONFIRMATION_PARAMETERS)
    }

    private fun runScenario(
        sharePaymentDetailsResult: Result<SharePaymentDetails> = Result.success(SHARE_PAYMENT_DETAILS),
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val linkAccountManager = RecordingLinkAccountManager().apply {
            sharePaymentDetails = sharePaymentDetailsResult
        }
        val scenario = Scenario(
            definition = LinkPassthroughConfirmationDefinition(linkAccountManager),
            linkAccountManager = linkAccountManager,
            launcherResults = Turbine(),
        )

        scenario.block()

        linkAccountManager.sharePaymentDetailsCalls.ensureAllEventsConsumed()
        scenario.launcherResults.ensureAllEventsConsumed()
    }

    private data class Scenario(
        val definition: LinkPassthroughConfirmationDefinition,
        val linkAccountManager: RecordingLinkAccountManager,
        val launcherResults: Turbine<LinkPassthroughConfirmationDefinition.Result>,
    )

    private class RecordingLinkAccountManager : FakeLinkAccountManager() {
        val sharePaymentDetailsCalls = Turbine<SharePaymentDetailsCall>()

        override suspend fun sharePaymentDetails(
            paymentDetailsId: String,
            expectedPaymentMethodType: String?,
            billingPhone: String?,
            cvc: String?,
            allowRedisplay: String?,
            apiKey: String?,
        ): Result<SharePaymentDetails> {
            sharePaymentDetailsCalls.add(
                SharePaymentDetailsCall(
                    paymentDetailsId = paymentDetailsId,
                    expectedPaymentMethodType = expectedPaymentMethodType,
                    billingPhone = billingPhone,
                    cvc = cvc,
                    allowRedisplay = allowRedisplay,
                    apiKey = apiKey,
                )
            )
            return sharePaymentDetails
        }
    }

    private data class SharePaymentDetailsCall(
        val paymentDetailsId: String,
        val expectedPaymentMethodType: String?,
        val billingPhone: String?,
        val cvc: String?,
        val allowRedisplay: String?,
        val apiKey: String?,
    ) {
        fun assertWith(option: LinkPassthroughConfirmationOption) {
            assertThat(paymentDetailsId).isEqualTo(option.paymentDetailsId)
            assertThat(cvc).isEqualTo(option.cvc)
            assertThat(allowRedisplay).isEqualTo(option.allowRedisplay?.value)
            assertThat(expectedPaymentMethodType).isEqualTo(option.expectedPaymentMethodType)
            assertThat(billingPhone).isEqualTo(option.billingPhone)
            assertThat(apiKey).isNull()
        }
    }

    private companion object {
        val EXCEPTION = IllegalStateException("Failed to share payment details")

        val CONFIRMATION_OPTION = LinkPassthroughConfirmationOption(
            paymentDetailsId = "pd_123",
            expectedPaymentMethodType = "card",
            cvc = "123",
            billingPhone = "+15555555555",
            allowRedisplay = PaymentMethod.AllowRedisplay.ALWAYS,
        )

        val SHARE_PAYMENT_DETAILS = SharePaymentDetails(
            paymentMethodId = "pm_123",
            encodedPaymentMethod = """
                {
                  "id": "pm_123",
                  "type": "card",
                  "card": {
                    "brand": "visa",
                    "last4": "4242"
                  }
                }
            """.trimIndent(),
        )

        val NEXT_CONFIRMATION_OPTION = PaymentMethodConfirmationOption.Saved(
            paymentMethod = PaymentMethod.Builder()
                .setId("pm_123")
                .setType(PaymentMethod.Type.Card)
                .build(),
            optionsParams = null,
            shippingInformation = null,
            originatedFromWallet = true,
            newPMTransformedForConfirmation = true,
        )
    }
}
