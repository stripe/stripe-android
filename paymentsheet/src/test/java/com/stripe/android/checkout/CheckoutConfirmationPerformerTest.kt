package com.stripe.android.checkout

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import com.stripe.android.isInstanceOf
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.LinkBrand
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.FakeConfirmationHandler
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import com.stripe.android.paymentelement.confirmation.gpay.GooglePayConfirmationOption
import com.stripe.android.paymentelement.confirmation.link.LinkConfirmationOption
import com.stripe.android.paymentelement.embedded.content.SheetStateHolder
import com.stripe.android.paymentsheet.analytics.FakeEventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import com.stripe.android.paymentsheet.state.LinkState
import com.stripe.android.paymentsheet.utils.LinkTestUtils
import com.stripe.android.testing.FakeStripeImageLoader
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@OptIn(CheckoutSessionPreview::class)
@RunWith(RobolectricTestRunner::class)
internal class CheckoutConfirmationPerformerTest {

    @Test
    fun `confirm does nothing when state is not loaded`() = runScenario(state = null) {
        performer.confirm()
    }

    @Test
    fun `confirm does nothing when there is no selection`() = runScenario(
        state = CheckoutControllerStateFactory.create(paymentSelection = null),
    ) {
        performer.confirm()
    }

    @Test
    fun `confirm does nothing when the selection cannot be converted to a confirmation option`() = runScenario(
        state = CheckoutControllerStateFactory.create(
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(merchantCountry = null),
            paymentSelection = PaymentSelection.GooglePay,
        ),
    ) {
        performer.confirm()
    }

    @Test
    fun `confirm starts confirmation with a Google Pay option`() = runScenario(
        statusBarColor = STATUS_BAR_COLOR,
        state = googlePayState(paymentSelection = PaymentSelection.GooglePay),
    ) {
        performer.confirm()

        val args = confirmationHandler.startTurbine.awaitItem()
        assertThat(args.confirmationOption).isInstanceOf<GooglePayConfirmationOption>()
        assertThat(args.paymentMethodMetadata)
            .isEqualTo(stateHolder.state?.paymentMethodMetadata)
        assertThat(args.statusBarColor).isEqualTo(STATUS_BAR_COLOR)
    }

    @Test
    fun `confirm starts confirmation with a Link option`() = runScenario(
        state = linkState(),
    ) {
        performer.confirm()

        val args = confirmationHandler.startTurbine.awaitItem()
        assertThat(args.confirmationOption).isInstanceOf<LinkConfirmationOption>()
    }

    @Test
    fun `confirm requires a mandate for saved SEPA before the merchant accesses content`() = runScenario(
        state = savedSepaState(embeddedViewDisplaysMandateText = true),
    ) {
        performer.confirm()

        val option = confirmationHandler.startTurbine.awaitItem().confirmationOption
            as PaymentMethodConfirmationOption.Saved
        assertThat(option.hasAcknowledgedSepaMandate).isFalse()
    }

    @Test
    fun `disabling embedded mandate text does not acknowledge saved SEPA`() = runScenario(
        state = savedSepaState(embeddedViewDisplaysMandateText = false),
    ) {
        performer.confirm()

        val option = confirmationHandler.startTurbine.awaitItem().confirmationOption
            as PaymentMethodConfirmationOption.Saved
        assertThat(option.hasAcknowledgedSepaMandate).isFalse()
    }

    @Test
    fun `confirm acknowledges saved SEPA after content is accessed`() = runScenario(
        state = savedSepaState(embeddedViewDisplaysMandateText = true),
    ) {
        mandateState.recordContentAccess(requireNotNull(stateHolder.state).mandateAcknowledgementId)

        performer.confirm()

        val option = confirmationHandler.startTurbine.awaitItem().confirmationOption
            as PaymentMethodConfirmationOption.Saved
        assertThat(option.hasAcknowledgedSepaMandate).isTrue()
    }

    @Test
    fun `content access acknowledges saved SEPA when embedded mandate text is disabled`() = runScenario(
        state = savedSepaState(embeddedViewDisplaysMandateText = false),
    ) {
        mandateState.recordContentAccess(requireNotNull(stateHolder.state).mandateAcknowledgementId)

        performer.confirm()

        val option = confirmationHandler.startTurbine.awaitItem().confirmationOption
            as PaymentMethodConfirmationOption.Saved
        assertThat(option.hasAcknowledgedSepaMandate).isTrue()
    }

