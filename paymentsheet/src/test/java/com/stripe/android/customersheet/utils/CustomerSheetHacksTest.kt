package com.stripe.android.customersheet.utils

import androidx.lifecycle.testing.TestLifecycleOwner
import com.google.common.truth.Truth.assertThat
import com.stripe.android.customersheet.ExperimentalCustomerSheetApi
import com.stripe.android.customersheet.FakeCustomerAdapter
import com.stripe.android.customersheet.util.CustomerSheetHacks
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCustomerSheetApi::class)
class CustomerSheetHacksTest {
    @After
    fun teardown() {
        CustomerSheetHacks.clear()
    }

    @Test
    fun `on initialize, should initialize all data sources as expected`() = runTest {
        CustomerSheetHacks.initialize(TestLifecycleOwner(), FakeCustomerAdapter())

        assertThat(CustomerSheetHacks.intentDataSource.awaitWithTimeout()).isNotNull()
        assertThat(CustomerSheetHacks.paymentMethodDataSource.awaitWithTimeout()).isNotNull()
        assertThat(CustomerSheetHacks.savedSelectionDataSource.awaitWithTimeout()).isNotNull()
    }

    @Test
    fun `on clear, should clear all data sources as expected`() = runTest {
        CustomerSheetHacks.initialize(TestLifecycleOwner(), FakeCustomerAdapter())
        CustomerSheetHacks.clear()

        assertThat(CustomerSheetHacks.intentDataSource.awaitWithTimeout()).isNull()
        assertThat(CustomerSheetHacks.paymentMethodDataSource.awaitWithTimeout()).isNull()
        assertThat(CustomerSheetHacks.savedSelectionDataSource.awaitWithTimeout()).isNull()
    }

    private suspend fun <T> Deferred<T>.awaitWithTimeout(): T? {
        return withTimeoutOrNull(1.seconds) { await() }
    }
}
