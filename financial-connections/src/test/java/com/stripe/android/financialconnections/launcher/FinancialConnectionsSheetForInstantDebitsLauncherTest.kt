package com.stripe.android.financialconnections.launcher

import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.utils.FakeActivityResultRegistry
import com.stripe.android.financialconnections.utils.TestFragment
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FinancialConnectionsSheetForInstantDebitsLauncherTest {

    private val configuration = FinancialConnectionsSheet.Configuration("", "")

    @Test
    fun `create and present should return expected ConnectionsSheetResult#Completed`() {
        val testRegistry = FakeActivityResultRegistry(
            FinancialConnectionsSheetInstantDebitsResult.Completed(
                paymentMethodId = "pm_123",
                last4 = "1234",
                bankName = "Bank of America",
            )
        )

        with(
            launchFragmentInContainer(initialState = Lifecycle.State.CREATED) { TestFragment() }
        ) {
            onFragment { fragment ->
                val results = mutableListOf<FinancialConnectionsSheetInstantDebitsResult>()
                val launcher = FinancialConnectionsSheetForInstantDebitsLauncher(
                    fragment,
                    testRegistry
                ) {
                    results.add(it)
                }

                moveToState(Lifecycle.State.RESUMED)
                launcher.present(configuration)
                assertThat(results)
                    .containsExactly(
                        FinancialConnectionsSheetInstantDebitsResult.Completed(
                            paymentMethodId = "pm_123",
                            last4 = "1234",
                            bankName = "Bank of America",
                        )
                    )
            }
        }
    }

    @Test
    fun `create and present should return expected ConnectionsSheetResult#Cancelled`() {
        val testRegistry = FakeActivityResultRegistry(FinancialConnectionsSheetInstantDebitsResult.Canceled)

        with(
            launchFragmentInContainer(initialState = Lifecycle.State.CREATED) { TestFragment() }
        ) {
            onFragment { fragment ->
                val results = mutableListOf<FinancialConnectionsSheetInstantDebitsResult>()
                val launcher = FinancialConnectionsSheetForInstantDebitsLauncher(
                    fragment,
                    testRegistry
                ) {
                    results.add(it)
                }

                moveToState(Lifecycle.State.RESUMED)
                launcher.present(configuration)
                assertThat(results).containsExactly(FinancialConnectionsSheetInstantDebitsResult.Canceled)
            }
        }
    }
}
