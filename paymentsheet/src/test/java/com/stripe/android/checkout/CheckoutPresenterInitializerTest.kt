package com.stripe.android.checkout

import androidx.activity.result.ActivityResultCaller
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.testing.TestLifecycleOwner
import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentelement.confirmation.FakeConfirmationHandler
import com.stripe.android.paymentelement.embedded.FakeEmbeddedSheetLauncher
import com.stripe.android.paymentelement.embedded.content.EmbeddedSheetLauncher
import com.stripe.android.paymentelement.embedded.content.SheetStateHolder
import com.stripe.android.testing.CoroutineTestRule
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.mockito.kotlin.mock
import kotlin.test.Test

internal class CheckoutPresenterInitializerTest {

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    @Test
    fun `initialize registers the confirmation handler with the caller and lifecycle owner`() = runScenario {
        initializer.initialize()

        val registerCall = confirmationHandler.registerTurbine.awaitItem()
        assertThat(registerCall.activityResultCaller).isSameInstanceAs(activityResultCaller)
        assertThat(registerCall.lifecycleOwner).isSameInstanceAs(lifecycleOwner)
    }

    @Test
    fun `initialize registers the sheet launcher into the holder`() = runScenario {
        assertThat(sheetStateHolder.sheetLauncher).isNull()

        initializer.initialize()

        assertThat(sheetStateHolder.sheetLauncher).isSameInstanceAs(sheetLauncher)
        confirmationHandler.registerTurbine.awaitItem()
    }

    @Test
    fun `sheet launcher is cleared from the holder when the lifecycle is destroyed`() = runScenario {
        initializer.initialize()
        assertThat(sheetStateHolder.sheetLauncher).isSameInstanceAs(sheetLauncher)

        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)

        assertThat(sheetStateHolder.sheetLauncher).isNull()
        confirmationHandler.registerTurbine.awaitItem()
    }

    private fun runScenario(
        lifecycleOwner: TestLifecycleOwner = TestLifecycleOwner(),
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val confirmationHandler = FakeConfirmationHandler()
        val activityResultCaller = mock<ActivityResultCaller>()
        val sheetLauncher = FakeEmbeddedSheetLauncher()
        val sheetStateHolder = SheetStateHolder(SavedStateHandle())
        val initializer = CheckoutPresenterInitializer(
            confirmationHandler = confirmationHandler,
            activityResultCaller = activityResultCaller,
            lifecycleOwner = lifecycleOwner,
            sheetLauncher = sheetLauncher,
            sheetStateHolder = sheetStateHolder,
        )

        Scenario(
            initializer = initializer,
            confirmationHandler = confirmationHandler,
            activityResultCaller = activityResultCaller,
            lifecycleOwner = lifecycleOwner,
            sheetLauncher = sheetLauncher,
            sheetStateHolder = sheetStateHolder,
        ).block()

        confirmationHandler.validate()
    }

    private class Scenario(
        val initializer: CheckoutPresenterInitializer,
        val confirmationHandler: FakeConfirmationHandler,
        val activityResultCaller: ActivityResultCaller,
        val lifecycleOwner: TestLifecycleOwner,
        val sheetLauncher: EmbeddedSheetLauncher,
        val sheetStateHolder: SheetStateHolder,
    )
}
