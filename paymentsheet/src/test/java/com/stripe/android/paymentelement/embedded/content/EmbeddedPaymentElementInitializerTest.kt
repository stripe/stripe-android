package com.stripe.android.paymentelement.embedded.content

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.testing.TestLifecycleOwner
import com.google.common.truth.Truth.assertThat
import com.stripe.android.testing.CoroutineTestRule
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.mockito.Mockito.mock
import kotlin.test.Test

internal class EmbeddedPaymentElementInitializerTest {
    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    @Test
    fun `initialize init and clear sheetLauncher`() = testScenario {
        assertThat(contentHelper.testSheetLauncher).isNull()
        initializer.initialize()
        assertThat(contentHelper.testSheetLauncher).isNotNull()
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        assertThat(contentHelper.testSheetLauncher).isNull()
    }

    private fun testScenario(
        block: Scenario.() -> Unit,
    ) = runTest {
        val contentHelper = FakeEmbeddedContentHelper()
        val lifecycleOwner = TestLifecycleOwner()
        val initializer = EmbeddedPaymentElementInitializer(
            sheetLauncher = mock(),
            contentHelper = contentHelper,
            lifecycleOwner = lifecycleOwner,
        )
        Scenario(
            initializer = initializer,
            contentHelper = contentHelper,
            lifecycleOwner = lifecycleOwner,
        ).block()
    }

    private class Scenario(
        val initializer: EmbeddedPaymentElementInitializer,
        val contentHelper: FakeEmbeddedContentHelper,
        val lifecycleOwner: TestLifecycleOwner,
    )
}
