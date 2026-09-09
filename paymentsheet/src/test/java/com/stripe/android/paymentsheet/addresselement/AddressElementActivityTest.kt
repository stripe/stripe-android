package com.stripe.android.paymentsheet.addresselement

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP
import android.os.Build
import android.os.Bundle
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.createComposeCleanupRule
import com.stripe.android.utils.InjectableActivityScenario
import com.stripe.android.utils.TestUtils.viewModelFactoryFor
import com.stripe.android.utils.injectableActivityScenario
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
internal class AddressElementActivityTest {
    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    @Test
    fun `loading transitions to ready intent`() {
        val loadingIntent = checkoutIntent(
            AddressElementActivityContract.Args.CheckoutShipping.Loading("pk_test_123")
        )
        val readyIntent = checkoutIntent(
            AddressElementActivityContract.Args.CheckoutShipping.Ready(
                "pk_test_123",
                null,
                CheckoutSessionResponseFactory.create(),
            )
        )
        val controller = Robolectric.buildActivity(AddressElementActivity::class.java, loadingIntent)
            .create()
            .start()
            .resume()
            .visible()

        controller.newIntent(readyIntent)

        assertThat(controller.get().intent).isSameInstanceAs(readyIntent)
        controller.pause().stop().destroy()
    }

    @Test
    fun `loading ignores another loading intent`() {
        val initialIntent = checkoutIntent(
            AddressElementActivityContract.Args.CheckoutShipping.Loading("pk_test_123")
        )
        val updatedIntent = checkoutIntent(
            AddressElementActivityContract.Args.CheckoutShipping.Loading("pk_test_456")
        )
        val controller = Robolectric.buildActivity(AddressElementActivity::class.java, initialIntent)
            .create()
            .start()
            .resume()
            .visible()

        controller.newIntent(updatedIntent)

        assertThat(controller.get().intent).isSameInstanceAs(initialIntent)
        controller.pause().stop().destroy()
    }

    @Test
    fun `ready ignores another ready intent`() {
        val loadingIntent = checkoutIntent(
            AddressElementActivityContract.Args.CheckoutShipping.Loading("pk_test_123")
        )
        val initialReadyIntent = checkoutIntent(
            AddressElementActivityContract.Args.CheckoutShipping.Ready(
                "pk_test_123",
                null,
                CheckoutSessionResponseFactory.create(),
            )
        )
        val updatedReadyIntent = checkoutIntent(
            AddressElementActivityContract.Args.CheckoutShipping.Ready(
                "pk_test_456",
                null,
                CheckoutSessionResponseFactory.create(),
            )
        )
        val controller = Robolectric.buildActivity(AddressElementActivity::class.java, loadingIntent)
            .create()
            .start()
            .resume()
            .visible()

        controller.newIntent(initialReadyIntent)
        controller.newIntent(updatedReadyIntent)

        assertThat(controller.get().intent).isSameInstanceAs(initialReadyIntent)
        controller.pause().stop().destroy()
    }

    @Test
    fun `finishing activity ignores ready intent`() {
        val loadingIntent = checkoutIntent(
            AddressElementActivityContract.Args.CheckoutShipping.Loading("pk_test_123")
        )
        val readyIntent = checkoutIntent(
            AddressElementActivityContract.Args.CheckoutShipping.Ready(
                "pk_test_123",
                null,
                CheckoutSessionResponseFactory.create(),
            )
        )
        val controller = Robolectric.buildActivity(AddressElementActivity::class.java, loadingIntent)
            .create()
            .start()
            .resume()
            .visible()

        controller.get().finish()
        controller.newIntent(readyIntent)

        assertThat(controller.get().intent).isSameInstanceAs(loadingIntent)
        controller.pause().stop().destroy()
    }

