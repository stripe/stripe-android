package com.stripe.android.paymentsheet.verticalmode

import com.stripe.android.paymentsheet.verticalmode.PaymentMethodLayoutInitialVisibilityTracker.Companion.EMBEDDED_PAYMENT_METHOD_LAYOUT_NAME
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class PaymentMethodLayoutInitialVisibilityTrackerTest {

    private val callback: () -> Unit = mock()

    @Test
    fun `updateVisibility - ignores items not in expected list`() = runTest {
        val tracker = getTracker()

        val coordinates = FakeLayoutCoordinatesFixtures.FULLY_VISIBLE_COORDINATES

        tracker.updateVisibility("unknown_method", coordinates)
        verifyNoCallback(callback)
    }

    @Test
    fun `visible item invokes callback`() = runTest {
        val tracker = getTracker()

        val coordinates = FakeLayoutCoordinatesFixtures.FULLY_VISIBLE_COORDINATES

        tracker.updateVisibility(EMBEDDED_PAYMENT_METHOD_LAYOUT_NAME, coordinates)
        verify(callback).invoke()
    }

    @Test
    fun `hidden item does not invoke callback`() = runTest {
        val tracker = getTracker()

        val coordinates = FakeLayoutCoordinatesFixtures.FULLY_HIDDEN_COORDINATES

        tracker.updateVisibility(EMBEDDED_PAYMENT_METHOD_LAYOUT_NAME, coordinates)
        verifyNoCallback(callback)
    }

    @Test
    fun `visibility calculation - partially visible above threshold invokes callback`() = runTest {
        val tracker = getTracker()

        tracker.updateVisibility(
            itemCode = EMBEDDED_PAYMENT_METHOD_LAYOUT_NAME,
            coordinates = FakeLayoutCoordinatesFixtures.getCoordinatesBasedOnPercentVisible(0.5F)
        )

        verify(callback).invoke()
    }

    @Test
    fun `visibility calculation - partially visible below threshold does not invoke callback`() = runTest {
        val tracker = getTracker()

        tracker.updateVisibility(
            itemCode = EMBEDDED_PAYMENT_METHOD_LAYOUT_NAME,
            coordinates = FakeLayoutCoordinatesFixtures.getCoordinatesBasedOnPercentVisible(0.1F)
        )

        // Should not dispatch because item doesn't meet visibility threshold
        verifyNoCallback(callback)
    }

    @Test
    fun `visibility calculation - simulate scrolling invokes callback`() = runTest {
        val tracker = getTracker()

        tracker.updateVisibility(
            EMBEDDED_PAYMENT_METHOD_LAYOUT_NAME,
            FakeLayoutCoordinatesFixtures.getCoordinatesBasedOnPercentVisible(0F)
        )

        tracker.updateVisibility(
            EMBEDDED_PAYMENT_METHOD_LAYOUT_NAME,
            FakeLayoutCoordinatesFixtures.getCoordinatesBasedOnPercentVisible(0.06F)
        )

        tracker.updateVisibility(
            EMBEDDED_PAYMENT_METHOD_LAYOUT_NAME,
            FakeLayoutCoordinatesFixtures.getCoordinatesBasedOnPercentVisible(0.12F)
        )

        tracker.updateVisibility(
            EMBEDDED_PAYMENT_METHOD_LAYOUT_NAME,
            FakeLayoutCoordinatesFixtures.getCoordinatesBasedOnPercentVisible(0.18F)
        )

        tracker.updateVisibility(
            EMBEDDED_PAYMENT_METHOD_LAYOUT_NAME,
            FakeLayoutCoordinatesFixtures.getCoordinatesBasedOnPercentVisible(0.24F)
        )

        // Should not dispatch because item doesn't meet visibility threshold
        verifyNoCallback(callback)

        tracker.updateVisibility(
            EMBEDDED_PAYMENT_METHOD_LAYOUT_NAME,
            FakeLayoutCoordinatesFixtures.getCoordinatesBasedOnPercentVisible(0.3F)
        )

        verify(callback).invoke()
    }

    private fun getTracker(): PaymentMethodLayoutInitialVisibilityTracker {
        return PaymentMethodLayoutInitialVisibilityTracker(
            callback = callback
        )
    }

    private fun verifyNoCallback(callback: () -> Unit) {
        verify(callback, never()).invoke()
    }
}
