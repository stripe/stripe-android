package com.stripe.android.uicore.utils

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test

class StateFlowsTest {
    @Test
    fun `'flatMapLatestAsStateFlow' should only emit latest value of initially received 'StateFlow'`() = runTest {
        val nestedFlow = MutableStateFlow(0)

        nestedFlow.value = 1

        val state = MutableStateFlow(nestedFlow)

        val flattened = state.flatMapLatestAsStateFlow { it }

        flattened.test {
            assertThat(awaitItem()).isEqualTo(1)
        }
    }

    @Test
    fun `'flatMapLatestAsStateFlow' should emit produced values of received 'StateFlow'`() = runTest {
        val nestedFlow = MutableStateFlow(0)
        val state = MutableStateFlow(nestedFlow)

        val flattened = state.flatMapLatestAsStateFlow { it }

        flattened.test {
            assertThat(awaitItem()).isEqualTo(0)

            nestedFlow.value = 1

            assertThat(awaitItem()).isEqualTo(1)

            nestedFlow.value = 2

            assertThat(awaitItem()).isEqualTo(2)
        }
    }

    @Test
    fun `'flatMapLatestAsStateFlow' should only emit latest value of next received 'StateFlow'`() = runTest {
        val initialNestedFlow = MutableStateFlow(0)
        val state = MutableStateFlow(initialNestedFlow)

        val flattened = state.flatMapLatestAsStateFlow { it }

        flattened.test {
            assertThat(awaitItem()).isEqualTo(0)

            initialNestedFlow.value = 1

            assertThat(awaitItem()).isEqualTo(1)

            val nextNestedFlow = MutableStateFlow(2)

            nextNestedFlow.value = 3
            nextNestedFlow.value = 4

            state.value = nextNestedFlow

            assertThat(awaitItem()).isEqualTo(4)
        }
    }

    @Test
    fun `'flatMapLatestAsStateFlow' should emit any values from previous 'StateFlow'`() = runTest {
        val initialNestedFlow = MutableStateFlow(0)
        val state = MutableStateFlow(initialNestedFlow)

        val flattened = state.flatMapLatestAsStateFlow { it }

        flattened.test {
            assertThat(awaitItem()).isEqualTo(0)

            val nextNestedFlow = MutableStateFlow(1)

            state.value = nextNestedFlow

            assertThat(awaitItem()).isEqualTo(1)

            initialNestedFlow.value = 2

            assertThat(flattened.value).isEqualTo(1)
        }
    }
}
