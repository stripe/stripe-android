package com.stripe.android.paymentsheet.verticalmode

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntSize
import com.stripe.android.testing.CoroutineTestRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class PaymentMethodInitialVisibilityTrackerTest {

    private val TIME_ADVANCE_1S = 1000L

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    private val callback: (Map<String, Boolean>) -> Unit = mock()

    @Test
    fun `updateVisibility - ignores items not in expected list`() = runTest {
        val tracker = PaymentMethodInitialVisibilityTracker(
            expectedItems = listOf("card", "klarna"),
            renderedLpmCallback = callback
        )
        val coordinates = FakeLayoutCoordinates.create(
            size = defaultCoordinateSize,
            bounds = defaultBounds,
        )

        tracker.updateVisibility("unknown_method", coordinates)

        // Should not affect tracking since item is not expected
        advanceTimeBy(TIME_ADVANCE_1S)
        verify(callback, never()).invoke(any())
    }

    @Test
    fun `updateVisibility - does nothing for empty expected items`() = runTest {
        val tracker = PaymentMethodInitialVisibilityTracker(
            expectedItems = emptyList(),
            renderedLpmCallback = callback
        )
        val coordinates = FakeLayoutCoordinates.create(
            size = defaultCoordinateSize,
            bounds = defaultBounds,
        )

        tracker.updateVisibility("card", coordinates)

        advanceTimeBy(TIME_ADVANCE_1S)
        verify(callback, never()).invoke(any())
    }

    @Test
    fun `visibility calculation - stable fully visible item invokes callback`() = runTest {
        val tracker = PaymentMethodInitialVisibilityTracker(
            expectedItems = listOf("card"),
            renderedLpmCallback = callback
        )

        // Create coordinates where item is fully visible (95%+ threshold)
        val coordinates1 = FakeLayoutCoordinates.create(
            size = defaultCoordinateSize,
            bounds = defaultBounds,
        )

        // Update twice to achieve stability
        tracker.updateVisibility("card", coordinates1)
        tracker.updateVisibility("card", coordinates1)

        advanceTimeBy(TIME_ADVANCE_1S)

        verify(callback).invoke(mapOf("card" to true))
    }

    @Test
    fun `visibility calculation - hidden item does not invoke callback`() = runTest {
        val tracker = PaymentMethodInitialVisibilityTracker(
            expectedItems = listOf("card"),
            renderedLpmCallback = callback
        )

        // Create coordinates where item is completely hidden
        val coordinates1 = FakeLayoutCoordinates.create(
            size = defaultCoordinateSize,
            bounds = Rect(0f, 0f, 0f, 0f) // Hidden
        )

        // Update twice to achieve stability
        tracker.updateVisibility("card", coordinates1)
        tracker.updateVisibility("card", coordinates1)

        // Should not dispatch because no items are visible
        advanceTimeBy(TIME_ADVANCE_1S)
        verify(callback, never()).invoke(any())
    }

    @Test
    fun `visibility calculation - partially visible above threshold invokes callback`() = runTest {
        val tracker = PaymentMethodInitialVisibilityTracker(
            expectedItems = listOf("card"),
            renderedLpmCallback = callback
        )

        // Create coordinates where 98% is visible (above 95% threshold)
        val coordinates1 = FakeLayoutCoordinates.create(
            size = defaultCoordinateSize,
            bounds = Rect(0f, 0f, 100f, 49f) // 98% visible
        )

        // Update twice to achieve stability
        tracker.updateVisibility("card", coordinates1)
        tracker.updateVisibility("card", coordinates1)

        // Should dispatch because item meets visibility threshold
        advanceTimeBy(TIME_ADVANCE_1S)
        verify(callback).invoke(mapOf("card" to true))
    }

    @Test
    fun `visibility calculation - partially visible below threshold does not invoke callback`() = runTest {
        val tracker = PaymentMethodInitialVisibilityTracker(
            expectedItems = listOf("card"),
            renderedLpmCallback = callback
        )

        // Create coordinates where only 50% is visible (below 95% threshold)
        val coordinates1 = FakeLayoutCoordinates.create(
            size = defaultCoordinateSize,
            bounds = Rect(0f, 0f, 100f, 25f) // 50% visible
        )

        // Update twice to achieve stability
        tracker.updateVisibility("card", coordinates1)
        tracker.updateVisibility("card", coordinates1)

        // Should not dispatch because item doesn't meet visibility threshold
        advanceTimeBy(TIME_ADVANCE_1S)
        verify(callback, never()).invoke(any())
    }

    @Test
    fun `coordinate stability - changing coordinates does not invoke callback`() = runTest {
        val tracker = PaymentMethodInitialVisibilityTracker(
            expectedItems = listOf("card"),
            renderedLpmCallback = callback
        )

        val coordinates1 = FakeLayoutCoordinates.create(
            size = defaultCoordinateSize,
            bounds = Rect(0f, 0f, 100f, 50f)
        )
        val coordinates2 = FakeLayoutCoordinates.create(
            size = defaultCoordinateSize,
            bounds = Rect(10f, 10f, 110f, 60f) // Different position
        )

        tracker.updateVisibility("card", coordinates1)
        tracker.updateVisibility("card", coordinates2) // Changes position

        advanceTimeBy(TIME_ADVANCE_1S)
        verify(callback, never()).invoke(any()) // Should not dispatch due to instability
    }

    @Test
    fun `debounce mechanism - waits for stability before dispatching`() = runTest {
        val tracker = PaymentMethodInitialVisibilityTracker(
            expectedItems = listOf("card"),
            renderedLpmCallback = callback
        )

        val coordinates = FakeLayoutCoordinatesFixtures.FULLY_VISIBLE_COORDINATES

        // Update twice to achieve stability
        tracker.updateVisibility("card", coordinates)
        tracker.updateVisibility("card", coordinates)

        // Should not dispatch immediately
        verify(callback, never()).invoke(any())

        // Should dispatch after debounce delay
        advanceTimeBy(TIME_ADVANCE_1S)
        verify(callback).invoke(mapOf("card" to true))
    }

    @Test
    fun `debounce mechanism - subsequent update resets timer`() = runTest {
        val tracker = PaymentMethodInitialVisibilityTracker(
            expectedItems = listOf("card"),
            renderedLpmCallback = callback
        )

        val coordinates = FakeLayoutCoordinatesFixtures.FULLY_VISIBLE_COORDINATES

        // Update twice to achieve stability
        tracker.updateVisibility("card", coordinates)
        tracker.updateVisibility("card", coordinates)

        // Should not dispatch immediately
        verify(callback, never()).invoke(any())

        advanceTimeBy(500)
        verify(callback, never()).invoke(any())

        tracker.updateVisibility("card", coordinates)

        advanceTimeBy(500)
        verify(callback, never()).invoke(any())

        // Should dispatch after debounce delay
        advanceTimeBy(TIME_ADVANCE_1S)
        verify(callback).invoke(mapOf("card" to true))
    }

    @Test
    fun `single dispatch - only dispatches once per tracker instance`() = runTest {
        val tracker = PaymentMethodInitialVisibilityTracker(
            expectedItems = listOf("card"),
            renderedLpmCallback = callback
        )

        val coordinates = FakeLayoutCoordinatesFixtures.FULLY_VISIBLE_COORDINATES

        // Update multiple times to achieve stability
        tracker.updateVisibility("card", coordinates)
        tracker.updateVisibility("card", coordinates)

        advanceTimeBy(TIME_ADVANCE_1S)
        verify(callback).invoke(mapOf("card" to true))

        // Further updates should not trigger additional dispatches
        tracker.updateVisibility("card", coordinates)
        advanceTimeBy(TIME_ADVANCE_1S)
        verify(callback).invoke(any()) // Should still only be called once total
    }

    @Test
    fun `multiple payment methods - waits for all to be stable`() = runTest {
        val tracker = PaymentMethodInitialVisibilityTracker(
            expectedItems = listOf("card", "klarna", "paypal"),
            renderedLpmCallback = callback
        )

        val coordinates = FakeLayoutCoordinatesFixtures.FULLY_VISIBLE_COORDINATES

        // Update only two of three items
        tracker.updateVisibility("card", coordinates)
        tracker.updateVisibility("card", coordinates) // Make stable
        tracker.updateVisibility("klarna", coordinates)
        tracker.updateVisibility("klarna", coordinates) // Make stable

        // Should not dispatch yet (missing paypal)
        advanceTimeBy(TIME_ADVANCE_1S)
        verify(callback, never()).invoke(any())

        // Add the third item
        tracker.updateVisibility("paypal", coordinates)
        tracker.updateVisibility("paypal", coordinates) // Make stable

        // Now should dispatch
        advanceTimeBy(TIME_ADVANCE_1S)
        verify(callback).invoke(
            mapOf(
                "card" to true,
                "klarna" to true,
                "paypal" to true
            )
        )
    }

    @Test
    fun `dispose - cancels pending jobs and cleans up resources`() = runTest {
        val tracker = PaymentMethodInitialVisibilityTracker(
            expectedItems = listOf("card"),
            renderedLpmCallback = callback
        )

        val coordinates = FakeLayoutCoordinatesFixtures.FULLY_VISIBLE_COORDINATES

        // Set up for dispatch but dispose before it happens
        tracker.updateVisibility("card", coordinates)
        tracker.updateVisibility("card", coordinates)

        tracker.dispose()

        // Should not dispatch even after delay
        advanceTimeBy(TIME_ADVANCE_1S)
        verify(callback, never()).invoke(any())
    }

    @Test
    fun `mixed visibility states - dispatches correct visibility map`() = runTest {
        val tracker = PaymentMethodInitialVisibilityTracker(
            expectedItems = listOf("card", "klarna"),
            renderedLpmCallback = callback
        )

        val visibleCoordinates = FakeLayoutCoordinatesFixtures.FULLY_VISIBLE_COORDINATES

        val hiddenCoordinates = FakeLayoutCoordinatesFixtures.FULLY_HIDDEN_COORDINATES

        // Make card visible and klarna hidden, both stable
        tracker.updateVisibility("card", visibleCoordinates)
        tracker.updateVisibility("card", visibleCoordinates)
        tracker.updateVisibility("klarna", hiddenCoordinates)
        tracker.updateVisibility("klarna", hiddenCoordinates)

        advanceTimeBy(TIME_ADVANCE_1S)

        verify(callback).invoke(
            mapOf(
                "card" to true,
                "klarna" to false
            )
        )
    }

    @Test
    fun `mixed visibility states partially hidden - dispatches correct visibility map`() = runTest {
        val tracker = PaymentMethodInitialVisibilityTracker(
            expectedItems = listOf("card", "klarna"),
            renderedLpmCallback = callback
        )

        val visibleCoordinates = FakeLayoutCoordinatesFixtures.FULLY_VISIBLE_COORDINATES

        val hiddenCoordinates = FakeLayoutCoordinatesFixtures.FULLY_HIDDEN_COORDINATES

        // Make card visible and klarna hidden, both stable
        tracker.updateVisibility("card", visibleCoordinates)
        tracker.updateVisibility("card", visibleCoordinates)
        tracker.updateVisibility("klarna", hiddenCoordinates)
        tracker.updateVisibility("klarna", hiddenCoordinates)

        advanceTimeBy(TIME_ADVANCE_1S)

        verify(callback).invoke(
            mapOf(
                "card" to true,
                "klarna" to false
            )
        )
    }

    @Test
    fun `start fully hidden, reveals payment methods, then settles - dispatches correct visibility map`() = runTest {
        val tracker = PaymentMethodInitialVisibilityTracker(
            expectedItems = listOf("card", "klarna", "paypal"),
            renderedLpmCallback = callback
        )

        val fullyHiddenCoordinates = FakeLayoutCoordinatesFixtures.FULLY_HIDDEN_COORDINATES

        tracker.updateVisibility("card", fullyHiddenCoordinates)
        tracker.updateVisibility("klarna", fullyHiddenCoordinates)
        tracker.updateVisibility("paypal", fullyHiddenCoordinates)

        val partiallyHiddenCoordinates = FakeLayoutCoordinatesFixtures.PARTIALLY_HIDDEN_COORDINATES

        tracker.updateVisibility("card", partiallyHiddenCoordinates)
        tracker.updateVisibility("klarna", fullyHiddenCoordinates)
        tracker.updateVisibility("paypal", fullyHiddenCoordinates)

        advanceTimeBy(TIME_ADVANCE_1S)
        verify(callback, never()).invoke(any())

        val fullyVisibleCoordinates = FakeLayoutCoordinatesFixtures.FULLY_VISIBLE_COORDINATES

        tracker.updateVisibility("card", fullyVisibleCoordinates)
        tracker.updateVisibility("klarna", partiallyHiddenCoordinates)
        tracker.updateVisibility("paypal", fullyHiddenCoordinates)

        advanceTimeBy(TIME_ADVANCE_1S)
        verify(callback, never()).invoke(any())

        tracker.updateVisibility("card", fullyVisibleCoordinates)
        tracker.updateVisibility("klarna", partiallyHiddenCoordinates)
        tracker.updateVisibility("paypal", fullyHiddenCoordinates)

        advanceTimeBy(TIME_ADVANCE_1S)

        verify(callback).invoke(
            mapOf(
                "card" to true,
                "klarna" to false,
                "paypal" to false,
            )
        )
    }

    private val defaultCoordinateSize = IntSize(100, 50)
    private val defaultBounds = Rect(0f, 0f, 100f, 50f)
}
