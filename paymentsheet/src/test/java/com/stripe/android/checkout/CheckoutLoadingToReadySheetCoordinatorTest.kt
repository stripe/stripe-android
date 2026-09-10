package com.stripe.android.checkout

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.testing.TestLifecycleOwner
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentelement.embedded.content.SheetStateHolder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertFailsWith

@RunWith(RobolectricTestRunner::class)
internal class CheckoutLoadingToReadySheetCoordinatorTest {

    @Test
    fun `idle presentation launches Ready directly`() = runScenario {
        present()

        assertThat(launches.initialReadyCalls.awaitItem()).isEqualTo(Unit)
        launches.loadingCalls.expectNoEvents()
        launches.pendingReadyCalls.expectNoEvents()
        assertThat(awaitingReadyState.isAwaitingReady).isFalse()
        assertThat(sheetStateHolder.sheetIsOpen).isTrue()
    }

    @Test
    fun `updating presentation launches Loading immediately then Ready once`() = runScenario(
        isUpdating = true,
    ) {
        present()

        assertThat(launches.loadingCalls.awaitItem()).isEqualTo(Unit)
        launches.initialReadyCalls.expectNoEvents()
        assertThat(awaitingReadyState.isAwaitingReady).isTrue()

        coordinator.resumePendingReadyLaunch()
        isUpdating.value = false
        runCurrent()

        assertThat(launches.pendingReadyCalls.awaitItem()).isEqualTo(Unit)
        coordinator.resumePendingReadyLaunch()
        runCurrent()
        launches.pendingReadyCalls.expectNoEvents()
        assertThat(awaitingReadyState.isAwaitingReady).isFalse()
        assertThat(sheetStateHolder.sheetIsOpen).isTrue()
    }

    @Test
    fun `closing Loading suppresses Ready and releases sheet gate`() = runScenario(
        isUpdating = true,
    ) {
        present()
        assertThat(launches.loadingCalls.awaitItem()).isEqualTo(Unit)

        coordinator.close()
        isUpdating.value = false
        runCurrent()

        launches.pendingReadyCalls.expectNoEvents()
        assertThat(awaitingReadyState.isAwaitingReady).isFalse()
        assertThat(sheetStateHolder.sheetIsOpen).isFalse()
    }

    @Test
    fun `recreation resumes pending Ready launch exactly once`() = runScenario(
        isUpdating = true,
    ) {
        present()
        assertThat(launches.loadingCalls.awaitItem()).isEqualTo(Unit)

        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        isUpdating.value = false
        runCurrent()
        assertThat(awaitingReadyState.isAwaitingReady).isTrue()
        launches.pendingReadyCalls.expectNoEvents()

        val recreatedState = awaitingReadyState.recreate()
        val recreatedCoordinator = createCoordinator(TestLifecycleOwner(), recreatedState)
        recreatedCoordinator.resumePendingReadyLaunch()
        recreatedCoordinator.resumePendingReadyLaunch()
        runCurrent()

        assertThat(launches.pendingReadyCalls.awaitItem()).isEqualTo(Unit)
        launches.pendingReadyCalls.expectNoEvents()
        assertThat(recreatedState.isAwaitingReady).isFalse()
    }

    @Test
    fun `duplicate presentation is rejected while sheet is open`() = runScenario {
        present()
        present()

        assertThat(launches.initialReadyCalls.awaitItem()).isEqualTo(Unit)
        launches.initialReadyCalls.expectNoEvents()
        launches.loadingCalls.expectNoEvents()
    }

    @Test
    fun `closed root suppresses pending Ready launch`() = runScenario(
        isUpdating = true,
    ) {
        present()
        assertThat(launches.loadingCalls.awaitItem()).isEqualTo(Unit)

        sheetStateHolder.sheetIsOpen = false
        isUpdating.value = false
        runCurrent()

        launches.pendingReadyCalls.expectNoEvents()
        assertThat(awaitingReadyState.isAwaitingReady).isFalse()
        assertThat(sheetStateHolder.sheetIsOpen).isFalse()
    }

    @Test
    fun `missing refreshed state keeps Loading dismissible`() = runScenario(
        isUpdating = true,
    ) {
        launches.pendingReadyResult = ReadyLaunchResult.KeepLoading
        present()
        assertThat(launches.loadingCalls.awaitItem()).isEqualTo(Unit)

        isUpdating.value = false
        runCurrent()

        assertThat(launches.pendingReadyCalls.awaitItem()).isEqualTo(Unit)
        assertThat(awaitingReadyState.isAwaitingReady).isTrue()
        assertThat(sheetStateHolder.sheetIsOpen).isTrue()

        coordinator.close()
        assertThat(awaitingReadyState.isAwaitingReady).isFalse()
        assertThat(sheetStateHolder.sheetIsOpen).isFalse()
    }

    @Test
    fun `Loading launch exception rolls back presentation`() = runScenario(
        isUpdating = true,
    ) {
        launches.loadingError = IllegalStateException("Loading failed")

        assertFailsWith<IllegalStateException> { present() }

        assertThat(launches.loadingCalls.awaitItem()).isEqualTo(Unit)
        assertThat(awaitingReadyState.isAwaitingReady).isFalse()
        assertThat(sheetStateHolder.sheetIsOpen).isFalse()
    }

