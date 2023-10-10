package com.stripe.android.financialconnections.utils

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.TestFinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane.PARTNER_AUTH
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.MockitoAnnotations

@RunWith(JUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class ClickHandlerTest {

    private val eventTracker: TestFinancialConnectionsAnalyticsTracker =
        TestFinancialConnectionsAnalyticsTracker()
    private val logger: Logger = Logger.noop()

    private val clickHandler: ClickHandler = ClickHandler(
        uriUtils = UriUtils(
            logger = logger,
            tracker = eventTracker
        ),
        eventTracker = eventTracker,
        logger = logger
    )

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
    }

    @Test
    fun `Given network URL, when handled, then onNetworkUrlClick is invoked`() = runTest {
        val networkUrl = "http://example.com"
        val pane = PARTNER_AUTH
        var networkUrlClicked = false

        clickHandler.handle(
            uri = networkUrl,
            pane = pane,
            onNetworkUrlClick = { networkUrlClicked = it == networkUrl },
            clickActions = emptyMap()
        )

        assertTrue(networkUrlClicked)
    }

    @Test
    fun `Given URL with event name, when handled, then event is tracked`() = runTest {
        val uriWithEventName = "scheme://path?eventName=testEvent"
        val pane = PARTNER_AUTH
        val eventName = "linked_accounts.testEvent"
        var actionExecuted = false

        // Set actionExecuted to true when the action for the given URI is executed
        val clickActions = mapOf(uriWithEventName to { actionExecuted = true })

        clickHandler.handle(
            uri = uriWithEventName,
            pane = pane,
            onNetworkUrlClick = { _ -> },
            clickActions = clickActions
        )

        // Assert that the action was actually executed
        assertThat(actionExecuted).isTrue()

        eventTracker.assertContainsEvent(
            expectedEventName = eventName,
            expectedParams = mapOf("pane" to pane.value)
        )
    }

    @Test
    fun `Given URI that matches clickAction, when handled, then action is executed`() = runTest {
        val uriAction = "scheme://action"
        val pane = PARTNER_AUTH
        var actionTriggered = false

        val clickActions = mapOf(
            uriAction to { actionTriggered = true }
        )

        clickHandler.handle(
            uri = uriAction,
            pane = pane,
            onNetworkUrlClick = { _ -> },
            clickActions = clickActions
        )

        assertTrue(actionTriggered)
    }

    @Test
    fun `Given non-network URI that doesn't match clickAction, when handled, then error is logged`() =
        runTest {
            val unknownUri = "unknown://uri"
            val pane = PARTNER_AUTH

            clickHandler.handle(
                uri = unknownUri,
                pane = pane,
                onNetworkUrlClick = { _ -> },
                clickActions = mapOf("known://uri" to {})
            )

            eventTracker.assertContainsEvent(
                expectedEventName = "linked_accounts.error.unexpected",
                expectedParams = mapOf(
                    "pane" to pane.value,
                    "error" to "IllegalArgumentException",
                    "error_type" to "IllegalArgumentException",
                )
            )
        }
}
