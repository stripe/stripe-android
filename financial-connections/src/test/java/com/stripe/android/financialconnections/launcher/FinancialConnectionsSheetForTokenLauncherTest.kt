package com.stripe.android.financialconnections.launcher

import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.financialconnections.FinancialConnectionsSheetConfiguration
import com.stripe.android.financialconnections.FinancialConnectionsSheetForTokenResult
import com.stripe.android.financialconnections.bankAccountToken
import com.stripe.android.financialconnections.financialConnectionsSessionWithNoMoreAccounts
import com.stripe.android.financialconnections.intentBuilder
import com.stripe.android.financialconnections.utils.FakeActivityResultRegistry
import com.stripe.android.financialconnections.utils.TestFragment
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FinancialConnectionsSheetForTokenLauncherTest {

    private val configuration = FinancialConnectionsSheetConfiguration("", "")

    @Test
    fun `create and present should return expected ConnectionsSheetForTokenResult#Completed`() {
        val testRegistry = FakeActivityResultRegistry(
            FinancialConnectionsSheetForTokenResult.Completed(
                financialConnectionsSession = financialConnectionsSessionWithNoMoreAccounts,
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
                    testRegistry,
                    intentBuilder(fragment.requireContext())
                ) {
                    results.add(it)
                }

                moveToState(Lifecycle.State.RESUMED)
                launcher.present(configuration)
                assertThat(results)
                    .containsExactly(
                        FinancialConnectionsSheetForTokenResult.Completed(
                            financialConnectionsSessionWithNoMoreAccounts,
                            bankAccountToken
                        )
                    )
            }
        }
    }

    @Test
    fun `create and present should return expected ConnectionsSheetForTokenResult#Cancelled`() {
        val testRegistry = FakeActivityResultRegistry(FinancialConnectionsSheetForTokenResult.Canceled)

        with(
            launchFragmentInContainer(initialState = Lifecycle.State.CREATED) { TestFragment() }
        ) {
            onFragment { fragment ->
                val results = mutableListOf<FinancialConnectionsSheetForTokenResult>()
                val launcher = FinancialConnectionsSheetForTokenLauncher(
                    fragment,
                    testRegistry,
                    intentBuilder(fragment.requireContext())
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
