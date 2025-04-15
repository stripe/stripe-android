package com.stripe.android.paymentsheet.analytics

import androidx.lifecycle.SavedStateHandle
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runTest
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.eq
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import kotlin.test.Test

class PaymentSheetAnalyticsListenerTest {
    @Test
    fun `cannotProperlyReturnFromLinkAndOtherLPMs should report event at maximum once`() = runScenario {
        analyticsListener.cannotProperlyReturnFromLinkAndOtherLPMs()
        analyticsListener.cannotProperlyReturnFromLinkAndOtherLPMs()
        analyticsListener.cannotProperlyReturnFromLinkAndOtherLPMs()
        analyticsListener.cannotProperlyReturnFromLinkAndOtherLPMs()

        verify(eventReporter, times(1)).onCannotProperlyReturnFromLinkAndOtherLPMs()
    }

    @Test
    fun `on field interaction, should report event`() = runScenario {
        analyticsListener.reportFieldInteraction("card")

        verify(eventReporter).onPaymentMethodFormInteraction("card")
    }

    @Test
    fun `on multiple field interactions with same payment form, should report event only once`() = runScenario {
        analyticsListener.reportFieldInteraction("card")
        analyticsListener.reportFieldInteraction("card")
        analyticsListener.reportFieldInteraction("card")

        verify(eventReporter, times(1)).onPaymentMethodFormInteraction("card")
    }

    @Test
    fun `reportPaymentSheetHidden reports for UpdatePaymentMethod`() = runScenario {
        analyticsListener.reportPaymentSheetHidden(mock<PaymentSheetScreen.UpdatePaymentMethod>())

        verify(eventReporter).onHideEditablePaymentOption()
    }

    @Test
    fun `reportPaymentSheetHidden does not report for non edit screens`() = runScenario {
        analyticsListener.reportPaymentSheetHidden(PaymentSheetScreen.Loading)

        verifyNoInteractions(eventReporter)
    }

    @Test
    fun `onShowManageSavedPaymentMethods is called when screen updates to ManageSavedPaymentMethods`() = runScenario {
        verifyNoInteractions(eventReporter)
        currentScreen.value = mock<PaymentSheetScreen.ManageSavedPaymentMethods>()
        testScheduler.advanceUntilIdle()
        verify(eventReporter).onShowManageSavedPaymentMethods()
    }

    @Test
    fun `onShowEditablePaymentOption is called when screen updates to UpdatePaymentMethod`() = runScenario {
        verifyNoInteractions(eventReporter)
        currentScreen.value = mock<PaymentSheetScreen.UpdatePaymentMethod>()
        testScheduler.advanceUntilIdle()
        verify(eventReporter).onShowEditablePaymentOption()
    }

    @Test
    fun `onShowExistingPaymentOptions is called when screen updates to SelectSavedPaymentMethods`() = runScenario {
        currentScreen.value = mock<PaymentSheetScreen.SelectSavedPaymentMethods>()
        testScheduler.advanceUntilIdle()
        verify(eventReporter).onShowExistingPaymentOptions()
    }

    @Test
    fun `form events are emitted only once when screen changes to one with a form`() = runScenario {
        val screenTypes = listOf(
            PaymentSheetScreen.AddFirstPaymentMethod::class,
            PaymentSheetScreen.AddAnotherPaymentMethod::class,
            PaymentSheetScreen.VerticalMode::class,
        )
        screenTypes.forEach { screenType ->
            currentScreen.value = mock(screenType.java)
            testScheduler.advanceUntilIdle()
        }

        // Only emit the first time the screen is shown.
        verify(eventReporter).onShowNewPaymentOptions()
        // Only once since it didn't change.
        verify(eventReporter).onPaymentMethodFormShown(eq("card"))
    }

    @Test
    fun `debounced analytics are re-emitted after navigating to SelectSavedPaymentMethods`() = runScenario {
        currentScreen.value = mock<PaymentSheetScreen.AddAnotherPaymentMethod>()
        testScheduler.advanceUntilIdle()
        verify(eventReporter).onShowNewPaymentOptions()
        verify(eventReporter).onPaymentMethodFormShown(eq("card"))
        analyticsListener.reportFieldInteraction("card")
        verify(eventReporter).onPaymentMethodFormInteraction(eq("card"))
        verifyNoMoreInteractions(eventReporter)
        clearInvocations(eventReporter)

        currentScreen.value = mock<PaymentSheetScreen.SelectSavedPaymentMethods>()
        testScheduler.advanceUntilIdle()
        verifyNoMoreInteractions(eventReporter)
        clearInvocations(eventReporter)

        currentScreen.value = mock<PaymentSheetScreen.AddAnotherPaymentMethod>()
        testScheduler.advanceUntilIdle()
        verify(eventReporter).onPaymentMethodFormShown(eq("card"))
        analyticsListener.reportFieldInteraction("card")
        verify(eventReporter).onPaymentMethodFormInteraction(eq("card"))
    }

    private fun runScenario(block: Scenario.() -> Unit) {
        runTest {
            val eventReporter = mock<EventReporter>()
            val currentScreen = MutableStateFlow<PaymentSheetScreen>(PaymentSheetScreen.Loading)
            val currentPaymentMethodTypeProvider = { "card" }
            val coroutineScope = CoroutineScope(coroutineContext + SupervisorJob())
            val analyticsListener = PaymentSheetAnalyticsListener(
                savedStateHandle = SavedStateHandle(),
                eventReporter = eventReporter,
                currentScreen = currentScreen,
                coroutineScope = coroutineScope,
                currentPaymentMethodTypeProvider = currentPaymentMethodTypeProvider
            )
            Scenario(
                analyticsListener = analyticsListener,
                eventReporter = eventReporter,
                currentScreen = currentScreen,
                testScheduler = testScheduler,
            ).apply(block)
            testScheduler.advanceUntilIdle()
            coroutineScope.cancel()
            verifyNoMoreInteractions(eventReporter)
        }
    }

    private class Scenario(
        val analyticsListener: PaymentSheetAnalyticsListener,
        val eventReporter: EventReporter,
        val currentScreen: MutableStateFlow<PaymentSheetScreen>,
        val testScheduler: TestCoroutineScheduler,
    )
}
