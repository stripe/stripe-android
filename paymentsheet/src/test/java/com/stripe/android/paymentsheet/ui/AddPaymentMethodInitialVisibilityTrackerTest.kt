package com.stripe.android.paymentsheet.ui

import com.stripe.android.paymentsheet.ui.AddPaymentMethodInitialVisibilityTrackerDataFixtures.FOUR_ITEMS_ONE_95_PERCENT_VISIBLE
import com.stripe.android.paymentsheet.ui.AddPaymentMethodInitialVisibilityTrackerDataFixtures.FOUR_ITEMS_ONE_95_PERCENT_VISIBLE_EXPECTED_VISIBLE
import com.stripe.android.paymentsheet.ui.AddPaymentMethodInitialVisibilityTrackerDataFixtures.FOUR_ITEMS_ONE_BELOW_THRESHOLD
import com.stripe.android.paymentsheet.ui.AddPaymentMethodInitialVisibilityTrackerDataFixtures.FOUR_ITEMS_ONE_BELOW_THRESHOLD_EXPECTED_HIDDEN
import com.stripe.android.paymentsheet.ui.AddPaymentMethodInitialVisibilityTrackerDataFixtures.FOUR_ITEMS_ONE_BELOW_THRESHOLD_EXPECTED_VISIBLE
import com.stripe.android.paymentsheet.ui.AddPaymentMethodInitialVisibilityTrackerDataFixtures.MANY_ITEMS_ONE_PARTIALLY_VISIBLE
import com.stripe.android.paymentsheet.ui.AddPaymentMethodInitialVisibilityTrackerDataFixtures.MANY_ITEMS_ONE_PARTIALLY_VISIBLE_EXPECTED_HIDDEN
import com.stripe.android.paymentsheet.ui.AddPaymentMethodInitialVisibilityTrackerDataFixtures.MANY_ITEMS_ONE_PARTIALLY_VISIBLE_EXPECTED_VISIBLE
import com.stripe.android.paymentsheet.ui.AddPaymentMethodInitialVisibilityTrackerDataFixtures.NO_VISIBLE_ITEMS
import com.stripe.android.paymentsheet.ui.AddPaymentMethodInitialVisibilityTrackerDataFixtures.ONE_ITEM
import com.stripe.android.paymentsheet.ui.AddPaymentMethodInitialVisibilityTrackerDataFixtures.ONE_ITEM_EXPECTED_VISIBLE
import com.stripe.android.paymentsheet.ui.AddPaymentMethodInitialVisibilityTrackerDataFixtures.THREE_ITEMS
import com.stripe.android.paymentsheet.ui.AddPaymentMethodInitialVisibilityTrackerDataFixtures.THREE_ITEMS_EXPECTED_VISIBLE
import com.stripe.android.paymentsheet.ui.AddPaymentMethodInitialVisibilityTrackerDataFixtures.TWO_ITEMS
import com.stripe.android.paymentsheet.ui.AddPaymentMethodInitialVisibilityTrackerDataFixtures.TWO_ITEMS_EXPECTED_VISIBLE
import kotlinx.coroutines.test.runTest
import org.mockito.Mockito.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import kotlin.test.Test

class AddPaymentMethodInitialVisibilityTrackerTest {

    private val callback: (List<String>, List<String>) -> Unit = mock()

    @Test
    fun expect_no_visible_item_reported() = runTest {
        reportInitialPaymentMethodVisibilitySnapshot(
            data = NO_VISIBLE_ITEMS,
            expectedVisible = emptyList(),
            expectedHidden = emptyList(),
        )
    }

    @Test
    fun expect_one_visible_item_reported() = runTest {
        reportInitialPaymentMethodVisibilitySnapshot(
            data = ONE_ITEM,
            expectedVisible = ONE_ITEM_EXPECTED_VISIBLE,
            expectedHidden = emptyList(),
        )
    }

    @Test
    fun expect_two_visible_items_reported() = runTest {
        reportInitialPaymentMethodVisibilitySnapshot(
            data = TWO_ITEMS,
            expectedVisible = TWO_ITEMS_EXPECTED_VISIBLE,
            expectedHidden = emptyList(),
        )
    }

    @Test
    fun expect_three_visible_items_reported() = runTest {
        reportInitialPaymentMethodVisibilitySnapshot(
            data = THREE_ITEMS,
            expectedVisible = THREE_ITEMS_EXPECTED_VISIBLE,
            expectedHidden = emptyList(),
        )
    }

    @Test
    fun expect_four_visible_items_reported() = runTest {
        reportInitialPaymentMethodVisibilitySnapshot(
            data = FOUR_ITEMS_ONE_95_PERCENT_VISIBLE,
            expectedVisible = FOUR_ITEMS_ONE_95_PERCENT_VISIBLE_EXPECTED_VISIBLE,
            expectedHidden = emptyList(),
        )
    }

    @Test
    fun expect_three_visible_items_reported_one_hidden() = runTest {
        reportInitialPaymentMethodVisibilitySnapshot(
            data = FOUR_ITEMS_ONE_BELOW_THRESHOLD,
            expectedVisible = FOUR_ITEMS_ONE_BELOW_THRESHOLD_EXPECTED_VISIBLE,
            expectedHidden = FOUR_ITEMS_ONE_BELOW_THRESHOLD_EXPECTED_HIDDEN,
        )
    }

    @Test
    fun expect_three_visible_items_reported_many_hidden() = runTest {
        reportInitialPaymentMethodVisibilitySnapshot(
            data = MANY_ITEMS_ONE_PARTIALLY_VISIBLE,
            expectedVisible = MANY_ITEMS_ONE_PARTIALLY_VISIBLE_EXPECTED_VISIBLE,
            expectedHidden = MANY_ITEMS_ONE_PARTIALLY_VISIBLE_EXPECTED_HIDDEN,
        )
    }

    private fun reportInitialPaymentMethodVisibilitySnapshot(
        data: AddPaymentMethodInitialVisibilityTrackerData,
        expectedVisible: List<String>,
        expectedHidden: List<String>
    ) {
        AddPaymentMethodInitialVisibilityTracker.reportInitialPaymentMethodVisibilitySnapshot(
            data = data,
            callback = callback,
        )

        verify(callback, times(1))
            .invoke(expectedVisible, expectedHidden)
    }
}
