package com.stripe.android.checkout

import androidx.activity.result.ActivityResultCaller
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.testing.TestLifecycleOwner
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.elements.ShippingAddressElement
import com.stripe.android.paymentelement.confirmation.FakeConfirmationHandler
import com.stripe.android.paymentelement.embedded.FakeEmbeddedSheetLauncher
import com.stripe.android.paymentelement.embedded.content.EmbeddedSheetLauncher
import com.stripe.android.paymentelement.embedded.content.SheetStateHolder
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.uicore.StripeTheme
import dagger.Lazy
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.mockito.kotlin.mock
import kotlin.test.Test

@OptIn(
    com.stripe.android.paymentelement.CheckoutSessionPreview::class,
    com.stripe.android.paymentelement.AppearanceAPIAdditionsPreview::class,
)
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

    @Test
    fun `initialize resolves the shipping address element when it is configured`() = runScenario(
        configuration = CheckoutController.Configuration()
            .shippingAddressElement(ShippingAddressElement.Configuration())
            .build(),
    ) {
        initializer.initialize()

        shippingAddressElementProvider.getCalls.awaitItem()
        confirmationHandler.registerTurbine.awaitItem()
    }

    @Test
    fun `initialize does not resolve the shipping address element when it is not configured`() = runScenario(
        configuration = CheckoutController.Configuration().build(),
    ) {
        initializer.initialize()

        shippingAddressElementProvider.getCalls.expectNoEvents()
        confirmationHandler.registerTurbine.awaitItem()
    }

    @Test
    fun `initialize applies the restored embedded appearance`() = runTest {
        val appearance = PaymentSheet.Appearance.Builder()
            .colorsLight(PaymentSheet.Colors.Builder.light().primary(0xFF123456.toInt()).build())
            .build()
        val restoredState = CheckoutControllerStateFactory.create(
            embeddedConfiguration = com.stripe.android.paymentelement.EmbeddedPaymentElement.Configuration
                .Builder("Example, Inc.")
                .appearance(appearance)
                .build(),
        )
        val previousTheme = StripeThemeSnapshot()
        try {
            val initializer = CheckoutPresenterInitializer(
                confirmationHandler = FakeConfirmationHandler(),
                activityResultCaller = mock(),
                lifecycleOwner = TestLifecycleOwner(),
                sheetLauncher = FakeEmbeddedSheetLauncher(),
                shippingAddressElementProvider = { error("Shipping Address Element is not configured") },
                sheetStateHolder = SheetStateHolder(SavedStateHandle()),
                stateHolder = CheckoutControllerStateFactory.createStateHolder(
                    SavedStateHandle(mapOf(CheckoutControllerStateHolder.STATE_KEY to restoredState))
                ),
            )

            initializer.initialize()

            assertThat(StripeTheme.colorsLightMutable.materialColors.primary.toArgb())
                .isEqualTo(0xFF123456.toInt())
        } finally {
            previousTheme.restore()
        }
    }

    private fun runScenario(
        lifecycleOwner: TestLifecycleOwner = TestLifecycleOwner(),
        configuration: CheckoutController.Configuration.State? = null,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val confirmationHandler = FakeConfirmationHandler()
        val activityResultCaller = mock<ActivityResultCaller>()
        val sheetLauncher = FakeEmbeddedSheetLauncher()
        val sheetStateHolder = SheetStateHolder(SavedStateHandle())
        val shippingAddressElementProvider = FakeLazy(mock<ShippingAddressElement>())
        val stateHolder = CheckoutControllerStateFactory.createStateHolder(SavedStateHandle())
        if (configuration != null) {
            stateHolder.state = CheckoutControllerStateFactory.create(configuration = configuration)
        }
        val initializer = CheckoutPresenterInitializer(
            confirmationHandler = confirmationHandler,
            activityResultCaller = activityResultCaller,
            lifecycleOwner = lifecycleOwner,
            sheetLauncher = sheetLauncher,
            shippingAddressElementProvider = shippingAddressElementProvider,
            sheetStateHolder = sheetStateHolder,
            stateHolder = stateHolder,
        )

        Scenario(
            initializer = initializer,
            confirmationHandler = confirmationHandler,
            activityResultCaller = activityResultCaller,
            lifecycleOwner = lifecycleOwner,
            sheetLauncher = sheetLauncher,
            sheetStateHolder = sheetStateHolder,
            shippingAddressElementProvider = shippingAddressElementProvider,
        ).block()

        confirmationHandler.validate()
        shippingAddressElementProvider.ensureAllEventsConsumed()
    }

    private class FakeLazy<T>(private val value: T) : Lazy<T> {
        val getCalls = Turbine<Unit>()

        override fun get(): T {
            getCalls.add(Unit)
            return value
        }

        fun ensureAllEventsConsumed() {
            getCalls.ensureAllEventsConsumed()
        }
    }

    private class Scenario(
        val initializer: CheckoutPresenterInitializer,
        val confirmationHandler: FakeConfirmationHandler,
        val activityResultCaller: ActivityResultCaller,
        val lifecycleOwner: TestLifecycleOwner,
        val sheetLauncher: EmbeddedSheetLauncher,
        val sheetStateHolder: SheetStateHolder,
        val shippingAddressElementProvider: FakeLazy<ShippingAddressElement>,
    )
}