    @Test
    fun loadingCheckoutActivityDoesNotCreateReadyViewModel() {
        val args = AddressElementActivityContract.Args.CheckoutShipping.Loading("pk_test_123")
        val intent = AddressElementActivityContract.CheckoutShipping.createIntent(application, args)
        var viewModelCreateCalls = 0
        val controller = Robolectric.buildActivity(AddressElementActivity::class.java, intent)
        controller.get().viewModelFactory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                viewModelCreateCalls += 1
                error("Loading must not create Address Element dependencies")
            }
        }

        controller.create().start().resume().visible()

        assertThat(viewModelCreateCalls).isEqualTo(0)
        controller.pause().stop().destroy()
    }

    @Test
    fun loadingCheckoutActivityIsDismissible() {
        val args = AddressElementActivityContract.Args.CheckoutShipping.Loading("pk_test_123")
        val intent = AddressElementActivityContract.CheckoutShipping.createIntent(application, args)

        ActivityScenario.launchActivityForResult<AddressElementActivity>(intent).use { scenario ->
            assertThat(scenario.state).isEqualTo(Lifecycle.State.RESUMED)

            composeTestRule.waitForIdle()
            scenario.onActivity { activity ->
                activity.onBackPressedDispatcher.onBackPressed()
            }
            composeTestRule.waitForIdle()
            assertThat(
                AddressElementActivityContract.CheckoutShipping.parseResult(
                    scenario.result.resultCode,
                    scenario.result.resultData,
                )
            ).isEqualTo(AddressElementActivityContract.Result.Canceled)
        }
    }

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    @get:Rule
    val composeCleanupRule = createComposeCleanupRule()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(UnconfinedTestDispatcher())

    private val application = ApplicationProvider.getApplicationContext<Application>()

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
        val args = standaloneArgs()

        val intent = AddressElementActivityContract.Standalone.createIntent(application, args)

        assertThat(intent.component?.className).isEqualTo(AddressElementActivity::class.java.name)
        assertThat(AddressElementActivityContract.Args.fromIntent(intent)).isEqualTo(args)
        assertThat(intent.flags and FLAG_ACTIVITY_SINGLE_TOP).isNotEqualTo(0)
    }

    @Test
    fun `checkout shipping contract creates intent with checkout shipping args`() {
        val response = CheckoutSessionResponseFactory.create()
        val args = checkoutArgs(response)

        val intent = AddressElementActivityContract.CheckoutShipping.createIntent(application, args)

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
        val result = checkoutResult(CheckoutSessionResponseFactory.create())

        val parsed = AddressElementActivityContract.Standalone.parseResult(
            resultCode = result.resultCode,
            intent = Intent().putExtras(result.toBundle()),
        )

        assertThat(parsed).isEqualTo(AddressLauncherResult.Canceled())
    }

    @Test
    fun `checkout shipping contract preserves checkout shipping success`() {
        val result = checkoutResult(CheckoutSessionResponseFactory.create())

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
    fun `configuration recreation retains processing and blocks back cancellation`() {
        launchActivity(standaloneArgs()).use { testScenario ->
            composeTestRule.waitForIdle()
            assertThat(testScenario.stateHolder.tryStartProcessing()).isTrue()
            composeTestRule.waitForIdle()

            testScenario.scenario.onActivity { activity ->
                activity.onBackPressedDispatcher.onBackPressed()
                assertThat(activity.isFinishing).isFalse()
            }

            assertThat(testScenario.stateHolder.state.value)
                .isEqualTo(AddressElementActivityStateHolder.State.Processing)

            testScenario.scenario.recreate()
            composeTestRule.waitForIdle()

            assertThat(testScenario.stateHolder.state.value)
                .isEqualTo(AddressElementActivityStateHolder.State.Processing)
        }
    }

    @Test
    fun `standalone completion hides sheet finishes and delivers result`() {
        val result = AddressElementActivityContract.Result.StandaloneSucceeded(AddressDetails())

        launchActivity(standaloneArgs()).use { testScenario ->
            composeTestRule.waitForIdle()
            assertThat(testScenario.stateHolder.complete(result)).isTrue()
            composeTestRule.waitForIdle()

            assertThat(testScenario.scenario.state).isEqualTo(Lifecycle.State.DESTROYED)
            assertThat(
                AddressElementActivityContract.Standalone.parseResult(
                    testScenario.scenario.getResult().resultCode,
                    testScenario.scenario.getResult().resultData,
                )
            ).isEqualTo(AddressLauncherResult.Succeeded(result.address))
        }
    }

    @Test
    fun `checkout completion hides sheet finishes and delivers result`() {
        val response = CheckoutSessionResponseFactory.create(customerEmail = "updated@example.com")
        val result = checkoutResult(response)

        launchActivity(checkoutArgs(response)).use { testScenario ->
            composeTestRule.waitForIdle()
            assertThat(testScenario.stateHolder.tryStartProcessing()).isTrue()
            composeTestRule.waitForIdle()
            assertThat(testScenario.stateHolder.complete(result)).isTrue()
            composeTestRule.waitForIdle()

            assertThat(testScenario.scenario.state).isEqualTo(Lifecycle.State.DESTROYED)
            assertThat(
                AddressElementActivityContract.CheckoutShipping.parseResult(
                    testScenario.scenario.getResult().resultCode,
                    testScenario.scenario.getResult().resultData,
                )
            ).isEqualTo(result)
        }
    }

    private fun launchActivity(args: AddressElementActivityContract.Args): TestScenario {
        val viewModel = AddressElementViewModel.Factory(
            applicationSupplier = { application },
            starterArgsSupplier = { args },
        ).create(AddressElementViewModel::class.java)
        val scenario = injectableActivityScenario<AddressElementActivity> {
            injectActivity {
                viewModelFactory = viewModelFactoryFor(viewModel)
            }
        }.apply {
            launchForResult(
                Intent(application, AddressElementActivity::class.java).putExtra(
                    AddressElementActivityContract.EXTRA_ARGS,
                    args,
                )
            )
        }

        return TestScenario(scenario, viewModel.stateHolder)
    }

    private fun standaloneArgs() = AddressElementActivityContract.Args.Standalone(
        publishableKey = "pk_test_123",
        config = null,
    )

    private fun checkoutArgs(response: CheckoutSessionResponse) =
        AddressElementActivityContract.Args.CheckoutShipping.Ready(
            publishableKey = "pk_test_123",
            config = null,
            checkoutSessionResponse = response,
        )

    private fun checkoutResult(response: CheckoutSessionResponse) =
        AddressElementActivityContract.Result.CheckoutShippingSucceeded(
            address = AddressDetails(),
            updatedResponse = response,
        )

    private class TestScenario(
        val scenario: InjectableActivityScenario<AddressElementActivity>,
        val stateHolder: AddressElementActivityStateHolder,
    ) : AutoCloseable {
        override fun close() {
            scenario.close()
        }
    }

    private fun checkoutIntent(args: AddressElementActivityContract.Args.CheckoutShipping): Intent {
        return AddressElementActivityContract.CheckoutShipping.createIntent(
            ApplicationProvider.getApplicationContext(),
            args,
        )
    }
}
