package com.stripe.android.uicore.utils

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test

class StateFlowsTest {
    @Test
    fun `'combineAsStateFlow' with seven flows emits updates from the seventh flow`() = runTest {
        val flow1 = MutableStateFlow(1)
        val flow2 = MutableStateFlow(2)
        val flow3 = MutableStateFlow(3)
        val flow4 = MutableStateFlow(4)
        val flow5 = MutableStateFlow(5)
        val flow6 = MutableStateFlow(6)
        val flow7 = MutableStateFlow(7)

        val combined = combineAsStateFlow(flow1, flow2, flow3, flow4, flow5, flow6, flow7) {
                value1, value2, value3, value4, value5, value6, value7 ->
            value1 + value2 + value3 + value4 + value5 + value6 + value7
        }

        combined.test {
            assertThat(awaitItem()).isEqualTo(28)

            flow7.value = 8

            assertThat(awaitItem()).isEqualTo(29)
        }
    }

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
