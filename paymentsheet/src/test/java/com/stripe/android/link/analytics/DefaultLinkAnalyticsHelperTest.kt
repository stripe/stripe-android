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
        analyticsHelper.onLinkLaunched()

        assertThat(eventsReporter.popupCalls.awaitItem())
            .isEqualTo(PopupEvent.Show)
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
        )

        assertThat(eventsReporter.popupCalls.awaitItem())
            .isEqualTo(PopupEvent.Success)
    }

    @Test
    fun `test onLinkResult with Completed calls onPopupSuccess`() = runScenario {
        analyticsHelper.onLinkResult(
            linkActivityResult = LinkActivityResult.Completed(
                linkAccountUpdate = LinkAccountUpdate.None
            ),
        )

        assertThat(eventsReporter.popupCalls.awaitItem())
            .isEqualTo(PopupEvent.Success)
    }

    @Test
    fun `test onLinkResult with Failed calls onPopupError`() = runScenario {
        val error = IllegalStateException()

        analyticsHelper.onLinkResult(
            linkActivityResult = LinkActivityResult.Failed(
                error = error,
                linkAccountUpdate = LinkAccountUpdate.None
            ),
        )

        assertThat(eventsReporter.popupCalls.awaitItem())
            .isEqualTo(PopupEvent.Error(error))
    }

    @Test
    fun `test onLinkResult with CanceledBackPressed calls onPopupCancel`() = runScenario {
        analyticsHelper.onLinkResult(
            linkActivityResult = LinkActivityResult.Canceled(
                reason = LinkActivityResult.Canceled.Reason.BackPressed,
                linkAccountUpdate = LinkAccountUpdate.None
            ),
        )

        assertThat(eventsReporter.popupCalls.awaitItem())
            .isEqualTo(PopupEvent.Cancel)
    }

    @Test
    fun `test onLinkResult with CanceledLoggedOut calls onPopupLogout`() = runScenario {
        analyticsHelper.onLinkResult(
            linkActivityResult = LinkActivityResult.Canceled(
                reason = LinkActivityResult.Canceled.Reason.LoggedOut,
                linkAccountUpdate = LinkAccountUpdate.None
            ),
        )

        assertThat(eventsReporter.popupCalls.awaitItem())
            .isEqualTo(PopupEvent.Logout)
    }

    @Test
    fun `test onLinkPopupSkipped calls onPopupSkipped`() = runScenario {
        analyticsHelper.onLinkPopupSkipped()

        assertThat(eventsReporter.popupCalls.awaitItem())
            .isEqualTo(PopupEvent.Skipped)
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

    private class PopupRecordingLinkEventsReporter : FakeLinkEventsReporter() {
        val popupCalls = Turbine<PopupEvent>()

        override fun onPopupShow() {
            popupCalls.add(PopupEvent.Show)
        }

        override fun onPopupSuccess() {
            popupCalls.add(PopupEvent.Success)
        }

        override fun onPopupCancel() {
            popupCalls.add(PopupEvent.Cancel)
        }

        override fun onPopupError(error: Throwable) {
            popupCalls.add(PopupEvent.Error(error))
        }

        override fun onPopupLogout() {
            popupCalls.add(PopupEvent.Logout)
        }

        override fun onPopupSkipped() {
            popupCalls.add(PopupEvent.Skipped)
        }
    }
}
