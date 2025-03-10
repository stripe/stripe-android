package com.stripe.android.paymentelement.embedded.content

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.testing.TestLifecycleOwner
import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentelement.embedded.FakeEmbeddedSheetLauncher
import com.stripe.android.paymentsheet.analytics.FakeEventReporter
import com.stripe.android.testing.CoroutineTestRule
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test

internal class EmbeddedPaymentElementInitializerTest {
    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    @Test
    fun `initialize init and clear sheetLauncher`() = testScenario {
        assertThat(contentHelper.testSheetLauncher).isNull()
        initializer.initialize(true)
        assertThat(contentHelper.testSheetLauncher).isNotNull()
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        assertThat(contentHelper.testSheetLauncher).isNull()
    }

    @Test
    fun `initialize when not applicationIsTaskOwner emits analytics event once`() = testScenario {
        initializer.initialize(false)
        assertThat(eventReporter.cannotProperlyReturnFromLinkAndOtherLPMsCalls.awaitItem()).isEqualTo(Unit)
        initializer.initialize(false)
        eventReporter.cannotProperlyReturnFromLinkAndOtherLPMsCalls.ensureAllEventsConsumed()
    }

    private fun testScenario(
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val contentHelper = FakeEmbeddedContentHelper()
        val lifecycleOwner = TestLifecycleOwner()
        val eventReporter = FakeEventReporter()
        val initializer = EmbeddedPaymentElementInitializer(
            sheetLauncher = FakeEmbeddedSheetLauncher(),
            contentHelper = contentHelper,
            lifecycleOwner = lifecycleOwner,
            savedStateHandle = SavedStateHandle(),
            eventReporter = eventReporter,
        )
        Scenario(
            initializer = initializer,
            contentHelper = contentHelper,
            lifecycleOwner = lifecycleOwner,
            eventReporter = eventReporter,
        ).block()
        eventReporter.validate()
    }

    private class Scenario(
        val initializer: EmbeddedPaymentElementInitializer,
        val contentHelper: FakeEmbeddedContentHelper,
        val lifecycleOwner: TestLifecycleOwner,
        val eventReporter: FakeEventReporter,
    )
}
