package com.stripe.android.connections.launcher

import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.connections.ConnectionsSheet
import com.stripe.android.connections.ConnectionsSheetContract
import com.stripe.android.connections.ConnectionsSheetForTokenResult
import com.stripe.android.connections.bankAccountToken
import com.stripe.android.connections.linkAccountSessionWithNoMoreAccounts
import com.stripe.android.connections.utils.FakeActivityResultRegistry
import com.stripe.android.connections.utils.TestFragment
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ConnectionsSheetForTokenLauncherTest {

    private val configuration = ConnectionsSheet.Configuration("", "")

    @Test
    fun `create and present should return expected ConnectionsSheetForTokenResult#Completed`() {
        val testRegistry = FakeActivityResultRegistry(
            ConnectionsSheetContract.Result.Completed(
                linkAccountSession = linkAccountSessionWithNoMoreAccounts,
                token = bankAccountToken
            )
        )

        with(
            launchFragmentInContainer(initialState = Lifecycle.State.CREATED) { TestFragment() }
        ) {
            onFragment { fragment ->
                val results = mutableListOf<ConnectionsSheetForTokenResult>()
                val launcher = ConnectionsSheetForTokenLauncher(
                    fragment,
                    testRegistry
                ) {
                    results.add(it)
                }

                moveToState(Lifecycle.State.RESUMED)
                launcher.present(configuration)
                assertThat(results)
                    .containsExactly(
                        ConnectionsSheetForTokenResult.Completed(
                            linkAccountSessionWithNoMoreAccounts,
                            bankAccountToken
                        )
                    )
            }
        }
    }

    @Test
    fun `create and present should return expected ConnectionsSheetForTokenResult#Cancelled`() {
        val testRegistry = FakeActivityResultRegistry(ConnectionsSheetContract.Result.Canceled)

        with(
            launchFragmentInContainer(initialState = Lifecycle.State.CREATED) { TestFragment() }
        ) {
            onFragment { fragment ->
                val results = mutableListOf<ConnectionsSheetForTokenResult>()
                val launcher = ConnectionsSheetForTokenLauncher(
                    fragment,
                    testRegistry
                ) {
                    results.add(it)
                }

                moveToState(Lifecycle.State.RESUMED)
                launcher.present(configuration)
                assertThat(results).containsExactly(ConnectionsSheetForTokenResult.Canceled)
            }
        }
    }

}