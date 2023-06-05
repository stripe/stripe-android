package com.stripe.android.customersheet

import android.content.Context
import android.os.Build
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.stripe.android.utils.InjectableActivityScenario
import com.stripe.android.utils.TestUtils.viewModelFactoryFor
import com.stripe.android.utils.injectableActivityScenario
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
internal class CustomerSheetActivityTest {
    @get:Rule
    val rule = InstantTaskExecutorRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val contract = CustomerSheetContract()
    private val intent = contract.createIntent(
        context = context,
        input = CustomerSheetContract.Args
    )

    @Test
    fun `Finish with cancel on back press`() {
        val scenario = activityScenario().launchForResult(intent)

        scenario.onActivity {
            pressBack()
        }

        assertThat(
            contract.parseResult(
                scenario.getResult().resultCode,
                scenario.getResult().resultData
            )
        ).isEqualTo(
            InternalCustomerSheetResult.Canceled
        )
    }

    private fun activityScenario(): InjectableActivityScenario<CustomerSheetActivity> {
        val viewModel = mock<CustomerSheetViewModel>()

        whenever(viewModel.viewState).thenReturn(
            MutableStateFlow(CustomerSheetViewState.Loading)
        )

        return injectableActivityScenario {
            injectActivity {
                viewModelFactory = viewModelFactoryFor(viewModel)
            }
        }
    }
}
