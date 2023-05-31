package com.stripe.android.customersheet

import android.content.Context
import android.os.Build
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
internal class CustomerSheetActivityTest {
    @get:Rule
    val rule = InstantTaskExecutorRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val contract = CustomerSheetContract()

    @Test
    fun `Finish with cancel on back press`() {
        val intent = contract.createIntent(
            context = context,
            input = CustomerSheetContract.Args
        )

        val scenario = ActivityScenario.launchActivityForResult<CustomerSheetActivity>(intent)

        scenario.onActivity {
            pressBack()
        }

        assertThat(
            contract.parseResult(
                scenario.result.resultCode,
                scenario.result.resultData
            )
        ).isInstanceOf(
            InternalCustomerSheetResult.Canceled::class.java
        )
    }
}
