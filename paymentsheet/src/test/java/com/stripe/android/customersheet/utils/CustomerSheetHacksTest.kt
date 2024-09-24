package com.stripe.android.customersheet.utils

import android.app.Application
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.stripe.android.customersheet.ExperimentalCustomerSheetApi
import com.stripe.android.customersheet.FakeCustomerAdapter
import com.stripe.android.customersheet.util.CustomerSheetHacks
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.runner.RunWith
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCustomerSheetApi::class)
@RunWith(AndroidJUnit4::class)
class CustomerSheetHacksTest {
    private val application = ApplicationProvider.getApplicationContext<Application>()

    @After
    fun teardown() {
        CustomerSheetHacks.clear()
    }

    @Test
    fun `on initialize, should initialize all data sources as expected`() = runTest {
        CustomerSheetHacks.initialize(application, TestLifecycleOwner(), FakeCustomerAdapter())

        assertThat(CustomerSheetHacks.initializationDataSource.awaitWithTimeout()).isNotNull()
        assertThat(CustomerSheetHacks.intentDataSource.awaitWithTimeout()).isNotNull()
        assertThat(CustomerSheetHacks.paymentMethodDataSource.awaitWithTimeout()).isNotNull()
        assertThat(CustomerSheetHacks.savedSelectionDataSource.awaitWithTimeout()).isNotNull()
    }

    @Test
    fun `on clear, should clear all data sources as expected`() = runTest {
        CustomerSheetHacks.initialize(application, TestLifecycleOwner(), FakeCustomerAdapter())
        CustomerSheetHacks.clear()

        assertThat(CustomerSheetHacks.initializationDataSource.awaitWithTimeout()).isNull()
        assertThat(CustomerSheetHacks.intentDataSource.awaitWithTimeout()).isNull()
        assertThat(CustomerSheetHacks.paymentMethodDataSource.awaitWithTimeout()).isNull()
        assertThat(CustomerSheetHacks.savedSelectionDataSource.awaitWithTimeout()).isNull()
    }

    private suspend fun <T> Deferred<T>.awaitWithTimeout(): T? {
        return withTimeoutOrNull(1.seconds) { await() }
    }
}
