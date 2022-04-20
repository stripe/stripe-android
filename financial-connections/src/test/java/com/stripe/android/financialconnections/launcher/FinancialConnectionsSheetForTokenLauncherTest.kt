package com.stripe.android.connections.launcher

import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.connections.FinancialConnectionsSheetForTokenResult
import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.FinancialConnectionsSheetContract
import com.stripe.android.financialconnections.bankAccountToken
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetForTokenLauncher
import com.stripe.android.financialconnections.linkAccountSessionWithNoMoreAccounts
import com.stripe.android.financialconnections.utils.FakeActivityResultRegistry
import com.stripe.android.financialconnections.utils.TestFragment
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FinancialConnectionsSheetForTokenLauncherTest {

    private val configuration = FinancialConnectionsSheet.Configuration("", "")

    @Test
    fun `create and present should return expected ConnectionsSheetForTokenResult#Completed`() {
        val testRegistry = FakeActivityResultRegistry(
            FinancialConnectionsSheetContract.Result.Completed(
                linkAccountSession = linkAccountSessionWithNoMoreAccounts,
                token = bankAccountToken
            )
        )

        with(
            launchFragmentInContainer(initialState = Lifecycle.State.CREATED) { TestFragment() }
        ) {
            onFragment { fragment ->
                val results = mutableListOf<FinancialConnectionsSheetForTokenResult>()
                val launcher = FinancialConnectionsSheetForTokenLauncher(
                    fragment,
                    testRegistry
                ) {
                    results.add(it)
                }

                moveToState(Lifecycle.State.RESUMED)
                launcher.present(configuration)
                assertThat(results)
                    .containsExactly(
                        FinancialConnectionsSheetForTokenResult.Completed(
                            linkAccountSessionWithNoMoreAccounts,
                            bankAccountToken
                        )
                    )
            }
        }
    }

    @Test
    fun `create and present should return expected ConnectionsSheetForTokenResult#Cancelled`() {
        val testRegistry = FakeActivityResultRegistry(FinancialConnectionsSheetContract.Result.Canceled)

        with(
            launchFragmentInContainer(initialState = Lifecycle.State.CREATED) { TestFragment() }
        ) {
            onFragment { fragment ->
                val results = mutableListOf<FinancialConnectionsSheetForTokenResult>()
                val launcher = FinancialConnectionsSheetForTokenLauncher(
                    fragment,
                    testRegistry
                ) {
                    results.add(it)
                }

                moveToState(Lifecycle.State.RESUMED)
                launcher.present(configuration)
                assertThat(results).containsExactly(FinancialConnectionsSheetForTokenResult.Canceled)
            }
        }
    }
}
