package com.stripe.android.paymentsheet.navigation

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.CVCRecollectionFixtures
import com.stripe.android.paymentsheet.ui.PaymentSheetTopBarState
import com.stripe.android.paymentsheet.ui.PaymentSheetTopBarStateFactory
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.mock

internal class PaymentSheetScreenCVCRecollectionTest {

    private val screen = PaymentSheetScreen.CvcRecollection(
        args = CVCRecollectionFixtures.contractArgs(),
        interactor = mock()
    )

    @Test
    fun `complete flow and enabled wallet returns null title`() = runTest {
        screen.title(isCompleteFlow = true, isWalletEnabled = true).test {
            assertThat(awaitItem()).isEqualTo(null)
        }
    }

    @Test
    fun `complete flow and disabled wallet returns null title`() = runTest {
        screen.title(isCompleteFlow = true, isWalletEnabled = false).test {
            assertThat(awaitItem()).isEqualTo(null)
        }
    }

    @Test
    fun `incomplete flow and enabled wallet returns null title`() = runTest {
        screen.title(isCompleteFlow = false, isWalletEnabled = true).test {
            assertThat(awaitItem()).isEqualTo(null)
        }
    }

    @Test
    fun `incomplete flow and disabled wallet returns null title`() = runTest {
        screen.title(isCompleteFlow = false, isWalletEnabled = false).test {
            assertThat(awaitItem()).isEqualTo(null)
        }
    }

    @Test
    fun `top bar returns correct state`() = runTest {
        screen.topBarState().test {
            assertThat(awaitItem()).isEqualTo(
                PaymentSheetTopBarStateFactory.create(
                    hasBackStack = true,
                    isLiveMode = false,
                    editable = PaymentSheetTopBarState.Editable.Never,
                )
            )
        }
    }

    @Test
    fun `complete flow for showsWalletsHeader should return false`() = runTest {
        screen.showsWalletsHeader(isCompleteFlow = true).test {
            assertThat(awaitItem()).isEqualTo(false)
        }
    }

    @Test
    fun `incomplete flow for showsWalletsHeader should return false`() = runTest {
        screen.showsWalletsHeader(isCompleteFlow = false).test {
            assertThat(awaitItem()).isEqualTo(false)
        }
    }
}
