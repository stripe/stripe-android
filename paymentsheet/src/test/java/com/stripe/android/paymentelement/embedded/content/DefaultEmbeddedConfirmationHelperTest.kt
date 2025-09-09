package com.stripe.android.paymentelement.embedded.content

import android.os.Bundle
import androidx.activity.result.ActivityResultCaller
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.testing.TestLifecycleOwner
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.isInstanceOf
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PassiveCaptchaParamsFactory
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.FakeConfirmationHandler
import com.stripe.android.paymentelement.confirmation.gpay.GooglePayConfirmationOption
import com.stripe.android.paymentelement.confirmation.link.LinkConfirmationOption
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.analytics.FakeEventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.LinkState
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.paymentsheet.utils.LinkTestUtils
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.utils.FakePassiveChallengeWarmer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.mockito.Mockito.mock
import kotlin.test.Test

internal class DefaultEmbeddedConfirmationHelperTest {
    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    @Test
    fun constructorWiresUpConfirmationHandlerToResultCallback() = testScenario {
        val exception = IllegalStateException("Test failure.")
        confirmationHandler.state.value = ConfirmationHandler.State.Complete(
            ConfirmationHandler.Result.Failed(
                cause = exception,
                message = "Error".resolvableString,
                type = ConfirmationHandler.Result.Failed.ErrorType.Internal,
            )
        )
        assertThat(callbackHelper.callbackTurbine.awaitItem()).isEqualTo(
            EmbeddedPaymentElement.Result.Failed(exception)
        )
        assertThat(callbackHelper.stateHelper.stateTurbine.awaitItem()).isNotNull()
    }

    @Test
    fun `successful confirm clears state`() = testScenario {
        assertThat(confirmationStateHolder.state).isNotNull()
        assertThat(callbackHelper.stateHelper.stateTurbine.awaitItem()).isNotNull()
        confirmationHandler.state.value = ConfirmationHandler.State.Complete(
            ConfirmationHandler.Result.Succeeded(
                intent = PaymentIntentFixtures.PI_SUCCEEDED,
                deferredIntentConfirmationType = null,
            )
        )
        assertThat(callbackHelper.stateHelper.stateTurbine.awaitItem()).isNull()
        assertThat(callbackHelper.callbackTurbine.awaitItem()).isInstanceOf<EmbeddedPaymentElement.Result.Completed>()
    }

    @Test
    fun `cancelled confirm does not clear state`() = testScenario {
        assertThat(confirmationStateHolder.state).isNotNull()
        assertThat(callbackHelper.stateHelper.stateTurbine.expectMostRecentItem()).isNotNull()
        confirmationHandler.state.value = ConfirmationHandler.State.Complete(
            ConfirmationHandler.Result.Canceled(ConfirmationHandler.Result.Canceled.Action.InformCancellation)
        )
        assertThat(callbackHelper.callbackTurbine.awaitItem()).isInstanceOf<EmbeddedPaymentElement.Result.Canceled>()
        assertThat(confirmationStateHolder.state).isNotNull()
        callbackHelper.stateHelper.stateTurbine.ensureAllEventsConsumed()
    }

    @Test
    fun confirmCallsResultCallbackWithFailureWhenLoadedStateIsNull() = testScenario(
        loadedState = null,
    ) {
        confirmationHelper.confirm()
        assertThat(callbackHelper.callbackTurbine.awaitItem()).isInstanceOf<EmbeddedPaymentElement.Result.Failed>()
        assertThat(callbackHelper.stateHelper.stateTurbine.awaitItem()).isNull()
    }

    @Test
    fun confirmCallsResultCallbackWithFailureWhenNoSelection() = testScenario(
        loadedState = defaultLoadedState().copy(selection = null),
    ) {
        confirmationHelper.confirm()
        assertThat(callbackHelper.callbackTurbine.awaitItem()).isInstanceOf<EmbeddedPaymentElement.Result.Failed>()
        assertThat(callbackHelper.stateHelper.stateTurbine.awaitItem()).isNotNull()
    }

    @Test
    fun confirmCallsConfirmationHandlerStart() = testScenario {
        confirmationHelper.confirm()
        val args = confirmationHandler.startTurbine.awaitItem()
        assertThat(args.confirmationOption).isInstanceOf<GooglePayConfirmationOption>()
        assertThat(callbackHelper.stateHelper.stateTurbine.awaitItem()).isNotNull()
    }

