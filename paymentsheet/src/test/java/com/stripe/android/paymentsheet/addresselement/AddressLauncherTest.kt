package com.stripe.android.paymentsheet.addresselement

import android.app.Application
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.app.ActivityOptionsCompat
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class AddressLauncherTest {
    @Test
    fun `present launches standalone mode`() = runTest {
        val activityLauncher = RecordingActivityResultLauncher()
        val launcher = AddressLauncher(
            application = ApplicationProvider.getApplicationContext<Application>(),
            activityResultLauncher = activityLauncher,
        )

        launcher.present(publishableKey = "pk_test_123")

        val args = activityLauncher.launchCalls.awaitItem()
        assertThat(args.publishableKey).isEqualTo("pk_test_123")
        activityLauncher.launchCalls.ensureAllEventsConsumed()
    }

    private class RecordingActivityResultLauncher :
        ActivityResultLauncher<AddressElementActivityContract.Args.Standalone>() {
        val launchCalls = Turbine<AddressElementActivityContract.Args.Standalone>()

        override fun launch(
            input: AddressElementActivityContract.Args.Standalone,
            options: ActivityOptionsCompat?,
        ) {
            launchCalls.add(input)
        }

        override fun unregister() = Unit

        override val contract: ActivityResultContract<AddressElementActivityContract.Args.Standalone, *>
            get() = AddressElementActivityContract.Standalone
    }
}
