package com.stripe.android.paymentelement.confirmation.link

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.isInstanceOf
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.link.LinkActivityResult
import com.stripe.android.link.LinkPaymentLauncher
import com.stripe.android.link.TestFactory
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.link.account.LinkStore
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.FakeConfirmationOption
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import com.stripe.android.paymentelement.confirmation.asCanceled
import com.stripe.android.paymentelement.confirmation.asFailed
import com.stripe.android.paymentelement.confirmation.asLaunch
import com.stripe.android.paymentelement.confirmation.asNextStep
import com.stripe.android.paymentelement.confirmation.asSaved
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.testing.PaymentIntentFactory
import com.stripe.android.testing.PaymentMethodFactory
import com.stripe.android.utils.DummyActivityResultCaller
import com.stripe.android.utils.RecordingLinkPaymentLauncher
import com.stripe.android.utils.RecordingLinkStore
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

internal class LinkConfirmationDefinitionTest {
    @Test
    fun `'key' should be 'Link'`() {
        val definition = createLinkConfirmationDefinition()

        assertThat(definition.key).isEqualTo("Link")
    }

    @Test
    fun `'option' return casted 'LinkConfirmationOption'`() {
        val definition = createLinkConfirmationDefinition()

        assertThat(definition.option(LINK_CONFIRMATION_OPTION)).isEqualTo(LINK_CONFIRMATION_OPTION)
    }

    @Test
    fun `'option' return null for unknown option`() {
        val definition = createLinkConfirmationDefinition()

        assertThat(definition.option(FakeConfirmationOption())).isNull()
    }

    @Test
    fun `'createLauncher' should create launcher properly`() = test {
        val definition = createLinkConfirmationDefinition(linkPaymentLauncher = launcherScenario.launcher)

        val activityResultCaller = DummyActivityResultCaller.noOp()
        val onResult: (LinkActivityResult) -> Unit = {}

        definition.createLauncher(
            activityResultCaller = activityResultCaller,
            onResult = onResult,
        )

        val registerCall = launcherScenario.registerCalls.awaitItem()

        assertThat(registerCall.activityResultCaller).isEqualTo(activityResultCaller)
        assertThat(registerCall.callback).isEqualTo(onResult)
    }

    @Test
    fun `'unregister' should unregister launcher properly`() = test {
        val definition = createLinkConfirmationDefinition()

        definition.unregister(launcherScenario.launcher)

        assertThat(launcherScenario.unregisterCalls.awaitItem()).isNotNull()
    }

    @Test
    fun `'action' should return expected 'Launch' action`() = test {
        val definition = createLinkConfirmationDefinition()

        val action = definition.action(
            confirmationOption = LINK_CONFIRMATION_OPTION,
            confirmationParameters = CONFIRMATION_PARAMETERS,
        )

        assertThat(action).isInstanceOf<ConfirmationDefinition.Action.Launch<Unit>>()

        val launchAction = action.asLaunch()

        assertThat(launchAction.launcherArguments).isEqualTo(Unit)
        assertThat(launchAction.receivesResultInProcess).isFalse()
        assertThat(launchAction.deferredIntentConfirmationType).isNull()
    }

    @Test
    fun `'launch' should launch properly with provided parameters`() = test {
        val definition = createLinkConfirmationDefinition()

        definition.launch(
            confirmationOption = LINK_CONFIRMATION_OPTION,
            confirmationParameters = CONFIRMATION_PARAMETERS,
            launcher = launcherScenario.launcher,
            arguments = Unit,
        )

        val presentCall = launcherScenario.presentCalls.awaitItem()

        assertThat(presentCall.configuration).isEqualTo(LINK_CONFIRMATION_OPTION.configuration)
    }

    @Test
    fun `'toResult' should return 'NextStep' when result is 'PaymentMethodObtained' & also mark Link as used`() = test {
        val definition = createLinkConfirmationDefinition(linkStore = storeScenario.linkStore)

        val paymentMethod = PaymentMethodFactory.card()

        val result = definition.toResult(
            confirmationOption = LINK_CONFIRMATION_OPTION,
            confirmationParameters = CONFIRMATION_PARAMETERS,
            result = LinkActivityResult.PaymentMethodObtained(paymentMethod),
            deferredIntentConfirmationType = null,
        )

        assertThat(result).isInstanceOf<ConfirmationDefinition.Result.NextStep>()

        val nextStepResult = result.asNextStep()

        assertThat(nextStepResult.confirmationOption).isInstanceOf<PaymentMethodConfirmationOption.Saved>()
        assertThat(nextStepResult.parameters).isEqualTo(CONFIRMATION_PARAMETERS)

        val savedOption = nextStepResult.confirmationOption.asSaved()

        assertThat(savedOption.paymentMethod).isEqualTo(paymentMethod)
        assertThat(savedOption.optionsParams).isNull()

        assertThat(storeScenario.markAsUsedCalls.awaitItem()).isNotNull()
    }

    @Test
    fun `'toResult' should return 'Succeeded' when result is 'Completed' & also mark Link as used`() = test {
        val definition = createLinkConfirmationDefinition(linkStore = storeScenario.linkStore)

        val result = definition.toResult(
            confirmationOption = LINK_CONFIRMATION_OPTION,
            confirmationParameters = CONFIRMATION_PARAMETERS,
            result = LinkActivityResult.Completed(
                linkAccountUpdate = LinkAccountUpdate.Value(TestFactory.LINK_ACCOUNT)
            ),
            deferredIntentConfirmationType = null,
        )

        assertThat(result).isEqualTo(
            ConfirmationDefinition.Result.Succeeded(
                intent = CONFIRMATION_PARAMETERS.intent,
                deferredIntentConfirmationType = null
            )
        )
        assertThat(storeScenario.markAsUsedCalls.awaitItem()).isNotNull()
    }

