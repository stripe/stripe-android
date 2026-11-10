package com.stripe.android.link.analytics

import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.link.LinkActivityResult
import com.stripe.android.model.PaymentMethod
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class DefaultLinkAnalyticsHelperTest {
    @Test
    fun `test onLinkLaunched calls onPopupShow`() = runScenario {
        analyticsHelper.onLinkLaunched(CHECKOUT_SESSION_ID)

        assertThat(eventsReporter.popupCalls.awaitItem())
            .isEqualTo(PopupCall(PopupEvent.Show, CHECKOUT_SESSION_ID))
    }

    @Test
    fun `test onLinkResult with PaymentMethodObtained calls onPopupSuccess`() = runScenario {
        analyticsHelper.onLinkResult(
            linkActivityResult = LinkActivityResult.PaymentMethodObtained(
                paymentMethod = PaymentMethod(
                    id = "pm_123",
                    created = null,
                    liveMode = false,
                    code = null,
                    type = null
                )
            ),
            checkoutSessionId = CHECKOUT_SESSION_ID,
        )

        assertThat(eventsReporter.popupCalls.awaitItem())
            .isEqualTo(PopupCall(PopupEvent.Success, CHECKOUT_SESSION_ID))
    }

    @Test
    fun `test onLinkResult with Completed calls onPopupSuccess`() = runScenario {
        analyticsHelper.onLinkResult(
            linkActivityResult = LinkActivityResult.Completed(
                linkAccountUpdate = LinkAccountUpdate.None
            ),
            checkoutSessionId = CHECKOUT_SESSION_ID,
        )

        assertThat(eventsReporter.popupCalls.awaitItem())
            .isEqualTo(PopupCall(PopupEvent.Success, CHECKOUT_SESSION_ID))
    }

    @Test
    fun `test onLinkResult with Failed calls onPopupError`() = runScenario {
        val error = IllegalStateException()

        analyticsHelper.onLinkResult(
            linkActivityResult = LinkActivityResult.Failed(
                error = error,
                linkAccountUpdate = LinkAccountUpdate.None
            ),
            checkoutSessionId = CHECKOUT_SESSION_ID,
        )

        assertThat(eventsReporter.popupCalls.awaitItem())
            .isEqualTo(PopupCall(PopupEvent.Error(error), CHECKOUT_SESSION_ID))
    }

    @Test
    fun `test onLinkResult with CanceledBackPressed calls onPopupCancel`() = runScenario {
        analyticsHelper.onLinkResult(
            linkActivityResult = LinkActivityResult.Canceled(
                reason = LinkActivityResult.Canceled.Reason.BackPressed,
                linkAccountUpdate = LinkAccountUpdate.None
            ),
            checkoutSessionId = CHECKOUT_SESSION_ID,
        )

        assertThat(eventsReporter.popupCalls.awaitItem())
            .isEqualTo(PopupCall(PopupEvent.Cancel, CHECKOUT_SESSION_ID))
    }

    @Test
    fun `test onLinkResult with CanceledLoggedOut calls onPopupLogout`() = runScenario {
        analyticsHelper.onLinkResult(
            linkActivityResult = LinkActivityResult.Canceled(
                reason = LinkActivityResult.Canceled.Reason.LoggedOut,
                linkAccountUpdate = LinkAccountUpdate.None
            ),
            checkoutSessionId = CHECKOUT_SESSION_ID,
        )

        assertThat(eventsReporter.popupCalls.awaitItem())
            .isEqualTo(PopupCall(PopupEvent.Logout, CHECKOUT_SESSION_ID))
    }

    @Test
    fun `test onLinkPopupSkipped calls onPopupSkipped`() = runScenario {
        analyticsHelper.onLinkPopupSkipped(CHECKOUT_SESSION_ID)

        assertThat(eventsReporter.popupCalls.awaitItem())
            .isEqualTo(PopupCall(PopupEvent.Skipped, CHECKOUT_SESSION_ID))
    }

    private fun runScenario(
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val eventsReporter = PopupRecordingLinkEventsReporter()

        Scenario(
            analyticsHelper = DefaultLinkAnalyticsHelper(eventsReporter),
            eventsReporter = eventsReporter,
        ).block()

        eventsReporter.popupCalls.ensureAllEventsConsumed()
    }

    private class Scenario(
        val analyticsHelper: DefaultLinkAnalyticsHelper,
        val eventsReporter: PopupRecordingLinkEventsReporter,
    )

    private sealed interface PopupEvent {
        data object Show : PopupEvent
        data object Success : PopupEvent
        data object Cancel : PopupEvent
        data class Error(val error: Throwable) : PopupEvent
        data object Logout : PopupEvent
        data object Skipped : PopupEvent
    }

    private data class PopupCall(
        val event: PopupEvent,
        val checkoutSessionId: String?,
    )

    private class PopupRecordingLinkEventsReporter : FakeLinkEventsReporter() {
        val popupCalls = Turbine<PopupCall>()

        override fun onPopupShow(checkoutSessionId: String?) {
            popupCalls.add(PopupCall(PopupEvent.Show, checkoutSessionId))
        }

        override fun onPopupSuccess(checkoutSessionId: String?) {
            popupCalls.add(PopupCall(PopupEvent.Success, checkoutSessionId))
        }

        override fun onPopupCancel(checkoutSessionId: String?) {
            popupCalls.add(PopupCall(PopupEvent.Cancel, checkoutSessionId))
        }

        override fun onPopupError(error: Throwable, checkoutSessionId: String?) {
            popupCalls.add(PopupCall(PopupEvent.Error(error), checkoutSessionId))
        }

        override fun onPopupLogout(checkoutSessionId: String?) {
            popupCalls.add(PopupCall(PopupEvent.Logout, checkoutSessionId))
        }

        override fun onPopupSkipped(checkoutSessionId: String?) {
            popupCalls.add(PopupCall(PopupEvent.Skipped, checkoutSessionId))
        }
    }

    private companion object {
        const val CHECKOUT_SESSION_ID = "cs_test_123"
    }
}