    @Test
    fun confirmCallsConfirmationHandlerStartWithLink() = testScenario(
        loadedState = defaultLoadedState().copy(
            selection = PaymentSelection.Link(),
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                linkState = LinkState(
                    configuration = LinkTestUtils.createLinkConfiguration(),
                    loginState = LinkState.LoginState.NeedsVerification,
                    signupMode = null,
                )
            )
        )
    ) {
        confirmationHelper.confirm()
        val args = confirmationHandler.startTurbine.awaitItem()
        assertThat(args.confirmationOption).isInstanceOf<LinkConfirmationOption>()
        assertThat(callbackHelper.stateHelper.stateTurbine.awaitItem()).isNotNull()
    }

    @Test
    fun `On helper creation, should register PassiveChallengeWarmer`() = testScenario {
        val registerCall = passiveChallengeWarmer.awaitRegisterCall()

        assertThat(registerCall.activityResultCaller).isEqualTo(activityResultCaller)
        assertThat(registerCall.lifecycleOwner).isEqualTo(lifecycleOwner)

        passiveChallengeWarmer.ensureAllEventsConsumed()
        assertThat(callbackHelper.stateHelper.stateTurbine.awaitItem()).isNotNull()
    }

    @Test
    fun `PassiveChallengeWarmer should be started when passive captcha params are available`() = testScenario(
        loadedState = defaultLoadedState().copy(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                passiveCaptchaParams = PassiveCaptchaParamsFactory.passiveCaptchaParams()
            )
        ),
    ) {
        passiveChallengeWarmer.awaitRegisterCall()

        val startCall = passiveChallengeWarmer.awaitStartCall()
        assertThat(startCall.passiveCaptchaParams).isEqualTo(PassiveCaptchaParamsFactory.passiveCaptchaParams())
        assertThat(startCall.publishableKey).isEqualTo("pk_test_embedded_12345")
        assertThat(startCall.productUsage).containsExactlyElementsIn(setOf("EmbeddedPaymentElement"))

        passiveChallengeWarmer.ensureAllEventsConsumed()
        assertThat(callbackHelper.stateHelper.stateTurbine.awaitItem()).isNotNull()
    }

    @Test
    fun `PassiveChallengeWarmer should not be started when passive captcha params are not available`() = testScenario {
        passiveChallengeWarmer.awaitRegisterCall()
        passiveChallengeWarmer.ensureAllEventsConsumed()
        assertThat(callbackHelper.stateHelper.stateTurbine.awaitItem()).isNotNull()
    }

    private fun defaultLoadedState(): EmbeddedConfirmationStateHolder.State {
        return EmbeddedConfirmationStateHolder.State(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
            selection = PaymentSelection.GooglePay,
            initializationMode = PaymentElementLoader.InitializationMode.DeferredIntent(
                PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                        amount = 5000,
                        currency = "USD",
                    ),
                ),
            ),
            configuration = EmbeddedPaymentElement.Configuration.Builder("Example, Inc")
                .googlePay(
                    PaymentSheet.GooglePayConfiguration(
                        environment = PaymentSheet.GooglePayConfiguration.Environment.Test,
                        countryCode = "US",
                    )
                )
                .build()
        )
    }

    private fun testScenario(
        loadedState: EmbeddedConfirmationStateHolder.State? = defaultLoadedState(),
        passiveChallengeWarmer: FakePassiveChallengeWarmer = FakePassiveChallengeWarmer(),
        productUsage: Set<String> = setOf("EmbeddedPaymentElement"),
        publishableKeyProvider: () -> String = { "pk_test_embedded_12345" },
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val confirmationHandler = FakeConfirmationHandler()
        val savedStateHandle = SavedStateHandle()
        val selectionHolder = EmbeddedSelectionHolder(savedStateHandle)
        selectionHolder.set(loadedState?.selection)
        val confirmationStateHolder = EmbeddedConfirmationStateHolder(
            savedStateHandle = savedStateHandle,
            selectionHolder = selectionHolder,
            coroutineScope = CoroutineScope(UnconfinedTestDispatcher()),
        )
        confirmationStateHolder.state = loadedState
        val stateHelper = FakeEmbeddedStateHelper()
        stateHelper.state = if (loadedState != null) EmbeddedPaymentElement.State(loadedState, null, Bundle()) else null
        val callbackHelper = FakeEmbeddedResultCallbackHelper(
            stateHelper = stateHelper
        )
        val activityResultCaller = mock<ActivityResultCaller>()
        val lifecycleOwner = TestLifecycleOwner(coroutineDispatcher = Dispatchers.Unconfined)
        val confirmationHelper = DefaultEmbeddedConfirmationHelper(
            confirmationStarter = EmbeddedConfirmationStarter(
                confirmationHandler = confirmationHandler,
                coroutineScope = backgroundScope,
            ),
            activityResultCaller = activityResultCaller,
            lifecycleOwner = lifecycleOwner,
            confirmationStateHolder = confirmationStateHolder,
            eventReporter = FakeEventReporter(),
            embeddedResultCallbackHelper = callbackHelper,
            passiveChallengeWarmer = passiveChallengeWarmer,
            productUsage = productUsage,
            publishableKeyProvider = publishableKeyProvider
        )
        assertThat(confirmationHandler.registerTurbine.awaitItem()).isNotNull()
        Scenario(
            confirmationHelper = confirmationHelper,
            confirmationHandler = confirmationHandler,
            confirmationStateHolder = confirmationStateHolder,
            callbackHelper = callbackHelper,
            passiveChallengeWarmer = passiveChallengeWarmer,
            activityResultCaller = activityResultCaller,
            lifecycleOwner = lifecycleOwner,
        ).block()
        callbackHelper.validate()
        confirmationHandler.validate()
    }

    private class Scenario(
        val confirmationHelper: EmbeddedConfirmationHelper,
        val confirmationHandler: FakeConfirmationHandler,
        val confirmationStateHolder: EmbeddedConfirmationStateHolder,
        val callbackHelper: FakeEmbeddedResultCallbackHelper,
        val passiveChallengeWarmer: FakePassiveChallengeWarmer,
        val activityResultCaller: ActivityResultCaller,
        val lifecycleOwner: TestLifecycleOwner,
    )
}