    @Test
    fun `initial Ready launch exception rolls back presentation`() = runScenario {
        launches.initialReadyError = IllegalStateException("Ready failed")

        assertFailsWith<IllegalStateException> { present() }

        assertThat(launches.initialReadyCalls.awaitItem()).isEqualTo(Unit)
        assertThat(awaitingReadyState.isAwaitingReady).isFalse()
        assertThat(sheetStateHolder.sheetIsOpen).isFalse()
    }

    @Test
    fun `resumed Ready launch exception rolls back presentation`() = runScenario(
        isUpdating = true,
    ) {
        launches.pendingReadyError = IllegalStateException("Ready failed")
        present()
        assertThat(launches.loadingCalls.awaitItem()).isEqualTo(Unit)

        isUpdating.value = false
        assertFailsWith<IllegalStateException> { runCurrent() }

        assertThat(launches.pendingReadyCalls.awaitItem()).isEqualTo(Unit)
        assertThat(awaitingReadyState.isAwaitingReady).isFalse()
        assertThat(sheetStateHolder.sheetIsOpen).isFalse()
    }

    private fun runScenario(
        isUpdating: Boolean = false,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val lifecycleOwner = TestLifecycleOwner()
        val sheetStateHolder = SheetStateHolder(SavedStateHandle())
        val awaitingReadyState = TestAwaitingReadyState()
        val isUpdatingState = MutableStateFlow(isUpdating)
        val launches = FakeLaunches()

        fun createCoordinator(
            owner: TestLifecycleOwner,
            state: AwaitingReadyState = awaitingReadyState,
        ): CheckoutLoadingToReadySheetCoordinator {
            return CheckoutLoadingToReadySheetCoordinator(
                lifecycleOwner = owner,
                sheetStateHolder = sheetStateHolder,
                isUpdating = isUpdatingState,
                awaitingReadyState = state,
                launchPendingReady = launches::launchPendingReady,
            )
        }

        val coordinator = createCoordinator(lifecycleOwner)
        Scenario(
            lifecycleOwner = lifecycleOwner,
            coordinator = coordinator,
            sheetStateHolder = sheetStateHolder,
            awaitingReadyState = awaitingReadyState,
            isUpdating = isUpdatingState,
            launches = launches,
            createCoordinator = ::createCoordinator,
            runCurrent = testScheduler::runCurrent,
        ).block()

        launches.ensureAllEventsConsumed()
    }

    private class Scenario(
        val lifecycleOwner: TestLifecycleOwner,
        val coordinator: CheckoutLoadingToReadySheetCoordinator,
        val sheetStateHolder: SheetStateHolder,
        val awaitingReadyState: TestAwaitingReadyState,
        val isUpdating: MutableStateFlow<Boolean>,
        val launches: FakeLaunches,
        val createCoordinator: (
            TestLifecycleOwner,
            AwaitingReadyState,
        ) -> CheckoutLoadingToReadySheetCoordinator,
        private val runCurrent: () -> Unit,
    ) {
        fun present() {
            coordinator.present(
                launchLoading = launches::launchLoading,
                launchReady = launches::launchInitialReady,
            )
        }

        fun runCurrent() {
            runCurrent.invoke()
        }
    }

    private class TestAwaitingReadyState(
        private val savedStateHandle: SavedStateHandle = SavedStateHandle(),
    ) : AwaitingReadyState {
        override var isAwaitingReady: Boolean
            get() = savedStateHandle[IS_AWAITING_READY_KEY] ?: false
            set(value) {
                savedStateHandle[IS_AWAITING_READY_KEY] = value
            }

        fun recreate(): TestAwaitingReadyState {
            return TestAwaitingReadyState(savedStateHandle)
        }

        private companion object {
            const val IS_AWAITING_READY_KEY = "isAwaitingReady"
        }
    }

    private class FakeLaunches {
        val loadingCalls = Turbine<Unit>()
        val initialReadyCalls = Turbine<Unit>()
        val pendingReadyCalls = Turbine<Unit>()

        var loadingError: Exception? = null
        var initialReadyError: Exception? = null
        var pendingReadyError: Exception? = null
        var pendingReadyResult: ReadyLaunchResult = ReadyLaunchResult.Launched

        fun launchLoading() {
            loadingCalls.add(Unit)
            loadingError?.let { throw it }
        }

        fun launchInitialReady() {
            initialReadyCalls.add(Unit)
            initialReadyError?.let { throw it }
        }

        fun launchPendingReady(): ReadyLaunchResult {
            pendingReadyCalls.add(Unit)
            pendingReadyError?.let { throw it }
            return pendingReadyResult
        }

        fun ensureAllEventsConsumed() {
            loadingCalls.ensureAllEventsConsumed()
            initialReadyCalls.ensureAllEventsConsumed()
            pendingReadyCalls.ensureAllEventsConsumed()
        }
    }
}