    @Test
    fun `'toResult' should return 'Failed' when result is 'Failed' & also mark Link as used`() = test {
        val linkAccountHolder = mock<LinkAccountHolder>()

        val definition = createLinkConfirmationDefinition(
            linkStore = storeScenario.linkStore,
            linkAccountHolder = linkAccountHolder
        )

        val exception = IllegalStateException("Failed!")

        val result = definition.toResult(
            confirmationOption = LINK_CONFIRMATION_OPTION,
            confirmationParameters = CONFIRMATION_PARAMETERS,
            result = LinkActivityResult.Failed(
                error = exception,
                linkAccountUpdate = LinkAccountUpdate.Value(null)

            ),
            deferredIntentConfirmationType = null
        )

        assertThat(result).isInstanceOf<ConfirmationDefinition.Result.Failed>()

        val failedResult = result.asFailed()

        assertThat(failedResult.cause).isEqualTo(exception)
        assertThat(failedResult.message).isEqualTo(R.string.stripe_something_went_wrong.resolvableString)
        assertThat(failedResult.type).isEqualTo(ConfirmationHandler.Result.Failed.ErrorType.Payment)

        assertThat(storeScenario.markAsUsedCalls.awaitItem()).isNotNull()
        verify(linkAccountHolder).set(null)
    }

    @Test
    fun `'toResult' should be 'Canceled' when result is 'Canceled' with 'LoggedOut' reason & mark Link used`() = test {
        val linkAccountHolder = mock<LinkAccountHolder>()

        val definition = createLinkConfirmationDefinition(
            linkStore = storeScenario.linkStore,
            linkAccountHolder = linkAccountHolder
        )

        val result = definition.toResult(
            confirmationOption = LINK_CONFIRMATION_OPTION,
            confirmationParameters = CONFIRMATION_PARAMETERS,
            result = LinkActivityResult.Canceled(
                reason = LinkActivityResult.Canceled.Reason.LoggedOut,
                linkAccountUpdate = LinkAccountUpdate.Value(TestFactory.LINK_ACCOUNT)
            ),
            deferredIntentConfirmationType = null,
        )

        assertThat(result).isInstanceOf<ConfirmationDefinition.Result.Canceled>()

        val failedResult = result.asCanceled()

        assertThat(failedResult.action).isEqualTo(ConfirmationHandler.Result.Canceled.Action.InformCancellation)

        assertThat(storeScenario.markAsUsedCalls.awaitItem()).isNotNull()
        verify(linkAccountHolder).set(TestFactory.LINK_ACCOUNT)
    }

    @Test
    fun `'toResult' should be 'Canceled' when result is 'Canceled' with 'BackPressed' reason`() = test {
        val linkStore = mock<LinkStore>()
        val linkAccountHolder = mock<LinkAccountHolder>()
        val definition = createLinkConfirmationDefinition(
            linkStore = linkStore,
            linkAccountHolder = linkAccountHolder
        )

        val result = definition.toResult(
            confirmationOption = LINK_CONFIRMATION_OPTION,
            confirmationParameters = CONFIRMATION_PARAMETERS,
            result = LinkActivityResult.Canceled(
                reason = LinkActivityResult.Canceled.Reason.BackPressed,
                linkAccountUpdate = LinkAccountUpdate.None
            ),
            deferredIntentConfirmationType = null
        )

        assertThat(result).isInstanceOf<ConfirmationDefinition.Result.Canceled>()

        val failedResult = result.asCanceled()

        assertThat(failedResult.action).isEqualTo(ConfirmationHandler.Result.Canceled.Action.InformCancellation)
        verify(linkAccountHolder, times(0)).set(any())
    }

    private fun test(test: suspend Scenario.() -> Unit) = runTest {
        RecordingLinkPaymentLauncher.test {
            val launcherScenario = this

            RecordingLinkStore.test {
                val linkStoreScenario = this

                test(
                    Scenario(
                        launcherScenario = launcherScenario,
                        storeScenario = linkStoreScenario,
                    )
                )
            }
        }
    }

    private fun createLinkConfirmationDefinition(
        linkPaymentLauncher: LinkPaymentLauncher = mock(),
        linkStore: LinkStore = mock(),
        linkAccountHolder: LinkAccountHolder = LinkAccountHolder(SavedStateHandle())
    ): LinkConfirmationDefinition {
        return LinkConfirmationDefinition(
            linkPaymentLauncher = linkPaymentLauncher,
            linkStore = linkStore,
            linkAccountHolder = linkAccountHolder
        )
    }

    data class Scenario(
        val launcherScenario: RecordingLinkPaymentLauncher.Scenario,
        val storeScenario: RecordingLinkStore.Scenario,
    )

    private companion object {
        private val PAYMENT_INTENT = PaymentIntentFactory.create()

        private val CONFIRMATION_PARAMETERS = ConfirmationDefinition.Parameters(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = "pi_123_secret_123",
            ),
            intent = PAYMENT_INTENT,
            appearance = PaymentSheet.Appearance(),
            shippingDetails = AddressDetails(),
        )

        private val LINK_CONFIRMATION_OPTION = LinkConfirmationOption(
            configuration = TestFactory.LINK_CONFIGURATION,
        )
    }
}
