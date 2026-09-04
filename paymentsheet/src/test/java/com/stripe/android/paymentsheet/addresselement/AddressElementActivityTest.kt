package com.stripe.android.paymentsheet.addresselement

import android.os.Bundle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.utils.injectableActivityScenario
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class AddressElementActivityTest {
    @Test
    fun `success from result state holder finishes with the existing activity result`() {
        val expectedResult = AddressLauncherResult.Succeeded(AddressDetails())

        runScenario { resultStateHolder, scenario ->
            resultStateHolder.setResult(expectedResult)

            assertThat(
                AddressElementActivityContract.parseResult(
                    scenario.result.resultCode,
                    scenario.result.resultData,
                )
            ).isEqualTo(expectedResult)
            assertThat(scenario.state).isEqualTo(Lifecycle.State.DESTROYED)
        }
    }

    @Test
    fun `cancellation from result state holder finishes with the existing activity result`() {
        val expectedResult = AddressLauncherResult.Canceled()

        runScenario { resultStateHolder, scenario ->
            resultStateHolder.setResult(expectedResult)

            assertThat(
                AddressElementActivityContract.parseResult(
                    scenario.result.resultCode,
                    scenario.result.resultData,
                )
            ).isEqualTo(expectedResult)
            assertThat(scenario.state).isEqualTo(Lifecycle.State.DESTROYED)
        }
    }

    @Test
    fun `when launched without args should finish with canceled result`() {
        ActivityScenario.launchActivityForResult(
            AddressElementActivity::class.java,
            Bundle.EMPTY
        ).use { activityScenario ->
            assertThat(activityScenario.state).isEqualTo(Lifecycle.State.DESTROYED)
            val result = AddressElementActivityContract.parseResult(0, activityScenario.result.resultData)
            assertThat(result).isEqualTo(AddressLauncherResult.Canceled())
        }
    }

    private fun runScenario(
        test: (AddressElementResultStateHolder, ActivityScenario<AddressElementActivity>) -> Unit,
    ) {
        val args = AddressElementActivityContract.Args(
            publishableKey = "pk_123",
            config = AddressLauncher.Configuration(),
        )
        lateinit var resultStateHolder: AddressElementResultStateHolder

        injectableActivityScenario<AddressElementActivity> {
            injectActivity {
                viewModelFactory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return AddressElementViewModel.Factory(
                            applicationSupplier = { application },
                            starterArgsSupplier = { args },
                        ).create(modelClass).also { viewModel ->
                            resultStateHolder = (viewModel as AddressElementViewModel).resultStateHolder
                        }
                    }
                }
            }
        }.use { scenario ->
            scenario.launchForResult(
                AddressElementActivityContract.createIntent(
                    ApplicationProvider.getApplicationContext(),
                    args,
                )
            )

            test(resultStateHolder, scenario)
        }
    }
}
