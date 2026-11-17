package com.stripe.android.paymentelement.embedded.content

import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.testing.TestLifecycleOwner
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.isInstanceOf
import com.stripe.android.link.LinkPaymentMethod
import com.stripe.android.link.TestFactory
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.LinkBrand
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.FakeConfirmationHandler
import com.stripe.android.paymentelement.confirmation.gpay.GooglePayConfirmationOption
import com.stripe.android.paymentelement.confirmation.link.LinkConfirmationOption
import com.stripe.android.paymentelement.embedded.DefaultEmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.linkinline.EmbeddedLinkInlineAuthController.ConfirmVerificationResult
import com.stripe.android.paymentelement.embedded.linkinline.FakeEmbeddedLinkInlineAuth
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.analytics.FakeEventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.LinkState
import com.stripe.android.paymentsheet.utils.LinkTestUtils
import com.stripe.android.testing.CoroutineTestRule
import kotlinx.coroutines.Dispatchers
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
    fun confirmFailsWhileLinkVerificationIsPresented() = testScenario(
        linkInlineAuth = FakeEmbeddedLinkInlineAuth(isPresentingDialog = true),
    ) {
        confirmationHelper.confirm()

        val result = callbackHelper.callbackTurbine.awaitItem()
        assertThat(result).isInstanceOf<EmbeddedPaymentElement.Result.Failed>()
        assertThat((result as EmbeddedPaymentElement.Result.Failed).error.message)
            .isEqualTo("confirm was called while Link verification is presented.")
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
            selection = PaymentSelection.Link(brand = LinkBrand.Link),
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
    fun confirmEndsTypingInTheLinkCard() {
        val linkInlineAuth = FakeEmbeddedLinkInlineAuth()
        testScenario(linkInlineAuth = linkInlineAuth) {
            confirmationHelper.confirm()

            linkInlineAuth.endTypingCalls.awaitItem()
            confirmationHandler.startTurbine.awaitItem()
            assertThat(callbackHelper.stateHelper.stateTurbine.awaitItem()).isNotNull()
        }
        linkInlineAuth.ensureAllEventsConsumed()
    }

    @Test
    fun confirmVerifiesLinkInTheDialogFirstAfterTheCardWasClosed() {
        val linkInlineAuth = FakeEmbeddedLinkInlineAuth(
            verifiesBeforeConfirming = true,
            verifyBeforeConfirmingResult = ConfirmVerificationResult.Verified,
        )
        testScenario(loadedState = linkLoadedState(), linkInlineAuth = linkInlineAuth) {
            confirmationHelper.confirm()

            linkInlineAuth.endTypingCalls.awaitItem()
            linkInlineAuth.verifyBeforeConfirmingCalls.awaitItem()
            assertThat(confirmationHandler.startTurbine.awaitItem().confirmationOption)
                .isInstanceOf<LinkConfirmationOption>()
            assertThat(callbackHelper.stateHelper.stateTurbine.awaitItem()).isNotNull()
        }
        linkInlineAuth.ensureAllEventsConsumed()
    }

    @Test
    fun confirmIsCanceledWhenLinkVerificationIsClosed() {
        val linkInlineAuth = FakeEmbeddedLinkInlineAuth(
            verifiesBeforeConfirming = true,
            verifyBeforeConfirmingResult = ConfirmVerificationResult.Canceled,
        )
        testScenario(loadedState = linkLoadedState(), linkInlineAuth = linkInlineAuth) {
            confirmationHelper.confirm()

            linkInlineAuth.endTypingCalls.awaitItem()
            linkInlineAuth.verifyBeforeConfirmingCalls.awaitItem()
            assertThat(callbackHelper.callbackTurbine.awaitItem())
                .isInstanceOf<EmbeddedPaymentElement.Result.Canceled>()
            assertThat(callbackHelper.stateHelper.stateTurbine.awaitItem()).isNotNull()
        }
        linkInlineAuth.ensureAllEventsConsumed()
    }

    @Test
    fun confirmContinuesWhenTheLinkDialogCantBeShown() {
        val linkInlineAuth = FakeEmbeddedLinkInlineAuth(
            verifiesBeforeConfirming = true,
            verifyBeforeConfirmingResult = ConfirmVerificationResult.Unavailable,
        )
        testScenario(loadedState = linkLoadedState(), linkInlineAuth = linkInlineAuth) {
            confirmationHelper.confirm()

            linkInlineAuth.endTypingCalls.awaitItem()
            linkInlineAuth.verifyBeforeConfirmingCalls.awaitItem()
            assertThat(confirmationHandler.startTurbine.awaitItem().confirmationOption)
                .isInstanceOf<LinkConfirmationOption>()
            assertThat(callbackHelper.stateHelper.stateTurbine.awaitItem()).isNotNull()
        }
        linkInlineAuth.ensureAllEventsConsumed()
    }

    @Test
    fun confirmWithALinkPickDoesNotVerifyFirst() {
        val linkInlineAuth = FakeEmbeddedLinkInlineAuth(verifiesBeforeConfirming = true)
        val pick = LinkPaymentMethod.ConsumerPaymentDetails(
            details = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD,
            collectedCvc = null,
            billingPhone = null,
        )
        testScenario(
            loadedState = linkLoadedState().copy(
                selection = PaymentSelection.Link(brand = LinkBrand.Link, selectedPayment = pick),
            ),
            linkInlineAuth = linkInlineAuth,
        ) {
            confirmationHelper.confirm()

            linkInlineAuth.endTypingCalls.awaitItem()
            assertThat(confirmationHandler.startTurbine.awaitItem().confirmationOption)
                .isInstanceOf<LinkConfirmationOption>()
            assertThat(callbackHelper.stateHelper.stateTurbine.awaitItem()).isNotNull()
        }
        linkInlineAuth.ensureAllEventsConsumed()
    }

    private fun linkLoadedState() = defaultLoadedState().copy(
        selection = PaymentSelection.Link(brand = LinkBrand.Link),
        paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            linkState = LinkState(
                configuration = LinkTestUtils.createLinkConfiguration(),
                loginState = LinkState.LoginState.NeedsVerification,
                signupMode = null,
            )
        )
    )

    private fun defaultLoadedState(): EmbeddedConfirmationStateHolder.State {
        return EmbeddedConfirmationStateHolder.State(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
            selection = PaymentSelection.GooglePay,
            statusBarColor = null,
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
        linkInlineAuth: FakeEmbeddedLinkInlineAuth = FakeEmbeddedLinkInlineAuth(),
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val confirmationHandler = FakeConfirmationHandler()
        val savedStateHandle = SavedStateHandle()
        val selectionHolder = DefaultEmbeddedSelectionHolder(savedStateHandle)
        selectionHolder.setSelection(loadedState?.selection)
        val confirmationStateHolder = EmbeddedConfirmationStateHolder(
            savedStateHandle = savedStateHandle,
            selectionHolder = selectionHolder,
            coroutineScope = backgroundScope,
        )
        confirmationStateHolder.state = loadedState
        val stateHelper = FakeEmbeddedStateHelper()
        stateHelper.state = if (loadedState != null) EmbeddedPaymentElement.State(loadedState, null, Bundle()) else null
        val callbackHelper = FakeEmbeddedResultCallbackHelper(
            stateHelper = stateHelper
        )
        val confirmationHelper = DefaultEmbeddedConfirmationHelper(
            confirmationStarter = EmbeddedConfirmationStarter(
                confirmationHandler = confirmationHandler,
                coroutineScope = backgroundScope,
            ),
            activityResultCaller = mock(),
            lifecycleOwner = TestLifecycleOwner(coroutineDispatcher = Dispatchers.Unconfined),
            confirmationStateHolder = confirmationStateHolder,
            eventReporter = FakeEventReporter(),
            embeddedResultCallbackHelper = callbackHelper,
            linkInlineAuth = linkInlineAuth,
        )
        assertThat(confirmationHandler.registerTurbine.awaitItem()).isNotNull()
        Scenario(
            confirmationHelper = confirmationHelper,
            confirmationHandler = confirmationHandler,
            confirmationStateHolder = confirmationStateHolder,
            callbackHelper = callbackHelper,
        ).block()
        callbackHelper.validate()
        confirmationHandler.validate()
    }

    private class Scenario(
        val confirmationHelper: EmbeddedConfirmationHelper,
        val confirmationHandler: FakeConfirmationHandler,
        val confirmationStateHolder: EmbeddedConfirmationStateHolder,
        val callbackHelper: FakeEmbeddedResultCallbackHelper
    )
}
