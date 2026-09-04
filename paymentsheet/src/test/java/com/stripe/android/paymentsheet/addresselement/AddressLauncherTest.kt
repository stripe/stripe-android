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
        assertThat(args.launchMode).isEqualTo(AddressElementActivityContract.LaunchMode.Standalone)
        activityLauncher.launchCalls.ensureAllEventsConsumed()
    }

    @Test
    fun `public callback receives standalone success as address launcher result`() = runTest {
        val callbackResults = Turbine<AddressLauncherResult>()
        val callback = AddressLauncherResultCallback(callbackResults::add)
        val address = AddressDetails()

        callback.onAddressElementActivityResult(
            AddressElementActivityContract.Result.StandaloneSucceeded(address)
        )

        assertThat(callbackResults.awaitItem()).isEqualTo(AddressLauncherResult.Succeeded(address))
        callbackResults.ensureAllEventsConsumed()
    }

    private class RecordingActivityResultLauncher :
        ActivityResultLauncher<AddressElementActivityContract.Args>() {
        val launchCalls = Turbine<AddressElementActivityContract.Args>()

        override fun launch(
            input: AddressElementActivityContract.Args,
            options: ActivityOptionsCompat?,
        ) {
            launchCalls.add(input)
        }

        override fun unregister() = Unit

        override val contract: ActivityResultContract<AddressElementActivityContract.Args, *>
            get() = AddressElementActivityContract
    }
}