    @Test
    fun `confirm acknowledges saved SEPA after mandate text is accessed`() = runScenario(
        state = savedSepaState(embeddedViewDisplaysMandateText = false),
    ) {
        mandateState.recordMandateTextAccess(
            requireNotNull(stateHolder.state).mandateAcknowledgementId,
            PaymentMethodFixtures.SEPA_DEBIT_PAYMENT_METHOD,
        )

        performer.confirm()

        val option = confirmationHandler.startTurbine.awaitItem().confirmationOption
            as PaymentMethodConfirmationOption.Saved
        assertThat(option.hasAcknowledgedSepaMandate).isTrue()
    }

    @Test
    fun `confirm preserves saved SEPA acknowledgement from continuing the sheet`() = runScenario(
        state = savedSepaState(embeddedViewDisplaysMandateText = true).apply {
            paymentSelection?.hasAcknowledgedSepaMandate = true
        },
    ) {
        performer.confirm()

        val option = confirmationHandler.startTurbine.awaitItem().confirmationOption
            as PaymentMethodConfirmationOption.Saved
        assertThat(option.hasAcknowledgedSepaMandate).isTrue()
    }

    @Test
    fun `reading a null card mandate does not acknowledge a later saved SEPA selection`() = runScenario(
        state = CheckoutControllerStateFactory.create(
            paymentSelection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD),
        ),
    ) {
        val cardOption = requireNotNull(stateHolder.session.value?.paymentOption)
        assertThat(cardOption.mandateText).isNull()
        stateHolder.setSelection(PaymentSelection.Saved(PaymentMethodFixtures.SEPA_DEBIT_PAYMENT_METHOD))

        performer.confirm()

        val option = confirmationHandler.startTurbine.awaitItem().confirmationOption
            as PaymentMethodConfirmationOption.Saved
        assertThat(option.hasAcknowledgedSepaMandate).isFalse()
    }

    @Test
    fun `reading one saved SEPA mandate does not acknowledge another saved SEPA selection`() = runScenario(
        state = savedSepaState(embeddedViewDisplaysMandateText = false),
    ) {
        assertThat(stateHolder.session.value?.paymentOption?.mandateText).isNotNull()
        val anotherPaymentMethod = PaymentMethodFixtures.SEPA_DEBIT_PAYMENT_METHOD.copy(id = "pm_another_sepa")
        stateHolder.setSelection(PaymentSelection.Saved(anotherPaymentMethod))

        performer.confirm()

        val option = confirmationHandler.startTurbine.awaitItem().confirmationOption
            as PaymentMethodConfirmationOption.Saved
        assertThat(option.hasAcknowledgedSepaMandate).isFalse()
    }

    @Test
    fun `content access for an earlier configuration does not acknowledge saved SEPA`() = runScenario(
        state = CheckoutControllerStateFactory.create(
            paymentSelection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD),
        ),
    ) {
        mandateState.recordContentAccess(requireNotNull(stateHolder.state).mandateAcknowledgementId)
        stateHolder.state = savedSepaState(embeddedViewDisplaysMandateText = true).copy(
            mandateAcknowledgementId = "new_configuration",
        )

        performer.confirm()

        val option = confirmationHandler.startTurbine.awaitItem().confirmationOption
            as PaymentMethodConfirmationOption.Saved
        assertThat(option.hasAcknowledgedSepaMandate).isFalse()
    }

    @Test
    fun `reading an old payment option does not acknowledge saved SEPA after reconfiguration`() = runScenario(
        state = savedSepaState(embeddedViewDisplaysMandateText = false),
    ) {
        val oldOption = requireNotNull(stateHolder.session.value?.paymentOption)
        stateHolder.state = requireNotNull(stateHolder.state).copy(mandateAcknowledgementId = "new_configuration")
        assertThat(oldOption.mandateText).isNotNull()

        performer.confirm()

        val option = confirmationHandler.startTurbine.awaitItem().confirmationOption
            as PaymentMethodConfirmationOption.Saved
        assertThat(option.hasAcknowledgedSepaMandate).isFalse()
    }

    @Test
    fun `reading the current payment option acknowledges saved SEPA after reconfiguration`() = runScenario(
        state = savedSepaState(embeddedViewDisplaysMandateText = false),
    ) {
        stateHolder.state = requireNotNull(stateHolder.state).copy(mandateAcknowledgementId = "new_configuration")
        assertThat(stateHolder.session.value?.paymentOption?.mandateText).isNotNull()

        performer.confirm()

        val option = confirmationHandler.startTurbine.awaitItem().confirmationOption
            as PaymentMethodConfirmationOption.Saved
        assertThat(option.hasAcknowledgedSepaMandate).isTrue()
    }

    @Test
    fun `confirm records the payment selection for analytics`() = runScenario(
        state = googlePayState(paymentSelection = PaymentSelection.GooglePay),
    ) {
        performer.confirm()
        confirmationHandler.startTurbine.awaitItem()
        confirmationHandler.state.value = ConfirmationHandler.State.Complete(
            ConfirmationHandler.Result.Succeeded(PaymentIntentFixtures.PI_SUCCEEDED)
        )

        assertThat(eventReporter.paymentSuccessCalls.awaitItem().paymentSelection)
            .isEqualTo(PaymentSelection.GooglePay)
    }

    private fun googlePayState(
        paymentSelection: PaymentSelection?,
    ): CheckoutControllerState {
        return CheckoutControllerStateFactory.create(
            paymentSelection = paymentSelection,
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(merchantCountry = "US"),
        )
    }

    private fun linkState(): CheckoutControllerState {
        return CheckoutControllerStateFactory.create(
            paymentSelection = PaymentSelection.Link(brand = LinkBrand.Link),
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                linkState = LinkState(
                    configuration = LinkTestUtils.createLinkConfiguration(),
                    loginState = LinkState.LoginState.NeedsVerification,
                    signupMode = null,
                ),
            ),
        )
    }

    private fun savedSepaState(
        embeddedViewDisplaysMandateText: Boolean,
    ): CheckoutControllerState {
        return CheckoutControllerStateFactory.create(
            embeddedConfiguration = EmbeddedPaymentElement.Configuration.Builder("Example, Inc.")
                .embeddedViewDisplaysMandateText(embeddedViewDisplaysMandateText)
                .build(),
            paymentSelection = PaymentSelection.Saved(PaymentMethodFixtures.SEPA_DEBIT_PAYMENT_METHOD),
        )
    }

    private fun runScenario(
        state: CheckoutControllerState?,
        statusBarColor: Int? = null,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val confirmationHandler = FakeConfirmationHandler()
        val savedStateHandle = SavedStateHandle()
        val mandateState = CheckoutMandateState(savedStateHandle)
        val context = ApplicationProvider.getApplicationContext<Context>()
        val imageLoader = FakeStripeImageLoader()
        val stateHolder = CheckoutControllerStateFactory.createStateHolder(
            savedStateHandle = savedStateHandle,
            paymentOptionFactory = DefaultCheckoutPaymentOptionDisplayDataFactory(
                iconLoader = PaymentSelection.IconLoader(context.resources, imageLoader),
                cardArtDrawableLoader = { null },
                context = context,
                linkAccountHolder = LinkAccountHolder(savedStateHandle),
                mandateState = mandateState,
            ),
        )
        stateHolder.state = state
        val sessionRefresher = FakeCheckoutSessionRefresher()
        val operationCoordinator = CheckoutOperationCoordinator(
            confirmationHandler = confirmationHandler,
            sheetStateHolder = SheetStateHolder(savedStateHandle),
            sessionRefresher = sessionRefresher,
            logger = Logger.noop(),
            resultCallback = {},
            viewModelScope = backgroundScope,
        )
        val eventReporter = FakeEventReporter()
        val analyticsPerformer = CheckoutAnalyticsPerformer(
            confirmationHandler = confirmationHandler,
            eventReporter = eventReporter,
            savedStateHandle = savedStateHandle,
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            analyticsPerformer.reportConfirmationResults()
        }
        val performer = CheckoutConfirmationPerformer(
            confirmationHandler = confirmationHandler,
            stateHolder = stateHolder,
            operationCoordinator = operationCoordinator,
            analyticsPerformer = analyticsPerformer,
            commonConfigurationFactory = CheckoutCommonConfigurationFactory(appName = "Test App"),
            mandateState = mandateState,
            statusBarColor = statusBarColor,
            viewModelScope = backgroundScope,
        )

        Scenario(
            performer = performer,
            confirmationHandler = confirmationHandler,
            eventReporter = eventReporter,
            stateHolder = stateHolder,
            mandateState = mandateState,
        ).block()

        confirmationHandler.validate()
        sessionRefresher.ensureAllEventsConsumed()
        eventReporter.validate()
        imageLoader.ensureAllEventsConsumed()
    }

    private class Scenario(
        val performer: CheckoutConfirmationPerformer,
        val confirmationHandler: FakeConfirmationHandler,
        val eventReporter: FakeEventReporter,
        val stateHolder: CheckoutControllerStateHolder,
        val mandateState: CheckoutMandateState,
    )

    private companion object {
        const val STATUS_BAR_COLOR = 0x00FF00
    }
}
