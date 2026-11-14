package com.stripe.android.paymentelement.embedded.sheet

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.isInstanceOf
import com.stripe.android.link.LinkLaunchMode
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.LinkBrand
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.confirmation.FakeConfirmationHandler
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import com.stripe.android.paymentelement.confirmation.link.LinkConfirmationOption
import com.stripe.android.paymentelement.embedded.DefaultEmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.form.OnClickDelegateOverrideImpl
import com.stripe.android.paymentsheet.analytics.FakeEventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.LinkState
import com.stripe.android.paymentsheet.utils.LinkTestUtils
import com.stripe.android.testing.CoroutineTestRule
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

internal class DefaultSheetActivityConfirmationHelperTest {

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    @Test
    fun `confirm invokes onClickOverride instead of continue coordinator when set`() = testScenario(
        configurationModifier = { formSheetAction(EmbeddedPaymentElement.FormSheetAction.Continue) },
    ) {
        val onClickTurbine = Turbine<Unit>()
        onClickDelegate.set {
            onClickTurbine.add(Unit)
        }

        confirmationHelper.confirm()

        assertThat(onClickTurbine.awaitItem()).isNotNull()
        onClickTurbine.ensureAllEventsConsumed()
        confirmationHandler.startTurbine.expectNoEvents()
        continueCoordinator.onContinueCalls.expectNoEvents()
    }

    @Test
    fun `confirm does not invoke onClickOverride after clearing`() = testScenario {
        onClickDelegate.set { }
        onClickDelegate.clear()

        selectionHolder.setSelection(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)
        confirmationHelper.confirm()

        assertThat(confirmationHandler.startTurbine.awaitItem()).isNotNull()
        assertThat(eventReporter.pressConfirmButtonCalls.awaitItem())
            .isEqualTo(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)
    }

    @Test
    fun `confirm starts confirmation with correct option when selection is not null`() = testScenario {
        selectionHolder.setSelection(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)

        confirmationHelper.confirm()

        val args = confirmationHandler.startTurbine.awaitItem()
        assertThat(args.confirmationOption).isInstanceOf<PaymentMethodConfirmationOption.New>()
        assertThat(eventReporter.pressConfirmButtonCalls.awaitItem())
            .isEqualTo(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)
    }

    @Test
    fun `confirm hides Link secondary button`() = testScenario(
        paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            linkState = LinkState(
                configuration = LinkTestUtils.createLinkConfiguration(),
                loginState = LinkState.LoginState.NeedsVerification,
                signupMode = null,
            )
        )
    ) {
        val selection = PaymentSelection.Link(brand = LinkBrand.Link)
        selectionHolder.setSelection(selection)

        confirmationHelper.confirm()

        val args = confirmationHandler.startTurbine.awaitItem()
        assertThat(args.confirmationOption).isInstanceOf<LinkConfirmationOption>()
        val option = args.confirmationOption as LinkConfirmationOption
        assertThat(option.linkLaunchMode).isEqualTo(
            LinkLaunchMode.Full(showSecondaryButton = false)
        )
        assertThat(eventReporter.pressConfirmButtonCalls.awaitItem()).isEqualTo(selection)
    }

    @Test
    fun `confirm does not start confirmation or report event when selection is null`() = testScenario {
        confirmationHelper.confirm()

        confirmationHandler.startTurbine.ensureAllEventsConsumed()
        eventReporter.pressConfirmButtonCalls.ensureAllEventsConsumed()
    }

    @Test
    fun `when formSheetAction=continue confirm delegates to continue coordinator`() = testScenario(
        configurationModifier = { formSheetAction(EmbeddedPaymentElement.FormSheetAction.Continue) }
    ) {
        selectionHolder.setSelection(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)

        confirmationHelper.confirm()

        assertThat(continueCoordinator.onContinueCalls.awaitItem()).isEqualTo(Unit)

        assertThat(eventReporter.pressConfirmButtonCalls.awaitItem())
            .isEqualTo(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)
    }

    private fun testScenario(
        paymentMethodMetadata: PaymentMethodMetadata = PaymentMethodMetadataFactory.create(),
        configurationModifier:
        EmbeddedPaymentElement.Configuration.Builder.() -> EmbeddedPaymentElement.Configuration.Builder = {
            this
        },
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val confirmationHandler = FakeConfirmationHandler()
        val savedStateHandle = SavedStateHandle()
        val selectionHolder = DefaultEmbeddedSelectionHolder(savedStateHandle)
        val configuration = EmbeddedPaymentElement.Configuration.Builder("Example, Inc.")
            .formSheetAction(EmbeddedPaymentElement.FormSheetAction.Confirm)
            .configurationModifier()
            .build()
        val continueCoordinator = FakeSheetActivityContinueCoordinator()
        val onClickDelegate = OnClickDelegateOverrideImpl()
        val eventReporter = FakeEventReporter()
        val confirmationHelper = DefaultSheetActivityConfirmationHelper(
            paymentMethodMetadata = paymentMethodMetadata,
            confirmationHandler = confirmationHandler,
            configuration = configuration,
            selectionHolder = selectionHolder,
            continueCoordinator = continueCoordinator,
            onClickDelegate = onClickDelegate,
            eventReporter = eventReporter,
            coroutineScope = backgroundScope,
            statusBarColor = null,
        )

        Scenario(
            confirmationHelper = confirmationHelper,
            confirmationHandler = confirmationHandler,
            continueCoordinator = continueCoordinator,
            onClickDelegate = onClickDelegate,
            eventReporter = eventReporter,
            selectionHolder = selectionHolder,
        ).block()
        eventReporter.validate()
        confirmationHandler.validate()
        continueCoordinator.validate()
    }

    private class Scenario(
        val confirmationHelper: DefaultSheetActivityConfirmationHelper,
        val confirmationHandler: FakeConfirmationHandler,
        val continueCoordinator: FakeSheetActivityContinueCoordinator,
        val onClickDelegate: OnClickDelegateOverrideImpl,
        val eventReporter: FakeEventReporter,
        val selectionHolder: EmbeddedSelectionHolder,
    )
}
