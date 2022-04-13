package com.stripe.android.connections.launcher

import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.connections.ConnectionsSheet
import com.stripe.android.connections.ConnectionsSheetContract
import com.stripe.android.connections.ConnectionsSheetResult
import com.stripe.android.connections.linkAccountSessionWithNoMoreAccounts
import com.stripe.android.connections.utils.FakeActivityResultRegistry
import com.stripe.android.connections.utils.TestFragment
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DefaultConnectionsSheetLauncherTest {

    private val configuration = ConnectionsSheet.Configuration("", "")

    @Test
    fun `create and present should return expected ConnectionsSheetResult#Completed`() {
        val testRegistry = FakeActivityResultRegistry(
            ConnectionsSheetContract.Result.Completed(
                linkAccountSession = linkAccountSessionWithNoMoreAccounts,
                token = null
            )
        )

        with(
            launchFragmentInContainer(initialState = Lifecycle.State.CREATED) { TestFragment() }
        ) {
            onFragment { fragment ->
                val results = mutableListOf<ConnectionsSheetResult>()
                val launcher = DefaultConnectionsSheetLauncher(
                    fragment,
                    testRegistry
                ) {
                    results.add(it)
                }

                moveToState(Lifecycle.State.RESUMED)
                launcher.present(configuration)
                assertThat(results)
                    .containsExactly(
                        ConnectionsSheetResult.Completed(
                            linkAccountSessionWithNoMoreAccounts
                        )
                    )
            }
        }
    }

    @Test
    fun `create and present should return expected ConnectionsSheetResult#Cancelled`() {
        val testRegistry = FakeActivityResultRegistry(ConnectionsSheetContract.Result.Canceled)

        with(
            launchFragmentInContainer(initialState = Lifecycle.State.CREATED) { TestFragment() }
        ) {
            onFragment { fragment ->
                val results = mutableListOf<ConnectionsSheetResult>()
                val launcher = DefaultConnectionsSheetLauncher(
                    fragment,
                    testRegistry
                ) {
                    results.add(it)
                }

                moveToState(Lifecycle.State.RESUMED)
                launcher.present(configuration)
                assertThat(results).containsExactly(ConnectionsSheetResult.Canceled)
            }
        }
    }
}
