package com.stripe.android.financialconnections.ui.components

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class BottomSheetDismissalControllerTest {

    @Test
    fun `Notifies listener if dismissed by swipe`() = runTest {
        val isBottomSheetVisibleFlow = MutableSharedFlow<Boolean>()
        val currentPaneFlow = MutableSharedFlow<Pane>()

        val controller = BottomSheetDismissalController(
            isBottomSheetVisibleFlow = isBottomSheetVisibleFlow,
            currentPaneFlow = currentPaneFlow,
        )

        val onDismissedBySwipe = callbackFlow {
            controller.onDismissedBySwipe {
                trySend(true)
            }
        }

        onDismissedBySwipe.test {
            currentPaneFlow.emit(Pane.INSTITUTION_PICKER)
            isBottomSheetVisibleFlow.emit(false)
            expectNoEvents()

            currentPaneFlow.emit(Pane.EXIT)
            isBottomSheetVisibleFlow.emit(true)
            expectNoEvents()

            isBottomSheetVisibleFlow.emit(false)
            assertThat(awaitItem()).isTrue()
        }
    }

    @Test
    fun `Does not notify listener if dismissed another way`() = runTest {
        val isBottomSheetVisibleFlow = MutableSharedFlow<Boolean>()
        val currentPaneFlow = MutableSharedFlow<Pane>()

        val controller = BottomSheetDismissalController(
            isBottomSheetVisibleFlow = isBottomSheetVisibleFlow,
            currentPaneFlow = currentPaneFlow,
        )

        val onDismissedBySwipe = callbackFlow {
            controller.onDismissedBySwipe {
                trySend(true)
            }
        }

        onDismissedBySwipe.test {
            currentPaneFlow.emit(Pane.INSTITUTION_PICKER)
            isBottomSheetVisibleFlow.emit(false)

            currentPaneFlow.emit(Pane.EXIT)
            isBottomSheetVisibleFlow.emit(true)

            currentPaneFlow.emit(Pane.INSTITUTION_PICKER)
            isBottomSheetVisibleFlow.emit(false)

            expectNoEvents()
        }
    }
}
