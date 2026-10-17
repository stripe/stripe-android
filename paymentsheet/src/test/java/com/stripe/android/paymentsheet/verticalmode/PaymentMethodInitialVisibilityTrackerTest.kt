package com.stripe.android.paymentsheet.verticalmode

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntSize
import com.stripe.android.testing.CoroutineTestRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class PaymentMethodInitialVisibilityTrackerTest {

    private val DEBOUNCE_DELAY = 50

    private val TIME_ADVANCE_LESSER_THAN_DEBOUNCE_DELAY = DEBOUNCE_DELAY - 10L

    private val TIME_ADVANCE_GREATER_THAN_DEBOUNCE_DELAY = DEBOUNCE_DELAY + 10L

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    private val callback: (List<String>, List<String>) -> Unit = mock()

    @Test
    fun `updateVisibility - ignores items not in expected list`() = runTest {
        val tracker = getTracker(
            expectedItems = listOf("card", "klarna")
        )
        val coordinates = FakeLayoutCoordinates.create(
            size = defaultCoordinateSize,
            bounds = defaultBounds,
        )

        tracker.updateVisibility("unknown_method", coordinates)

        // Should not affect tracking since item is not expected
        advanceTimeBy(TIME_ADVANCE_GREATER_THAN_DEBOUNCE_DELAY)
        verifyNoCallback(callback)
    }

    @Test
    fun `updateVisibility - does nothing for empty expected items`() = runTest {
        val tracker = getTracker(
            expectedItems = emptyList(),
        )
        val coordinates = FakeLayoutCoordinates.create(
            size = defaultCoordinateSize,
            bounds = defaultBounds,
        )

        tracker.updateVisibility("card", coordinates)

        advanceTimeBy(TIME_ADVANCE_GREATER_THAN_DEBOUNCE_DELAY)
        verifyNoCallback(callback)
    }

    @Test
    fun `visibility calculation - stable fully visible item invokes callback`() = runTest {
        val tracker = getTracker(
            expectedItems = listOf("card"),
        )

        // Create coordinates where item is fully visible (95%+ threshold)
        val coordinates1 = FakeLayoutCoordinates.create(
            size = defaultCoordinateSize,
            bounds = defaultBounds,
        )

        tracker.updateVisibility("card", coordinates1)

        advanceTimeBy(TIME_ADVANCE_GREATER_THAN_DEBOUNCE_DELAY)

        verify(callback).invoke(listOf("card"), emptyList())
    }

    @Test
    fun `visibility calculation - hidden item does not invoke callback`() = runTest {
        val tracker = getTracker(
            expectedItems = listOf("card"),
        )

        // Create coordinates where item is completely hidden
        val coordinates1 = FakeLayoutCoordinates.create(
            size = defaultCoordinateSize,
            bounds = Rect(0f, 0f, 0f, 0f) // Hidden
        )

        tracker.updateVisibility("card", coordinates1)

        // Should not dispatch because no items are visible
        advanceTimeBy(TIME_ADVANCE_GREATER_THAN_DEBOUNCE_DELAY)
        verifyNoCallback(callback)
    }

    @Test
    fun `visibility calculation - partially visible above threshold invokes callback`() = runTest {
        val tracker = getTracker(
            expectedItems = listOf("card"),
        )

        // Create coordinates where 98% is visible (above 95% threshold)
        val coordinates1 = FakeLayoutCoordinates.create(
            size = defaultCoordinateSize,
            bounds = Rect(0f, 0f, 100f, 49f) // 98% visible
        )

        tracker.updateVisibility("card", coordinates1)

        // Should dispatch because item meets visibility threshold
        advanceTimeBy(TIME_ADVANCE_GREATER_THAN_DEBOUNCE_DELAY)
        verify(callback).invoke(listOf("card"), emptyList())
    }

    @Test
    fun `visibility calculation - partially visible below threshold does not invoke callback`() = runTest {
        val tracker = getTracker(
            expectedItems = listOf("card"),
        )

        // Create coordinates where only 50% is visible (below 95% threshold)
        val coordinates1 = FakeLayoutCoordinates.create(
            size = defaultCoordinateSize,
            bounds = Rect(0f, 0f, 100f, 25f) // 50% visible
        )

        tracker.updateVisibility("card", coordinates1)

        // Should not dispatch because item doesn't meet visibility threshold
        advanceTimeBy(TIME_ADVANCE_GREATER_THAN_DEBOUNCE_DELAY)
        verifyNoCallback(callback)
    }

    @Test
    fun `coordinate stability - changing coordinates resets debounce timer`() = runTest {
        val tracker = getTracker(
            expectedItems = listOf("card"),
        )

        val coordinates1 = FakeLayoutCoordinates.create(
            size = defaultCoordinateSize,
            bounds = Rect(0f, 0f, 100f, 50f)
        )
        val coordinates2 = FakeLayoutCoordinates.create(
            size = defaultCoordinateSize,
            bounds = Rect(1f, 1f, 101f, 51f) // Different position, still above the visibility threshold
        )

        tracker.updateVisibility("card", coordinates1)
        advanceTimeBy(TIME_ADVANCE_LESSER_THAN_DEBOUNCE_DELAY)
        tracker.updateVisibility("card", coordinates2)

        advanceTimeBy(TIME_ADVANCE_LESSER_THAN_DEBOUNCE_DELAY)
        verifyNoCallback(callback)

        advanceUntilIdle()
        verify(callback).invoke(listOf("card"), emptyList())
    }

    @Test
    fun `debounce mechanism - waits for stability before dispatching`() = runTest {
        val tracker = getTracker(
            expectedItems = listOf("card"),
        )

        val coordinates = FakeLayoutCoordinatesFixtures.FULLY_VISIBLE_COORDINATES

        tracker.updateVisibility("card", coordinates)

        // Should not dispatch immediately
        verifyNoCallback(callback)

        // Should dispatch after debounce delay
        advanceTimeBy(TIME_ADVANCE_GREATER_THAN_DEBOUNCE_DELAY)
        verify(callback).invoke(listOf("card"), emptyList())
    }

    @Test
    fun `debounce mechanism - subsequent update resets timer`() = runTest {
        val tracker = getTracker(
            expectedItems = listOf("card"),
        )

        val coordinates = FakeLayoutCoordinatesFixtures.FULLY_VISIBLE_COORDINATES

        tracker.updateVisibility("card", coordinates)

        // Should not dispatch immediately
        verifyNoCallback(callback)

        advanceTimeBy(TIME_ADVANCE_LESSER_THAN_DEBOUNCE_DELAY)
        verifyNoCallback(callback)

        tracker.updateVisibility("card", coordinates)

        advanceTimeBy(TIME_ADVANCE_LESSER_THAN_DEBOUNCE_DELAY)
        verifyNoCallback(callback)

        // Should dispatch after debounce delay
        advanceTimeBy(TIME_ADVANCE_GREATER_THAN_DEBOUNCE_DELAY)
        verify(callback).invoke(listOf("card"), emptyList())
    }

    @Test
    fun `single dispatch - only dispatches once per tracker instance`() = runTest {
        val tracker = getTracker(
            expectedItems = listOf("card"),
        )

        val coordinates = FakeLayoutCoordinatesFixtures.FULLY_VISIBLE_COORDINATES

        tracker.updateVisibility("card", coordinates)

        advanceTimeBy(TIME_ADVANCE_GREATER_THAN_DEBOUNCE_DELAY)
        verify(callback, times(1)).invoke(listOf("card"), emptyList())

        // Further updates should not trigger additional dispatches
        tracker.updateVisibility("card", coordinates)
        advanceTimeBy(TIME_ADVANCE_GREATER_THAN_DEBOUNCE_DELAY)
    }

    @Test
    fun `multiple payment methods - waits for all to be stable`() = runTest {
        val tracker = getTracker(
            expectedItems = listOf("card", "klarna", "paypal"),
        )

        val coordinates = FakeLayoutCoordinatesFixtures.FULLY_VISIBLE_COORDINATES

        // Update only two of three items
        tracker.updateVisibility("card", coordinates)
        tracker.updateVisibility("klarna", coordinates)

        // Should not dispatch yet (missing paypal)
        advanceTimeBy(TIME_ADVANCE_GREATER_THAN_DEBOUNCE_DELAY)
        verifyNoCallback(callback)

        // Add the third item
        tracker.updateVisibility("paypal", coordinates)

        // Now should dispatch
        advanceTimeBy(TIME_ADVANCE_GREATER_THAN_DEBOUNCE_DELAY)
        verify(callback).invoke(listOf("card", "klarna", "paypal"), emptyList())
    }

    @Test
    fun `reset - cancels pending jobs and clears tracking state`() = runTest {
        val tracker = getTracker(
            expectedItems = listOf("card"),
        )

        val coordinates = FakeLayoutCoordinatesFixtures.FULLY_VISIBLE_COORDINATES

        // Set up for dispatch but dispose before it happens
        tracker.updateVisibility("card", coordinates)

        advanceTimeBy(TIME_ADVANCE_LESSER_THAN_DEBOUNCE_DELAY)
        tracker.reset()

        // Should not dispatch even after delay
        advanceTimeBy(TIME_ADVANCE_GREATER_THAN_DEBOUNCE_DELAY)
        verifyNoCallback(callback)
    }

    @Test
    fun `reset - tracker can dispatch a new event after reset`() = runTest {
        val tracker = getTracker(
            expectedItems = listOf("card"),
        )
        val coordinates = FakeLayoutCoordinatesFixtures.FULLY_VISIBLE_COORDINATES

        tracker.updateVisibility("card", coordinates)
        tracker.reset()

        tracker.updateVisibility("card", coordinates)
        advanceTimeBy(TIME_ADVANCE_GREATER_THAN_DEBOUNCE_DELAY)

        verify(callback).invoke(listOf("card"), emptyList())
    }

    @Test
    fun `mixed visibility states - dispatches correct visibility map`() = runTest {
        val tracker = getTracker(
            expectedItems = listOf("card", "klarna"),
        )

        val visibleCoordinates = FakeLayoutCoordinatesFixtures.FULLY_VISIBLE_COORDINATES

        val hiddenCoordinates = FakeLayoutCoordinatesFixtures.FULLY_HIDDEN_COORDINATES

        tracker.updateVisibility("card", visibleCoordinates)
        tracker.updateVisibility("klarna", hiddenCoordinates)

        advanceTimeBy(TIME_ADVANCE_GREATER_THAN_DEBOUNCE_DELAY)

        verify(callback).invoke(listOf("card"), listOf("klarna"))
    }

    @Test
    fun `mixed visibility states partially hidden - dispatches correct visibility map`() = runTest {
        val tracker = getTracker(
            expectedItems = listOf("card", "klarna"),
        )

        val visibleCoordinates = FakeLayoutCoordinatesFixtures.FULLY_VISIBLE_COORDINATES

        val hiddenCoordinates = FakeLayoutCoordinatesFixtures.FULLY_HIDDEN_COORDINATES

        tracker.updateVisibility("card", visibleCoordinates)
        tracker.updateVisibility("klarna", hiddenCoordinates)

        advanceTimeBy(TIME_ADVANCE_GREATER_THAN_DEBOUNCE_DELAY)

        verify(callback).invoke(listOf("card"), listOf("klarna"))
    }

    @Test
    fun `start fully hidden, reveals payment methods, then settles - dispatches correct visibility map`() = runTest {
        val tracker = getTracker(
            expectedItems = listOf("card", "klarna", "paypal"),
        )

        val fullyHiddenCoordinates = FakeLayoutCoordinatesFixtures.FULLY_HIDDEN_COORDINATES

        tracker.updateVisibility("card", fullyHiddenCoordinates)
        tracker.updateVisibility("klarna", fullyHiddenCoordinates)
        tracker.updateVisibility("paypal", fullyHiddenCoordinates)

        val partiallyHiddenCoordinates = FakeLayoutCoordinatesFixtures.PARTIALLY_HIDDEN_COORDINATES

        tracker.updateVisibility("card", partiallyHiddenCoordinates)
        tracker.updateVisibility("klarna", fullyHiddenCoordinates)
        tracker.updateVisibility("paypal", fullyHiddenCoordinates)

        advanceTimeBy(TIME_ADVANCE_GREATER_THAN_DEBOUNCE_DELAY)
        verifyNoCallback(callback)

        val fullyVisibleCoordinates = FakeLayoutCoordinatesFixtures.FULLY_VISIBLE_COORDINATES

        tracker.updateVisibility("card", fullyVisibleCoordinates)
        tracker.updateVisibility("klarna", partiallyHiddenCoordinates)
        tracker.updateVisibility("paypal", fullyHiddenCoordinates)

        advanceTimeBy(TIME_ADVANCE_GREATER_THAN_DEBOUNCE_DELAY)

        verify(callback).invoke(listOf("card"), listOf("klarna", "paypal"))
    }

    private val defaultCoordinateSize = IntSize(100, 50)
    private val defaultBounds = Rect(0f, 0f, 100f, 50f)

    private fun TestScope.getTracker(expectedItems: List<String>): PaymentMethodInitialVisibilityTracker {
        return PaymentMethodInitialVisibilityTracker(
            expectedItems = expectedItems,
            renderedLpmCallback = callback,
            coroutineScope = this,
        )
    }

    private fun verifyNoCallback(callback: (List<String>, List<String>) -> Unit) {
        verify(callback, never()).invoke(any(), any())
    }
}
