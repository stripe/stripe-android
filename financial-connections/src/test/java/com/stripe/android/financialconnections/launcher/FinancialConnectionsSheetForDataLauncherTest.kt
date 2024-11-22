package com.stripe.android.financialconnections.launcher

import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.FinancialConnectionsSheetInternalResult
import com.stripe.android.financialconnections.FinancialConnectionsSheetResult
import com.stripe.android.financialconnections.financialConnectionsSessionWithNoMoreAccounts
import com.stripe.android.financialconnections.toPublicResult
import com.stripe.android.financialconnections.utils.FakeActivityResultRegistry
import com.stripe.android.financialconnections.utils.TestFragment
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FinancialConnectionsSheetForDataLauncherTest {

    private val configuration = FinancialConnectionsSheet.Configuration("", "")

    @Test
    fun `create and present should return expected ConnectionsSheetResult#Completed`() {
        val testRegistry = FakeActivityResultRegistry(
            FinancialConnectionsSheetInternalResult.Completed(
                financialConnectionsSession = financialConnectionsSessionWithNoMoreAccounts,
                manualEntryUsesMicrodeposits = false,
            )
        )

        with(
            launchFragmentInContainer(initialState = Lifecycle.State.CREATED) { TestFragment() }
        ) {
            onFragment { fragment ->
                val results = mutableListOf<FinancialConnectionsSheetResult>()
                val launcher = FinancialConnectionsSheetForDataLauncher(
                    fragment,
                    testRegistry
                ) {
                    results.add(it.toPublicResult())
                }

                moveToState(Lifecycle.State.RESUMED)
                launcher.present(configuration)
                assertThat(results)
                    .containsExactly(
                        FinancialConnectionsSheetResult.Completed(
                            financialConnectionsSessionWithNoMoreAccounts
                        )
                    )
            }
        }
    }

    @Test
    fun `create and present should return expected ConnectionsSheetResult#Cancelled`() {
        val testRegistry = FakeActivityResultRegistry(FinancialConnectionsSheetInternalResult.Canceled)

        with(
            launchFragmentInContainer(initialState = Lifecycle.State.CREATED) { TestFragment() }
        ) {
            onFragment { fragment ->
                val results = mutableListOf<FinancialConnectionsSheetResult>()
                val launcher = FinancialConnectionsSheetForDataLauncher(
                    fragment,
                    testRegistry
                ) {
                    results.add(it.toPublicResult())
                }

                moveToState(Lifecycle.State.RESUMED)
                launcher.present(configuration)
                assertThat(results).containsExactly(FinancialConnectionsSheetResult.Canceled)
            }
        }
    }
}
