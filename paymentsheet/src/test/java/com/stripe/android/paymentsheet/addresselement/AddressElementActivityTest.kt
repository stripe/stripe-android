package com.stripe.android.paymentsheet.addresselement

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class AddressElementActivityTest {
    @Test
    fun `when launched without args should finish with canceled result`() {
        ActivityScenario.launchActivityForResult(
            AddressElementActivity::class.java,
            Bundle.EMPTY
        ).use { activityScenario ->
            assertThat(activityScenario.state).isEqualTo(Lifecycle.State.DESTROYED)
            val result = AddressElementActivityContract.Standalone.parseResult(
                activityScenario.result.resultCode,
                activityScenario.result.resultData,
            )
            assertThat(result).isEqualTo(AddressLauncherResult.Canceled())
        }
    }

    @Test
    fun `standalone contract creates intent with standalone args`() {
        val args = AddressElementActivityContract.Args.Standalone(
            publishableKey = "pk_test_123",
            config = null,
        )

        val intent = AddressElementActivityContract.Standalone.createIntent(
            ApplicationProvider.getApplicationContext(),
            args,
        )

        assertThat(intent.component?.className).isEqualTo(AddressElementActivity::class.java.name)
        assertThat(AddressElementActivityContract.Args.fromIntent(intent)).isEqualTo(args)
    }

    @Test
    fun `checkout shipping contract creates intent with checkout shipping args`() {
        val response = CheckoutSessionResponseFactory.create()
        val args = AddressElementActivityContract.Args.CheckoutShipping(
            publishableKey = "pk_test_123",
            config = null,
            checkoutSessionResponse = response,
        )

        val intent = AddressElementActivityContract.CheckoutShipping.createIntent(
            ApplicationProvider.getApplicationContext(),
            args,
        )

        assertThat(intent.component?.className).isEqualTo(AddressElementActivity::class.java.name)
        assertThat(AddressElementActivityContract.Args.fromIntent(intent)).isEqualTo(args)
    }

    @Test
    fun `standalone contract maps standalone success to public success`() {
        val result = AddressElementActivityContract.Result.StandaloneSucceeded(AddressDetails())

        val parsed = AddressElementActivityContract.Standalone.parseResult(
            resultCode = result.resultCode,
            intent = Intent().putExtras(result.toBundle()),
        )

        assertThat(parsed).isEqualTo(AddressLauncherResult.Succeeded(result.address))
    }

    @Test
    fun `standalone contract maps missing result to canceled`() {
        val parsed = AddressElementActivityContract.Standalone.parseResult(
            resultCode = Activity.RESULT_CANCELED,
            intent = Intent(),
        )

        assertThat(parsed).isEqualTo(AddressLauncherResult.Canceled())
    }

    @Test
    fun `standalone contract maps canceled result to public canceled`() {
        val result = AddressElementActivityContract.Result.Canceled

        val parsed = AddressElementActivityContract.Standalone.parseResult(
            resultCode = result.resultCode,
            intent = Intent().putExtras(result.toBundle()),
        )

        assertThat(parsed).isEqualTo(AddressLauncherResult.Canceled())
    }

    @Test
    fun `standalone contract maps checkout shipping success to canceled`() {
        val result = AddressElementActivityContract.Result.CheckoutShippingSucceeded(
            address = AddressDetails(),
            updatedResponse = CheckoutSessionResponseFactory.create(),
        )

        val parsed = AddressElementActivityContract.Standalone.parseResult(
            resultCode = result.resultCode,
            intent = Intent().putExtras(result.toBundle()),
        )

        assertThat(parsed).isEqualTo(AddressLauncherResult.Canceled())
    }

    @Test
    fun `checkout shipping contract preserves checkout shipping success`() {
        val result = AddressElementActivityContract.Result.CheckoutShippingSucceeded(
            address = AddressDetails(),
            updatedResponse = CheckoutSessionResponseFactory.create(),
        )

        val parsed = AddressElementActivityContract.CheckoutShipping.parseResult(
            resultCode = result.resultCode,
            intent = Intent().putExtras(result.toBundle()),
        )

        assertThat(parsed).isEqualTo(result)
    }

    @Test
    fun `checkout shipping contract maps missing result to canceled`() {
        val parsed = AddressElementActivityContract.CheckoutShipping.parseResult(
            resultCode = Activity.RESULT_CANCELED,
            intent = Intent(),
        )

        assertThat(parsed).isEqualTo(AddressElementActivityContract.Result.Canceled)
    }

    @Test
    fun `checkout shipping contract preserves canceled result`() {
        val result = AddressElementActivityContract.Result.Canceled

        val parsed = AddressElementActivityContract.CheckoutShipping.parseResult(
            resultCode = result.resultCode,
            intent = Intent().putExtras(result.toBundle()),
        )

        assertThat(parsed).isEqualTo(result)
    }

    @Test
    fun `checkout shipping contract maps standalone success to canceled`() {
        val result = AddressElementActivityContract.Result.StandaloneSucceeded(AddressDetails())

        val parsed = AddressElementActivityContract.CheckoutShipping.parseResult(
            resultCode = result.resultCode,
            intent = Intent().putExtras(result.toBundle()),
        )

        assertThat(parsed).isEqualTo(AddressElementActivityContract.Result.Canceled)
    }

    @Test
    fun `configuration recreation retains processing and blocks back and navigator dismissal`() {
        val intent = Intent(
            ApplicationProvider.getApplicationContext(),
            AddressElementActivity::class.java,
        ).putExtra(
            AddressElementActivityContract.EXTRA_ARGS,
            AddressElementActivityContract.Args.Standalone(
                publishableKey = "pk_test_123",
                config = null,
            )
        )

        ActivityScenario.launch<AddressElementActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                assertThat(activity.viewModel.processingState.tryStartProcessing()).isTrue()
                activity.onBackPressedDispatcher.onBackPressed()
                activity.viewModel.navigator.dismissWithResult(
                    AddressElementActivityContract.Result.Canceled
                )
                assertThat(activity.isFinishing).isFalse()
            }

            scenario.recreate()

            scenario.onActivity { activity ->
                assertThat(activity.viewModel.processingState.isProcessing.value).isTrue()
            }
        }
    }

    @Test
    fun `checkout success finishes while processing`() {
        val response = CheckoutSessionResponseFactory.create(customerEmail = "updated@example.com")
        val result = AddressElementActivityContract.Result.CheckoutShippingSucceeded(
            address = AddressDetails(),
            updatedResponse = response,
        )
        val intent = Intent(
            ApplicationProvider.getApplicationContext(),
            AddressElementActivity::class.java,
        ).putExtra(
            AddressElementActivityContract.EXTRA_ARGS,
            AddressElementActivityContract.Args.CheckoutShipping(
                publishableKey = "pk_test_123",
                config = null,
                checkoutSessionResponse = response,
            )
        )

        ActivityScenario.launchActivityForResult<AddressElementActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                activity.viewModel.processingState.tryStartProcessing()
                activity.completeCheckoutShipping(result)
                assertThat(activity.isFinishing).isTrue()
            }
            assertThat(
                AddressElementActivityContract.CheckoutShipping.parseResult(
                    scenario.result.resultCode,
                    scenario.result.resultData,
                )
            ).isEqualTo(result)
        }
    }
}
