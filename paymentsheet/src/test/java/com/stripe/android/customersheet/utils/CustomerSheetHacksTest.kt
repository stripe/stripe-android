package com.stripe.android.customersheet.utils

import android.app.Application
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.coroutines.Single
import com.stripe.android.customersheet.CustomerSheetIntegration
import com.stripe.android.customersheet.FakeCustomerAdapter
import com.stripe.android.customersheet.data.CustomerAdapterDataSource
import com.stripe.android.customersheet.data.CustomerSessionInitializationDataSource
import com.stripe.android.customersheet.data.CustomerSessionIntentDataSource
import com.stripe.android.customersheet.data.CustomerSessionPaymentMethodDataSource
import com.stripe.android.customersheet.data.CustomerSessionSavedSelectionDataSource
import com.stripe.android.customersheet.util.CustomerSheetHacks
import com.stripe.android.isInstanceOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.runner.RunWith
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.seconds

@RunWith(AndroidJUnit4::class)
class CustomerSheetHacksTest {
    private val application = ApplicationProvider.getApplicationContext<Application>()

    @After
    fun teardown() {
        CustomerSheetHacks.clear()
    }

    @Test
    fun `on initialize with adapter, should initialize all data sources as expected`() = runTest {
        CustomerSheetHacks.initialize(
            application = application,
            lifecycleOwner = TestLifecycleOwner(),
            integration = CustomerSheetIntegration.Adapter(FakeCustomerAdapter())
        )

        assertThat(CustomerSheetHacks.initializationDataSource.awaitWithTimeout())
            .isInstanceOf<CustomerAdapterDataSource>()
        assertThat(CustomerSheetHacks.intentDataSource.awaitWithTimeout())
            .isInstanceOf<CustomerAdapterDataSource>()
        assertThat(CustomerSheetHacks.paymentMethodDataSource.awaitWithTimeout())
            .isInstanceOf<CustomerAdapterDataSource>()
        assertThat(CustomerSheetHacks.savedSelectionDataSource.awaitWithTimeout())
            .isInstanceOf<CustomerAdapterDataSource>()
    }

    @Test
    fun `on initialize with customer session, should initialize all data sources as expected`() = runTest {
        CustomerSheetHacks.initialize(
            application = application,
            lifecycleOwner = TestLifecycleOwner(),
            integration = CustomerSheetIntegration.CustomerSession(FakeCustomerSessionProvider())
        )

        assertThat(CustomerSheetHacks.initializationDataSource.awaitWithTimeout())
            .isInstanceOf<CustomerSessionInitializationDataSource>()
        assertThat(CustomerSheetHacks.intentDataSource.awaitWithTimeout())
            .isInstanceOf<CustomerSessionIntentDataSource>()
        assertThat(CustomerSheetHacks.paymentMethodDataSource.awaitWithTimeout())
            .isInstanceOf<CustomerSessionPaymentMethodDataSource>()
        assertThat(CustomerSheetHacks.savedSelectionDataSource.awaitWithTimeout())
            .isInstanceOf<CustomerSessionSavedSelectionDataSource>()
    }

    @Test
    fun `on clear, should clear all data sources as expected`() = runTest {
        CustomerSheetHacks.initialize(
            application = application,
            lifecycleOwner = TestLifecycleOwner(),
            integration = CustomerSheetIntegration.Adapter(FakeCustomerAdapter())
        )
        CustomerSheetHacks.clear()

        assertThat(CustomerSheetHacks.initializationDataSource.awaitWithTimeout()).isNull()
        assertThat(CustomerSheetHacks.intentDataSource.awaitWithTimeout()).isNull()
        assertThat(CustomerSheetHacks.paymentMethodDataSource.awaitWithTimeout()).isNull()
        assertThat(CustomerSheetHacks.savedSelectionDataSource.awaitWithTimeout()).isNull()
    }

