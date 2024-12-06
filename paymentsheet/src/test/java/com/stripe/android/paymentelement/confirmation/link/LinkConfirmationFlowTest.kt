package com.stripe.android.paymentelement.confirmation.link

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.isInstanceOf
import com.stripe.android.link.LinkActivityResult
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.ConfirmationMediator
import com.stripe.android.paymentelement.confirmation.ConfirmationMediator.Parameters
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import com.stripe.android.paymentelement.confirmation.asLaunch
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.testing.PaymentIntentFactory
import com.stripe.android.testing.PaymentMethodFactory
import com.stripe.android.utils.DummyActivityResultCaller
import com.stripe.android.utils.RecordingLinkPaymentLauncher
import com.stripe.android.utils.RecordingLinkStore
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class LinkConfirmationFlowTest {
    @Test
    fun `on launch, should persist parameters & launch using launcher as expected`() = test {
        val savedStateHandle = SavedStateHandle()
        val mediator = ConfirmationMediator(
            savedStateHandle,
            LinkConfirmationDefinition(
                linkPaymentLauncher = launcher,
                linkStore = RecordingLinkStore.noOp(),
            )
        )

        val activityResultCaller = DummyActivityResultCaller.noOp()

        mediator.register(
            activityResultCaller = activityResultCaller,
            onResult = {}
        )

        val registerCall = registerCalls.awaitItem()

        assertThat(registerCall.activityResultCaller).isEqualTo(activityResultCaller)

        val action = mediator.action(
            option = LINK_CONFIRMATION_OPTION,
            intent = PAYMENT_INTENT,
        )

        assertThat(action).isInstanceOf<ConfirmationMediator.Action.Launch>()

        val launchAction = action.asLaunch()

        launchAction.launch()

        val presentCall = presentCalls.awaitItem()

        assertThat(presentCall.configuration).isEqualTo(LINK_CONFIRMATION_OPTION.configuration)

        val parameters = savedStateHandle.get<Parameters<LinkConfirmationOption>>("LinkParameters")

        assertThat(parameters?.confirmationOption).isEqualTo(LINK_CONFIRMATION_OPTION)
        assertThat(parameters?.intent).isEqualTo(PAYMENT_INTENT)
        assertThat(parameters?.deferredIntentConfirmationType).isNull()
    }

    @Test
    fun `on result, should return confirmation result as expected`() = test {
        val countDownLatch = CountDownLatch(1)

        val savedStateHandle = SavedStateHandle().apply {
            set(
                "LinkParameters",
                Parameters(
                    confirmationOption = LINK_CONFIRMATION_OPTION,
                    intent = PAYMENT_INTENT,
                    deferredIntentConfirmationType = null,
                )
            )
        }

        val mediator = ConfirmationMediator(
            savedStateHandle,
            LinkConfirmationDefinition(
                linkPaymentLauncher = launcher,
                linkStore = RecordingLinkStore.noOp(),
            )
        )

        var result: ConfirmationDefinition.Result? = null

        val activityResultCaller = DummyActivityResultCaller.noOp()
        val onResult: (ConfirmationDefinition.Result) -> Unit = {
            result = it

            countDownLatch.countDown()
        }

        mediator.register(
            activityResultCaller = activityResultCaller,
            onResult = onResult
        )

        val registerCall = registerCalls.awaitItem()

        assertThat(registerCall.activityResultCaller).isEqualTo(activityResultCaller)

        registerCall.callback(LinkActivityResult.Completed(PAYMENT_METHOD))

        assertThat(countDownLatch.await(5, TimeUnit.SECONDS)).isTrue()

        assertThat(result).isEqualTo(
            ConfirmationDefinition.Result.NextStep(
                intent = PAYMENT_INTENT,
                confirmationOption = PaymentMethodConfirmationOption.Saved(
                    initializationMode = LINK_CONFIRMATION_OPTION.initializationMode,
                    shippingDetails = LINK_CONFIRMATION_OPTION.shippingDetails,
                    paymentMethod = PAYMENT_METHOD,
                    optionsParams = null,
                )
            )
        )
    }

    private fun test(
        test: suspend RecordingLinkPaymentLauncher.Scenario.() -> Unit,
    ) = runTest {
        RecordingLinkPaymentLauncher.test {
            test(this)
        }
    }

    private companion object {
        private val PAYMENT_METHOD = PaymentMethodFactory.card()

        private val PAYMENT_INTENT = PaymentIntentFactory.create()

        private val LINK_CONFIRMATION_OPTION = LinkConfirmationOption(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = "pi_123_secret_123",
            ),
            shippingDetails = null,
            configuration = LinkConfiguration(
                stripeIntent = PAYMENT_INTENT,
                merchantName = "Merchant Inc.",
                merchantCountryCode = "CA",
                customerInfo = LinkConfiguration.CustomerInfo(
                    name = "Jphn Doe",
                    email = "johndoe@email.com",
                    phone = "+1123456789",
                    billingCountryCode = "CA"
                ),
                shippingValues = mapOf(),
                passthroughModeEnabled = false,
                flags = mapOf(),
                cardBrandChoice = null,
            ),
        )
    }
}
