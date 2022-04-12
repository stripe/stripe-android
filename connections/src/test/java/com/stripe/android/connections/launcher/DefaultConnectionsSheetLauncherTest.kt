package com.stripe.android.connections.launcher

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultCallback
import com.google.common.truth.Truth.assertThat
import com.stripe.android.connections.ConnectionsSheet
import com.stripe.android.connections.ConnectionsSheetResult
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DefaultConnectionsSheetLauncherTest {

    private val configuration = ConnectionsSheet.Configuration("", "")

    @Test
    fun `test test`() {
        val activity = mock<ComponentActivity>()
        givenLaunchActivityReturns(activity, ConnectionsSheetContract.Result.Canceled)

        DefaultConnectionsSheetLauncher(
            activity = activity,
            callback = {
                assertThat(it).isInstanceOf(ConnectionsSheetResult.Canceled::class.java)
            }
        ).present(configuration)
    }

    private fun givenLaunchActivityReturns(
        activity: ComponentActivity,
        result: ConnectionsSheetContract.Result
    ) {
        whenever(
            activity.registerForActivityResult(
                any<ConnectionsSheetContract>(),
                any<ActivityResultCallback<ConnectionsSheetContract.Result>>()
            )
        ).thenAnswer {
            val callback =
                it.getArgument<ActivityResultCallback<ConnectionsSheetContract.Result>>(1)
            callback.onActivityResult(result)
        }
    }

}