    @Test
    fun `initializing a second adapter session throws without replacing the active data sources`() = runScenario {
        val error = assertFailsWith<IllegalStateException> {
            CustomerSheetHacks.initialize(
                application = application,
                lifecycleOwner = TestLifecycleOwner(),
                integration = CustomerSheetIntegration.Adapter(FakeCustomerAdapter()),
            )
        }

        assertThat(error.message).isEqualTo("Cannot have more than one active CustomerSheet instance!")
    }

    @Test
    fun `initializing a second customer session throws`() = runScenario(
        integration = CustomerSheetIntegration.CustomerSession(FakeCustomerSessionProvider()),
    ) {
        val error = assertFailsWith<IllegalStateException> {
            CustomerSheetHacks.initialize(
                application = application,
                lifecycleOwner = TestLifecycleOwner(),
                integration = CustomerSheetIntegration.CustomerSession(FakeCustomerSessionProvider()),
            )
        }

        assertThat(error.message).isEqualTo("Cannot have more than one active CustomerSheet instance!")
    }

    @Test
    fun `destroying the lifecycle owner allows a new session to initialize`() = runScenario {
        assertThat(CustomerSheetHacks.initializationDataSource.await()).isInstanceOf<CustomerAdapterDataSource>()
        assertThat(CustomerSheetHacks.intentDataSource.await()).isInstanceOf<CustomerAdapterDataSource>()
        assertThat(CustomerSheetHacks.paymentMethodDataSource.await()).isInstanceOf<CustomerAdapterDataSource>()
        assertThat(CustomerSheetHacks.savedSelectionDataSource.await()).isInstanceOf<CustomerAdapterDataSource>()

        lifecycleOwner.currentState = Lifecycle.State.DESTROYED

        assertThat(CustomerSheetHacks.initializationDataSource.awaitWithTimeout()).isNull()
        assertThat(CustomerSheetHacks.intentDataSource.awaitWithTimeout()).isNull()
        assertThat(CustomerSheetHacks.paymentMethodDataSource.awaitWithTimeout()).isNull()
        assertThat(CustomerSheetHacks.savedSelectionDataSource.awaitWithTimeout()).isNull()

        CustomerSheetHacks.initialize(
            application = application,
            lifecycleOwner = TestLifecycleOwner(),
            integration = CustomerSheetIntegration.CustomerSession(FakeCustomerSessionProvider()),
        )

        assertThat(CustomerSheetHacks.initializationDataSource.await())
            .isInstanceOf<CustomerSessionInitializationDataSource>()
        assertThat(CustomerSheetHacks.intentDataSource.await()).isInstanceOf<CustomerAdapterDataSource>()
        assertThat(CustomerSheetHacks.paymentMethodDataSource.await()).isInstanceOf<CustomerAdapterDataSource>()
        assertThat(CustomerSheetHacks.savedSelectionDataSource.await()).isInstanceOf<CustomerAdapterDataSource>()
    }

    @Test
    fun `clearing the active session allows a new session to initialize`() = runScenario {
        CustomerSheetHacks.clear()

        CustomerSheetHacks.initialize(
            application = application,
            lifecycleOwner = lifecycleOwner,
            integration = CustomerSheetIntegration.CustomerSession(FakeCustomerSessionProvider()),
        )

        assertThat(CustomerSheetHacks.initializationDataSource.await())
            .isInstanceOf<CustomerSessionInitializationDataSource>()
    }

    private fun runScenario(
        integration: CustomerSheetIntegration = CustomerSheetIntegration.Adapter(FakeCustomerAdapter()),
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val lifecycleOwner = TestLifecycleOwner(coroutineDispatcher = Dispatchers.Unconfined)
        CustomerSheetHacks.initialize(
            application = application,
            lifecycleOwner = lifecycleOwner,
            integration = integration,
        )

        Scenario(lifecycleOwner).block()
    }

    private class Scenario(
        val lifecycleOwner: TestLifecycleOwner,
    )

    private suspend fun <T> Single<T>.awaitWithTimeout(): T? {
        return withTimeoutOrNull(1.seconds) { await() }
    }
}
